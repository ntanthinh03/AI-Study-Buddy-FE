package com.thinh.aistudybuddy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.aistudybuddy.data.models.LeaderboardEntry
import com.thinh.aistudybuddy.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {
    private val _entries = mutableStateListOf<LeaderboardEntry>()
    val entries: List<LeaderboardEntry> get() = _entries

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadLeaderboard() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            error = null
            try {
                val response = RetrofitClient.instance.getLeaderboard()
                if (response.isSuccessful) {
                    _entries.clear()
                    response.body()?.let { _entries.addAll(it) }
                } else {
                    error = "Failed to load leaderboard"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }
}
