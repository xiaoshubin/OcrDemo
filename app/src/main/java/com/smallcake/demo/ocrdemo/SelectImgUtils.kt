package com.smallcake.demo.ocrdemo

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.util.*

/**
 * Date:2021/7/14 13:38
 * Author:SmallCake
 * Desc:用于选择多张图片
 * 1.可删除选择的图片
 * 2.限制最大图片选择数量
 * 3.点击已选图片查看大图
 **/
object SelectImgUtils {
    private const val TAG = "SelectImgUtils"


    /**
     * 获取单张图片
     * @param activity AppCompatActivity
     * @param cb Function1<String, Unit>
     */
     fun getOnePic(activity: AppCompatActivity, cb: (String) -> Unit){
         val permissions = arrayOf(Permission.MANAGE_EXTERNAL_STORAGE, Permission.CAMERA)
         if (XXPermissions.isGranted(activity, permissions)) {
             getSinglePic(activity,cb)
         } else {
             XXPermissions.with(activity)
                 .permission(permissions)
                 .request(object : OnPermissionCallback {
                     override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                         if (all) {
                             getSinglePic(activity,cb)
                         } else {
                             Log.e("TAG","获取部分权限成功,但部分权限未正常授予")
                         }
                     }
                     override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                         if (never) {
                             // 如果是被永久拒绝就跳转到应用权限系统设置页面
                             XXPermissions.startPermissionActivity(activity, permissions)
                         } else {
                             Log.e("TAG","获取权限失败")
                         }
                     }
                 })
         }
    }


    /**
     * 选择单张图片
     * @param activity AppCompatActivity
     */
    private fun getSinglePic(activity: AppCompatActivity, cb: (String) -> Unit) {
        PictureSelector.create(activity)
            .openGallery(SelectMimeType.ofImage())
            .setImageEngine(GlideEngine.createGlideEngine())
            .setSelectionMode(SelectModeConfig.SINGLE)
            .setMaxSelectNum(1)
            .setImageSpanCount(3)
            .isDisplayCamera(true)// 是否显示拍照按钮
            .isPreviewImage(false)//不能预览，避免本来想选中，
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: ArrayList<LocalMedia>) {
                   if (result.size>0){
                       val media = result.first()
                       val filePath = if (media.isCompressed)media.compressPath else media.realPath
                       cb(filePath)
                   }

                }
                override fun onCancel() {

                }
            })
    }


    /**
     * 打印选择的文件信息
     * @param media LocalMedia
     */
    private fun printFileInfo(media: LocalMedia) {
        Log.i(TAG,
            "是否压缩:" + media.isCompressed +
                    "\n压缩:" + media.compressPath +
                    "\n原图:" + media.path +
                    "\n绝对路径:" + media.realPath +
                    "\n是否裁剪:" + media.isCut +
                    "\n裁剪:" + media.cutPath +
                    "\n是否开启原图:" + media.isOriginal +
                    "\n原图路径:" + media.originalPath +
                    "\n宽高: " + media.width + "x" + media.height +
                    "\n图片大小: " + media.size
        )
    }

}
