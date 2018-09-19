package com.xiumiing.okhttp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //请求头部
        Headers headers = new Headers.Builder()
                .build();

        //请求体
        FormBody formBody = new FormBody.Builder()
                .add("data", "")
                .add("token", "")
                .build();

        //请求 整体
        final Request request = new Request.Builder()
                .url("base_url")
                .post(formBody)
                .headers(headers)
                .build();

        //okHttp 客户端
        OkHttpClient mClient = getOkHttpClient();

        Call call = mClient.newCall(request);

        //同步
        try {
            Response execute = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //异步 回调在子线程
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }


    //读超时长，单位：毫秒
    public static final int READ_TIME_OUT = 7676;
    //连接时长，单位：毫秒
    public static final int CONNECT_TIME_OUT = 7676;

    /*************************缓存设置*********************/
    /*
    1. noCache 不使用缓存，全部走网络

    2. noStore 不使用缓存，也不存储缓存

    3. onlyIfCached 只使用缓存

    4. maxAge 设置最大失效时间，失效则不使用 需要服务器配合

    5. maxStale 设置最大失效时间，失效则不使用 需要服务器配合 感觉这两个类似 还没怎么弄清楚，清楚的同学欢迎留言

    6. minFresh 设置有效时间，依旧如上

    7. FORCE_NETWORK 只走网络

    8. FORCE_CACHE 只走缓存*/

    /**
     * 设缓存有效期为两天
     */
    private static final long CACHE_STALE_SEC = 60 * 60 * 24 * 2;
    /**
     * if-only-cache时只查询缓存而不会请求服务器，max-stale可以配合设置缓存失效时间
     * max-stale 指示用户可以接收超出超时期间的响应消息。如果指定max-stale消息的值，那么用户可接收超出超时期指定值之内的响应消息。
     */
    private static final String CACHE_CONTROL_CACHE = "only-if-cached, max-stale=" + CACHE_STALE_SEC;
    /**
     * 查询网络的Cache-Control设置，头部Cache-Control设为max-age=0
     * (假如请求了服务器并在a时刻返回响应结果，则在max-age规定的秒数内，浏览器将不会发送对应的请求到服务器，数据由缓存直接返回)时则不会使用缓存而请求服务器
     */
    private static final String CACHE_CONTROL_AGE = "max-age=0";

    /**
     * 获取 OkHttpClient
     *
     * @return
     */
    public static OkHttpClient getOkHttpClient() {

////        //开启Log
//        final HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
//        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        //缓存
//        File cacheFile = new File(App.getAppContext().getCacheDir(), AppConstant.CACHE);
//        final Cache cache = new Cache(cacheFile, 1024 * 1024 * 100);

        //增加头部信息
        //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
        final Interceptor headerInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                return chain.proceed(chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build());
            }
        };

        /**
         * 云端响应头拦截器，用来配置缓存策略
         * Dangerous interceptor that rewrites the server's cache-control header.
         */
        final Interceptor mRewriteCacheControlInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                String cacheControl = request.cacheControl().toString();
//                if (!NetWorkUtils.isNetConnected(App.getAppContext())) {
                request = request.newBuilder()
                        .cacheControl(TextUtils.isEmpty(cacheControl) ? CacheControl.FORCE_NETWORK : CacheControl.FORCE_CACHE)
                        .build();
//                }
                Response originalResponse = chain.proceed(request);
//                if (NetWorkUtils.isNetConnected(App.getAppContext())) {
                return originalResponse.newBuilder()
                        .header("Cache-Control", cacheControl)
                        .removeHeader("Pragma")
                        .build();
//                } else {
//                    return originalResponse.newBuilder()
//                            .header("Cache-Control", "public, only-if-cached, max-stale=" + CACHE_STALE_SEC)
//                            .removeHeader("Pragma")
//                            .build();
//                }
            }
        };

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        builder.sslSocketFactory(Utils.getSslSocketFactory());
        builder.readTimeout(READ_TIME_OUT, TimeUnit.MILLISECONDS);
        builder.connectTimeout(CONNECT_TIME_OUT, TimeUnit.MILLISECONDS);
        builder.addInterceptor(mRewriteCacheControlInterceptor);
        builder.addNetworkInterceptor(mRewriteCacheControlInterceptor);
        builder.addInterceptor(headerInterceptor);
//        builder.addInterceptor(logInterceptor);
//        if (BuildConfig.DEBUG && Utils.isEmulator(App.getAppContext())) {
//            builder.dns(new WkDns());
//        }
//        builder.cache(cache);
        return builder.build();
    }
}
