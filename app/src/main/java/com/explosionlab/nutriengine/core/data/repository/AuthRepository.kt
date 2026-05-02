package com.explosionlab.nutriengine.core.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.explosionlab.nutriengine.R
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.model.ResultadoLogin
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepository(private val context: Context) {

    private val webClientId = context.getString(R.string.web_client_id)
    private val httpClient = NetworkModule.httpClient

    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "nutriengine_prefs"
        private const val KEY_JWT = "jwt_token"
        private const val KEY_NAME = "user_name"
    }

    //Tokens

    fun salvarToken(token: String, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_JWT, token)
                .putString(KEY_NAME, name)
        }
    }

    fun carregarToken(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JWT, null)

    fun carregarNome(): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "")!! // ou simplesmente confiar no default

    fun limparToken() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { clear() }
    }

    fun estaLogado(): Boolean = carregarToken() != null

    //Login Google

    suspend fun fazerLoginGoogle(activity: Activity): ResultadoLogin {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = CredentialManager.create(activity)
                .getCredential(context = activity, request = request)

            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) return ResultadoLogin(false, mensagem = "Tipo de credencial inesperado.")

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            enviarTokenAoBackend(idToken)

        } catch (e: NoCredentialException) {
            Log.e(TAG, "Usuário sem conta: ${e.message}")
            ResultadoLogin(false, mensagem = "Erro ao localizar conta, tente novamente.")

        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado: ${e.message}")
            ResultadoLogin(false, mensagem = "Erro: ${e.message}")
        }
    }

    private suspend fun enviarTokenAoBackend(idToken: String): ResultadoLogin =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply { put("id_token", idToken) }
                    .toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${NetworkModule.BACKEND_URL}/auth/google/android")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val respBody = response.body.string()
                    if (response.isSuccessful) {
                        val json = JSONObject(respBody)
                        val jwt = json.getString("token")
                        val name = json.optString("name", "Usuário")
                        salvarToken(jwt, name)
                        ResultadoLogin(sucesso = true, nome = name)
                    } else {
                        val msg = JSONObject(respBody).optString("error", "Erro desconhecido")
                        ResultadoLogin(false, mensagem = msg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro de conexão: ${e.message}")
                ResultadoLogin(
                    false,
                    mensagem = "Erro ao acessar o servidor do NutriEngine\nVerifique sua conexão e tente novamente."
                )
            }
        }
}