package com.cqebd.live.ui.aty

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import cqebd.student.viewmodel.ClassViewModel

@Route(path = "/app/aty/lockScreen")
class LockScreenActivity : AppCompatActivity() {
    private lateinit var viewModel: ClassViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)
    }
}
