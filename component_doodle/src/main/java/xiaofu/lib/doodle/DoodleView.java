package xiaofu.lib.doodle;

import android.content.Context;
import android.graphics.*;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的用于涂鸦的 View
 * <p>
 * Created by developerHaoz on 2017/7/12.
 */

public class DoodleView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mSurfaceHolder = null;

    // 当前所选画笔的形状
    private BaseAction curAction = null;
    // 默认画笔为黑色
    private int currentColor = Color.BLACK;
    // 画笔的粗细
    private int currentSize = 5;

    private Paint mPaint;

    private List<BaseAction> mBaseActions;

    private Bitmap mBitmap;

    private ActionType mActionType = ActionType.Path;

    private boolean canDoodle = true;

    public DoodleView(Context context) {
        super(context);
        init();
    }

    public DoodleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mSurfaceHolder = this.getHolder();

        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

//        setZOrderMediaOverlay(true);
//        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);


        mSurfaceHolder.addCallback(this);
        this.setFocusable(true);

        mPaint = new Paint();
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setStrokeWidth(currentSize);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        mBaseActions = new ArrayList<>();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL) {
            return false;
        }

        if (canDoodle) {
            float touchX = event.getX();
            float touchY = event.getY();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    setCurAction(touchX, touchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    Canvas canvas = mSurfaceHolder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    for (BaseAction baseAction : mBaseActions) {
                        baseAction.draw(canvas);
                    }
                    curAction.move(touchX, touchY);
                    curAction.draw(canvas);
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                    break;
                case MotionEvent.ACTION_UP:
                    mBaseActions.add(curAction);
                    curAction = null;
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /**
     * 得到当前画笔的类型，并进行实例化
     *
     * @param x
     * @param y
     */
    private void setCurAction(float x, float y) {
        switch (mActionType) {
            case Point:
                curAction = new MyPoint(x, y, currentColor);
                break;
            case Path:
                curAction = new MyPath(x, y, currentSize, currentColor);
                break;
            case Line:
                curAction = new MyLine(x, y, currentSize, currentColor);
                break;
            case Rect:
                curAction = new MyRect(x, y, currentSize, currentColor);
                break;
            case Circle:
                curAction = new MyCircle(x, y, currentSize, currentColor);
                break;
            case FillEcRect:
                curAction = new MyFillRect(x, y, currentSize, currentColor);
                break;
            case FilledCircle:
                curAction = new MyFillCircle(x, y, currentSize, currentColor);
                break;
            default:
                break;
        }
    }

    /**
     * 设置画笔的颜色
     *
     * @param color 颜色
     */
    public void setColor(String color) {
        this.currentColor = Color.parseColor(color);
    }

    /**
     * 设置画笔的粗细
     *
     * @param size 画笔的粗细
     */
    public void setSize(int size) {
        this.currentSize = size;
    }

    /**
     * 设置画笔的形状
     *
     * @param type 画笔的形状
     */
    public void setType(ActionType type) {
        this.mActionType = type;
    }

    /**
     * 将当前的画布转换成一个 Bitmap
     *
     * @return Bitmap
     */
    public Bitmap getBitmap() {
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        doDraw(canvas);
        return mBitmap;
    }

    /**
     * 保存涂鸦后的图片
     *
     * @param doodleView
     * @return 图片的保存路径
     */
    public String saveBitmap(DoodleView doodleView) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/diandian/answer/" + System.currentTimeMillis() + ".jpg";
        if (!new File(path).exists()) {
            new File(path).getParentFile().mkdir();
        }
        savePicByPNG(doodleView.getBitmap(), path);
        return path;
    }

    /**
     * 保存涂鸦后的图片
     *
     * @param doodleView
     * @param backBitmap 背景
     * @return 图片的保存路径
     */
    public String saveBitmap(DoodleView doodleView, Bitmap backBitmap) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/diandian/answer/" + System.currentTimeMillis() + ".jpg";
        if (!new File(path).exists()) {
            new File(path).getParentFile().mkdir();
        }
        savePicByPNG(mergeBitmap(backBitmap, doodleView.getBitmap()), path);
        return path;
    }

    /**
     * 合并两个Bitmap
     *
     * @param backBitmap  背景
     * @param frontBitmap 前景
     */
    private Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap) {
        if (backBitmap.isRecycled() || frontBitmap.isRecycled()) {
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect = new Rect(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
        canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
        return bitmap;
    }

    /**
     * 将一个 Bitmap 保存在一个指定的路径中
     *
     * @param bitmap
     * @param filePath
     */
    public static void savePicByPNG(Bitmap bitmap, String filePath) {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(filePath);
            if (null != fileOutputStream) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始进行绘画
     *
     * @param canvas
     */
    private void doDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        for (BaseAction action : mBaseActions) {
            action.draw(canvas);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }


    /**
     * 回退
     *
     * @return 是否已经回退成功
     */
    public boolean back() {
        if (mBaseActions != null && mBaseActions.size() > 0) {
            mBaseActions.remove(mBaseActions.size() - 1);
            Canvas canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (BaseAction action : mBaseActions) {
                action.draw(canvas);
            }
            mSurfaceHolder.unlockCanvasAndPost(canvas);
            return true;
        }
        return false;
    }

    /**
     * 重置签名
     */
    public void reset() {
        if (mBaseActions != null && mBaseActions.size() > 0) {
            mBaseActions.clear();
            Canvas canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (BaseAction action : mBaseActions) {
                action.draw(canvas);
            }
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 设置涂鸦控件是否可以绘画
     */
    public void canDoodle(Boolean canDoodle) {
        this.canDoodle = canDoodle;
    }

    public enum ActionType {
        Point, Path, Line, Rect, Circle, FillEcRect, FilledCircle
    }
}






















