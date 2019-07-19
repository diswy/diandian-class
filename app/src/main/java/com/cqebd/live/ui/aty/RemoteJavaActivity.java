package com.cqebd.live.ui.aty;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import androidx.lifecycle.Observer;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.cqebd.live.R;
import com.cqebd.live.TCommand;
import com.cqebd.live.TImg;
import com.cqebd.live.databinding.ActivityRemoteBinding;
import com.cqebd.live.socketTool.KTool;
import com.cqebd.live.socketTool.MySocketClient;
import com.jeremyliao.liveeventbus.LiveEventBus;
import cqebd.student.commandline.CacheKey;
import cqebd.student.commandline.Command;
import cqebd.student.tools.ByteTools;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;
import xiaofu.lib.base.activity.BaseBindActivity;
import xiaofu.lib.cache.ACache;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 远程桌面，显示PC端内容，能控制
 */
@Route(path = "/app/aty/remote_java")
public class RemoteJavaActivity extends BaseBindActivity<ActivityRemoteBinding> {
    // Debug调试
    private String format = "TCP命令名称：%s;文件大小:%d;文件名称:%d";
    private String udpFormat = "UDP---!!!---命令名称：%s;图片编号:%d;图片大小:%d发送次数:%d;最后一个包大小:%d";

    // TCP使用的内存
    private byte[] buffer = new byte[24];
    private byte[] imgBuffer = new byte[4096];
    private byte[] intBuffer = new byte[4];

    // TCP转发各种命令的监听
    private Disposable disposable;
    private byte[] cmd = KTool.INSTANCE.getByte();
    private boolean isContinue = true;// 是否保持远程桌面连接
    private Observer<String> observer = new Observer<String>() {
        @Override
        public void onChanged(String s) {
            if (Command.BROADCAST_STOP.equals(s)
                    || Command.DEMON_STOP.equals(s)) {
                isContinue = false;
                if (disposable != null) {
                    disposable.dispose();
                }
                finish();
            }
        }
    };

    // 坐标计算
    private int screenshotImageViewX, screenshotImageViewY;
    private int xCord, yCord, initX, initY;
    private int userId;
    private boolean mouseMoved = false;
    long currentPressTime, lastPressTime;

    @Autowired
    boolean isControl = false;

    @Override
    protected boolean isFullScreen() {
        return true;
    }

    @Override
    protected boolean isKeepScreenOn() {
        return true;
    }

    @NotNull
    @Override
    public String getLoggerTag() {
        return RemoteJavaActivity.class.getName();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_remote;
    }

    @Override
    public void initialize(@NotNull ActivityRemoteBinding binding) {
        ARouter.getInstance().inject(this);

        LiveEventBus.get().with(Command.COMMAND, String.class).observe(this, observer);

        File directory = new File(KTool.INSTANCE.getSDPath());
        if (!directory.exists()) {
            directory.mkdirs();
        }

        ViewTreeObserver vto = binding.ivRemote.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                screenshotImageViewX = binding.ivRemote.getHeight();
                screenshotImageViewY = binding.ivRemote.getWidth();
                ViewTreeObserver obs = binding.ivRemote.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
            }
        });

        // 图片显示线程
        updateScreenshot();

        if (isControl) {
            remoteControl(binding.ivRemote);
            tcpConnect();
        } else {
            newUdpConnect();
//            udpConnect();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isContinue = false;
    }

    private LinkedBlockingQueue<Bitmap> imgQueue = new LinkedBlockingQueue<>(5);
    private final int cacheImgCount = 5;

    /**
     * 图片显示用线程
     */
    private void updateScreenshot() {
        Disposable d = Flowable.interval(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Log.w("远程桌面", "执行中");
                    runOnUiThread(() -> {
                        Bitmap img = imgQueue.poll();
                        Log.w("远程桌面", "---!!!--->>>队列中待出队图片数量：" + imgQueue.size());
                        if (img != null) {
                            binding.ivRemote.setImageBitmap(img);
                        }
                    });

                }, throwable -> Log.e("远程桌面", "图片加载异常：" + throwable.getMessage()));
        getMDisposablePool().add(d);
    }

    /**
     * 远程控制坐标处理
     */
    @SuppressLint("ClickableViewAccessibility")
    private void remoteControl(ImageView iv) {
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        xCord = screenshotImageViewX - (int) event.getY();
                        yCord = (int) event.getX();
                        initX = xCord;
                        initY = yCord;
                        sendCommand(Command.MOUSE_CLICK, (float) xCord / screenshotImageViewX, (float) yCord / screenshotImageViewY);
                        mouseMoved = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        xCord = screenshotImageViewX - (int) event.getY();
                        yCord = (int) event.getX();
                        if ((xCord - initX) != 0 && (yCord - initY) != 0) {
                            initX = xCord;
                            initY = yCord;
                            sendCommand(Command.MOUSE_MOVE, (float) xCord / screenshotImageViewX, (float) yCord / screenshotImageViewY);
                            mouseMoved = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        currentPressTime = System.currentTimeMillis();
                        long interval = currentPressTime - lastPressTime;
                        if (interval <= 1400 && !mouseMoved) {
                            sendCommand(Command.MOUSE_DOUBLE, (float) initX / screenshotImageViewX, (float) initY / screenshotImageViewY);
                        }
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 发送远程控制的响应命令
     */
    private final static String actionFormat = "%s %d %f %f";

    private void sendCommand(String command, float x, float y) {
        LiveEventBus.get()
                .with(Command.COMMAND, String.class)
                .post(String.format(Locale.CHINA, actionFormat, command, userId, x, y));
    }

    //------------------byte数组接收队列，只接
    private LinkedBlockingQueue<byte[]> byteQueue = new LinkedBlockingQueue<>();
    private boolean myImgFlag = false;// 图片标识
    private int myImgCopyOffset = 0;// 图片拷贝偏移量
    private int myImgCurrentPos = 0;// 当前图片编号;
    private int myImgInnerPos = 0;// 模拟for循环的次数；
    private int myImgSendTimes = 0;// 当前图片发送次数；
    private int myImgEndSize = 0;// 当前图片最后一次包长度；

    private void resolveByte() {
        Disposable disposable = Flowable.just(1)
                .delay(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(integer -> {
                    while (isContinue) {
                        if (byteQueue.size() > 0) {
                            byte[] temp = byteQueue.poll();
                            if (temp != null) {
                                newExeCmd(temp);
                            }
                        }
                    }
                }, throwable -> Log.e("远程桌面", "错误内容：" + throwable.getMessage()));
        getMDisposablePool().add(disposable);
    }

    //------------------1对1，tcp连接
    private void tcpConnect() {
        Log.w("远程桌面", "进入TCP");
        ACache cache = ACache.get(this);
        String ip = cache.getAsString(CacheKey.IP_ADDRESS);
        String ids = cache.getAsString(CacheKey.KEY_ID);
        try {
            userId = Integer.parseInt(ids);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            userId = 0;
        }
        disposable = MySocketClient.Companion.get().connect(ip, 2021, new MySocketClient.Socket() {
            @Override
            public void connectSuccess(@NotNull OutputStream os, @NotNull InputStream is) {
                try {
                    os.write(cmd);
                    os.flush();
                    Log.w("远程桌面", "已发送请求远程桌面,准备接收");
                    while (isContinue) {
                        int cmdRead = is.read(buffer, 0, 24);
                        String command = new String(buffer, 0, 16, Charset.forName("UTF-8")).trim();
                        int fileSize = ByteTools.bytesArray2IntUntil17to20(buffer);
                        int number = ByteTools.bytesArray2IntUntil21to24(buffer);
                        Log.w("远程桌面", String.format(format, command, fileSize, number));// Info
                        if (command.equals(Command.SCREENS_RESPONSE)) {
                            int remaining = fileSize;
                            int read = 0;
                            int offset = 0;
                            byte[] imgByteArray = new byte[fileSize];
                            while ((read = is.read(imgBuffer, 0, Math.min(imgBuffer.length, remaining))) > 0) {
                                System.arraycopy(imgBuffer, 0, imgByteArray, offset, read);
                                remaining -= read;
                                offset += read;
                            }

                            if (imgQueue.size() < cacheImgCount) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, fileSize);
                                imgQueue.offer(bitmap);
                                bitmap = null;
                                Log.w("远程桌面", "---!!!---<<<<<<<<<<<<从服务端接收的图片" + imgQueue.size());
                            }
                            imgByteArray = null;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("远程桌面", "IO异常：" + e.getMessage());
                }
            }
        });
    }

    //------------------1对多，udp连接
    private MulticastSocket ds;
    private final String host = "224.0.0.1";
    private InetAddress receiveAddress;
    private int port = 3021;
    private int receiveBufferSize = 1024;// 单次接收大小
    private int imgCopySize = 4088;// 图片拷贝的长度固定-8个长度
    private byte[] oneBuffer = new byte[4096];

    private void newUdpConnect() {
        Disposable disposable = Flowable.just(1)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(integer -> {
                    Log.w("远程桌面", "UDP,开始");
                    ds = new MulticastSocket(port);
//                    ds.setReceiveBufferSize(4096000);// 4M
                    receiveAddress = InetAddress.getByName(host);
                    ds.joinGroup(receiveAddress);
                    ds.setSoTimeout(120);//设置超时
                    DatagramPacket dp = new DatagramPacket(oneBuffer, oneBuffer.length, receiveAddress, port);
                    resolveByte();// 会延迟200ms后启动
                    while (isContinue) {
                        try {
                            ds.receive(dp);
                            if (byteQueue.size() < 100) {
                                byteQueue.offer(oneBuffer);
                            }
                        } catch (SocketTimeoutException e) {
                            Log.i("远程桌面", "~~~超时--->>>当前第" + count + "次循环");
                        } finally {
                            count++;
                        }
                    }
                }, throwable -> Log.e("远程桌面", "错误内容：" + throwable.getMessage()));
        getMDisposablePool().add(disposable);
    }

    //----------------udp接收
    private byte[] cmdBuffer = new byte[4096];
    private byte[] udpBuffer = new byte[4096];
    private byte[] endBuffer;
    private byte[] allImgBuffer;

    int count = 1;

    private void udpConnect() {
        Disposable disposable = Flowable.just(1)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(integer -> {

                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/udp-look.txt";
                    File file = new File(path);
//                    if (!file.exists()) {
//                        file.mkdirs();
//                    }
                    OutputStream os = new FileOutputStream(file);

                    Log.w("远程桌面", "UDP,开始");
                    ds = new MulticastSocket(port);
//                    ds.setReceiveBufferSize(4096000);
                    receiveAddress = InetAddress.getByName(host);
                    ds.joinGroup(receiveAddress);
                    ds.setSoTimeout(120);
                    DatagramPacket cmdDp = new DatagramPacket(cmdBuffer, cmdBuffer.length, receiveAddress, port);
                    while (isContinue) {
                        Log.i("远程桌面", "~~~开始-》》》当前第" + count + "次循环");
                        try {
                            ds.receive(cmdDp);
                        } catch (SocketTimeoutException e) {
                            Log.i("远程桌面", "~~~超时-》》》当前第" + count + "次循环");
                        }

//                        testUDP(cmdBuffer,os);
//                        count++;
//                        Log.i("远程桌面", "~~~接收命令-》》》当前第" + count + "次循环");
                        exeCmd(cmdBuffer);
                        Log.i("远程桌面", "~~~结束-》》》当前第" + count + "次循环");
                        count++;
                    }
                    os.close();
                }, throwable -> {
                    Log.e("远程桌面", "错误内容：" + throwable.getMessage());
                });
        getMDisposablePool().add(disposable);
    }


    private boolean isCmd = false;

    /**
     * 检测图片命令
     */
    private void exeCmd(byte[] buffer) throws IOException {
        String command = new String(buffer, 0, 16, Charset.forName("UTF-8")).trim();
        int imgPos = ByteTools.bytesArray2IntUntil17to20(buffer);// 图片编号
        int imgSize = ByteTools.bytesArray2IntUntil21to24(buffer);// 图片大小
        int sendTimes = ByteTools.bytesArray2IntUntil25to28(buffer);// 发送次数
        int finalSize = ByteTools.bytesArray2IntUntil29to32(buffer);// 最后一次包大小
        Log.i("远程桌面", String.format(udpFormat, command, imgPos, imgSize, sendTimes, finalSize));

        if (command.equals(Command.SCREENS_RESPONSE)) {// 是图片
            int imgOffset = 0;// 图片拷贝偏移量
            allImgBuffer = new byte[imgSize];

            for (int i = 1; i <= sendTimes; i++) {
                DatagramPacket imgDp = new DatagramPacket(udpBuffer, udpBuffer.length, receiveAddress, port);

                try {
                    ds.receive(imgDp);
                } catch (SocketTimeoutException e) {
                    Log.i("远程桌面", "~~~内部循环超时-》》》当前第" + count + "次循环");
                    allImgBuffer = null;
                    break;
                }

                System.arraycopy(udpBuffer, 0, intBuffer, 0, 4);
                int pos = ByteTools.bytes2Int(intBuffer);
                System.arraycopy(udpBuffer, 4, intBuffer, 0, 4);
                int pos2 = ByteTools.bytes2Int(intBuffer);
                Log.i("远程桌面", "UDP，内部循环，当前第" + i + "次接收;图片编号：" + pos + ";接收顺序：" + pos2);

                if (pos != imgPos) {// 丢弃
                    allImgBuffer = null;
                    isCmd = true;
                    break;
                }

                if (i != pos2) {
                    allImgBuffer = null;
                    isCmd = true;
                    break;
                }

                if (i == sendTimes) {// 最后一张
                    endBuffer = new byte[finalSize];
                    System.arraycopy(udpBuffer, 8, allImgBuffer, imgOffset, finalSize);
                    endBuffer = null;
                } else {// 不是最后一张
                    System.arraycopy(udpBuffer, 8, allImgBuffer, imgOffset, 4088);
                    imgOffset += 4088;
                }
            }
            // -- 组装图片
            if (allImgBuffer != null) {
                if (imgQueue.size() < cacheImgCount) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(allImgBuffer, 0, allImgBuffer.length);
                    imgQueue.offer(bitmap);
                    bitmap = null;
                    Log.w("远程桌面", "UDP，生产一张图片------>>>>>>>>>>>>>>>>>>>>>>" + imgQueue.size());
                }
            }

        }
        Log.i("远程桌面", "循环次数，当前时间：" + System.currentTimeMillis());

//        if (isCmd) {
//            isCmd = false;
//            exeCmd(udpBuffer);
//        }
    }

    /**
     * 检测图片命令
     */
    private void newExeCmd(byte[] buffer) {
        if (myImgFlag) {// 是图，拼接图片
            int pos = ByteTools.bytesArray2IntUntil1to4(buffer);// 图片编号，对应
            int innerPos = ByteTools.bytesArray2IntUntil5to8(buffer);// 校验码
            Log.i("远程桌面", "UDP---!!!---，模拟出内部循环，当前第" + myImgInnerPos + "次接收;图片编号：" + pos + ";接收顺序：" + innerPos);
            if (pos != myImgCurrentPos) {// 出现丢包，抛弃。寻找下一次图片命令
                allImgBuffer = null;
                myImgFlag = false;
                return;
            }

            if (innerPos != myImgInnerPos) {// 出现丢包，抛弃。寻找下一次图片命令
                allImgBuffer = null;
                myImgFlag = false;
                return;
            }

            // 接下来说明图片接收完整，待组装图片
            if (myImgInnerPos == myImgSendTimes) {// 此次buff是图片最后一个包
                System.arraycopy(buffer, 8, allImgBuffer, myImgCopyOffset, myImgEndSize);
                // 数组拷贝完毕
                // ---组装图片
                if (allImgBuffer != null) {// 安全机制
                    if (imgQueue.size() < cacheImgCount) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(allImgBuffer, 0, allImgBuffer.length);
                        imgQueue.offer(bitmap);
                        bitmap = null;
                        Log.w("远程桌面", "---!!!---<<<<<<<<<<<<从服务端接收的图片,来自UDP,当前队列长度：" + imgQueue.size());
                    }
                }
                myImgFlag = false;
            } else {
                System.arraycopy(buffer, 8, allImgBuffer, myImgCopyOffset, imgCopySize);
                myImgCopyOffset += imgCopySize;
                myImgInnerPos++;// 模拟的for循环i.自增一次
            }
        } else {// 寻找图片命令
            String command = new String(buffer, 0, 16, Charset.forName("UTF-8")).trim();
            System.arraycopy(buffer,16,intBuffer,0,4);
            int imgPos = ByteTools.bytes2Int(intBuffer);// 图片编号
            System.arraycopy(buffer,20,intBuffer,0,4);
            int imgSize = ByteTools.bytes2Int(intBuffer);// 图片大小
            System.arraycopy(buffer,24,intBuffer,0,4);
            int sendTimes = ByteTools.bytes2Int(intBuffer);// 发送次数
            System.arraycopy(buffer,28,intBuffer,0,4);
            int finalSize = ByteTools.bytes2Int(intBuffer);// 最后一次包大小
            Log.i("远程桌面", String.format(udpFormat, command, imgPos, imgSize, sendTimes, finalSize));
            if (command.equals(Command.SCREENS_RESPONSE)) {
                myImgFlag = true;
                allImgBuffer = new byte[imgSize];// 声明图片数组大小
                myImgCopyOffset = 0;// 重置拷贝数组偏移
                myImgCurrentPos = imgPos;// 重置当前需要解析的图片编号
                myImgInnerPos = 1;// 模拟for循环的自增数，用于判断丢包，或包位置
                myImgSendTimes = sendTimes;// 重置当前图片发送的次数
                myImgEndSize = finalSize;// 重置当前图片最后一个包大小
            }
        }
    }

    //-----------测试udp
    private List<TCommand> cmdList = new ArrayList<>();
    private List<TImg> imgList = new ArrayList<>();

    private void testUDP(byte[] buffer, OutputStream os) throws IOException {
        String command = new String(buffer, 0, 16, Charset.forName("UTF-8")).trim();
        System.arraycopy(buffer, 16, intBuffer, 0, 4);
        int imgPos = ByteTools.bytes2Int(intBuffer);// 图片编号
        System.arraycopy(buffer, 20, intBuffer, 0, 4);
        int imgSize = ByteTools.bytes2Int(intBuffer);// 图片大小
        System.arraycopy(buffer, 24, intBuffer, 0, 4);
        int sendTimes = ByteTools.bytes2Int(intBuffer);// 发送次数
        System.arraycopy(buffer, 28, intBuffer, 0, 4);
        int finalSize = ByteTools.bytes2Int(intBuffer);// 最后一次包大小

        if (command.equals(Command.SCREENS_RESPONSE)) {// 是命令
            String s = count + "、" + "--------当前第" + imgPos + "张图------发送次数:" + sendTimes + "------\n";
            os.write(s.getBytes());
        } else {
            System.arraycopy(buffer, 0, intBuffer, 0, 4);
            int pos = ByteTools.bytes2Int(intBuffer);
            System.arraycopy(buffer, 4, intBuffer, 0, 4);
            int pos2 = ByteTools.bytes2Int(intBuffer);
//            TImg tImg = new TImg(pos, pos2,count);
            String s = count + "、" + "第" + pos + "张,接收顺序:" + pos2 + "\n";
            os.write(s.getBytes());
        }

    }

}
