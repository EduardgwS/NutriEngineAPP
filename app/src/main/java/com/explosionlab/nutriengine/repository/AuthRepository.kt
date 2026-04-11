package com.explosionlab.nutriengine.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.explosionlab.nutriengine.NetworkModule
import com.explosionlab.nutriengine.model.ResultadoLogin
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepository(private val context: Context) {

    private val TAG           = "AuthRepository"
    private val PREFS_NAME    = "nutriengine_prefs"
    private val KEY_JWT       = "jwt_token"
    private val KEY_NAME      = "user_name"
    private val WEB_CLIENT_ID = context.getString(
        context.resources.getIdentifier("web_client_id", "string", context.packageName)
    )

    private val httpClient = NetworkModule.httpClient

    companion object {
        const val BACKEND_URL = "https://nutriengine.explosionlab.com"
    }

    // ── Token (SharedPreferences) ──────────────────────────────────────────────

    fun salvarToken(token: String, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_JWT, token)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun carregarToken(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JWT, null)

    fun carregarNome(): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, "") ?: ""

    fun limparToken() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    fun estaLogado(): Boolean = carregarToken() != null

    // ── Login Google ───────────────────────────────────────────────────────────

    /**
     * Executa o fluxo completo de login:
     *  1. Abre o seletor de conta Google via Credential Manager
     *  2. Obtém o ID Token
     *  3. Envia ao backend → recebe JWT próprio
     *  4. Salva o JWT localmente
     *
     * Recebe a Activity como parâmetro porque o Credential Manager
     * precisa de um contexto de Activity para exibir o seletor de contas.
     */
    suspend fun fazerLoginGoogle(activity: android.app.Activity): ResultadoLogin {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
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

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager: ${e.message}")
            ResultadoLogin(false, mensagem = "Não foi possível fazer login. Tente novamente.")
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
                    .url("$BACKEND_URL/auth/google/android")
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val respBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val json = JSONObject(respBody)
                        val jwt  = json.getString("token")
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
                ResultadoLogin(false, mensagem = "O servidor está DESLIGADO\nPeça para o Eduard ligar se quiser testar o app\n\n\nSim, servidor custa caro")
            }
        }
}
