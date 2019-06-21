package com.cqebd.live.ui.adapter

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Space
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.live.R
import com.google.gson.Gson
import cqebd.student.vo.CAnswerCommit
import java.lang.Exception

/**
 *
 * Created by @author xiaofu on 2019/6/14.
 */
class SingleChoiceAdapter : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.answer_item_single) {

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

    override fun convert(helper: BaseViewHolder?, item: Int) {
        helper ?: return

        helper.setText(R.id.tv_indicator, "${helper.layoutPosition + 1}、")

        when (item) {
            3 -> {
                setVisibility(helper.getView<RadioButton>(R.id.single_a), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_b), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_c), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_d), false)
                setVisibility(helper.getView<RadioButton>(R.id.single_e), false)
                setVisibility(helper.getView<RadioButton>(R.id.single_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), false)
                setVisibility(helper.getView<Space>(R.id.space_d), false)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            4 -> {
                setVisibility(helper.getView<RadioButton>(R.id.single_a), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_b), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_c), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_d), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_e), false)
                setVisibility(helper.getView<RadioButton>(R.id.single_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), false)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            5 -> {
                setVisibility(helper.getView<RadioButton>(R.id.single_a), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_b), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_c), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_d), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_e), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), true)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            6 -> {
                setVisibility(helper.getView<RadioButton>(R.id.single_a), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_b), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_c), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_d), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_e), true)
                setVisibility(helper.getView<RadioButton>(R.id.single_f), true)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), true)
                setVisibility(helper.getView<Space>(R.id.space_e), true)
            }
        }

        val radioGroup: RadioGroup = helper.getView(R.id.rg_single) ?: return
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            try {
                when (checkedId) {
                    R.id.single_a -> answers[helper.layoutPosition].Content = "A"
                    R.id.single_b -> answers[helper.layoutPosition].Content = "B"
                    R.id.single_c -> answers[helper.layoutPosition].Content = "C"
                    R.id.single_d -> answers[helper.layoutPosition].Content = "D"
                    R.id.single_e -> answers[helper.layoutPosition].Content = "E"
                    R.id.single_f -> answers[helper.layoutPosition].Content = "F"
                }
            } catch (e: Exception) {

            }
        }

        helper.setEnabled(R.id.single_a, enabled)
        helper.setEnabled(R.id.single_b, enabled)
        helper.setEnabled(R.id.single_c, enabled)
        helper.setEnabled(R.id.single_d, enabled)
        helper.setEnabled(R.id.single_e, enabled)
        helper.setEnabled(R.id.single_f, enabled)
    }

    // 获取打包答案
    fun getSingleAnswerPack(): String {
        return Gson().toJson(answers)
    }

    // 刷新UI
    private fun setEnabled(enable: Boolean) {
        this.enabled = enable
        notifyDataSetChanged()
    }

    private fun setVisibility(view: View, bool: Boolean) {
        view.visibility = if (bool) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}