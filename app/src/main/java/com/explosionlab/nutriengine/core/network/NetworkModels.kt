package com.explosionlab.nutriengine.core.network

import com.google.gson.annotations.SerializedName

//Authenticação

data class LoginRequest(
    @SerializedName("id_token")
    val idToken: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("name")
    val name: String? = null
)

//Pesquisar

data class AlimentoDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("macros")
    val macros: MacrosDto
)

data class MacrosDto(
    @SerializedName("kcal")
    val kcal: Double,
    @SerializedName("protein")
    val protein: Double,
    @SerializedName("carbs")
    val carbs: Double,
    @SerializedName("fat")
    val fat: Double
)

data class IdentificacaoResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("alimento")
    val alimento: String? = null,
    @SerializedName("gramas")
    val gramas: Double? = null
)

//Mercado

data class RecomendacoesRequest(
    @SerializedName("necessidades")
    val necessidades: List<String>
)

data class RecomendacaoProdutoDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("marca")
    val marca: String? = null,
    @SerializedName("imagem_url")
    val imagemUrl: String? = null,
    @SerializedName("nome_mercado")
    val nomeMercado: String? = null,
    @SerializedName("logo_mercado")
    val logoMercado: String? = null,
    @SerializedName("preco_atual")
    val precoAtual: Double,
    @SerializedName("preco_antigo")
    val precoAntigo: Double? = null,
    @SerializedName("quantidade_g")
    val quantidadeG: Double? = null,
    @SerializedName("motivo")
    val motivo: String? = null,
    @SerializedName("url_compra")
    val urlCompra: String,
    @SerializedName("categoria")
    val categoria: String? = null,
    @SerializedName("kcal")
    val kcal: Double? = null,
    @SerializedName("proteinas")
    val proteinas: Double? = null,
    @SerializedName("carboidratos")
    val carboidratos: Double? = null,
    @SerializedName("gorduras")
    val gorduras: Double? = null
)

data class RecomendacoesResponse(
    @SerializedName("recomendacoes")
    val recomendacoes: List<RecomendacaoProdutoDto>
)

data class ParceiroDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("nome")
    val nome: String,
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    @SerializedName("site_url")
    val siteUrl: String? = null
)

data class ParceirosResponse(
    @SerializedName("parceiros")
    val parceiros: List<ParceiroDto>
)

//Megumi

data class ChatRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("user_name")
    val userName: String? = null,
    @SerializedName("historico_saude")
    val historicoSaude: Any? = null // Pode ser um objeto JSON stringificado ou um Map
)

data class ChatResponse(
    @SerializedName("response")
    val response: String
)

data class InsightRequest(
    @SerializedName("historico_saude")
    val historicoSaude: Any? = null
)

data class InsightResponse(
    @SerializedName("insight")
    val insight: String
)

data class MensagemDto(
    @SerializedName("mensagem")
    val mensagem: String,
    @SerializedName("papel")
    val papel: String
)

data class HistoricoResponse(
    @SerializedName("mensagens")
    val mensagens: List<MensagemDto>
)
