package com.example.tticediting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.databinding.ChopFragmentBinding

/**
 * 处理图像裁剪。
 */
class ChopFragment : Fragment() {
    private lateinit var binding: ChopFragmentBinding
    private lateinit var imageEdit: ImageEditCore

    private val editActivity: EditActivity
        get() = activity as EditActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = ChopFragmentBinding.inflate(inflater)
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

        binding.chopView.setImageBound(imageEdit.getImageBox())
        binding.chopView.setChopBox(imageEdit.getImageBox())
        imageEdit.imageUpdate.observe(viewLifecycleOwner) {
            binding.chopView.setImageBound(imageEdit.getImageBox())
        }

        binding.chopMode.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chopModeFree -> binding.chopView.setFreeAspectRatio()
                R.id.chopMode1to1 -> binding.chopView.setFixAspectRatio(1, 1)
                R.id.chopMode4to3 -> binding.chopView.setFixAspectRatio(4, 3)
                R.id.chopMode3to4 -> binding.chopView.setFixAspectRatio(3, 4)
                R.id.chopMode16to9 -> binding.chopView.setFixAspectRatio(16, 9)
                R.id.chopMode9to16 -> binding.chopView.setFixAspectRatio(9, 16)
            }
        }
        binding.confirm.setOnClickListener {
            imageEdit.chopImageOnView(binding.chopView.getChopBox())
            editActivity.backToPreview()
        }
        binding.discard.setOnClickListener {
            editActivity.backToPreview()
        }
    }
}