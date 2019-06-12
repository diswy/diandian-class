package cqebd.student.commandline

/**
 *
 * Created by @author xiaofu on 2019/6/4.
 */

object Command {
    const val COMMAND = "COMMAND"
    const val END = "\r\n"

    const val CONNECT_IP = "CONNECT_IP"

    //--------S->T--------
    const val LOGIN_ROOM = "LOGIN_ROOM"
    const val STUDENT_INFO_UPDATE = "STUDENT_INFO_UPDATE"
    const val JSON_CLASS = "JSON_CLASS"
    const val ANSWER_SUBMIT = "ANSWER_SUBMIT"
    const val EAGER_ANSWER = "EAGER_ANSWER"// 抢答


    //--------T->S--------
    const val LOGIN_ROOM_RESULT = "LOGIN_ROOM_RESULT"
    const val ANSWER_START = "ANSWER_START"// 测试
    const val ANSWER_STOP = "ANSWER_STOP"// 测试结束
    const val EAGER_ANSWER_START = "EAGER_ANSWER_START"// 开始抢答
    const val EAGER_ANSWER_STOP = "EAGER_ANSWER_STOP"// 结束抢答
    const val EAGER_PRIZE = "EAGER_PRIZE"// 抢答结束，群发消息

    const val LOCK_SCREEN = "LOCK_SCREEN"// 锁屏
    const val UNLOCK_SCREEN = "UNLOCK_SCREEN"// 解锁
    const val SHUTDOWN = "SHUTDOWN"// 关机


}