package xiaofu.lib.ui

import com.alibaba.android.arouter.facade.annotation.Route
import com.github.chrisbanes.photoview.PhotoView
import xiaofu.lib.base.R
import xiaofu.lib.base.activity.BaseActivity
import xiaofu.lib.inline.loadUrl


/**
 * 简单的单图片预览界面，直接使用PictureSelector中的PhotoView
 */
@Route(path = "/module_base/photo_preview/simple_single")
class SinglePreviewActivity : BaseActivity() {

    private lateinit var photoView: PhotoView

    override fun getLayoutRes(): Int = R.layout.activity_single_preview

    override fun isTranslucentMode(): Boolean {
        return true
    }

    override fun initialize() {
        val imgUrl = intent.getStringExtra("imgUrl")

        photoView = findViewById(R.id.preview_photo_view)
        photoView.loadUrl(this, imgUrl)
    }

    override fun bindListener() {
        photoView.setOnClickListener {
            onBackPressed()
        }
    }

}
