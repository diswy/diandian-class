package com.cqebd.live.ui.adapter

import android.widget.EditText
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.live.R
import com.google.gson.Gson
import cqebd.student.vo.CAnswerCommit

/**
 *
 * Created by @author xiaofu on 2019/6/17.
 */
class BlankAdapter : BaseQuickAdapter<Int, BaseViewHolder>(R.layout.answer_item_blank) {

    private val etList = ArrayList<EditText>()
    private var enabled = true

    override fun setNewData(data: MutableList<Int>?) {
        super.setNewData(data)
        etList.clear()
    }

    override fun convert(helper: BaseViewHolder?, item: Int?) {
        helper ?: return

        val et: EditText = helper.getView(R.id.et_blank)
        etList.add(et)

        helper.setText(R.id.tv_indicator, "${helper.layoutPosition + 1}、")
    }

    // 刷新UI
    private fun setEnabled(enable: Boolean) {
        this.enabled = enable
        notifyDataSetChanged()
    }

    fun getBlankAnswerPack(): String {
        val answers = ArrayList<CAnswerCommit>()

        for (i in etList.indices) {
            answers.add(CAnswerCommit(i, etList[i].text.toString()))
        }

        return Gson().toJson(answers)
    }
}