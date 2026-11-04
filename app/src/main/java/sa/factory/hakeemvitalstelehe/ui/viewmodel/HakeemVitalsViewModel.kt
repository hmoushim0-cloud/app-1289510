package sa.factory.hakeemvitalstelehe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.hakeemvitalstelehe.domain.usecase.*
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsItem
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsOrder
import sa.factory.hakeemvitalstelehe.model.HakeemVitalsTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HakeemVitalsUiState(
    val catalog: List<HakeemVitalsItem> = emptyList(),
    val filteredCatalog: List<HakeemVitalsItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<HakeemVitalsItem> = emptyList(),
    val telemetry: HakeemVitalsTelemetry? = null,
    val lastOrder: HakeemVitalsOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class HakeemVitalsViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HakeemVitalsUiState())
    val uiState: StateFlow<HakeemVitalsUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: HakeemVitalsItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: HakeemVitalsItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
