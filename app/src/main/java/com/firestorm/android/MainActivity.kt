package com.firestorm.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firestorm.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appSummary.text = buildString {
            appendLine("Firestorm Kotlin conversion")
            appendLine("Renderer: OpenGL ES 3.2")
        }
    }

    override fun onResume() {
        super.onResume()
        binding.glSurface.onResume()
    }

    override fun onPause() {
        binding.glSurface.onPause()
        super.onPause()
    }
}
