package com.beetle.bauhinia.api;


import com.beetle.bauhinia.api.body.PostAuthRefreshToken;
import com.beetle.bauhinia.api.body.PostAuthToken;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.api.body.PostPhone;
import com.beetle.bauhinia.api.body.PostQRCode;
import com.beetle.bauhinia.api.body.PostTextValue;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Code;
import com.beetle.bauhinia.api.types.Image;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.api.types.Version;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import rx.Observable;

/**
 * Created by tsung on 10/10/14.
 */
public interface IMHttp {

    public static class Token {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("refresh_token")
        public String refreshToken;
        @SerializedName("expires_in")
        public int expireTimestamp;

        public long uid;
    }


    @GET("/version/android")
    Observable<Version> getLatestVersion();

    @GET("/verify_code")
    Observable<Code> getVerifyCode(@Query("zone") String zone, @Query("number") String number);

    @POST("/auth/token")
    Observable<Token> postAuthToken(@Body PostAuthToken code);

    @POST("/auth/refresh_token")
    Observable<Token> postAuthRefreshToken(@Body PostAuthRefreshToken refreshToken);

    @POST("/qrcode/sweep")
    Observable<Object> postQRCode(@Body PostQRCode qrcode);



    @Multipart
    @PUT("/users/me/avatar")
    Observable<Image> putUsersMeAvatar(@Part("file") TypedFile file);

    @PUT("/users/me/nickname")
    Observable<Object> putUsersMeNickname(@Body PostTextValue nickname);

    @PUT("/users/me/state")
    Observable<Object> putUsersMeState(@Body PostTextValue state);

    @POST("/users")
    Observable<ArrayList<User>> postUsers(@Body List<PostPhone> phones);
}
