package com.example.therepairmate.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.therepairmate.R
import com.example.therepairmate.data.Repo
import com.example.therepairmate.data.friendlyError
import com.example.therepairmate.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private var signup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        if (FirebaseApp.getApps(this).isEmpty()) {
            fail("Firebase is not configured. Add google-services.json (package com.example.therepairmate) to the app/ folder and rebuild.")
            b.btnSubmit.isEnabled = false
            b.btnGoogle.isEnabled = false
            return
        }

        render()
        b.tvToggle.setOnClickListener { signup = !signup; render() }
        b.btnSubmit.setOnClickListener { submit() }
        b.btnGoogle.setOnClickListener { googleSignIn() }
        b.tvForgot.setOnClickListener { forgot() }

        // Already signed in: go straight to the right dashboard, with visible feedback.
        if (Repo.uid != null) lifecycleScope.launch { route("Welcome back…") }
    }

    private fun render() {
        b.tilName.visibility = if (signup) View.VISIBLE else View.GONE
        b.btnSubmit.text = if (signup) "Create account" else "Sign in"
        b.tvToggle.text = if (signup) "Already have an account? Sign in" else "New here? Create an account"
        b.tvForgot.visibility = if (signup) View.GONE else View.VISIBLE
        b.tvError.text = ""
        b.tvError.shake()
    }

    private fun submit() {
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString()
        when {
            signup && name.isEmpty() -> return fail("Enter your name")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> return fail("Enter a valid email")
            pass.length < 6 -> return fail("Password must be at least 6 characters")
        }
        lifecycleScope.launch {
            busy(true, if (signup) "Creating your account…" else "Signing you in…")
            try {
                withTimeout(25_000) { if (signup) Repo.signUp(name, email, pass) else Repo.signIn(email, pass) }
                toast(if (signup) "Account created. Welcome!" else "Signed in successfully")
                route("Loading your workspace…")
            } catch (e: Exception) {
                busy(false)
                fail(friendlyError(e))
            }
        }
    }

    private fun googleSignIn() {
        lifecycleScope.launch {
            busy(true, "Opening Google…")
            try {
                // default_web_client_id is generated from google-services.json once Google sign-in is enabled in Firebase.
                val option = GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id)).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val cred = CredentialManager.create(this@LoginActivity).getCredential(this@LoginActivity, request).credential
                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    busy(true, "Signing you in with Google…")
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    withTimeout(25_000) { Repo.signInWithGoogle(token) }
                    toast("Signed in with Google")
                    route("Loading your workspace…")
                } else {
                    busy(false)
                    fail("Unsupported sign-in credential")
                }
            } catch (e: NoCredentialException) {
                busy(false)
                fail("No Google account found on this device. Add one in Settings, then try again.")
            } catch (e: GetCredentialCancellationException) {
                busy(false)
            } catch (e: GetCredentialException) {
                busy(false)
                fail("Google sign-in failed: ${e.message}")
            } catch (e: Exception) {
                busy(false)
                fail(friendlyError(e))
            }
        }
    }

    /** Admins go to the admin console, everyone else to the customer dashboard. */
    private suspend fun route(message: String) {
        busy(true, message)
        val role = try {
            withTimeout(10_000) { Repo.ensureProfile(); Repo.profile()?.second }
        } catch (e: Exception) { null }
        val target = if (role == "admin") AdminDashboardActivity::class.java else DashboardActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }

    private fun forgot() {
        val email = b.etEmail.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return fail("Enter your email above first")
        lifecycleScope.launch {
            try {
                withTimeout(15_000) { Repo.resetPassword(email) }
                b.tvError.text = ""
                busyText("Reset link sent. Check your inbox.")
            } catch (e: Exception) { fail(friendlyError(e)) }
        }
    }

    private fun busyText(msg: String) {
        b.tvStatus.text = msg
        b.tvStatus.visibility = View.VISIBLE
    }

    private fun busy(on: Boolean, msg: String = "") {
        b.progress.visibility = if (on) View.VISIBLE else View.GONE
        b.btnSubmit.isEnabled = !on
        b.btnGoogle.isEnabled = !on
        b.tvStatus.visibility = if (on) View.VISIBLE else View.GONE
        b.tvStatus.text = msg
        if (on) b.tvError.text = ""
    }

    private fun fail(msg: String) {
        b.tvStatus.visibility = View.GONE
        b.tvError.setTextColor(ContextCompat.getColor(this, R.color.qf_danger))
        b.tvError.text = msg
    }
}