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

    // 默认进入预览界面
    private var currentToolIndex = TOOL_PREVIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditActivityBinding.inflate(layoutInflater)
        imageEdit = ViewModelProvider(this)[ImageEditCore::class.java]

        handleCreateInstanceState(savedInstanceState)
        handleActivityInvoke()
        initUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        handlSaveInstanceState(outState)
    }

    private fun handleCreateInstanceState(inState: Bundle?) {
        if (inState != null) {
            currentToolIndex = inState.getInt(KEY_CURRENT_TOOL_INDEX)
        }
    }

    private fun handlSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_CURRENT_TOOL_INDEX, currentToolIndex)
    }

    // 处理上一个 Activity 调用
    private fun handleActivityInvoke() {
        val uri = getUriFromIntent(this)
        if (uri != null) {
            imageEdit.openImageFromUri(uri)
        }
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

    /**
     * 提供 ImageEditCore 的图像选取接口
     */
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
        selectToolFragment(TOOL_PREVIEW)
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
            backToPreview()
        }

        // 设置当前 Fragment
        selectToolFragment(currentToolIndex)
        binding.toolPicker.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectToolFragment(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // 处理主页工具栏的选择
    private fun selectToolFragment(position: Int) {
        Log.d(TAG, "Tools at position $position is selected")
        binding.toolPicker.apply {
            selectTab(getTabAt(position))
        }

        if (position == currentToolIndex) {
            return
        }

        currentToolIndex = position
        val fragment = when (position) {
            TOOL_PREVIEW -> null
            TOOL_CHOP -> ChopFragment()
            TOOL_ROTATION -> RotationFragment()
            TOOL_TEXT_EDIT -> workInProcess()
            TOOL_OPEN -> OpenFragment()
            TOOL_ADJUSTMENT -> workInProcess()
            TOOL_SAVE -> {
                imageEdit.saveImageToAlbum()
                null
            }

            else -> null
        }

        supportFragmentManager.beginTransaction().apply {
            binding.toolFragment.getFragment<Fragment?>()?.let {
                remove(it)
            }
            fragment?.let {
                replace(R.id.toolFragment, it)
            }
            commit()
        }
    }

    private fun workInProcess(): Fragment? {
        Snackbar.make(binding.root, R.string.wip, Snackbar.LENGTH_LONG).show()
        return null
    }

    companion object {
        private const val TOOL_PREVIEW = 0
        private const val TOOL_CHOP = 1
        private const val TOOL_ROTATION = 2
        private const val TOOL_TEXT_EDIT = 3
        private const val TOOL_OPEN = 4
        private const val TOOL_ADJUSTMENT = 5
        private const val TOOL_SAVE = 6

        private const val KEY_URI = "uri"
        private const val KEY_CURRENT_TOOL_INDEX = "currentToolIndex"

        // 从 Intent 获取图片传递的结果，获取后从Intent中删除，防止重新创建后误判。
        private fun getUriFromIntent(activity: EditActivity): Uri? {
            activity.intent.apply {
                @Suppress("DEPRECATION")
                val uri = getParcelableExtra<Uri>(KEY_URI)
                removeExtra(KEY_URI)
                return uri
            }
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