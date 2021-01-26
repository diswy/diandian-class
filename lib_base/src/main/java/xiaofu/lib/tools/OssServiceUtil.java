package xiaofu.lib.tools;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSCustomSignerCredentialProvider;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.orhanobut.logger.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class OssServiceUtil {

    private OSS oss;
    private String endpoint = "http://oss-cn-qingdao.aliyuncs.com";
    private String bucket = "djx-blog";
    private String accessKeyId = "LTAI4Fjo5Nt3uFR5S4eRwbN6";
    private String secretKeyId = "xR9aQuoDLMuMmVls0c0jBEsoncFk53";

    private OssServiceUtil() {
    }

    private static volatile OssServiceUtil ossUtils;

    public static OssServiceUtil getInstance() {
        if (ossUtils == null) {
            synchronized (OssServiceUtil.class) {
                if (ossUtils == null) {
                    ossUtils = new OssServiceUtil();
                }
            }
        }
        return ossUtils;
    }

    /**
     * 签名模式
     */
    public void initSignatureModel(Context context) {
        OSSCredentialProvider credentialProvider = new OSSCustomSignerCredentialProvider() {
            @Override
            public String signContent(String content) {
                return OSSUtils.sign(accessKeyId, secretKeyId, content);
            }
        };

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSSLog.enableLog();

        oss = new OSSClient(context, endpoint, credentialProvider, conf);
    }

    private String addLength(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        if (number < 10) {
            stringBuilder.append(0).append(number);
        } else {
            stringBuilder.append(number);
        }
        return stringBuilder.toString();
    }

    public void asyncUpLoad(String uploadFilePath, final OnAsyncUpLoadListener onAsyncUpLoadListener) {
        if (!TextUtils.isEmpty(uploadFilePath) && oss != null) {

            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            // 构造上传请求
            String objectKey = "blog/" + addLength(year) + "-" + addLength(month + 1) + "/" + randomFileName() + getFileSuffix(uploadFilePath);

            PutObjectRequest put = new PutObjectRequest(bucket, objectKey, uploadFilePath);
            // 异步上传时可以设置进度回调
            put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                @Override
                public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                    if (onAsyncUpLoadListener != null) {
                        onAsyncUpLoadListener.onProgress(request, currentSize, totalSize);
                    }
//                Logger.d("PutObject = %s", "currentSize: " + currentSize + " totalSize: " + totalSize);
                }
            });

            oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                @Override
                public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                    if (onAsyncUpLoadListener != null) {
                        String fileHttpPath = " https://djx-blog.oss-cn-qingdao.aliyuncs.com/" + request.getObjectKey();
                        onAsyncUpLoadListener.onUploadSuccess(request, result, fileHttpPath);
                    }
                    Logger.d("UploadSuccess");
                    Logger.d("ETag = %s", result.getETag());
                    Logger.d("RequestId = %s", result.getRequestId());
                }

                @Override
                public void onFailure(PutObjectRequest request, ClientException clientException, ServiceException serviceException) {
                    if (onAsyncUpLoadListener != null) {
                        onAsyncUpLoadListener.onUploadFailed(request, clientException, serviceException);
                    }
                    // 请求异常
                    if (clientException != null) {
                        // 本地异常如网络异常等
                        clientException.printStackTrace();
                    }
                    if (serviceException != null) {
                        // 服务异常
                        Logger.e("ErrorCode  = %s", serviceException.getErrorCode());
                        Logger.e("RequestId  = %s", serviceException.getRequestId());
                        Logger.e("HostId  = %s", serviceException.getHostId());
                        Logger.e("RawMessage = %s", serviceException.getRawMessage());
                    }
                }
            });
        }
    }

    /**
     * 同步上传接口
     */
    public interface OnAsyncUpLoadListener {

        void onProgress(PutObjectRequest request, long currentSize, long totalSize);

        void onUploadSuccess(PutObjectRequest request, PutObjectResult result, String url);

        void onUploadFailed(PutObjectRequest request, ClientException clientException, ServiceException exception);
    }

    /**
     * 获取文件后缀
     */
    public static String getFileSuffix(String filePath) {
        return filePath.substring(filePath.lastIndexOf("."));
    }


    /**
     * 生成随机文件名，"年月日时分秒"格式
     */
    public String randomFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        return simpleDateFormat.format(date);
    }
}
