package com.prayag.pocketscanner.splash.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prayag.pocketscanner.auth.domain.model.User
import com.prayag.pocketscanner.auth.presentation.login.SkyAnimationState
import com.prayag.pocketscanner.splash.domain.usecase.HandleSplashFlowUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashNavigationState {
    object Loading : SplashNavigationState()
    data class NavigateToHome(val animationState: SkyAnimationState) : SplashNavigationState()
    data class NavigateToLogin(val animationState: SkyAnimationState) : SplashNavigationState()
    data class NavigateToMainApp(val user: User) : SplashNavigationState()
}

class SplashViewModel(
    private val handleSplashFlowUseCase: HandleSplashFlowUseCase
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    fun handleSplashFlow(context: Context, animationState: SkyAnimationState) {
        viewModelScope.launch {
            when (val result = handleSplashFlowUseCase(context)) {
                is HandleSplashFlowUseCase.Result.NoInternet -> {
                    _navigationState.value = SplashNavigationState.NavigateToHome(animationState)
                }
                is HandleSplashFlowUseCase.Result.LoginSuccess -> {
                    _navigationState.value = SplashNavigationState.NavigateToMainApp(result.user)
                }
                is HandleSplashFlowUseCase.Result.LoginFailed -> {
                    _navigationState.value = SplashNavigationState.NavigateToLogin(animationState)
                }
            }
        }
    }

    fun resetNavigationState() {
        _navigationState.value = SplashNavigationState.Loading
    }
}