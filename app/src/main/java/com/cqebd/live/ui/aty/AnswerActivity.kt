package com.cqebd.live.ui.aty

import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityAnswerBinding
import com.cqebd.live.ui.adapter.BlankAdapter
import com.cqebd.live.ui.adapter.MultipleAdapter
import com.cqebd.live.ui.adapter.SingleChoiceAdapter
import com.cqebd.live.ui.adapter.TrueOrFalseAdapter
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import com.orhanobut.logger.Logger
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.vo.CAnswerInfo
import cqebd.student.vo.CAnswerType
import cqebd.student.vo.MyIntents
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.inline.loadUrl
import xiaofu.lib.utils.base64
import xiaofu.lib.utils.base64ToS


@Route(path = "/app/aty/answer")
class AnswerActivity : BaseBindActivity<ActivityAnswerBinding>() {

    @Autowired
    @JvmField
    var commands: String? = null

    private lateinit var singleAdapter: SingleChoiceAdapter
    private lateinit var multipleAdapter: MultipleAdapter
    private lateinit var trueOrFalseAdapter: TrueOrFalseAdapter
    private lateinit var blankAdapter: BlankAdapter
    private var currentType = -1
    private var userId = -1

    private val observer = Observer<String> {
        val commands = it.split(" ")
        when (commands[0]) {
            Command.ANSWER_STOP -> {// 答题结束，如果学生此刻答案不为空的话可以帮助学生提交
                this.finish()
            }
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_answer

    override fun initialize(binding: ActivityAnswerBinding) {
        ARouter.getInstance().inject(this)
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observe(this, observer)

        val cache = ACache.get(this)
        val ids = cache.getAsString(CacheKey.KEY_ID)
        try {
            userId = Integer.parseInt(ids)
        } catch (e: Exception) {

        }

        binding.rv.layoutManager = LinearLayoutManager(this)

        commands?.let {
            Log.i("xiaofu", it)
            val mCommands = it.split(" ")

            try {
                Log.wtf("xiaofu", mCommands[2].base64ToS())
                val mAnswerInfo: CAnswerInfo = Gson().fromJson(mCommands[2].base64ToS(), CAnswerInfo::class.java)
                // 加载图片答题卡
                binding.ivQuestion.loadUrl(this, mAnswerInfo.QuestionDesc)

                val options = ArrayList<Int>()// 选项数
                for (i in 1..mAnswerInfo.QuestionCount) {
                    options.add(mAnswerInfo.QuestionOptions)
                }

                currentType = mAnswerInfo.QuestionType
                when (mAnswerInfo.QuestionType) {
                    CAnswerType.SINGLE.value -> {
                        binding.tvTitle.text = "单选题"
                        singleAdapter = SingleChoiceAdapter()
                        binding.rv.adapter = singleAdapter
                        singleAdapter.setNewData(options)
                    }
                    CAnswerType.MULTIPLE.value -> {
                        binding.tvTitle.text = "多选题"
                        multipleAdapter = MultipleAdapter()
                        binding.rv.adapter = multipleAdapter
                        multipleAdapter.setNewData(options)
                    }
                    CAnswerType.TRUE_OR_FALSE.value -> {
                        binding.tvTitle.text = "判断题"
                        trueOrFalseAdapter = TrueOrFalseAdapter()
                        binding.rv.adapter = trueOrFalseAdapter
                        trueOrFalseAdapter.setNewData(options)
                    }
                    CAnswerType.BLANK.value -> {
                        binding.tvTitle.text = "填空题"
                        blankAdapter = BlankAdapter()
                        binding.rv.adapter = blankAdapter
                        blankAdapter.setNewData(options)
                    }
                    CAnswerType.SUBJECTIVE.value -> {
                        binding.tvTitle.text = "主观题"

                    }
                    else -> {
                        toast("没有对应类型的题目")
                    }
                }


            } catch (e: Exception) {
                Log.e("xiaofu", e.message)
                toast("数据解析失败")
            }

        }

    }

    override fun bindListener(binding: ActivityAnswerBinding) {
        binding.btnCommit.setOnClickListener {
            val answerCommit = "%s %d %s"
            var content = ""
            when (currentType) {
                CAnswerType.SINGLE.value -> {
                    content = singleAdapter.getSingleAnswerPack()
                }
                CAnswerType.MULTIPLE.value -> {
                    content = multipleAdapter.getMultipleAnswerPack()
                }
                CAnswerType.TRUE_OR_FALSE.value -> {
                    content = trueOrFalseAdapter.getTrueOrFalseAnswerPack()
                }
                CAnswerType.BLANK.value -> {
                    content = blankAdapter.getBlankAnswerPack()
                }
                CAnswerType.SUBJECTIVE.value -> {

                }
            }


//            LiveEventBus.get()
//                .with(Command.COMMAND, String::class.java)
//                .post(answerCommit.format(Command.ANSWER_SUBMIT, content))
//            Logger.d(answerCommit.format(Command.ANSWER_SUBMIT, content).base64())

            LiveEventBus.get()
                    .with(Command.COMMAND, String::class.java)
                    .post(answerCommit.format(Command.ANSWER_SUBMIT, userId, content.base64().replace("[\\s*\t\n\r]".toRegex(), "")))


        }
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
