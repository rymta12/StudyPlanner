package com.studyplanner.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// WEB_CLIENT_ID: Firebase Console → Authentication → Sign-in method → Google → Web client ID
private const val WEB_CLIENT_ID = "316460812061-blt72bvem22t2hb95htqspolfqhrr613.apps.googleusercontent.com"

suspend fun getGoogleIdToken(context: Context): Result<String> {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(WEB_CLIENT_ID)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Result.success(googleCredential.idToken)
        } else {
            Result.failure(Exception("Invalid credential type"))
        }
    } catch (e: GetCredentialException) {
        Result.failure(e)
    }
}
