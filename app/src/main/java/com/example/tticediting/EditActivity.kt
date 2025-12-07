package com.example.tticediting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.databinding.EditActivityBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

private const val TAG = "XX-EditActivity"

/**
 * 图片编辑器主页面。
 */
class EditActivity : AppCompatActivity(), ImagePicker.Handler {
    private val imagePicker = ImagePicker(this, this)

    private lateinit var binding: EditActivityBinding
    private lateinit var imageEdit: ImageEditCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditActivityBinding.inflate(layoutInflater)
        imageEdit = ViewModelProvider(this)[ImageEditCore::class.java]
        initUi()
    }

    override fun onPickImageResult(uri: Uri) {
        imageEdit.openImageFromUri(uri)
        backToPreview()
    }

    override fun onPermissionReject() {
        Toast.makeText(this, R.string.permission_deny, Toast.LENGTH_LONG).show()
    }

    override fun onCameraUnavailable() {
        Toast.makeText(this, R.string.camera_unavaliable, Toast.LENGTH_LONG).show()
    }

    fun pickImageByCamera() {
        imagePicker.pickImageByCamera()
    }

    fun pickImageFromFile() {
        imagePicker.pickImageFromFile()
    }

    /**
     * 用于 Fragment 返回到预览页面
     */
    fun backToPreview() {
        binding.toolPicker.selectTab(binding.toolPicker.getTabAt(0))
    }

    private fun initUi() {
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化编辑器视图
        binding.imagePreview.setImageEdit(imageEdit)
        imageEdit.imageUpdate.observe(this) {
            binding.imagePreview.invalidate()
        }

        // 监听图片保存事件
        imageEdit.imageSaveResult.observe(this) { success ->
            if (success) {
                Snackbar.make(binding.root, R.string.save_success, Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, R.string.save_failed, Snackbar.LENGTH_LONG).show()
            }
        }

        // 默认选择第一个预览视图，此时不需要加载 Fragment。
        binding.toolPicker.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectToolFragment(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 处理上一个 Activity 调用
        getParameterFromIntent(this)?.let { uri ->
            imageEdit.openImageFromUri(uri)
            intent.removeExtra(KEY_URI)
        }
    }

    // 处理主页工具栏的选择
    private fun selectToolFragment(position: Int) {
        Log.d(TAG, "Tools at position $position is selected")

        when (position) {
            0 -> replaceFragment(null)                      // Preview
            1 -> replaceFragment(ChopFragment(this))        // Chop
            2 -> replaceFragment(RotationFragment(this))    // Rotation
            3 -> workInProcess()                            // Text
            4 -> replaceFragment(OpenFragment(this))        // Open
            5 -> workInProcess()                            // Adjustment
            6 -> {                                          // Save
                imageEdit.saveImageToAlbum()
                replaceFragment(null)
            }
        }
    }

    private var currentFragment: Fragment? = null

    private fun replaceFragment(fragment: Fragment?) {
        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let {
                remove(it)
            }
            fragment?.let {
                replace(R.id.toolFragment, it)
            }
            currentFragment = fragment
            commit()
        }
    }

    private fun workInProcess() {
        Snackbar.make(binding.root, R.string.wip, Snackbar.LENGTH_LONG).show()
        backToPreview()
    }

    companion object {
        private const val KEY_URI = "uri"

        /**
         * 从 Intent 获取图片传递的结果。
         */
        private fun getParameterFromIntent(activity: EditActivity): Uri? {
            @Suppress("DEPRECATION")
            return activity.intent.getParcelableExtra("uri")
        }

        /**
         * 启动 Activity 编辑图片，uri 为编辑图像 URI
         */
        fun start(activity: AppCompatActivity, uri: Uri) {
            val intent = Intent()
            intent.putExtra(KEY_URI, uri)
            intent.setClass(activity, EditActivity::class.java)
            activity.startActivity(intent)
        }
    }
}