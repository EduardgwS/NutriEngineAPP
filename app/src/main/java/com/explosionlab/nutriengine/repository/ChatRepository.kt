package com.explosionlab.nutriengine.repository

import android.util.Log
import com.explosionlab.nutriengine.NetworkModule
import com.explosionlab.nutriengine.model.Mensagem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ChatRepository(private val authRepository: AuthRepository) {

    private val TAG        = "ChatRepository"
    private val httpClient = NetworkModule.httpClient
    private val backendUrl = AuthRepository.BACKEND_URL

    // ── Enviar mensagem ────────────────────────────────────────────────────────

    suspend fun enviarMensagem(
        texto:              String,
        imagemBytes:        ByteArray? = null,
        historicoSaudeJson: String?    = null,
    ): String = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken()
            ?: return@withContext "Sessão expirada. Faça login novamente."

        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("text", texto)
                .apply {
                    imagemBytes?.let {
                        addFormDataPart(
                            "image", "foto.jpg",
                            it.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, it.size)
                        )
                    }
                    historicoSaudeJson?.let {
                        addFormDataPart("historico_saude", it)
                    }
                }
                .build()

            val request = Request.Builder()
                .url("$backendUrl/megumi/chat")
                .addHeader("Authorization", "Bearer $token")
                .post(multipart)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> JSONObject(body).optString("response", "Sem resposta.")
                    response.code == 400  -> "Digite uma mensagem antes de enviar."
                    response.code == 401  -> "Sessão expirada. Faça login novamente."
                    else                  -> "Erro ${response.code}: tente novamente."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar mensagem: ${e.message}")
            "Erro de conexão: ${e.message}"
        }
    }

    // ── Carregar histórico do banco ────────────────────────────────────────────

    /**
     * Busca as últimas mensagens salvas no banco via GET /megumi/historico.
     * Retorna lista de Mensagem em ordem cronológica (mais antiga primeiro).
     */
    suspend fun carregarHistorico(limite: Int = 50): List<Mensagem> =
        withContext(Dispatchers.IO) {
            val token = authRepository.carregarToken()
                ?: return@withContext emptyList()

            try {
                val request = Request.Builder()
                    .url("$backendUrl/megumi/historico?limite=$limite")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()

                    val body  = response.body?.string() ?: return@withContext emptyList()
                    val json  = JSONObject(body)
                    val array = json.optJSONArray("mensagens") ?: return@withContext emptyList()

                    val lista = mutableListOf<Mensagem>()
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        lista.add(
                            Mensagem(
                                texto     = item.getString("mensagem"),
                                ehUsuario = item.getString("papel") == "user"
                            )
                        )
                    }
                    lista
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar histórico: ${e.message}")
                emptyList()
            }
        }

}