package cqebd.student.vo

/**
 * 试题信息
 * Created by @author xiaofu on 2019/3/21.
 */
data class ExamPaper(
        val Id: Int,
        val Name: String,
        val SubjectTypeName: String,
        val SubjectTypeId: Int,
        val Count: Int,
        val QuestionGruop: List<QuestionGroup> = ArrayList()
)

data class QuestionGroup(
        val Id: Int,
        val Name: String,
        val Question: List<QuestionInfo> = ArrayList()
)

data class QuestionInfo(
        val Id: Int,
        val QuestionTypeId: Int,
        val AlternativeContent: String,
        val AnswerType: String,
        val Fraction: Float,
        val Sort: Int,
        val StudentsAnswer: String?,
        val WriteType: Int?,
        val QuestionSubjectAttachment: List<Attachment> = ArrayList()
)