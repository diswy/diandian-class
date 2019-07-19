package com.cqebd.live;

/**
 * Created by @author xiaofu on 2019/7/10.
 */
public class UDPImg {
    private int currentPic;
    private int pos;
    private byte[] realImgArray;

    public UDPImg(int currentPic, int pos, byte[] byteArray) {
        this.currentPic = currentPic;
        this.pos = pos;
        this.realImgArray = byteArray;
    }

    public int getCurrentPic() {
        return currentPic;
    }

    public void setCurrentPic(int currentPic) {
        this.currentPic = currentPic;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public byte[] getByteArray() {
        return realImgArray;
    }

    public void setByteArray(byte[] byteArray) {
        this.realImgArray = byteArray;
    }
}
