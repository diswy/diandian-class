package xiaofu.lib.picture

import android.content.Context
import com.qingmei2.rximagepicker.entity.Result
import com.qingmei2.rximagepicker.entity.sources.Camera
import com.qingmei2.rximagepicker.entity.sources.Gallery
import io.reactivex.Observable

/**
 *
 * Created by @author xiaofu on 2019/5/7.
 */
interface MyImagePicker {
    @Gallery
    fun openGallery(context: Context): Observable<Result>

    @Camera
    fun openCamera(context: Context): Observable<Result>
}