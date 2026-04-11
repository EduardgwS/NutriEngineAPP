package com.explosionlab.nutriengine.repository

import android.util.Log
import com.explosionlab.nutriengine.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class Alimento(
    val id:           String,
    val descricao:    String,
    val categoria:    String,
    val kcal:         Double,
    val proteinas:    Double,
    val carboidratos: Double,
    val gorduras:     Double,
)

class AlimentoRepository(private val authRepository: AuthRepository) {

    private val TAG        = "AlimentoRepository"
    private val httpClient = NetworkModule.httpClient
    private val backendUrl = AuthRepository.BACKEND_URL

    suspend fun pesquisar(query: String): List<Alimento> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val url     = "$backendUrl/api/search?q=${query.trim().lowercase()}"
            val token   = authRepository.carregarToken() ?: return@withContext emptyList()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body  = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)
                val lista = mutableListOf<Alimento>()

                for (i in 0 until array.length()) {
                    val item   = array.getJSONObject(i)
                    val macros = item.getJSONObject("macros")
                    lista.add(
                        Alimento(
                            id           = item.getString("id"),
                            descricao    = item.getString("description"),
                            categoria    = item.optString("category", ""),
                            kcal         = macros.optDouble("kcal",    0.0),
                            proteinas    = macros.optDouble("protein", 0.0),
                            carboidratos = macros.optDouble("carbs",   0.0),
                            gorduras     = macros.optDouble("fat",     0.0),
                        )
                    )
                }
                lista
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na pesquisa: ${e.message}")
            emptyList()
        }
    }

    /**
     * Envia uma imagem para o endpoint /api/identificar-alimento e retorna
     * o nome normalizado do alimento identificado pela IA, ou null se não
     * encontrado / erro.
     */
    suspend fun identificarPorImagem(imagemBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val token = authRepository.carregarToken() ?: return@withContext null

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name     = "image",
                    filename = "foto.jpg",
                    body     = imagemBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$backendUrl/api/identificar-alimento")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.getString("status") != "success") return@withContext null
                json.optString("alimento").takeIf { it.isNotBlank() && it != "null" }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao identificar alimento por imagem: ${e.message}")
            null
        }
    }
}
