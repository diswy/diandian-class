package com.cqebd.live;

/**
 * Created by @author xiaofu on 2019/7/12.
 */
public class TImg {
    private int picPos;
    private int currentPos;
    private int myPos;

    public TImg(int picPos, int currentPos,int myPos) {
        this.picPos = picPos;
        this.currentPos = currentPos;
        this.myPos = myPos;
    }

    public int getPicPos() {
        return picPos;
    }

    public void setPicPos(int picPos) {
        this.picPos = picPos;
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public void setCurrentPos(int currentPos) {
        this.currentPos = currentPos;
    }

    public int getMyPos() {
        return myPos;
    }

    public void setMyPos(int myPos) {
        this.myPos = myPos;
    }
}
