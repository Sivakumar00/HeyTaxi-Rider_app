package com.siva.rider;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Double2;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.siva.rider.Common.Common;
import com.siva.rider.Remote.IGoogleAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by MANIKANDAN on 03-12-2017.
 */

public class BottomSheetRiderFragment extends BottomSheetDialogFragment {

    String mLocation,mDestination;
    TextView txtCalculate;
    IGoogleAPI mService;

    public static BottomSheetRiderFragment newInstance(String location,String destination){
        BottomSheetRiderFragment f=new BottomSheetRiderFragment();
        Bundle args=new Bundle();
        args.putString("location",location);
        args.putString("destination",destination);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation=getArguments().getString("location");
        mDestination=getArguments().getString("destination");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.bottom_sheet_rider,container,false);

        TextView txtLocation=(TextView)view.findViewById(R.id.txtLocation);
        TextView txtDestination=(TextView)view.findViewById(R.id.txtDestination);
        txtCalculate=(TextView)view.findViewById(R.id.txtCalculate);
        getPrice(mLocation,mDestination);
        mService= Common.getGoogleService();
        txtLocation.setText(mLocation);
        txtDestination.setText(mDestination);

        return view;
    }

    private void getPrice(String mLocation, String mDestination) {
        String requestUrl=null;
        try{
            requestUrl="https://maps.googleapis.com/maps/api/directions/json?"+"mode=driving&"+"transit_routing_preference=less_driving&"
                    +"origin="+mLocation+"&"+"destination="+mDestination+"&"+"key="+getResources().getString(R.string.google_api_key);
            Log.e("LINK",requestUrl);
            mService.getPath(requestUrl).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        //routes json
                        JSONObject jsonObject=new JSONObject(response.body().toString());
                        JSONArray routes=jsonObject.getJSONArray("routes");
                        JSONObject object=routes.getJSONObject(0);
                        JSONArray legs=object.getJSONArray("legs");
                        JSONObject legsObject=legs.getJSONObject(0);

                        //distance
                        JSONObject distance=legsObject.getJSONObject("distance");
                        String distance_text=distance.getString("text");

                        Double distance_value= Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+",""));
                        //time
                        JSONObject time=legsObject.getJSONObject("duration");
                        String time_text=time.getString("text");
                        Integer time_value=Integer.parseInt(time_text.replaceAll("\\D+",""));
                        String final_calculate=String.format("%s + %s = $%.2f",distance_text,time_text,Common.getPrice(distance_value,time_value));
                        txtCalculate.setText(final_calculate);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("ERROR",t.getMessage());

                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
