package sa.factory.hakeemvitalstelehe.di

import sa.factory.hakeemvitalstelehe.data.repository.HakeemVitalsRepositoryImpl
import sa.factory.hakeemvitalstelehe.domain.repository.HakeemVitalsRepository
import sa.factory.hakeemvitalstelehe.domain.usecase.*
import sa.factory.hakeemvitalstelehe.hardware.HardwareSensorDriver
import sa.factory.hakeemvitalstelehe.ui.viewmodel.HakeemVitalsViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: HakeemVitalsRepository = HakeemVitalsRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): HakeemVitalsViewModel = HakeemVitalsViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
