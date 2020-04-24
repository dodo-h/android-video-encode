package com.android.videotest

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 *
 * Author：hdd
 * Date：2020/4/24
 * Description：
 *
 */
open class PermissionActivity: AppCompatActivity(){

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val failList: MutableList<String> = ArrayList()
        val successList: MutableList<String> = ArrayList()
        when (requestCode) {
            REQUEST_CHECK_PERMISSION -> {
                val len = grantResults.size
                var i = 0
                while (i < len) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        successList.add(permissions[i])
                    } else {
                        failList.add(permissions[i])
                    }
                    i++
                }
                if (successList.size == len) {
                    onPermissionGranted(requestCode, successList)
                    return
                }
                onPermissionDenied(requestCode, failList)
            }
            else -> {
            }
        }
    }
    open fun checkPermission() {
        val permissions: ArrayList<String> = ArrayList()
        permissions.add(Manifest.permission.CAMERA)
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        checkAndRequest(permissions, REQUEST_CHECK_PERMISSION)
    }

    private fun onPermissionGranted(requestCode: Int, permissions: List<String>) {
        onCheckPermissionResult(true, null)
    }

    private fun onPermissionDenied(requestCode: Int, failPermissions: List<String>) {
        onCheckPermissionResult(false, failPermissions)
    }

    open fun onCheckPermissionResult(success: Boolean, failPermissions: List<String>?) {}

    private fun checkAndRequest(permissions: ArrayList<String>, requestCode: Int) {
        val iterator = permissions.iterator()
        while (iterator.hasNext()) {
            val permission = iterator.next()
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, permission)) {
                iterator.remove()
            }
        }
        val size = permissions.size
        if (size == 0) {
            onPermissionGranted(requestCode, permissions)
            return
        }
        val requestPermissions = arrayOfNulls<String>(size)
        permissions.toArray<String>(requestPermissions)
        ActivityCompat.requestPermissions(this, requestPermissions, requestCode)
    }

    companion object {
        public const val REQUEST_CHECK_PERMISSION = 100
    }

}