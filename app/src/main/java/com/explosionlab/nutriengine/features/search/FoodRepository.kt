package com.explosionlab.nutriengine.features.search

import android.util.Log
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.model.Alimento
import com.explosionlab.nutriengine.core.model.IdentificacaoResult
import com.explosionlab.nutriengine.core.network.AlimentoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class FoodRepository(private val authRepository: AuthRepository) {

    companion object {
        private const val TAG = "FoodRepository"
    }
    private val api = NetworkModule.api

    suspend fun pesquisar(query: String): List<Alimento> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val token = authRepository.carregarToken() ?: return@withContext emptyList()
            val response = api.pesquisarAlimento(query.trim().lowercase(), "Bearer $token")
            
            response.map { it.toAlimento() }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na pesquisa: ${e.message}")
            emptyList()
        }
    }


    suspend fun identificarPorImagem(imagemBytes: ByteArray): IdentificacaoResult? = withContext(Dispatchers.IO) {
        try {
            val token = authRepository.carregarToken() ?: return@withContext null

            val part = MultipartBody.Part.createFormData(
                name     = "image",
                filename = "foto.jpg",
                body     = imagemBytes.toRequestBody("image/jpeg".toMediaType())
            )

            val response = api.identificarAlimento(part, "Bearer $token")
            
            if (response.status != "success") return@withContext null

            val nome = response.alimento?.takeIf { it.isNotBlank() && it != "null" }
                ?: return@withContext null

            IdentificacaoResult(nome, response.gramas)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao identificar alimento por imagem: ${e.message}")
            null
        }
    }

    private fun AlimentoDto.toAlimento() = Alimento(
        id           = id,
        descricao    = description,
        categoria    = category ?: "",
        kcal         = macros.kcal,
        proteinas    = macros.protein,
        carboidratos = macros.carbs,
        gorduras     = macros.fat
    )
}
