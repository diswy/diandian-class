package com.cqebd.live.ui.aty

import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityLockScreenBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import cqebd.student.BaseApp
import cqebd.student.commandline.Command
import cqebd.student.viewmodel.ClassViewModel
import cqebd.student.vo.MyIntents
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity

@Route(path = "/app/aty/lockScreen")
class LockScreenActivity : BaseBindActivity<ActivityLockScreenBinding>() {

    private val observer = Observer<String> {
        when (it) {
            Command.UNLOCK_SCREEN -> {
                this.finish()
            }
        }
    }

    private lateinit var viewModel: ClassViewModel

    override fun isFullScreen(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_lock_screen

    override fun initialize(binding: ActivityLockScreenBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observe(this, observer)


        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
        viewModel.getTime().observe(this, Observer {
            binding.lockScreenDatetime.text = it
        })
        mDisposablePool.add(viewModel.startTime())

        binding.lockScreenDate.text = viewModel.getStringData()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (MyIntents.classStatus) {
                toast("正在上课，请认真听讲")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
