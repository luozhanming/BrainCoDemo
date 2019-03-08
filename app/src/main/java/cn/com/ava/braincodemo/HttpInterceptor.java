package cn.com.ava.braincodemo;

import com.orhanobut.logger.Logger;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.Charset;

public class HttpInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        //请求地址
        String url = request.url().toString();
        String headers = request.headers().toString();
        String requestbody = "";
        String responseString = "";
        ResponseBody responseBody = null;

        if (request.body() != null) {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            Charset charset = Charset.forName("UTF-8");
            requestbody = buffer.readString(charset);
        }

        Response response = chain.proceed(request);

        if (response.body() != null && response.body().contentType() != null) {
            MediaType mediaType = response.body().contentType();
            responseString = response.body().string();
            responseBody = ResponseBody.create(mediaType, responseString);
        }
        Response finalResponse = response.newBuilder().body(responseBody).build();
        String logFormat = "请求地址：%s \n" +
                "请求头：%s \n" +
                "请求体：%s \n" +
                "相应结果：%s";
        Logger.d(logFormat, url, headers, requestbody, responseString);
        return finalResponse;

    }
}
