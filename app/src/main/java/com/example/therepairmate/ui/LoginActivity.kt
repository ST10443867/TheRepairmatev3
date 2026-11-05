package com.example.therepairmate.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
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

class LoginActivity : BaseActivity() {
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
        val languages = arrayOf("English", "Afrikaans", "Setswana", "isiXhosa", "isiZulu")
        val tags = arrayOf("en", "af", "tn", "xh", "zu")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        b.spinnerLanguage.adapter = adapter

        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val currentIndex = tags.indexOfFirst { currentTag.startsWith(it) }.let { if (it < 0) 0 else it }
        b.spinnerLanguage.setSelection(currentIndex, false)

        b.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lang = tags[position.coerceIn(tags.indices)]
                LanguagePrefs.set(this@LoginActivity, lang)
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (!currentLocales.toLanguageTags().contains(lang)) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
                    if (Repo.uid != null) {
                        lifecycleScope.launch {
                            try { Repo.updateLanguage(lang) } catch (e: Exception) {}
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

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

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.contains("@") &&
                email.contains(".") &&
                !email.endsWith(".") &&
                !email.startsWith("@")
    }

    private fun submit() {
        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val pass = b.etPassword.text.toString()
        val tags = arrayOf("en", "af", "tn", "xh", "zu")
        val lang = tags[b.spinnerLanguage.selectedItemPosition.coerceIn(tags.indices)]
        when {
            signup && name.isEmpty() -> return fail("Enter your name")
            !isValidEmail(email) -> return fail("Enter a valid email address (e.g., user@example.com)")
            pass.length < 6 -> return fail("Password must be at least 6 characters")
            signup && !pass.any { it.isUpperCase() } -> return fail("Password must contain at least one uppercase letter (caps)")
            signup && !pass.any { it.isLowerCase() } -> return fail("Password must contain at least one lowercase letter")
            signup && !pass.any { !it.isLetterOrDigit() } -> return fail("Password must contain at least one symbol (e.g. !@#$%)")
        }
        lifecycleScope.launch {
            busy(true, if (signup) "Creating your account…" else "Signing you in…")
            try {
                withTimeout(25_000) {
                    if (signup) {
                        LanguagePrefs.set(this@LoginActivity, lang)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
                        Repo.signUp(name, email, pass, lang)
                    } else {
                        Repo.signIn(email, pass)
                        val userLang = Repo.getUserLanguage()
                        LanguagePrefs.set(this@LoginActivity, userLang)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(userLang))
                    }
                }
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
                    withTimeout(25_000) {
                        Repo.signInWithGoogle(token, "en")
                        val userLang = Repo.getUserLanguage()
                        LanguagePrefs.set(this@LoginActivity, userLang)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(userLang))
                    }
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
        val (role, lang) = try {
            withTimeout(10_000) {
                Repo.ensureProfile()
                val r = Repo.profile()?.second
                val l = Repo.getUserLanguage()
                r to l
            }
        } catch (e: Exception) { null to "en" }
        if (lang != null) {
            LanguagePrefs.set(this, lang)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
        val target = if (role == "admin") AdminDashboardActivity::class.java else DashboardActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }

    private fun forgot() {
        val email = b.etEmail.text.toString().trim()
        if (!isValidEmail(email)) return fail("Enter a valid email above first")
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
        b.tvError.shake()
        toast(msg)
    }
}