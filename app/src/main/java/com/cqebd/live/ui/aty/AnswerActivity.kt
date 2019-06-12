package com.cqebd.live.ui.aty

import android.view.KeyEvent
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityAnswerBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import cqebd.student.commandline.Command
import cqebd.student.vo.MyIntents
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity


@Route(path = "/app/aty/answer")
class AnswerActivity : BaseBindActivity<ActivityAnswerBinding>() {

    private val observer = Observer<String> {
        when (it) {
            Command.ANSWER_STOP -> {// 答题结束，如果学生此刻答案不为空的话可以帮助学生提交
                this.finish()
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_answer

    override fun initialize(binding: ActivityAnswerBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observe(this, observer)

    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (MyIntents.classStatus) {
                toast("返回后将无法答题，请不要点击返回键")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
