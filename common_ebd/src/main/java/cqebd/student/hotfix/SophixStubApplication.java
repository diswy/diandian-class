package cqebd.student.hotfix;

import android.content.Context;
import android.util.Log;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixApplication;
import com.taobao.sophix.SophixEntry;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

import androidx.annotation.Keep;
import cqebd.student.BaseApp;

/**
 * Sophix入口类，专门用于初始化Sophix，不应包含任何业务逻辑。
 * 此类必须继承自SophixApplication，onCreate方法不需要实现。
 * 此类不应与项目中的其他类有任何互相调用的逻辑，必须完全做到隔离。
 * AndroidManifest中设置application为此类，而SophixEntry中设为原先Application类。
 * 注意原先Application里不需要再重复初始化Sophix，并且需要避免混淆原先Application类。
 * 如有其它自定义改造，请咨询官方后妥善处理。
 */
public class SophixStubApplication extends SophixApplication {
    private final String TAG = "SophixStubApplication";

    // 此处SophixEntry应指定真正的Application，并且保证RealApplicationStub类名不被混淆。
    @Keep
    @SophixEntry(BaseApp.class)
    static class RealApplicationStub {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//         如果需要使用MultiDex，需要在此处调用。
//         MultiDex.install(this);
        initSophix();
    }

    private void initSophix() {
        String appVersion = "0.0.0";
        try {
            appVersion = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0)
                    .versionName;
        } catch (Exception ignored) {
        }
        final SophixManager instance = SophixManager.getInstance();
        instance.setContext(this)
                .setAppVersion(appVersion)
                .setSecretMetaData("25779264-1", "86bc4638bf9a965ab3825d7271d7018f", "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCaUssOi2maAoYiw6ucXDedBGWtx139WnmaKDJ1G79I8N25sZHncZvcMqxjbIcnj8kCR9AQaKdn3y6WTwVyHzEGwWy3UNVhsUnwrqBrKB0IiTHqUVZL+5W2YTx3/+ldjE/bNGINZrF7pWP/3Hv1O+piKwvtFmFYsab2rw4nwDOfoCzpvsf1oA5uWoUo3jNxFpWH8kbYRxq//qrpTmmyxRyAT9oknbVUPo/n+PP8kzyo+3xZ6cjdo5K2pdi+oQy1qQz1SMjPMsbPzDW46TVOTP3trD0ph1UleCM97CLVAGChr6OUNg7J4s12zLGJZ1xV8MTPbDVhMTOhEAHPw+HaG7k3AgMBAAECggEBAJShyRBpFXxPkgFJk6804htIcOW+brfku9nNfUttPf5yfhqq+2t98C9UXtQjYsino2Ge6LXlFdblDWmXEheoEv/+q0ajg+BjcGNuzPJn4A6olHqpfAQSe078t9CbmZeP3Bmzifx+O9JqqLV1Fa5L1qJV81aVh4x1DenpECkY5J+HltUe+T4l+zTvtGhyEf2fCYAzsaRrJit4cZBymoGgFeeObv1puRhYAxfnglGSoDuA/a0/U+XmHRHXffFdHJX3HOtTHmDII8bbMC3AZvxrIJ6uh0aFh4QsRf0MiZzY+9elVOjVGgjx9/q+XTOMyySUe5c/5Bqwa8rGvHNT30YU1AECgYEAyP2LuqCVsOIOZInwZcni+eLIOkHXnLHqPK/0Tv1XqE9g26lwAp2hn5QFWRepPw0ZlmRQ5Mqnlz2/3rufoexlddIc/g3ZaxvcgOLiL4PmJb8+k73MVFEGV2RGlg7+2aNkM0HB/VeJblcfOu5FswZRinStfw0kPGrgR0oWPpUXpOECgYEAxI+CfKJUkHG2UXOel6n83/rVW3a3WZ+jh7DNPmHeRV9xmHzD9lKcDH3UOHsN/6DPLAzXP2vYmp7oHnH9yp6dY7uaVD8E2ZkTPSNxCZE5G3/vvyOl69SPFh0wdQy6XsHwqx82eUhAD1HQRzoTtv9qqh3Duk8JHFl6QrCzVnYwCRcCgYBXdOQpKBkap7LFkrQVRpzKSSEBIIdWeqBHGfU/rcNHzIqD6p1/teGPE5SkKMSPV/7rTulYNMpp6AemPpP+nGOlufC9p38UuUHgn1j2fflWMXpkRTE0+zC8NDj2sL26K/u8xf9IYp+9cJF5ThbsCHLQwWaBI1HCLGs8+gLl5nAsYQKBgFZWvzdlyoVkHXRrt+bgmR39PQie/eyVLayloZHcRVjtkqUrcJxbLwZhov1oN/7oMPglb8sA4TURxd3Rrmv4/iNXvXNx9vxpu8SoM7fDHXWR3cp0qyYKyQsqqSVRAPAsej2ASVo+vTy9cCJJRW2lhcHRry7AFr1oJEb0/OFmqrTjAoGBAKnsqA8/4to/kcILyxc6ks1leRXlHS+CzXqTz392DOZAiwXNogSJxY5jr9KCzDgkAgpqiHL/7V8QjZ+BFY702Tfk2DrYuFwyrvHvl1GhLJ4wwuv3KRf2WHglymqk6IEKAEeC4HD5pBPaNfClOYqrO9ixfsx4m4HgT/dxD8b1JfO3")
                .setEnableDebug(false)
//                .setEnableFullLog()
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            Log.i(TAG, "sophix load patch success!");
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 如果需要在后台重启，建议此处用SharePreference保存状态。
                            Log.i(TAG, "sophix preload patch success. restart app to make effect.");
                        }
                    }
                }).initialize();
    }
}