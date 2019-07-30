package com.cqebd.live.ui.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.cqebd.live.R
import com.cqebd.live.vo.ClassingRoomInfo
import cqebd.student.commandline.CacheKey
import xiaofu.lib.cache.ACache

/**
 * 发现教室
 * Created by @author xiaofu on 2019/7/29.
 */
class DiscoverAdapter(private val ip: String?) : BaseQuickAdapter<ClassingRoomInfo, BaseViewHolder>(R.layout.item_discover_room) {

    override fun convert(helper: BaseViewHolder?, item: ClassingRoomInfo?) {
        helper ?: return
        item ?: return
        helper.setText(R.id.room_name, item.roomName)
                .setText(R.id.room_ip, "ip：${item.ip}")
                .setVisible(R.id.tv_is_current, item.ip == ip)
    }
}