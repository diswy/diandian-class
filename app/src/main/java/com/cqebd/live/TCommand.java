package com.cqebd.live;

/**
 * Created by @author xiaofu on 2019/7/12.
 */
public class TCommand {
    private String cmd;
    private int pos;
    private int times;
    private int myPos;

    public TCommand(String cmd, int pos, int times,int myPos) {
        this.cmd = cmd;
        this.pos = pos;
        this.times = times;
        this.myPos = myPos;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getMyPos() {
        return myPos;
    }

    public void setMyPos(int myPos) {
        this.myPos = myPos;
    }
}
