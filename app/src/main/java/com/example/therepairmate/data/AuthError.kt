package com.example.therepairmate.data


import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException

/** Turns Firebase and timeout errors into messages a customer can act on. */
fun friendlyError(e: Throwable): String = when (e) {
    is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "Incorrect email or password"
    is FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in."
    is FirebaseAuthWeakPasswordException -> "Choose a stronger password (6+ characters)"
    is FirebaseTooManyRequestsException -> "Too many attempts. Wait a moment and try again."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is TimeoutCancellationException -> "This is taking too long. Check your connection, and that Firestore is created in Firebase."
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access denied by Firestore rules. Publish the rules in the Firebase console."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Can't reach the server. Check your connection."
        else -> e.message ?: "Database error"
    }
    is FirebaseAuthException -> when (e.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method isn't enabled in Firebase (Authentication → Sign-in method)."
        else -> e.message ?: "Sign-in failed"
    }
    else -> e.message ?: "Something went wrong"
}