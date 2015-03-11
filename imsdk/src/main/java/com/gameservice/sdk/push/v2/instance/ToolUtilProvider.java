package com.gameservice.sdk.push.v2.instance;

import android.app.Activity;
import com.gameservice.sdk.push.v2.face.ToolUtilInterface;
import com.gameservice.sdk.push.util.PhoneModelUtil;

/**
 * ToolUtilProvider
 * Description:
 * Author:walker lee
 */
public class ToolUtilProvider implements ToolUtilInterface {

    @Override
    public void showGuideOnce(Activity activity) {
        PhoneModelUtil.showGuideOnce(activity);
    }

    @Override
    public boolean matchTargetModel() {
        return PhoneModelUtil.matchTargetModel();
    }

    @Override
    public void showGuide(Activity activity) {
        PhoneModelUtil.showGuide(activity);
    }
}
