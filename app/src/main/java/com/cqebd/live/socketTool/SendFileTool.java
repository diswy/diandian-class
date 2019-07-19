//package com.cqebd.live.socketTool;
//
//import android.annotation.SuppressLint;
//import android.util.SparseArray;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.ObjectOutputStream;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.util.HashMap;
//
//import io.reactivex.Flowable;
//import io.reactivex.Scheduler;
//import io.reactivex.disposables.Disposable;
//import io.reactivex.functions.Consumer;
//import io.reactivex.schedulers.Schedulers;
//
///**
// * Created by @author xiaofu on 2019/6/26.
// */
//public class SendFileTool {
//    private static Disposable disposable;
//    @SuppressLint("UseSparseArrays")
//    public static HashMap<Integer,String> map = new HashMap<>();
//
//    public static void startThread() {
//        disposable = Flowable.just(1)
//                .subscribeOn(Schedulers.io())
//                .subscribe(new Consumer<Integer>() {
//                    @Override
//                    public void accept(Integer integer) throws Exception {
//
//                    }
//                });
//    }
//
//
//    public void start() {
//        FileInputStream fis = null;
//        try {
//            SocketAddress socketAddress = new InetSocketAddress("192.168.1.206", 2021);
//            Socket socket = new Socket();
//            socket.connect(socketAddress, 3000);
//            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
//
//            File file = new File(path);
//            fis = new FileInputStream(file);
//            long fileSize = file.length();
//            byte[] buffer = new byte[4096];
//            int read = 0;
//            long totalRead = 0;
//            int remaining = (int) fileSize;
//            while (totalRead < fileSize && (read = fis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
//                totalRead += read;
//                remaining -= read;
//                oos.write(buffer, 0, read);
//                oos.flush();
//            }
//            oos.flush();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (fis != null) {
//                    fis.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//}
