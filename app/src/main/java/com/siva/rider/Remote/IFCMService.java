package com.siva.rider.Remote;


import com.siva.rider.Model.FCMResponse;
import com.siva.rider.Model.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by MANIKANDAN on 24-12-2017.
 */

public interface IFCMService {
    @Headers( {
            "Content-Type:application/json",
            "Authorization:key=AAAAfSiA34A:APA91bH0ItQruVlEb_J8LGnM1HSN3GzWVH1RpgIqtZRV3V3_GQtvzzsgCYcPCaDuc38fhxXIRR_enCcHM7ypDiqaRt3aHJ6xoTiI3H74mNlFTZXAjdknntlwjo5BaI0b0nfoe4p44Lr9"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessaage(@Body Sender body);
}
