package com.cqebd.live.ui.frag


import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.FragmentRaceHandBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import com.orhanobut.logger.Logger
import cqebd.student.commandline.Command
import xiaofu.lib.base.fragment.BaseBindFragment


/**
 * 抢答
 */
@Route(path = "/app/frag/race")
class RaceHandFragment : BaseBindFragment<FragmentRaceHandBinding>() {

    private val observer = Observer<String> {
        Log.e("xiaofu", it)
        val mCommand = it.split(" ")

        when (mCommand[0]) {
            Command.EAGER_PRIZE -> {
                try {
                    if (mCommand[1] == "1") {
                        raceSuccess()
                    }
                    if (mCommand[1] == "2") {
                        raceFail()
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.fragment_race_hand

    override fun initialize(activity: FragmentActivity, binding: FragmentRaceHandBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observe(this, observer)
    }

    override fun bindListener(activity: FragmentActivity, binding: FragmentRaceHandBinding) {
        binding.studentResponderImg.setOnClickListener {
            LiveEventBus.get()
                    .with(Command.COMMAND, String::class.java)
                    .post(Command.EAGER_ANSWER)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .removeObserver(observer)
    }

    private fun raceSuccess() {
        binding.studentResponderImg.visibility = View.GONE
        binding.hintTv.visibility = View.GONE

        binding.responderSuccessStuPhoto.visibility = View.VISIBLE
        binding.responderSuccessStuName.visibility = View.VISIBLE
        binding.responderSuccessImg.visibility = View.VISIBLE
        binding.responderSuccessBePraised.visibility = View.VISIBLE
    }

    private fun raceFail() {
        binding.studentResponderImg.visibility = View.GONE
        binding.hintTv.visibility = View.GONE

        binding.responderSuccessStuPhoto.visibility = View.VISIBLE
        binding.responderSuccessStuName.visibility = View.VISIBLE
        binding.btnSub.visibility = View.VISIBLE
        binding.responderFailPrise.visibility = View.VISIBLE
    }


}
