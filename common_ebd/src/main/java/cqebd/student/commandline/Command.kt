package cqebd.student.commandline

/**
 *
 * Created by @author xiaofu on 2019/6/4.
 */

object Command {
    const val COMMAND = "COMMAND"
    const val END = "\r\n"
    const val CLASS_END = "CLASS_END"

    const val CONNECT_IP = "CONNECT_IP"

    //--------S->T--------
    const val LOGIN_ROOM = "LOGIN_ROOM"
    const val STUDENT_INFO_UPDATE = "STUDENT_INFO_UPDATE"
    const val JSON_CLASS = "JSON_CLASS"
    const val ANSWER_SUBMIT = "ANSWER_SUBMIT"
    const val EAGER_ANSWER = "EAGER_ANSWER"// 抢答
    const val EAGER_PRAISE = "EAGER_PRAISE"// 点赞
    const val EAGER_RESULT = "EAGER_RESULT"// 点赞结果

    const val PRAISE = "PRAISE"// 累计点赞
    const val SYSTEM_CONFIG = "SYSTEM_CONFIG"// 全局配置

    const val SCREENS_REQUEST = "SCREENS_REQUEST"// 请求远程桌面
    const val SCREENS_RESPONSE = "SCREEN_RESPONSE"// 收到远程桌面
    const val SHARE_HOT = "SHARE_HOT"// 发送

    const val SHARE_DESKTOP = "SHARE_DESKTOP"// 学生飞屏开始
    const val SHARE_DESK_STOP = "SHARE_DESK_STOP"// 学生飞屏结束

    const val MOUSE_CLICK = "MOUSE_CLICK"// 远程控制 单点
    const val MOUSE_MOVE = "MOUSE_MOVE"// 远程控制 单点
    const val MOUSE_DOUBLE = "MOUSE_DOUBLE"// 远程控制 单点
    const val MOUSE_UP = "MOUSE_UP"
    const val MOUSE_DOWN = "MOUSE_DOWN"

    const val ANSWER_PIC = "ANSWER_PIC"// 远程控制 发送图片


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

    const val LOGIN_INFO_UPDATE = "ROOM_MEMBER_UPDATE"// 当前人数
    const val LOGIN_INFO_ADD = "ROOM_MEMBER_ADD"// 新进房间人数，每次+1
    const val LOGIN_INFO_REMOVE = "ROOM_MEMBER_REMOVE"// 离开房间人数，每次-1


    const val BROADCAST = "BROADCAST"// 老师广播桌面
    const val BROADCAST_STOP = "BROADCAST_STOP"// 停止广播

    const val DEMON_START = "DEMON_START"// 学生演示
    const val DEMON_STOP = "DEMON_STOP"// 学生演示结束

    const val SEND_FILE = "SEND_FILE"// 发送文件


    //--------内部通信--------

}