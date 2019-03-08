package cn.com.ava.braincodemo;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

import java.util.Map;

public interface HttpApi {

    @GET("/cgi-bin/plat.cgi")
    Observable<ResponseBody> commandHttpApi(@QueryMap Map<String, String> params);
}
