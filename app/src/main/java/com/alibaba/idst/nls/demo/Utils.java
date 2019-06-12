package com.alibaba.idst.nls.demo;

import android.util.Log;

import com.alibaba.idst.token.AccessToken;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Utils {
    public static long expireTime;
    public static String oldToken;


    public static String getToken() {
        try {
            if (System.currentTimeMillis() > expireTime || null == oldToken) {
                ExecutorService executorService = Executors.newCachedThreadPool();
                GetTokenTask task = new GetTokenTask();
                Future<String> future = executorService.submit(task);
                executorService.shutdown();
                oldToken = future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return oldToken;
    }

    static class GetTokenTask implements Callable<String> {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String accessKey = "";
        String accessKeySecret = "";

        @Override
        public String call() throws Exception {
            AccessToken accessToken = new AccessToken(accessKey, accessKeySecret);
            accessToken.apply();
            expireTime = accessToken.getExpireTime() * 1000;
            Log.e("whh", "expireTime\t" + expireTime + " \tformatTime" + dateFormat.format(new Date(expireTime)));
            return accessToken.getToken();
        }
    }
}
