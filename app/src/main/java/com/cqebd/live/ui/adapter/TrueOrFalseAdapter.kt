package com.cqebd.live.ui.adapter

import android.widget.RadioGroup
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.live.R
import com.google.gson.Gson
import cqebd.student.vo.CAnswerCommit
import java.lang.Exception

/**
 *
 * Created by @author xiaofu on 2019/6/17.
 */
class TrueOrFalseAdapter : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.answer_item_true_or_false){

    private val answers = ArrayList<CAnswerCommit>()
    private var enabled = true

    override fun setNewData(data: MutableList<Int>?) {
        super.setNewData(data)
        answers.clear()
        data?.let {
            for (i in data.indices) {
                answers.add(CAnswerCommit(i, ""))
            }
        }
    }

    override fun convert(helper: BaseViewHolder?, item: Int?) {
        helper ?: return

        helper.setText(R.id.tv_indicator, "${helper.layoutPosition + 1}、")


        val radioGroup: RadioGroup = helper.getView(R.id.rg_single) ?: return
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            try {
                when (checkedId) {
                    R.id.true_or_false_true -> answers[helper.layoutPosition].Content = "TRUE"
                    R.id.true_or_false_false -> answers[helper.layoutPosition].Content = "FALSE"
                }
            } catch (e: Exception) {

            }
        }

        helper.setEnabled(R.id.true_or_false_true, enabled)
        helper.setEnabled(R.id.true_or_false_false, enabled)

    }

    // 获取打包答案
    fun getTrueOrFalseAnswerPack():String {
        return Gson().toJson(answers)
    }

    // 刷新UI
    private fun setEnabled(enable:Boolean){
        this.enabled = enable
        notifyDataSetChanged()
    }
}