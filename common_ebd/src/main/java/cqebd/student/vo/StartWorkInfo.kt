package cqebd.student.vo

/**
 * 开始做作业返回的信息
 * Created by @author xiaofu on 2019/3/21.
 */
data class StartWorkInfo(
        val startDate: String,// 开始答题时间
        val submitMode: Int// 提交模式
)