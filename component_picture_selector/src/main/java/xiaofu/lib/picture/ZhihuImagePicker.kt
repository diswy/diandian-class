package xiaofu.lib.picture

import android.content.Context
import com.qingmei2.rximagepicker.entity.Result
import com.qingmei2.rximagepicker.entity.sources.Camera
import com.qingmei2.rximagepicker.entity.sources.Gallery
import com.qingmei2.rximagepicker.ui.ICustomPickerConfiguration
import com.qingmei2.rximagepicker_extension_zhihu.ui.ZhihuImagePickerActivity
import io.reactivex.Observable

/**
 *
 * Created by @author xiaofu on 2019/5/7.
 */
interface ZhihuImagePicker {
    @Gallery(componentClazz = ZhihuImagePickerActivity::class,
            openAsFragment = false)
    fun openGalleryAsNormal(context: Context,
                            config: ICustomPickerConfiguration): Observable<Result> //日间主题

    @Gallery(componentClazz = ZhihuImagePickerActivity::class,
            openAsFragment = false)
    fun openGalleryAsDracula(context: Context,
                             config: ICustomPickerConfiguration): Observable<Result> //夜间主题

    @Camera
    fun openCamera(context: Context): Observable<Result> //打开相机
}