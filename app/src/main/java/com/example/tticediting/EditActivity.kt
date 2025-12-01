package com.example.tticediting

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tticediting.databinding.EditActivityBinding
import com.google.android.material.tabs.TabLayout

private const val TAG = "XX-EditActivity"

/**
 * 图片编辑器主页面。
 */
class EditActivity : AppCompatActivity() {
    private lateinit var binding: EditActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditActivityBinding.inflate(layoutInflater)
        binding.editView.onCreate(this)
        initUi()
    }

    private fun initUi() {
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolPicker.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab) {
                Log.d(TAG, "Pick up tool: ${p0.text}")
            }

            override fun onTabUnselected(p0: TabLayout.Tab) {}
            override fun onTabReselected(p0: TabLayout.Tab) {}
        })
    }
}