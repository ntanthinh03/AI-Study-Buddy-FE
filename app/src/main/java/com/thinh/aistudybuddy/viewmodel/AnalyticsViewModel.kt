package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.*
import com.thinh.aistudybuddy.services.network.RetrofitClient
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnalyticsViewModel : ViewModel() {
    var stats by mutableStateOf<UserStats?>(null)
    var gamificationStats by mutableStateOf<GamificationStats?>(null)
    private val _chartData = mutableStateListOf<ChartDataPoint>()
    val chartData: List<ChartDataPoint> get() = _chartData

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            error = null
            try {
                val statsResult = RetrofitClient.instance.getStats()
                val chartResult = RetrofitClient.instance.getChartData()
                val gamificationResult = RetrofitClient.instance.getUserStats()
                
                stats = statsResult
                _chartData.clear()
                _chartData.addAll(chartResult)
                
                if (gamificationResult.isSuccessful) {
                    gamificationStats = gamificationResult.body()
                }
            } catch (e: Exception) {
                error = "Failed to load analytics: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
