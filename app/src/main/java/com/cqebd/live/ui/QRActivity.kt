package com.cqebd.live.ui

import com.cqebd.live.R
import com.king.zxing.CaptureFragment
import xiaofu.lib.base.activity.BaseActivity

class QRActivity : BaseActivity() {

    override fun getLayoutRes(): Int = R.layout.activity_qr

    override fun initialize() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.qr_container, CaptureFragment.newInstance())
                .commit()
    }


}


