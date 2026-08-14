package org.dergigi.boris.ui.auth

sealed interface AuthUiState {
    data object LoggedOut : AuthUiState
    data object MissingSigner : AuthUiState
    data class LoggedIn(val npub: String) : AuthUiState
    data class Connecting(val prior: AuthUiState) : AuthUiState
}
