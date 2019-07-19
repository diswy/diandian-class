package com.cqebd.live.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Space
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.live.R
import com.google.gson.Gson
import cqebd.student.vo.CAnswerCommit

/**
 *
 * Created by @author xiaofu on 2019/6/17.
 */
class MultipleAdapter : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.answer_item_multiple) {

    private var enabled = true
    private val cbList = ArrayList<ViewGroup>()

    override fun setNewData(data: MutableList<Int>?) {
        super.setNewData(data)
        cbList.clear()
    }

    override fun convert(helper: BaseViewHolder?, item: Int?) {
        helper ?: return

        helper.setText(R.id.tv_indicator, "${helper.layoutPosition + 1}、")

        cbList.add(helper.getView(R.id.ll_multiple))

        when (item) {
            3 -> {
                setVisibility(helper.getView<CheckBox>(R.id.cb_a), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_b), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_c), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_d), false)
                setVisibility(helper.getView<CheckBox>(R.id.cb_e), false)
                setVisibility(helper.getView<CheckBox>(R.id.cb_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), false)
                setVisibility(helper.getView<Space>(R.id.space_d), false)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            4 -> {
                setVisibility(helper.getView<CheckBox>(R.id.cb_a), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_b), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_c), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_d), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_e), false)
                setVisibility(helper.getView<CheckBox>(R.id.cb_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), false)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            5 -> {
                setVisibility(helper.getView<CheckBox>(R.id.cb_a), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_b), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_c), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_d), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_e), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_f), false)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), true)
                setVisibility(helper.getView<Space>(R.id.space_e), false)
            }
            6 -> {
                setVisibility(helper.getView<CheckBox>(R.id.cb_a), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_b), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_c), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_d), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_e), true)
                setVisibility(helper.getView<CheckBox>(R.id.cb_f), true)

                setVisibility(helper.getView<Space>(R.id.space_c), true)
                setVisibility(helper.getView<Space>(R.id.space_d), true)
                setVisibility(helper.getView<Space>(R.id.space_e), true)
            }
        }
    }

    // 刷新UI
    private fun setEnabled(enable: Boolean) {
        this.enabled = enable
        notifyDataSetChanged()
    }

    fun getMultipleAnswerPack(): String {
        val allAnswer = ArrayList<CAnswerCommit>()
        for (i in cbList.indices) {
//            val list = ArrayList<String>()
            var array = ""

            for (j in 0..cbList[i].childCount) {
                if (cbList[i].getChildAt(j) is CheckBox) {
                    val tempCb = cbList[i].getChildAt(j) as CheckBox
                    if (tempCb.isChecked) {
                        array += tempCb.text.toString()
                    }
                }
            }

            allAnswer.add(CAnswerCommit(i, array))
        }


//        val allAnswer = ArrayList<List<String>>()
//        for (i in cbList.indices) {
//            val list = ArrayList<String>()
//
//            for (j in 0..cbList[i].childCount) {
//                if (cbList[i].getChildAt(j) is CheckBox) {
//                    val tempCb = cbList[i].getChildAt(j) as CheckBox
//                    if (tempCb.isChecked) {
//                        list.add(tempCb.text.toString())
//                    }
//                }
//            }
//
//            allAnswer.add(list)
//        }
        return Gson().toJson(allAnswer)

    }

    private fun setVisibility(view: View, bool: Boolean) {
        view.visibility = if (bool) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}