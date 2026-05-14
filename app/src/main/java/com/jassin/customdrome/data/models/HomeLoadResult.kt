package com.jassin.customdrome.data.models

sealed class HomeLoadResult {
    data object Loading : HomeLoadResult()

    data object NotLoggedIn : HomeLoadResult()

    data class LoggedIn(
        val songCount: Int,
    ) : HomeLoadResult()

    data class Error(
        val message: String,
    ) : HomeLoadResult()
}
