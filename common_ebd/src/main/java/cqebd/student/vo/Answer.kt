package cqebd.student.vo

data class AlternativeContent(val Id: String, val content: String)// 服务器答案选项

data class StudentAnswer(var Answer: String, val Id: Int, val TypeId: Int)

data class AnswerType(val Content: String, val Id: String, val TypeId: Int)

data class PackageAnswerCommon(
        val data: PackageAnswer,
        val name: String = "answer",
        val version: Double = 1.1)

data class PackageAnswer(
        val Stu_Id: Int,
        val TaskId: Long,
        val Status: Int,//-1 默认,0做题中,1交卷
        val ExaminationPapersPushId: Long,
        val ExaminationPapersId: Long,
        val Version: String,
        val Common: String,
        val Source: String,
        val AnswerList: List<Answer>)

data class Answer(
        val QuestionId: Int,
        val QuestionTypeId: Int,
        val Answer: String)