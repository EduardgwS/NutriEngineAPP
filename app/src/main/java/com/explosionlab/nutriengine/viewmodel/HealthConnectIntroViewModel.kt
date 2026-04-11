package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.explosionlab.nutriengine.repository.HealthRepository


class HealthConnectIntroViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepo = HealthRepository(application)

    val hcDisponivel: Boolean = healthRepo.isDisponivel()
    val permissions           = healthRepo.permissions
}
