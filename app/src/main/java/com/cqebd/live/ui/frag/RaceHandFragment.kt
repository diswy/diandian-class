package com.cqebd.live.ui.frag


import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.FragmentRaceHandBinding
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import cqebd.student.BaseApp
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.vo.User
import org.jetbrains.anko.toast
import xiaofu.lib.base.fragment.BaseBindFragment
import xiaofu.lib.cache.ACache
import xiaofu.lib.inline.loadUrl


/**
 * 抢答
 */
@Route(path = "/app/frag/race")
class RaceHandFragment : BaseBindFragment<FragmentRaceHandBinding>() {

    private lateinit var cache: ACache
    private lateinit var user: User

    private val observer = Observer<String> {
        Log.e("xiaofu", it)
        val mCommand = it.split(" ")

        try {
            when (mCommand[0]) {
                Command.EAGER_PRIZE -> {
                    val userString = cache.getAsString(CacheKey.KEY_USER) ?: return@Observer

                    user = Gson().fromJson(userString, User::class.java)

                    if (mCommand[2] == user.ID.toString()) {
                        raceSuccess(mCommand[3], mCommand[4])
                    } else {
                        raceFail(mCommand[2], mCommand[3], mCommand[4])
                    }

                }
                Command.EAGER_RESULT -> {
                    val subProgress = Integer.parseInt(mCommand[4])
                    val count = BaseApp.instance.kRespository.getPeople()
                    Log.e("xiaofu", "当前人数：$count,当前点赞人数：$subProgress")
                    binding.responderSuccessBePraised.max = count - 1
                    binding.responderSuccessBePraised.progress = subProgress
                    binding.tvSubProgress.text = "${subProgress}/${count - 1}"
                }
            }
        } catch (e: Exception) {

        }

    }

    override fun getLayoutRes(): Int = R.layout.fragment_race_hand

    override fun initialize(activity: FragmentActivity, binding: FragmentRaceHandBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observe(this, observer)

        cache = ACache.get(activity)
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

    private fun raceSuccess(name: String, head: String) {
        binding.studentResponderImg.visibility = View.GONE
        binding.hintTv.visibility = View.GONE

        binding.responderSuccessStuPhoto.visibility = View.VISIBLE
        binding.responderSuccessStuName.visibility = View.VISIBLE
        binding.responderSuccessImg.visibility = View.VISIBLE
        binding.responderSuccessLl.visibility = View.VISIBLE

        binding.responderSuccessStuName.text = name
        binding.responderSuccessStuPhoto.loadUrl(this, head)
    }

    private fun raceFail(id: String, name: String, head: String) {
        binding.studentResponderImg.visibility = View.GONE
        binding.hintTv.visibility = View.GONE

        binding.responderSuccessStuPhoto.visibility = View.VISIBLE
        binding.responderSuccessStuName.visibility = View.VISIBLE
        binding.btnSub.visibility = View.VISIBLE
        binding.responderFailPrise.visibility = View.VISIBLE

        binding.responderSuccessStuName.text = name
        binding.responderSuccessStuPhoto.loadUrl(this, head)

        binding.btnSub.setOnClickListener {
            if (::user.isInitialized) {
                val subFormat = "%s %s %s 1"//命令，自己ID，点赞学生ID，点赞
                LiveEventBus.get()
                        .with(Command.COMMAND, String::class.java)
                        .post(subFormat.format(Command.EAGER_PRAISE, user.ID, id))

                requireActivity().toast("点赞成功！")
                binding.btnSub.visibility = View.GONE
            }
        }
    }


}
