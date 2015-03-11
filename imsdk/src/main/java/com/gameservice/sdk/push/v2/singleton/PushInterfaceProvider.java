package com.gameservice.sdk.push.v2.singleton;

import android.content.Context;
import com.gameservice.sdk.push.v2.face.SmartPushServiceInterface;
import com.gameservice.sdk.push.v2.face.ToolUtilInterface;
import com.gameservice.sdk.push.v2.instance.SmartPushServiceProvider;
import com.gameservice.sdk.push.v2.instance.ToolUtilProvider;

/**
 * NgdsInterfaceHelper
 * Description: sdk壳所使用的实体进行单例的集中管理,默认发布的常规版本从1.0开始.
 * Author:walker lee
 */
public class PushInterfaceProvider {
    private static ToolUtilInterface sToolUtilInstance;
    private static SmartPushServiceInterface sPushServiceInstance;

    public static void initProviders() {
        synchronized (PushInterfaceProvider.class) {
            try {
                sToolUtilInstance = ToolUtilProvider.class.newInstance();
                sPushServiceInstance = SmartPushServiceProvider.class.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取工具类实例,该方法默认调用场景应该是主进程
     *
     * @return
     */
    public static ToolUtilInterface getToolUtilInstance(Context context) {
        return sToolUtilInstance;
    }

    public static SmartPushServiceInterface getPushServiceInstance(Context context) {
        return sPushServiceInstance;
    }

    static {
        initProviders();
    }
}
