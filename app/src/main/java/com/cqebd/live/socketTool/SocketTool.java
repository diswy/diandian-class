package com.cqebd.live.socketTool;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import cqebd.student.commandline.Command;
import cqebd.student.tools.ByteTools;
import xiaofu.lib.tools.GlideApp;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * Created by @author xiaofu on 2019/6/25.
 */
public class SocketTool {
    public static boolean isContinue = true;
    public static String format = "命令名称：%s;文件大小:%d;文件名称:%d";

    public static void connect(String ip, String path, Activity ctx, ImageView iv) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Log.e("new-socket", "线程开启");

                try {
                    File directory = new File(path + "/picpic");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
//                    File imgFile = new File(path+"/picpic/screenshot.png");

//                    byte[] cmdRequest = String.format("%-16s", Command.SCREENS_REQUEST).getBytes();
//                    byte[] cmdEnd = ByteTools.intToByteArray(0);
//                    byte[] cmd = new byte[cmdRequest.length + cmdEnd.length];
//                    System.arraycopy(cmdRequest, 0, cmd, 0, cmdRequest.length);
//                    System.arraycopy(cmdEnd, 0, cmd, cmdRequest.length, cmdRequest.length);

                    byte[] cmd = KTool.INSTANCE.getByte();


                    SocketAddress socketAddress = new InetSocketAddress("192.168.1.124", 2021);
                    Socket socket = new Socket();
                    // 3s timeout
                    socket.connect(socketAddress, 3000);
                    Log.e("new-socket", "连接成功");
//                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    OutputStream os = socket.getOutputStream();
                    InputStream is = socket.getInputStream();
                    Log.e("new-socket", "创建成功");
                    //写入要发送给服务器的数据
                    os.write(cmd);
                    os.flush();
                    Log.e("new-socket", "已发送请求远程桌面");
//                    socket.shutdownOutput();

                    while (isContinue) {
                        Thread.sleep(1);
                        Log.e("new-socket", "等待读取");

                        FileOutputStream fos = null;
                        byte buffer[] = new byte[24];
                        int read = 0;
                        read = is.read(buffer, 0, 24);
                        String command = new String(buffer, 0, 16, Charset.forName("UTF-8")).trim();
                        byte[] lenByte = new byte[4];
                        System.arraycopy(buffer, 16, lenByte, 0, 4);
                        int fileSize = ByteTools.bytes2Int(lenByte);
                        byte[] numberByte = new byte[4];
                        System.arraycopy(buffer, 20, numberByte, 0, 4);
                        int number = ByteTools.bytes2Int(numberByte);

                        Log.e("new-socket", String.format(format,command, fileSize, number));// Info

                        if (command.equals(Command.SCREENS_RESPONSE)){
                            int remaining = fileSize;
                            read = 0;
                            File imgFile = new File(path + "/picpic/" + number + ".png");
                            fos = new FileOutputStream(imgFile);

                            byte imgBuffer[] = new byte[4096];
                            while ((read = is.read(imgBuffer, 0, Math.min(imgBuffer.length, remaining))) > 0) {
                                remaining -= read;
                                fos.write(imgBuffer, 0, read);
                            }
                            Log.e("new-socket","文件已输出");
                            fos.close();
                            fos = null;

                            ctx.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    GlideApp.with(ctx)
                                            .load(path + "/picpic/" + number + ".png")
                                            .into(iv);
                                }
                            });

                        }


                    }


//                    //解析服务器返回的数据
//                    InputStreamReader reader = new InputStreamReader(is);
//                    BufferedReader bufReader = new BufferedReader(reader);
//                    String s = null;
//                    final StringBuffer sb = new StringBuffer();
//                    while ((s = bufReader.readLine()) != null) {
//                        sb.append(s);
//                    }
//                    //3、关闭IO资源（注：实际开发中需要放到finally中）
//                    bufReader.close();
//                    reader.close();
                    os.close();
                    is.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("new-socket","IO异常:"+e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {

                }
            }
        }.start();
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 50, baos);
        return baos.toByteArray();
    }
}
