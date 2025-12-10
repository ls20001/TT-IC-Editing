package com.example.tticediting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.databinding.AdjustmentFragmentBinding

/**
 * 处理图像饱和度、亮度、对比度
 */
class AdjustmentFragment : Fragment() {
    private lateinit var binding: AdjustmentFragmentBinding
    private lateinit var imageEdit: ImageEditCore

    private val editActivity: EditActivity
        get() = activity as EditActivity

    private var brightness = 0f
    private var contrast = 0f
    private var saturation = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = AdjustmentFragmentBinding.inflate(inflater)
        imageEdit = ViewModelProvider(editActivity)[ImageEditCore::class.java]
        initUi()
        return binding.root
    }

    private fun initUi() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.confirm.setOnClickListener {
            imageEdit.commitUpdate()
            editActivity.backToPreview()
        }
        binding.discard.setOnClickListener {
            imageEdit.discardUpdate()
            editActivity.backToPreview()
        }

        binding.adjustBrightness.values = listOf(brightness)
        binding.adjustBrightness.addOnChangeListener { _, value, _ ->
            brightness = value
            imageEdit.adjustBrightness(value)
        }

        binding.adjustContrast.values = listOf(contrast)
        binding.adjustContrast.addOnChangeListener { _, value, _ ->
            contrast = value
            imageEdit.adjustContrast(value)
        }

        binding.adjustSaturation.values = listOf(saturation)
        binding.adjustSaturation.addOnChangeListener { _, value, _ ->
            saturation = value
            imageEdit.adjustSaturation(value)
        }
    }

    override fun onDetach() {
        super.onDetach()
        imageEdit.discardUpdate()
    }
}