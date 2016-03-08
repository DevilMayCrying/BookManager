package com.example.zane.bookmanager.utils;

import com.example.zane.bookmanager.config.RetrofitError;
import com.kermit.exutils.utils.ExUtils;

/**
 * Created by Zane on 16/3/7.
 */
public class JudgeNetError {
    private static String error;
    public static void judgeWhitchNetError(Throwable e){
        error = String.valueOf(e);
        if (error.equals(RetrofitError.NOT_FOUND)){
            ExUtils.Toast("抱歉，未能找到这本书～");
        } else if(error.equals(RetrofitError.NO_NET)){
            ExUtils.Toast("亲～检查您的网络设置");
        }
    }
}
