package cqebd.student.vo

/**
 *
 * Created by @author xiaofu on 2019/6/21.
 */
data class CAnswerCommit(val QuestionIndex: Int,
                         var Content: String,
                         val Video: String? = null)