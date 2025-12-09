package com.example.tticediting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.databinding.TextEditFragmentBinding

/**
 * 文字编辑模块
 */
class TextEditFragment : Fragment() {
    private lateinit var binding: TextEditFragmentBinding
    private lateinit var imageEdit: ImageEditCore

    private val editActivity: EditActivity
        get() = activity as EditActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TextEditFragmentBinding.inflate(inflater)
        imageEdit = ViewModelProvider(editActivity)[ImageEditCore::class.java]
        initUi()
        return binding.root
    }

    private fun initUi() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.confirm.setOnClickListener {
            val inputDialog = TextEditDialog()
            inputDialog.show(childFragmentManager, "")
//            imageEdit.commitUpdate()
//            editActivity.backToPreview()
        }
        binding.discard.setOnClickListener {
            imageEdit.discardUpdate()
            editActivity.backToPreview()
        }
        binding.textRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.textEditView.setFontRotation(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.resetRotation.setOnClickListener {
            binding.textRotation.progress = 0
            binding.textEditView.setFontRotation(0)
        }
    }
}