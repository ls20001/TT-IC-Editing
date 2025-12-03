package com.example.tticediting

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.ContentValues
import android.content.pm.PackageManager.FEATURE_CAMERA_ANY
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "XX-ImagePicker"

/**
 * 用于从相册或相机获取图片，成功后结果通过 Handler.onPickImageResult() 返回。在调用时处理
 * 对应的用户权限和设备可用性情况。拍摄成功后图片会保留在相册中，用户可以再次打开图片编辑。
 * 选取的图片以 URI 的形式返回，可通过 decodeBitmapFromUri() 从 contentResolver 中解码出 Bitmap。
 */
class ImagePicker(private val context: ComponentActivity, private val handler: Handler) {
    interface Handler {
        /**
         * 处理相机或相册选取图像的结果。
         */
        fun onPickImageResult(uri: Uri)

        /**
         * 处理相机或相册权限被拒绝。
         */
        fun onPermissionReject()

        /**
         * 处理系统没有相机的情况。
         */
        fun onCameraUnavailable()
    }

    /**
     * 通过相机拍照获取照片。
     */
    fun pickImageByCamera() {
        // 判断系统是否存在相机
        if (!context.packageManager.hasSystemFeature(FEATURE_CAMERA_ANY)) {
            handler.onCameraUnavailable()
        } else if (isCameraPermissionGranted()) {
            launchTakePhoto()
        } else {
            launchTakePhotoAfterPermissionRequest()
        }
    }

    /**
     * 用户从相册选取照片。对于 Android 14 及以上，申请照片部分访问权限。
     */
    fun pickImageFromFile() {
        if (isImageReadingPermissionGranted()) {
            launchPickImage()
        } else {
            launchPickImageAfterPermissionRequest()
        }
    }

    // 处理拍照
    // TakePicture 创建的 URI
    private var photoUri: Uri? = null

    private fun isCameraPermissionGranted(): Boolean {
        return context.checkSelfPermission(CAMERA) == PERMISSION_GRANTED
    }

    private fun launchTakePhotoAfterPermissionRequest() {
        requestCameraPermission.launch(arrayOf(CAMERA))
    }

    private fun launchTakePhoto() {
        context.contentResolver.insert(EXTERNAL_CONTENT_URI, ContentValues())?.let { uri ->
            photoUri = uri
            takePhoto.launch(uri)
        }
    }

    private val requestCameraPermission =
        context.registerForActivityResult(RequestMultiplePermissions()) { results ->
            if (results[CAMERA] == true) {
                launchTakePhoto()
            } else {
                handler.onPermissionReject()
            }
        }

    private val takePhoto = context.registerForActivityResult(TakePicture()) { success ->
        photoUri?.let { uri ->
            if (success) {
                handler.onPickImageResult(uri)
            } else {
                // 清理创建的图片，避免留下无效的条目
                context.contentResolver.delete(uri, null, null)
                photoUri = null
            }
        }
    }

    // 处理从相册选取
    private val imageReadingPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(READ_MEDIA_IMAGES)
        } else {
            arrayOf(READ_EXTERNAL_STORAGE)
        }

    private fun isImageReadingPermissionGranted(): Boolean {
        return imageReadingPermissions.any {
            context.checkSelfPermission(it) == PERMISSION_GRANTED
        }
    }

    private fun launchPickImage() {
        val request = PickVisualMediaRequest.Builder()
            .setMediaType(PickVisualMedia.ImageOnly)
            .build()
        pickImage.launch(request)
    }

    private fun launchPickImageAfterPermissionRequest() {
        requestImageReadingPermission.launch(imageReadingPermissions)
    }

    private val requestImageReadingPermission =
        context.registerForActivityResult(RequestMultiplePermissions()) { results ->
            if (results.any { it.value }) {
                launchPickImage()
            } else {
                handler.onPermissionReject()
            }
        }

    private val pickImage =
        context.registerForActivityResult(PickVisualMedia()) { uri ->
            if (uri != null) {
                handler.onPickImageResult(uri)
            }
        }
}