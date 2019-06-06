package cqebd.student.commandline

/**
 *
 * Created by @author xiaofu on 2019/6/4.
 */

object Command {
    const val COMMAND = "COMMAND"

    //--------S->T--------
    const val LOGIN_ROOM = "LOGIN_ROOM"
    const val STUDENT_INFO_UPDATE = "STUDENT_INFO_UPDATE"
    const val JSON_CLASS = "JSON_CLASS"
    const val ANSWER_SUBMIT = "ANSWER_SUBMIT"


    //--------T->S--------
    const val LOGIN_ROOM_RESULT = "LOGIN_ROOM_RESULT"
    const val ANSWER_START = "ANSWER_START"
    const val ANSWER_STOP = "ANSWER_STOP"
    const val EAGER_ANSWER_START = "EAGER_ANSWER_START"
    const val LOCK_SCREEN = "LOCAK_SCREEN"

}