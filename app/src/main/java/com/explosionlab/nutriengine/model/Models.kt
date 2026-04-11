package com.explosionlab.nutriengine.model

import java.util.UUID


data class Mensagem(
    val texto:     String,
    val ehUsuario: Boolean,
    val id:        String = UUID.randomUUID().toString()
)


data class ResultadoLogin(
    val sucesso:  Boolean,
    val nome:     String = "",
    val mensagem: String = ""
)
