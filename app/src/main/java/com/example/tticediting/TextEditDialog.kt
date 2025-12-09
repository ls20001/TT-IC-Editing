package com.example.tticediting

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.tticediting.databinding.TextDialogBinding

class TextEditDialog : DialogFragment() {
    private var binding: TextDialogBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        // TODO:
        val binding = TextDialogBinding.inflate(layoutInflater)
        val dialog = activity.let {
            AlertDialog.Builder(it).apply {
                setView(binding.root)
                setCancelable(false)
            }.create()
        }
        binding.discard.setOnClickListener {
            dialog.dismiss()
        }
        return dialog
    }


}