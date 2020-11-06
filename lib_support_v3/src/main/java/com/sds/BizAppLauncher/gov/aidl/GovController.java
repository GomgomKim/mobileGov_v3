package com.sds.BizAppLauncher.gov.aidl;

import android.app.Activity;

import kr.go.mobile.common.v3.CommonBasedAPI;

public class GovController {
    public static void startGovActivityForResult(final Activity loadingActivity, int govInitRequest, String verificationTokenAsByte64) {
        CommonBasedAPI.startCommonBaseInitActivityForResult(loadingActivity, govInitRequest);
    }
}
