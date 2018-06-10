package com.siva.rider.Common;

import com.siva.rider.Remote.FCMClient;
import com.siva.rider.Remote.IFCMService;
import com.siva.rider.Remote.IGoogleAPI;
import com.siva.rider.Remote.RetrofitClient;

/**
 * Created by MANIKANDAN on 23-12-2017.
 */

public class Common {
    public static final String driver_tbl="Drivers";
    public static final String user_driver_tbl="DriversInformation";
    public static final String user_rider_tbl="RidersInformation";
    public static final String pickup_request_tbl="PickupRequest";
    public static final String token_tbl="Tokens";
    public static double basic_fare=20;//basic ah 20 rupees
    public static double time_rate=5; //per min ku 5 rs
    public static double distance_rate=7; // per km ku 7rs
    public static final String user_field="usr";
    public static final String pwd_field="pwd";

  //  public static final String googleAPIUrl="https://maps.googleapis.com";
    public static final String baseURL="https://maps.googleapis.com";
    public static final String fcmURL="https://fcm.googleapis.com/";

    public static double getPrice(double km,int min){
        return (basic_fare+(time_rate*min)+(distance_rate*km));

    }
    public static IGoogleAPI getGoogleService(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);

    }
    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);

    }
}
