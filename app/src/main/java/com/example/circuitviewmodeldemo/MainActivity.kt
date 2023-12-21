package com.example.circuitviewmodeldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.NavigatorDefaults
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.parcelize.Parcelize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val circuit = Circuit.Builder()
            .addPresenterFactory { screen, navigator, _ ->
                when (screen) {
                    is RootScreen -> RootScreen.RootPresenter(navigator)
                    is DetailScreen -> DetailScreen.DetailPresenter(screen.id)
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
            .setDefaultNavDecoration(NavigatorDefaults.DefaultDecoration)
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
        val detailClick: () -> Unit,
    ) : CircuitUiState

    class RootPresenter(
        private val navigator: Navigator,
    ) : Presenter<State> {
        @Composable
        override fun present(): State {
            return State {
                navigator.goTo(DetailScreen(System.currentTimeMillis().toString()))
            }
        }
    }

    class RootUi : Ui<State> {
        @Composable
        override fun Content(state: State, modifier: Modifier) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = { state.detailClick() }) {
                        Text(text = "Go To Detail")
                    }
                }
            }
        }
    }
}


@Parcelize
data class DetailScreen(val id: String) : Screen {
    data class State(val id: String) : CircuitUiState

    class DetailPresenter(
        private val id: String,
    ) : Presenter<State> {
        @Composable
        override fun present(): State {
            val vm = viewModel<LoggingViewModel>(
                key = id,
                factory = remember(id) { LoggingViewModel.Factory(id) },
            )
            return State(id = id)
        }
    }

    class DetailUi : Ui<State> {
        @Composable
        override fun Content(state: State, modifier: Modifier) {
            Surface(color = MaterialTheme.colorScheme.inverseSurface) {
                Column(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Detail: ${state.id}")
                }
            }
        }
    }
}

@Stable
class LoggingViewModel(val id: String) : ViewModel() {
    init {
        println("DEMO: init $this : $id")
    }

    override fun onCleared() {
        println("DEMO: onCleared(): $this : $id")
    }

    class Factory(val id: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            val owner = extras[VIEW_MODEL_STORE_OWNER_KEY]
            println("DEMO: Factory.create(): $id : $application $owner")
            return LoggingViewModel(id) as T
        }
    }
}
