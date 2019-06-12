package com.cqebd.live.ui

import android.view.View
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityRaceHandBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import com.orhanobut.logger.Logger
import cqebd.student.commandline.Command
import xiaofu.lib.base.activity.BaseBindActivity

@Route(path = "/app/race")
class RaceHandActivity : BaseBindActivity<ActivityRaceHandBinding>() {

    private val observer = Observer<String> {
        Logger.d(it)
        when (it) {
            Command.EAGER_ANSWER_START -> {
//                binding.containerMain.visibility = View.VISIBLE
//
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.container_main, raceHandFrag)
//                    .commitAllowingStateLoss()
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_race_hand

    override fun initialize(binding: ActivityRaceHandBinding) {
        LiveEventBus.get()
            .with(Command.COMMAND, String::class.java)
            .observe(this, observer)
    }

    override fun bindListener(binding: ActivityRaceHandBinding) {
        binding.studentResponderImg.setOnClickListener {
            LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .post("qiangda")
        }
    }
}
