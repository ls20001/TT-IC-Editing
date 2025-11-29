package com.example.tticediting

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tticediting.databinding.GreetingActivityBinding

private const val TAG = "XX-GreetingActivity"

class GreetingActivity : AppCompatActivity(), ImagePicker.Handler {
    private val imagePicker = ImagePicker(this, this)

    private lateinit var binding: GreetingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GreetingActivityBinding.inflate(layoutInflater)
        initUi()
    }

    private fun initUi() {
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.actionTakePhoto.setOnClickListener {
            imagePicker.pickImageByCamera()
        }
        binding.actionPickImageFromFile.setOnClickListener {
            imagePicker.pickImageFromFile()
        }
    }

    override fun onPickImageResult(uri: Uri) {
        // TODO：跳转到编辑器页面进行处理
        val image = imagePicker.decodeBitmapFromUri(uri)

        Log.d(TAG, "Pick image: ${image.width} x ${image.height}")
        binding.layoutGreeting.setBackgroundDrawable(image.toDrawable(resources))
    }

    override fun onPermissionReject() {
        Toast.makeText(this, R.string.permission_deny, Toast.LENGTH_LONG).show()
    }

    override fun onCameraUnavailable() {
        Toast.makeText(this, R.string.camera_unavaliable, Toast.LENGTH_LONG).show()
    }
}