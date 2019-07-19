package com.cqebd.live.ui.aty

import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityRemotePlayerBinding
import xiaofu.lib.base.activity.BaseBindActivity

@Route(path = "/app/aty/remote_player")
class RemotePlayerActivity : BaseBindActivity<ActivityRemotePlayerBinding>() {
    private val source = "udp://@239.0.0.1:6666"

    override fun getLayoutRes(): Int = R.layout.activity_remote_player

    override fun initialize(binding: ActivityRemotePlayerBinding) {
//        lifecycle.addObserver(binding.player)
//        binding.player.playNoController("udp://@239.0.0.1:6666")

//        val options = ArrayList<String>()
//        options.add("-vvv")
//        binding.player2.setUp(source,true,"测试")
//        binding.player2.startPlayLogic()

    }

}
