package com.example.circuitviewmodeldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val circuit = Circuit.Builder()
            .addPresenterFactory { screen, navigator, _ ->
                when (screen) {
                    is RootScreen -> RootScreen.RootPresenter(navigator, HeavyResourceManager)
                    is DetailScreen -> DetailScreen.DetailPresenter(screen.id, HeavyResourceManager)
                    else -> null
                }
            }
            .addUiFactory { screen, _ ->
                when (screen) {
                    is RootScreen -> RootScreen.RootUi()
                    is DetailScreen -> DetailScreen.DetailUi()
                    else -> null
                }
            }
            .build()

        setContent {
            MaterialTheme {
                CircuitCompositionLocals(circuit) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val backstack = rememberSaveableBackStack { push(RootScreen) }
                        val navigator = rememberCircuitNavigator(backstack = backstack)
                        NavigableCircuitContent(
                            navigator = navigator,
                            backstack = backstack,
                        )
                    }
                }
            }
        }
    }
}

@Parcelize
data object RootScreen : Screen {
    data class State(
        val resourceRefCount: Int,
        val detailClick: () -> Unit,
    ) : CircuitUiState

    class RootPresenter(
        private val navigator: Navigator,
        private val heavyResourceManager: HeavyResourceManager,
    ) : Presenter<State> {
        @Composable
        override fun present(): State {
            val refCount = heavyResourceManager.refCount.collectAsState()
            return State(resourceRefCount = refCount.value) {
                navigator.goTo(DetailScreen(System.currentTimeMillis().toString()))
            }
        }
    }

    class RootUi : Ui<State> {
        @Composable
        override fun Content(state: State, modifier: Modifier) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "Refs: ${state.resourceRefCount}")
                Button(onClick = { state.detailClick() }) {
                    Text(text = "Go To Detail")
                }
            }
        }
    }
}


@Parcelize
data class DetailScreen(val id: String) : Screen {
    data class State(
        val id: String,
        val resourceRefCount: Int,
    ) : CircuitUiState

    class DetailPresenter(
        private val id: String,
        private val heavyResourceManager: HeavyResourceManager,
    ) : Presenter<State> {
        @Composable
        override fun present(): State {
            val vm = viewModel<HeavyResourceViewModel>(
                key = id,
                factory = remember(id) { HeavyResourceViewModel.Factory(id) }
            )
            val refCount = heavyResourceManager.refCount.collectAsState()
            // TODO: check(refCount.value > 0) { "This Screen should never see the resource closed" }
            return State(
                id = id,
                resourceRefCount = refCount.value,
            )
        }
    }

    class DetailUi : Ui<State> {
        @Composable
        override fun Content(state: State, modifier: Modifier) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (state.resourceRefCount > 0) Color.Black else Color.Red),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "ID: ${state.id}", color = Color.White)
                Text(text = "Refs: ${state.resourceRefCount}", color = Color.White)
            }
        }
    }
}

// Would be injected, object to simplify demo
object HeavyResourceManager {
    private val _refCount = MutableStateFlow<Int>(value = 0)
    val refCount: StateFlow<Int> = _refCount.asStateFlow()

    fun open() {
        println("DEMO: HeavyResourceManager.open(): ${_refCount.value} + 1")
        _refCount.update { it + 1 }
    }

    fun close() {
        println("DEMO: HeavyResourceManager.close(): ${_refCount.value} - 1")
        _refCount.update { it - 1 }
    }
}

class HeavyResourceViewModel(val id: String) : ViewModel() {
    init {
        println("DEMO: HeavyResourceViewModel.init: ${System.identityHashCode(this)}")
//        check(HeavyResourceManager.refCount.value == 0) {
//            """This ViewModel (${System.identityHashCode(this)}) is responsible for opening the resource.
//                |A previous one must have leaked""".trimMargin()
//        }
        // For demo, it's simpler than wiring an `ensureOpen` places
        HeavyResourceManager.open()
    }

    override fun onCleared() {
        println("DEMO: HeavyResourceViewModel.onCleared(): ${System.identityHashCode(this)}")
        HeavyResourceManager.close()
    }

    class Factory(private val id: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HeavyResourceViewModel(id = id) as T
        }
    }
}
