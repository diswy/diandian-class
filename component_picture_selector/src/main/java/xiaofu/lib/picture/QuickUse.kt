package xiaofu.lib.picture

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.fragment.app.Fragment
import com.qingmei2.rximagepicker.core.RxImagePicker
import com.qingmei2.rximagepicker_extension.MimeType
import com.qingmei2.rximagepicker_extension_zhihu.ZhihuConfigurationBuilder
import com.yalantis.ucrop.UCrop
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.*

/**
 *
 * Created by @author xiaofu on 2019/5/7.
 */

fun Activity.singleImagePicker(frag: Fragment): Disposable? {
    val imagePicker = RxImagePicker.create(ZhihuImagePicker::class.java)
    return imagePicker.openGalleryAsDracula(
            this,
            ZhihuConfigurationBuilder(MimeType.ofImage(), false)
                    .capture(true)
                    .maxSelectable(1)
                    .spanCount(4)
                    .theme(R.style.Zhihu_Dracula)
                    .build())
            .subscribe {
                val uCrop = UCrop.of(it.uri, Uri.fromFile(File(cacheDir, "${UUID.randomUUID()}.jpg")))
                val options = UCrop.Options()
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
                options.setCompressionQuality(90)
                options.setFreeStyleCropEnabled(true)
                options.setToolbarColor(0xff7FCCA1.toInt())
                options.setStatusBarColor(0xff7FCCA1.toInt())
                options.setActiveWidgetColor(0xff7FCCA1.toInt())
                options.setToolbarWidgetColor(0xffffffff.toInt())
                options.setActiveWidgetColor(0xff7FCCA1.toInt())
                options.setActiveControlsWidgetColor(0xff7FCCA1.toInt())
                uCrop.withOptions(options)
                uCrop.start(this, frag)
            }
}

fun Activity.singleImagePicker(): Disposable? {
    val imagePicker = RxImagePicker.create(ZhihuImagePicker::class.java)
    return imagePicker.openGalleryAsDracula(
            this,
            ZhihuConfigurationBuilder(MimeType.ofImage(), false)
                    .capture(true)
                    .maxSelectable(1)
                    .spanCount(4)
                    .theme(R.style.Zhihu_Dracula)
                    .build())
            .subscribe {
                val uCrop = UCrop.of(it.uri, Uri.fromFile(File(cacheDir, "${UUID.randomUUID()}.jpg")))
                val options = UCrop.Options()
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
                options.setCompressionQuality(90)
                options.setFreeStyleCropEnabled(true)
                options.setToolbarColor(0xff7FCCA1.toInt())
                options.setStatusBarColor(0xff7FCCA1.toInt())
                options.setActiveWidgetColor(0xff7FCCA1.toInt())
                options.setToolbarWidgetColor(0xffffffff.toInt())
                options.setActiveWidgetColor(0xff7FCCA1.toInt())
                options.setActiveControlsWidgetColor(0xff7FCCA1.toInt())
                uCrop.withOptions(options)
                uCrop.start(this)
            }
}