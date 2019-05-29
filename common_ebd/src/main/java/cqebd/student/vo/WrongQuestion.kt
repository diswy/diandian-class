package cqebd.student.vo

data class WrongQuestion(
        val Name: String,
        val DateTime: String,
        val ErrorCount: Int,
        val SubjectTypeName: String,
        val PapersTypeName: String,
        val ExaminationPapersId: Int,
        val StudentQuestionsTasksID: Int
)