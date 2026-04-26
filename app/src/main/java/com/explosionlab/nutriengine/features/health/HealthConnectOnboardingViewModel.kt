package com.explosionlab.nutriengine.features.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class HealthConnectOnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepo = HealthConnectRepository(application)

    val hcDisponivel: Boolean = healthRepo.isDisponivel()
    val permissions           = healthRepo.permissions
}
