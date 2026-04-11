package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecomendacaoReceita(
    val titulo:      String,
    val descricao:   String,
    val ingredientes: List<String>,
    val modoPreparo:  List<String>,
)


private val receitasPorObjetivo: Map<Objetivo, List<RecomendacaoReceita>> = mapOf(

    Objetivo.GANHAR_MUSCULOS to listOf(
        RecomendacaoReceita(
            titulo      = "Frango Grelhado com Batata-Doce",
            descricao   = "Refeição com boa quantidade de proteína e carboidrato, adequada para recuperação pós-treino.",
            ingredientes = listOf(
                "200 g de peito de frango",
                "1 batata-doce média (~150 g)",
                "1 colher de sopa de azeite",
                "Sal e pimenta a gosto",
                "1 dente de alho",
                "Salsinha a gosto"
            ),
            modoPreparo = listOf(
                "Tempere o frango com sal, pimenta e alho picado. Deixe descansar por 10 a 15 minutos.",
                "Cozinhe a batata-doce em água até ficar macia (cerca de 20 minutos).",
                "Aqueça uma frigideira com azeite em fogo médio.",
                "Grelhe o frango por cerca de 5 minutos de cada lado, até dourar e cozinhar completamente.",
                "Sirva com a batata-doce e finalize com salsinha."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Omelete de Claras com Aveia",
            descricao   = "Opção simples com proteína e carboidrato leve para o café da manhã ou pré-treino.",
            ingredientes = listOf(
                "4 claras de ovo",
                "2 colheres de sopa de aveia",
                "1/2 banana madura",
                "Canela a gosto",
                "1 colher de chá de óleo ou azeite"
            ),
            modoPreparo = listOf(
                "Amasse a banana até formar um purê.",
                "Misture com as claras, a aveia e a canela.",
                "Aqueça uma frigideira antiaderente com um pouco de óleo.",
                "Despeje a mistura e cozinhe em fogo baixo até firmar.",
                "Vire com cuidado e doure o outro lado.",
                "Sirva em seguida."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Salmão Assado com Arroz Integral",
            descricao   = "Fonte de proteína e gordura, combinada com carboidrato de digestão mais lenta.",
            ingredientes = listOf(
                "180 g de filé de salmão",
                "1/2 xícara de arroz integral cru",
                "Suco de 1/2 limão",
                "1 dente de alho",
                "Sal e azeite a gosto"
            ),
            modoPreparo = listOf(
                "Cozinhe o arroz integral conforme instruções da embalagem.",
                "Pré-aqueça o forno a 200 °C.",
                "Tempere o salmão com limão, alho e sal.",
                "Coloque em uma assadeira e regue com azeite.",
                "Asse por cerca de 15 minutos.",
                "Sirva com o arroz."
            ),
        ),


        RecomendacaoReceita(
            titulo      = "Carne Moída com Arroz e Legumes",
            descricao   = "Refeição completa com proteína, carboidrato e vegetais.",
            ingredientes = listOf(
                "150 g de carne moída",
                "1/2 xícara de arroz branco ou integral",
                "1/2 cenoura picada",
                "1/2 abobrinha picada",
                "1 colher de sopa de óleo",
                "Sal e pimenta a gosto"
            ),
            modoPreparo = listOf(
                "Cozinhe o arroz e reserve.",
                "Aqueça o óleo e refogue a carne moída até dourar.",
                "Adicione os legumes e cozinhe por alguns minutos.",
                "Tempere com sal e pimenta.",
                "Sirva junto com o arroz."
            ),
        ),
    ),

    Objetivo.PERDER_PESO to listOf(
        RecomendacaoReceita(
            titulo      = "Salada de Atum com Folhas",
            descricao   = "Refeição leve com proteína e baixo teor calórico.",
            ingredientes = listOf(
                "1 lata de atum em água",
                "Folhas verdes (alface, rúcula ou espinafre)",
                "1/2 pepino",
                "Tomate a gosto",
                "Suco de limão",
                "1 colher de chá de azeite",
                "Sal a gosto"
            ),
            modoPreparo = listOf(
                "Escorra o atum.",
                "Lave e corte os vegetais.",
                "Misture tudo em uma tigela.",
                "Tempere com limão, azeite e sal.",
                "Sirva imediatamente."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Wrap de Alface com Frango",
            descricao   = "Alternativa com menos carboidrato, usando alface no lugar da massa.",
            ingredientes = listOf(
                "Folhas grandes de alface",
                "150 g de frango desfiado",
                "1/4 de abacate",
                "Pimentão em tiras",
                "Sal e pimenta a gosto"
            ),
            modoPreparo = listOf(
                "Tempere o frango desfiado.",
                "Amasse o abacate levemente.",
                "Coloque os ingredientes sobre a folha de alface.",
                "Enrole formando um wrap.",
                "Sirva."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Sopa de Legumes",
            descricao   = "Opção leve para refeições noturnas.",
            ingredientes = listOf(
                "1 cenoura",
                "1/2 abobrinha",
                "1/2 cebola",
                "1 litro de água ou caldo",
                "Sal a gosto"
            ),
            modoPreparo = listOf(
                "Pique os legumes.",
                "Refogue a cebola rapidamente.",
                "Adicione os demais ingredientes e a água.",
                "Cozinhe até os legumes ficarem macios.",
                "Sirva quente."
            ),
        ),


        RecomendacaoReceita(
            titulo      = "Omelete de Legumes",
            descricao   = "Refeição simples, com baixo teor calórico e boa saciedade.",
            ingredientes = listOf(
                "2 ovos",
                "1/2 tomate picado",
                "1/4 de cebola",
                "Espinafre a gosto",
                "Sal e pimenta"
            ),
            modoPreparo = listOf(
                "Bata os ovos.",
                "Misture os legumes.",
                "Despeje em frigideira antiaderente.",
                "Cozinhe em fogo baixo até firmar.",
                "Sirva."
            ),
        ),
    ),

    Objetivo.MELHORAR_ALIMENTACAO to listOf(
        RecomendacaoReceita(
            titulo      = "Quinoa com Legumes",
            descricao   = "Refeição equilibrada com grãos e vegetais.",
            ingredientes = listOf(
                "1/2 xícara de quinoa",
                "Cenoura",
                "Beterraba",
                "Grão-de-bico",
                "Azeite e sal"
            ),
            modoPreparo = listOf(
                "Cozinhe a quinoa.",
                "Asse ou cozinhe os legumes.",
                "Misture tudo e tempere.",
                "Sirva."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Iogurte com Frutas e Granola",
            descricao   = "Opção prática para lanches.",
            ingredientes = listOf(
                "1 pote de iogurte natural",
                "Frutas a gosto",
                "2 colheres de granola"
            ),
            modoPreparo = listOf(
                "Coloque o iogurte em um recipiente.",
                "Adicione as frutas.",
                "Finalize com granola.",
                "Sirva."
            ),
        ),

        RecomendacaoReceita(
            titulo      = "Macarrão Integral com Molho de Tomate",
            descricao   = "Versão simples com maior teor de fibras.",
            ingredientes = listOf(
                "Macarrão integral",
                "Tomate",
                "Alho",
                "Cebola",
                "Azeite e sal"
            ),
            modoPreparo = listOf(
                "Cozinhe o macarrão.",
                "Prepare o molho com tomate, alho e cebola.",
                "Misture e sirva."
            ),
        ),

        // NOVA
        RecomendacaoReceita(
            titulo      = "Arroz, Feijão e Legumes",
            descricao   = "Base tradicional com bom equilíbrio nutricional.",
            ingredientes = listOf(
                "Arroz",
                "Feijão",
                "Legumes variados",
                "Sal e temperos"
            ),
            modoPreparo = listOf(
                "Cozinhe o arroz e o feijão separadamente.",
                "Prepare os legumes cozidos ou refogados.",
                "Monte o prato com todos os itens.",
                "Sirva."
            ),
        ),
    ),
)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val healthRepo     = HealthRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)

    private val _caloriasHoje = MutableStateFlow(0.0)
    val caloriasHoje: StateFlow<Double> = _caloriasHoje.asStateFlow()

    private val _caloriasRecomendadas = MutableStateFlow(0)
    val caloriasRecomendadas: StateFlow<Int> = _caloriasRecomendadas.asStateFlow()

    private val _recomendacaoReceita = MutableStateFlow<RecomendacaoReceita?>(null)
    val recomendacaoReceita: StateFlow<RecomendacaoReceita?> = _recomendacaoReceita.asStateFlow()

    init {
        carregarCalorias()
        observarMudancas()
    }

    fun recarregarCaloriasHome() = carregarCalorias()

    private fun carregarCalorias() {
        viewModelScope.launch {
            try {
                val hoje = LocalDate.now()
                var kcalParaExibir = 0.0

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val nutricaoHC = healthRepo.lerNutricaoDia(hoje)

                    if (nutricaoHC.calorias > 0) {
                        kcalParaExibir = nutricaoHC.calorias
                        consumoRepo.salvarConsumoLocal(
                            data      = hoje.toString(),
                            kcal      = nutricaoHC.calorias,
                            proteinaG = nutricaoHC.proteinas,
                            carboG    = nutricaoHC.carboidratos,
                            gorduraG  = nutricaoHC.gorduras,
                        )
                    }
                }

                if (kcalParaExibir <= 0) {
                    kcalParaExibir = consumoRepo.carregarConsumoLocal(hoje.toString()).kcal
                }

                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepository.carregarNome()
                )

                _caloriasHoje.value         = kcalParaExibir
                _caloriasRecomendadas.value = perfil.caloriasRecomendadas
                _recomendacaoReceita.value  = escolherReceitaDoDia(perfil.objetivo)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao carregar calorias: ${e.message}")
            }
        }
    }

    private fun observarMudancas() {
        viewModelScope.launch {
            consumoRepo.mudancas.collect {
                Log.d("HomeViewModel", "Consumo atualizado — recarregando calorias.")
                carregarCalorias()
            }
        }
    }

    /**
     * Seleciona uma receita da lista do objetivo usando o dia do ano como índice,
     * assim a recomendação muda diariamente de forma determinística (sem aleatoriedade).
     */
    private fun escolherReceitaDoDia(objetivo: Objetivo): RecomendacaoReceita? {
        val lista  = receitasPorObjetivo[objetivo] ?: return null
        val indice = LocalDate.now().dayOfYear % lista.size
        return lista[indice]
    }
}
