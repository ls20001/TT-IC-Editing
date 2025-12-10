package com.example.tticediting

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
@SuppressLint("ClickableViewAccessibility")
class TextEditFragment : Fragment(), TextEditDialog.ResultHandler {
    private lateinit var binding: TextEditFragmentBinding
    private lateinit var imageEdit: ImageEditCore

    private val editActivity: EditActivity
        get() = activity as EditActivity

    // 当前文字参数，从对话框设置
    private var dialogResult: TextEditDialog.DialogResult? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

        // 点击添加文字，双击编辑文字
        binding.textEditView.setTextEditRequestHandler {
            startDialog()
        }
        binding.textEditView.setOnTouchListener { v, event ->
            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
                binding.textEditView.setTextPosition(PointF(event.x, event.y))
                startDialog()
            }
            true
        }

        binding.confirm.setOnClickListener {
            dialogResult?.apply {
                imageEdit.drawTextOnView(
                    text,
                    makeTypeface(),
                    makeColorARGB(),
                    binding.textEditView.getTextRect()
                )
            }
            editActivity.backToPreview()
        }

        binding.discard.setOnClickListener {
            editActivity.backToPreview()
        }

        binding.textRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.textEditView.setTextRotation(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.resetRotation.setOnClickListener {
            binding.textRotation.progress = 0
            binding.textEditView.setTextRotation(0f)
        }
    }

    // 启动对话框，若对话框处于启动状态则忽略
    private var isDialogOnShow = false

    private fun startDialog() {
        if (!isDialogOnShow) {
            val dialog = TextEditDialog()
            dialog.setDialogResult(dialogResult)
            dialog.setResultHandler(this)
            dialog.show(parentFragmentManager, "TextEditFragment")
            isDialogOnShow = true
        }
    }

    override fun onConfirm(result: TextEditDialog.DialogResult) {
        binding.textEditView.setOnTouchListener(null)
        isDialogOnShow = false
        dialogResult = result

        binding.textEditView.setTextContent(result.text)
        binding.textEditView.setTextSize(14f * resources.displayMetrics.density)
        binding.textEditView.setTypeface(result.makeTypeface())
        binding.textEditView.setTextColor(result.makeColorARGB())
    }

    override fun onDiscard() {
        isDialogOnShow = false
    }
}