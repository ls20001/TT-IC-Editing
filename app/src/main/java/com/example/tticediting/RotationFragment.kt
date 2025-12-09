package com.example.tticediting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.databinding.RotationFragmentBinding

/**
 * 处理图像翻转和旋转
 */
class RotationFragment : Fragment() {
    private lateinit var binding: RotationFragmentBinding
    private lateinit var imageEdit: ImageEditCore

    private val editActivity: EditActivity
        get() = activity as EditActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = RotationFragmentBinding.inflate(inflater)
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
        binding.rotationCw.setOnClickListener {
            imageEdit.rotateImage(90)
        }
        binding.rotationCcw.setOnClickListener {
            imageEdit.rotateImage(-90)
        }
        binding.rotation180.setOnClickListener {
            imageEdit.rotateImage(180)
        }
        binding.flipVertival.setOnClickListener {
            imageEdit.flipImageVertical()
        }
        binding.flipHorizontal.setOnClickListener {
            imageEdit.flipImageHorizontal()
        }
    }

    override fun onDetach() {
        super.onDetach()
        imageEdit.discardUpdate()
    }
}