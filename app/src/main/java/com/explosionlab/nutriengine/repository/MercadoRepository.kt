package com.explosionlab.nutriengine.repository

import android.util.Log
import com.explosionlab.nutriengine.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// ── Modelos ───────────────────────────────────────────────────────────────────

data class Parceiro(
    val id:      String,
    val nome:    String,
    val logoUrl: String,
    val siteUrl: String,
)

data class RecomendacaoProduto(
    val produtoId:   String,
    val nome:        String,
    val marca:       String,
    val imagemUrl:   String,
    val nomeMercado: String,
    val logoMercado: String,
    val precoAtual:  Double,
    val precoAntigo: Double?,   // null = sem desconto
    val quantidadeG: Double,
    val motivo:      String,    // "Faltam 115g de proteína hoje"
    val urlCompra:   String,
    val categoria:   String,    // "Proteína", "Carboidrato", "Gordura"
    val kcal:         Double,
    val proteinas:    Double,
    val carboidratos: Double,
    val gorduras:     Double,
)

// ── Repositório ───────────────────────────────────────────────────────────────

class MercadoRepository(private val authRepository: AuthRepository) {

    private val TAG        = "MercadoRepository"
    private val httpClient = NetworkModule.httpClient
    private val backendUrl = AuthRepository.BACKEND_URL

    // ── Recomendações ─────────────────────────────────────────────────────────

    suspend fun buscarRecomendacoes(
        perfilJson:  JSONObject,
        consumoHoje: JSONObject,
        gapJson:     JSONObject,
    ): List<RecomendacaoProduto> = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken() ?: return@withContext emptyList()
        try {
            val payload = JSONObject().apply {
                put("perfil",       perfilJson)
                put("consumo_hoje", consumoHoje)
                put("gap",          gapJson)
            }.toString()

            val request = Request.Builder()
                .url("$backendUrl/mercado/recomendacoes")
                .addHeader("Authorization", "Bearer $token")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body  = response.body?.string() ?: return@withContext emptyList()
                val array = JSONObject(body).optJSONArray("recomendacoes") ?: JSONArray(body)
                parseRecomendacoes(array)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar recomendações: ${e.message}")
            emptyList()
        }
    }

    private fun parseRecomendacoes(array: JSONArray): List<RecomendacaoProduto> =
        (0 until array.length()).mapNotNull { i ->
            runCatching {
                val obj = array.getJSONObject(i)
                RecomendacaoProduto(
                    produtoId   = obj.getString("id"),
                    nome        = obj.getString("nome"),
                    marca       = obj.optString("marca", ""),
                    imagemUrl   = obj.optString("imagem_url", ""),
                    nomeMercado = obj.optString("nome_mercado", ""),
                    logoMercado = obj.optString("logo_mercado", ""),
                    precoAtual  = obj.getDouble("preco_atual"),
                    precoAntigo = obj.optDouble("preco_antigo", -1.0).takeIf { it > 0 },
                    quantidadeG = obj.optDouble("quantidade_g", 100.0),
                    motivo      = obj.optString("motivo", ""),
                    urlCompra   = obj.getString("url_compra"),
                    categoria   = obj.optString("categoria", ""),
                    kcal         = obj.optDouble("kcal",         0.0),
                    proteinas    = obj.optDouble("proteinas",    0.0),
                    carboidratos = obj.optDouble("carboidratos", 0.0),
                    gorduras     = obj.optDouble("gorduras",     0.0),
                )
            }.getOrNull()
        }

    // ── Parceiros ─────────────────────────────────────────────────────────────

    suspend fun listarParceiros(): List<Parceiro> = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken() ?: return@withContext emptyList()
        try {
            val request = Request.Builder()
                .url("$backendUrl/mercado/parceiros")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body  = response.body?.string() ?: return@withContext emptyList()
                val array = JSONObject(body).optJSONArray("parceiros") ?: JSONArray(body)
                (0 until array.length()).mapNotNull { i ->
                    runCatching {
                        val obj = array.getJSONObject(i)
                        Parceiro(
                            id      = obj.getString("id"),
                            nome    = obj.getString("nome"),
                            logoUrl = obj.optString("logo_url", ""),
                            siteUrl = obj.optString("site_url", ""),
                        )
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao listar parceiros: ${e.message}")
            emptyList()
        }
    }
}