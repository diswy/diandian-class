package cqebd.student.vo

import com.google.gson.annotations.SerializedName

/**
 * 作业列表信息
 * Created by @author xiaofu on 2019/2/21.
 */
data class WorkInfo(
        @SerializedName("StudentQuestionsTasksID")
        val TaskId: Long,
        @SerializedName("ExaminationPapersId")
        val PapersId: Long,
        @SerializedName("ExaminationPapersPushId")
        val PushId: Long,
        @SerializedName("ExaminationPapersTypeId")
        val TypeId: Int,
        @SerializedName("PapersTypeName")
        val TypeName: String,
        var Status: Int,
        val IsTasks: Boolean,
        val Name: String,
        @SerializedName("SubjectTypeId")
        val SubjectId: Int,
        @SerializedName("SubjectTypeName")
        val SubjectName: String,
        val CanStartDateTime: String,
        var StartTime: String?,
        val CanEndDateTime: String,
        val EndTime: String?,
        val Count: Int,
        var Duration: Int,
        @SerializedName("PuchDateTime")
        var publishTime: String,
        @SerializedName("ExaminationPapersAttachment")
        val attachments: List<Attachment>?,
        val Fraction: Double?,
        val IsMedal: Boolean
)