package com.cqebd.live

import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.databinding.ActivityMainBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import com.orhanobut.logger.Logger
import cqebd.student.BaseApp
import cqebd.student.commandline.Command
import cqebd.student.viewmodel.ClassViewModel
import xiaofu.lib.base.activity.BaseBindActivity

@Route(path = "/app/main")
class MainActivity : BaseBindActivity<ActivityMainBinding>() {

    private lateinit var viewModel: ClassViewModel

    private val raceHandFrag by lazy { navigationAsFrag("/app/frag/race") }

    private val observer = Observer<String> {
        Logger.d(it)
        when (it) {
            Command.EAGER_ANSWER_START -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.container_main, raceHandFrag)
                        .commitAllowingStateLoss()
            }
            "123" -> {
                supportFragmentManager.beginTransaction()
                        .remove(raceHandFrag)
                        .commitAllowingStateLoss()
            }
        }
    }

    override fun isFullScreen(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_main

    override fun initialize(binding: ActivityMainBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observeForever(observer)

        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
        viewModel.getTime().observe(this, Observer {
            Log.d("xiaofu", it)
        })

        viewModel.startTime()
    }


}
