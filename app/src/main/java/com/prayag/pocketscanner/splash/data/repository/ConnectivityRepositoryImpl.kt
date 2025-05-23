package com.prayag.pocketscanner.splash.data.repository


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.prayag.pocketscanner.splash.domain.repository.ConnectivityRepository

class ConnectivityRepositoryImpl(
    private val context: Context
) : ConnectivityRepository {

    override fun hasInternetConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}