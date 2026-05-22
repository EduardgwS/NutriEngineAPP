package com.explosionlab.nutriengine.features.market

import android.util.Log
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.network.ParceiroDto
import com.explosionlab.nutriengine.core.network.RecomendacaoProdutoDto
import com.explosionlab.nutriengine.core.network.RecomendacoesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val precoAntigo: Double?,
    val quantidadeG: Double,
    val motivo:      String,
    val urlCompra:   String,
    val categoria:   String,
    val kcal:         Double,
    val proteinas:    Double,
    val carboidratos: Double,
    val gorduras:     Double,
)

class MercadoRepository(private val authRepository: AuthRepository) {

    private val api = NetworkModule.api

    companion object {
        private const val TAG = "MercadoRepository"
    }

    //Recomendações

    suspend fun buscarRecomendacoes(
        necessidades: List<String>
    ): List<RecomendacaoProduto> = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken() ?: return@withContext emptyList()
        try {
            val response = api.getRecomendacoes(RecomendacoesRequest(necessidades), "Bearer $token")
            response.recomendacoes.map { it.toRecomendacao() }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar recomendações: ${e.message}")
            emptyList()
        }
    }

    private fun RecomendacaoProdutoDto.toRecomendacao() = RecomendacaoProduto(
        produtoId   = id,
        nome        = nome,
        marca       = marca ?: "",
        imagemUrl   = imagemUrl ?: "",
        nomeMercado = nomeMercado ?: "",
        logoMercado = logoMercado ?: "",
        precoAtual  = precoAtual,
        precoAntigo = precoAntigo.takeIf { it != null && it > 0 },
        quantidadeG = quantidadeG ?: 100.0,
        motivo      = motivo ?: "",
        urlCompra   = urlCompra,
        categoria   = categoria ?: "",
        kcal         = kcal ?: 0.0,
        proteinas    = proteinas ?: 0.0,
        carboidratos = carboidratos ?: 0.0,
        gorduras     = gorduras ?: 0.0,
    )

    //Parceiros

    suspend fun listarParceiros(): List<Parceiro> = withContext(Dispatchers.IO) {
        val token = authRepository.carregarToken() ?: return@withContext emptyList()
        try {
            val response = api.getParceiros("Bearer $token")
            response.parceiros.map { it.toParceiro() }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao listar parceiros: ${e.message}")
            emptyList()
        }
    }

    private fun ParceiroDto.toParceiro() = Parceiro(
        id      = id,
        nome    = nome,
        logoUrl = logoUrl ?: "",
        siteUrl = siteUrl ?: "",
    )
}
