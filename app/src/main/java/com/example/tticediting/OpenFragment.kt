package com.example.tticediting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.tticediting.databinding.OpenFragmentBinding

/**
 * 编辑器页面，处理图片的打开
 */
class OpenFragment(private val editActivity: EditActivity) : Fragment() {
    private lateinit var binding: OpenFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = OpenFragmentBinding.inflate(inflater)
        initUi()
        return binding.root
    }

    private fun initUi() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.actionTakePhoto.setOnClickListener {
            editActivity.pickImageByCamera()
        }
        binding.actionPickImageFromFile.setOnClickListener {
            editActivity.pickImageFromFile()
        }
    }
}