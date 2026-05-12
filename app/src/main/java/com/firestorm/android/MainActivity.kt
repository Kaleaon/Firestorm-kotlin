package com.firestorm.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firestorm.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val glSurfaceView get() = binding.glSurface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appSummary.text = buildString {
            appendLine("Firestorm Kotlin conversion")
            appendLine("OpenGL ES 3.2 renderer enabled for graphics baseline.")
            appendLine("LLSD formatting aligned with Libremetaverse / python-llsd conventions.")
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        glSurfaceView.onPause()
        super.onPause()
    }
}
