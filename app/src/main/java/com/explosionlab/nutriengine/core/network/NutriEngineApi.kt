package com.explosionlab.nutriengine.core.network

import com.explosionlab.nutriengine.core.model.DicaMacroResponse
import com.explosionlab.nutriengine.core.model.ReceitaResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface NutriEngineApi {

    //Authenticação

    @POST("auth/google/android")
    suspend fun loginGoogle(
        @Body request: LoginRequest
    ): LoginResponse

    //Nutriçãp

    @GET("api/receita-do-dia")
    suspend fun getReceitaDoDia(
        @Query("objetivo") objetivo: String
    ): ReceitaResponse

    @GET("api/dicas-macrocard")
    suspend fun getDicaMacro(
        @Query("maior_deficit") maiorDeficit: Int,
        @Query("proteina_consumida") proteinaConsumida: Double
    ): DicaMacroResponse

    @GET("api/search")
    suspend fun pesquisarAlimento(
        @Query("q") query: String,
        @Header("Authorization") token: String
    ): List<AlimentoDto>

    @Multipart
    @POST("api/identificar-alimento")
    suspend fun identificarAlimento(
        @Part image: MultipartBody.Part,
        @Header("Authorization") token: String
    ): IdentificacaoResponse

    //Mercado

    @POST("mercado/recomendacoes")
    suspend fun getRecomendacoes(
        @Body request: RecomendacoesRequest,
        @Header("Authorization") token: String
    ): RecomendacoesResponse

    @GET("mercado/parceiros")
    suspend fun getParceiros(
        @Header("Authorization") token: String
    ): ParceirosResponse


    //Megumi
    @POST("megumi/chat")
    suspend fun enviarMensagemChat(
        @Body request: ChatRequest,
        @Header("Authorization") token: String
    ): ChatResponse

    @POST("megumi/insight")
    suspend fun pedirInsight(
        @Body request: InsightRequest,
        @Header("Authorization") token: String
    ): InsightResponse

    @GET("megumi/historico")
    suspend fun getHistoricoChat(
        @Query("limite") limite: Int,
        @Header("Authorization") token: String
    ): HistoricoResponse
}
