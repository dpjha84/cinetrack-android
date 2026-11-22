package com.example.data.remote.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isGuest = MutableStateFlow(true)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    fun initialize(scope: CoroutineScope) {
        runCatching {
            val auth = Firebase.auth
            _currentUser.value = auth.currentUser
            _isGuest.value = auth.currentUser == null

            if (authListener == null) {
                authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    _currentUser.value = user
                    _isGuest.value = user == null
                }
                auth.addAuthStateListener(authListener!!)
            }
        }.onFailure { error ->
            Log.w(TAG, "FirebaseAuth unavailable at startup; continuing as guest: ${error.message}")
            _currentUser.value = null
            _isGuest.value = true
        }
    }

    fun continueAsGuest() {
        _isGuest.value = true
    }

    fun attemptAutoSignIn(
        context: Context,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            _currentUser.value = auth.currentUser
            _isGuest.value = false
            onSuccess(auth.currentUser!!)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onFailure()
            return
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        _currentUser.value = user
                        _isGuest.value = false
                        onSuccess(user)
                    } else {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto-sign in not available: ${e.message}")
                onFailure()
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing")
            return
        }

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        _currentUser.value = user
                        _isGuest.value = false
                        onSuccess(user)
                    } else {
                        onError("Sign-in succeeded but user session could not be established.")
                    }
                } else {
                    onError("Unexpected credential format received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In dismissed: ${e.message}")
                onCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Google Sign-In failed.")
            }
        }
    }

    fun signOut(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        Firebase.auth.signOut()
        _currentUser.value = null
        _isGuest.value = true

        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }

    fun deleteAccount(
        context: Context,
        scope: CoroutineScope,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = Firebase.auth.currentUser
        if (user == null) {
            onComplete(true, null)
            return
        }
        scope.launch {
            try {
                user.delete().await()
                _currentUser.value = null
                _isGuest.value = true
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                onComplete(true, null)
            } catch (e: Exception) {
                Log.e(TAG, "Account deletion failed: ${e.message}", e)
                onComplete(false, e.localizedMessage)
            }
        }
    }
}
