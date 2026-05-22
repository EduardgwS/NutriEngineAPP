package com.explosionlab.nutriengine.features.megumi

import android.util.Log
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.model.Mensagem
import com.explosionlab.nutriengine.core.network.ChatRequest
import com.explosionlab.nutriengine.core.network.InsightRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ChatRepository(private val authRepository: AuthRepository) {

    companion object{
        private const val TAG = "ChatRepository"
    }
    private val api = NetworkModule.api

    //Envio de mensagens

    suspend fun enviarMensagem(
        texto:              String,
        historicoSaudeJson: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken()
            ?: return@withContext "Sessão expirada. Faça login novamente."

        try {
            val historicoObj = historicoSaudeJson?.let {
                Gson().fromJson(it, JsonObject::class.java)
            }

            val request = ChatRequest(
                text = texto,
                userName = authRepository.carregarNome(),
                historicoSaude = historicoObj
            )

            val response = api.enviarMensagemChat(request, "Bearer $token")
            response.response

        } catch (e: HttpException) {
            when (e.code()) {
                400  -> "Digite uma mensagem antes de enviar."
                401  -> "Sessão expirada. Faça login novamente."
                530  -> "Desculpe, não consegui me conectar com o servidor."
                else -> "Erro ${e.code()}: tente novamente."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar mensagem: ${e.message}")
            "Erro de conexão: ${e.message}"
        }
    }

    suspend fun pedirInsight(
        historicoSaudeJson: String?
    ): String = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken()
            ?: return@withContext "Sessão expirada. Faça login novamente."

        try {
            val historicoObj = historicoSaudeJson?.let {
                Gson().fromJson(it, JsonObject::class.java)
            }

            val response = api.pedirInsight(InsightRequest(historicoObj), "Bearer $token")
            response.insight

        } catch (e: HttpException) {
            when (e.code()) {
                401  -> "Sessão expirada. Faça login novamente."
                530  -> "Desculpe, não consegui me conectar com o servidor."
                else -> "Erro ${e.code()}: tente novamente."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao pedir insight: ${e.message}")
            "Erro de conexão: ${e.message}"
        }
    }

    //Histórico do banco
    suspend fun carregarHistorico(limite: Int = 50): List<Mensagem> =
        withContext(Dispatchers.IO) {
            val token = authRepository.carregarToken()
                ?: return@withContext emptyList()

            try {
                val response = api.getHistoricoChat(limite, "Bearer $token")
                response.mensagens.map {
                    Mensagem(
                        texto = it.mensagem,
                        ehUsuario = it.papel == "user"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar histórico: ${e.message}")
                emptyList()
            }
        }

}
