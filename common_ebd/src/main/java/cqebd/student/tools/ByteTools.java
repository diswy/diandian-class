package cqebd.student.tools;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by @author xiaofu on 2019/6/24.
 */
public class ByteTools {
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    public static int bytes2Int(byte[] bytes) {
        int int1 = bytes[0] & 0xff;
        int int2 = (bytes[1] & 0xff) << 8;
        int int3 = (bytes[2] & 0xff) << 16;
        int int4 = (bytes[3] & 0xff) << 24;

        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil1to4(byte[] bytes) {
        int int1 = bytes[0] & 0xff;
        int int2 = (bytes[1] & 0xff) << 8;
        int int3 = (bytes[2] & 0xff) << 16;
        int int4 = (bytes[3] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil5to8(byte[] bytes) {
        int int1 = bytes[4] & 0xff;
        int int2 = (bytes[5] & 0xff) << 8;
        int int3 = (bytes[6] & 0xff) << 16;
        int int4 = (bytes[7] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil17to20(byte[] bytes) {
        int int1 = bytes[16] & 0xff;
        int int2 = (bytes[17] & 0xff) << 8;
        int int3 = (bytes[18] & 0xff) << 16;
        int int4 = (bytes[19] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil21to24(byte[] bytes) {
        int int1 = bytes[20] & 0xff;
        int int2 = (bytes[21] & 0xff) << 8;
        int int3 = (bytes[22] & 0xff) << 16;
        int int4 = (bytes[23] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil25to28(byte[] bytes) {
        int int1 = bytes[24] & 0xff;
        int int2 = (bytes[25] & 0xff) << 8;
        int int3 = (bytes[26] & 0xff) << 16;
        int int4 = (bytes[27] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    public static int bytesArray2IntUntil29to32(byte[] bytes) {
        int int1 = bytes[28] & 0xff;
        int int2 = (bytes[29] & 0xff) << 8;
        int int3 = (bytes[30] & 0xff) << 16;
        int int4 = (bytes[31] & 0xff) << 24;
        return int1 | int2 | int3 | int4;
    }

    /**
     * int转换为小端byte[]（高位放在高地址中）
     *
     * @param iValue
     * @return
     */
    public static byte[] Int2Bytes_LE(int iValue) {
        byte[] rst = new byte[4];
        // 先写int的最后一个字节
        rst[0] = (byte) (iValue & 0xFF);
        // int 倒数第二个字节
        rst[1] = (byte) ((iValue & 0xFF00) >> 8);
        // int 倒数第三个字节
        rst[2] = (byte) ((iValue & 0xFF0000) >> 16);
        // int 第一个字节
        rst[3] = (byte) ((iValue & 0xFF000000) >> 24);
        return rst;
    }

    /**
     * 将byte数组转换为表示16进制值的字符串， 如：byte[]{8,18}转换为：0813， 和public static byte[]
     * hexStrToByteArr(String strIn) 互为可逆的转换过程
     *
     * @param arrB 需要转换的byte数组
     * @return 转换后的字符串
     * @throws Exception 本方法不处理任何异常，所有异常全部抛出
     */
    public static String byteArrToHexStr(byte[] arrB) throws Exception {
        int iLen = arrB.length; // 每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍
        StringBuffer sb = new StringBuffer(iLen * 2);
        for (int b : arrB) {
            int intTmp = b; // 把负数转换为正数
            while (intTmp < 0) {
                intTmp = intTmp + 256;
            } // 小于0F的数需要在前面补0
            if (intTmp < 16) {
                sb.append("0");
            }
            sb.append(Integer.toString(intTmp, 16));
        }
        return sb.toString();
    }

    public static void saveBitmap(byte[] b, String path, int name) {
        try {
            File directory = new File(path + "/picpic");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(path + "/picpic/" + name + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(b, 0, b.length);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e("command", "发生了异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
