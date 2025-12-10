package com.example.tticediting

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.example.tticediting.databinding.TextDialogBinding
import kotlin.String
import kotlin.math.roundToInt

/**
 * 字体设置/调整对话框
 */
class TextEditDialog : DialogFragment() {
    private lateinit var binding: TextDialogBinding
    private lateinit var dialog: AlertDialog

    // 用于 onConfirm() 返回结果
    data class DialogResult(
        val text: String,
        val fontName: String,
        val fontStyle: Int,
        val colorRed: Int,
        val colorGreen: Int,
        val colorBlue: Int,
        val alpha: Int,
    ) {
        fun makeColorARGB(): Int {
            return makeColorARGB(colorRed, colorGreen, colorBlue, alpha)
        }

        fun makeTypeface(): Typeface {
            return Typeface.create(fontName, fontStyle)
        }
    }

    private var textContent = ""
    private var fontName = "default"
    private var fontStyle = Typeface.NORMAL
    private var colorRed = 0
    private var colorGreen = 0
    private var colorBlue = 0
    private var alpha = 255

    fun getDialogResult(): DialogResult {
        return DialogResult(
            text = textContent,
            fontName = fontName,
            fontStyle = fontStyle,
            colorRed = colorRed,
            colorGreen = colorGreen,
            colorBlue = colorBlue,
            alpha = alpha,
        )
    }

    /**
     * 设置对话框初始值
     */
    fun setDialogResult(result: DialogResult?) {
        if (result != null) {
            textContent = result.text
            fontName = result.fontName
            fontStyle = result.fontStyle
            colorRed = result.colorRed
            colorGreen = result.colorGreen
            colorBlue = result.colorBlue
            alpha = result.alpha
        }
    }

    private lateinit var fontNameList: Array<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fontNameList = resources.getStringArray(R.array.font_names)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        binding = TextDialogBinding.inflate(layoutInflater)
        isUserConfirmed = false
        initUi()

        dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()
        return dialog
    }

    private fun initUi() {
        // 文本输入
        binding.textContent.setText(textContent)
        binding.textContent.doAfterTextChanged { editable ->
            textContent = editable.toString()
        }

        // 字体选择
        binding.fontName.setSelection(fontNameList.indexOf(fontName))
        binding.fontName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (view != null) {
                    fontName = fontNameList[position]
                    updateTypeface()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                fontName = "default"
                updateTypeface()
            }
        }

        // 字体风格选择：加粗/斜体
        binding.fontStyle.check(
            when (fontStyle) {
                Typeface.NORMAL -> R.id.fontSizeRegular
                Typeface.BOLD -> R.id.fontStyleBold
                Typeface.ITALIC -> R.id.fontStyleItalic
                Typeface.BOLD_ITALIC -> R.id.fontStyleBoldItalic
                else -> error("Invalid item")
            }
        )
        binding.fontStyle.setOnCheckedChangeListener { group, checkedId ->
            fontStyle = when (checkedId) {
                R.id.fontSizeRegular -> Typeface.NORMAL
                R.id.fontStyleBold -> Typeface.BOLD
                R.id.fontStyleItalic -> Typeface.ITALIC
                R.id.fontStyleBoldItalic -> Typeface.BOLD_ITALIC
                else -> error("Invalid item")
            }
            updateTypeface()
        }

        // RGB调整
        binding.fontColorRed.progress = colorRed
        binding.fontColorRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                colorRed = progress
                updateTextColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.fontColorGreen.progress = colorGreen
        binding.fontColorGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                colorGreen = progress
                updateTextColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.fontColorBlue.progress = colorBlue
        binding.fontColorBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                colorBlue = progress
                updateTextColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 透明度调整，需要将透明度转换为不透明度，且将百分比转换为0~255之间
        binding.fontAlpha.values = listOf((1f - alpha.toFloat() / 255f) * 100f)
        binding.fontAlpha.addOnChangeListener { _, value, _ ->
            alpha = ((1f - value / 100f) * 255f).toInt()
            updateTextColor()
        }

        // 处理确认/取消
        binding.confirm.setOnClickListener {
            isUserConfirmed = true
            dialog.dismiss()
        }
        binding.discard.setOnClickListener {
            isUserConfirmed = false
            dialog.dismiss()
        }
    }

    private fun updateTextColor() {
        binding.textPreview.setTextColor(makeColorARGB(colorRed, colorGreen, colorBlue, alpha))
    }

    private fun updateTypeface() {
        binding.textPreview.setTypeface(Typeface.create(fontName, fontStyle))
    }

    /**
     * 处理对话框结果，onConfirm为用户点击确认后调用，onDiscard为用户取消时调用。
     */
    interface ResultHandler {
        fun onConfirm(result: DialogResult)
        fun onDiscard()
    }

    private var isUserConfirmed = false
    private var resultHandler: ResultHandler? = null

    fun setResultHandler(handler: ResultHandler) {
        resultHandler = handler
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isUserConfirmed) {
            resultHandler?.onConfirm(getDialogResult())
        } else {
            resultHandler?.onDiscard()
        }
    }
}