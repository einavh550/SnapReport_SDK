package com.example.snapreportdemo

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.snapreportdemo.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Demo activity.
 *
 * Shows a single "Report a bug" button wired to [MainViewModel].
 * Serves as the integration test for the M4 SDK pipeline:
 *   init → metadata collection → screenshot capture → image compression → upload
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.isSubmitting.observe(this) { submitting ->
            binding.btnReportBug.isEnabled  = !submitting
            binding.progressIndicator.visibility =
                if (submitting) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.statusEvent.observe(this) { message ->
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                viewModel.onStatusEventConsumed()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnReportBug.setOnClickListener {
            val description = binding.etDescription.text?.toString() ?: ""
            viewModel.reportBug(description)
        }
    }
}

