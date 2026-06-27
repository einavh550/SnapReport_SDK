package com.example.snapreportdemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapreport.sdk.SnapReportSdk
import kotlinx.coroutines.launch

/**
 * ViewModel for [MainActivity].
 *
 * Drives the "Report a bug" button state and exposes a one-time event
 * indicating the outcome of the last submission.
 */
class MainViewModel : ViewModel() {

    /** Button enabled/disabled state. */
    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    /** One-time status message shown in a Snackbar. */
    private val _statusEvent = MutableLiveData<String?>()
    val statusEvent: LiveData<String?> = _statusEvent

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Triggers a bug report with an optional user-supplied description.
     * The SDK runs the upload pipeline on its own IO scope; we just track
     * the button state here.
     */
    fun reportBug(description: String) {
        if (_isSubmitting.value == true) return
        _isSubmitting.value = true

        viewModelScope.launch {
            try {
                // triggerReport is non-blocking — it returns immediately after
                // enqueuing the work on the SDK's internal coroutine scope.
                SnapReportSdk.triggerReport(description.ifBlank { null })
                _statusEvent.postValue("Report submitted! Check the portal for your ticket.")
            } catch (e: Exception) {
                _statusEvent.postValue("Failed to submit report: ${e.message}")
            } finally {
                _isSubmitting.postValue(false)
            }
        }
    }

    /** Clears the status event after it has been consumed by the UI. */
    fun onStatusEventConsumed() {
        _statusEvent.value = null
    }
}

