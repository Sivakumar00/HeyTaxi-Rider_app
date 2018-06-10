package com.siva.rider;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;
import com.siva.rider.Common.Common;
import com.siva.rider.Helper.CustomInfoWindow;

import com.siva.rider.Model.FCMResponse;
import com.siva.rider.Model.Notification;
import com.siva.rider.Model.Rider;
import com.siva.rider.Model.Sender;
import com.siva.rider.Model.Token;
import com.siva.rider.Remote.IFCMService;

import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener {


    private GoogleMap mMap;

    //payservices
    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static final int PLAY_SERVICES_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL=5000;
    private static int FASTEST_INTERVAL=3000;
    private static int DISPLACEMENT=10;
    AutocompleteFilter typeFilter;
    DatabaseReference ref;
    GeoFire geoFire;
    Marker mUserMarker;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;
    FirebaseAuth mAuth;
    Boolean isDriverFound=true;
    String driverId="";
    int radius=1;
    int distance=1;
    private static final int LIMIT=3;
    IFCMService mservice;
//presence
    DatabaseReference driverAvailable;
    PlaceAutocompleteFragment place_location,place_destination;
    String mPlaceLocation,mPlaceDestination;


    //bottom sheet
     ImageView imgExpandable;
    BottomSheetRiderFragment mBottomSheet;
    Button btnPickupRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mAuth=FirebaseAuth.getInstance();
        Log.e("USER",mAuth.getCurrentUser().getUid().toString());
        mapFragment=(SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mservice=Common.getFCMService();
        ref= FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire=new GeoFire(ref);
        //init view
        imgExpandable=(ImageView)findViewById(R.id.imgExpandable);


        btnPickupRequest=(Button)findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());

                if(!isDriverFound){
                    Toast.makeText(Home.this,"MESSSS",Toast.LENGTH_LONG).show();
                    requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());

                }else
                {
                    sendRequestToDriver(driverId);

                }

            }
        });

        place_location=(PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_location);
        place_destination=(PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_destination);
        typeFilter=new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .setTypeFilter(3)
                .build();
        place_location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceLocation=place.getAddress().toString();
                mMap.clear();
                mUserMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.defaultMarker())
                        .title("Pickup Here"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15.0f));


            }

            @Override
            public void onError(Status status) {

            }
        });
        place_destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceDestination=place.getAddress().toString();
                mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),15.0f));
            //show in bottom sheet
                mBottomSheet=BottomSheetRiderFragment.newInstance(mPlaceLocation,mPlaceDestination);
                Home.super.onPostResume();
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());

            }

            @Override
            public void onError(Status status) {

            }
        });

        setUpLocation();
       updateFirebaseToken();

    }

    private void updateFirebaseToken() {
        FirebaseDatabase db= FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference(Common.token_tbl);
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();

        Token token=new Token(FirebaseInstanceId.getInstance().getToken());
        Log.e("user", FirebaseAuth.getInstance().getCurrentUser().getUid());
        Log.e("TOKENS",tokens.toString());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);


    }

    private void sendRequestToDriver(String driverId) {
        DatabaseReference tokens=FirebaseDatabase.getInstance().getReference().child(Common.token_tbl);
        tokens.orderByKey().equalTo(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapShot:dataSnapshot.getChildren()){
                    Token token = postSnapShot.getValue(Token.class);
                    //latlng to json
                    String json_lat_lng=new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                    String riderToken= FirebaseInstanceId.getInstance().getToken();
                    Notification data=new Notification(riderToken,json_lat_lng);  //send it to Driver app ... anga vice versa..pananum
                    Sender content=new Sender(token.getToken(),data);
                    mservice.sendMessaage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                             if(response.body().success==1){
                                Toast.makeText(Home.this,"Request Sent!!!",Toast.LENGTH_LONG).show();

                            }
                            else
                                Toast.makeText(Home.this,"Request Failed!!!",Toast.LENGTH_LONG).show();

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.e("ERROR",t.getMessage());

                        }


                    });



        }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest=FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire=new GeoFire(dbRequest);
        mGeoFire.setLocation(uid,new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

        if(mUserMarker.isVisible())
            mUserMarker.remove();
        //add neww marker
        mUserMarker=mMap.addMarker(new MarkerOptions()
                .title("Pickup Here")
                .snippet("")
                .position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mUserMarker.showInfoWindow();
        btnPickupRequest.setText("Getting Your Driver...");

        findDriver();

    }

    private void findDriver() {
        DatabaseReference drivers=FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        drivers.keepSynced(true);
        GeoFire gfDrivers=new GeoFire(drivers);

        GeoQuery geoQuery= gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),radius);
            geoQuery.removeAllListeners();
         geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
             @Override
             public void onKeyEntered(String key, GeoLocation location) {
                if(isDriverFound){
                    isDriverFound=true;
                    driverId=key;
                    btnPickupRequest.setText("CALL HELPER CELL");
                    Toast.makeText(Home.this,"User id: "+key,Toast.LENGTH_LONG).show();

                }
             }

             @Override
             public void onKeyExited(String key) {

             }

             @Override
             public void onKeyMoved(String key, GeoLocation location) {

             }

             @Override
             public void onGeoQueryReady() {
                if(!isDriverFound&&radius<LIMIT){
                    radius++;
                    findDriver();
                }else {
                    Toast.makeText(getApplicationContext(),"No Helper cell available near You..!",Toast.LENGTH_LONG).show();
                    btnPickupRequest.setText("PICK REQUEST");
                }
             }

             @Override
             public void onGeoQueryError(DatabaseError error) {

             }
         });
    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ){
            //runtime permission
            ActivityCompat.requestPermissions(this,new String[]{
                   android. Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }
        else{
            if(checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();

                    displayLocation();
            }
        }


    }


    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ){
            return;
        }
        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation!=null){
           {
               //create latlng
               LatLng center=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                LatLng northSide= SphericalUtil.computeOffset(center,100000,0);
               LatLng southSide= SphericalUtil.computeOffset(center,100000,180);

               LatLngBounds bounds=LatLngBounds.builder()
                       .include(northSide)
                       .include(southSide)
                       .build();
               place_location.setBoundsBias(bounds);
               place_location.setFilter(typeFilter);

               place_destination.setBoundsBias(bounds);
               place_destination.setFilter(typeFilter);


               //presence
               driverAvailable=FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
               driverAvailable.addValueEventListener(new ValueEventListener() {
                   @Override
                   public void onDataChange(DataSnapshot dataSnapshot) {
                       loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                   }

                   @Override
                   public void onCancelled(DatabaseError databaseError) {

                   }
               });
                final double latitude=mLastLocation.getLatitude();
                final double longitude=mLastLocation.getLongitude();


                        //Add marker
                        if(mUserMarker!=null){
                            mUserMarker.remove();
                        }
                        mUserMarker=mMap.addMarker(new MarkerOptions()

                                .position(new LatLng(latitude,longitude))
                                .title("Your Location"));
                        //move view to position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
               loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));


           }
        }else {
            Log.d("Error"," cannot get your location");
        }


    }

    private void loadAllAvailableDriver(final LatLng location) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(location)
                        .title("YOU"));

    //load all driver in 3km
        DatabaseReference driverLoaction=FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        driverLoaction.keepSynced(true);
        GeoFire gf=new GeoFire(driverLoaction);

        GeoQuery geoQuery=gf.queryAtLocation(new GeoLocation(location.latitude,location.longitude),distance);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                Log.e("KEY",key);
                FirebaseDatabase.getInstance().getReference("DriversInformation").child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //User.class= rider.class.. pojo class mathikuthuruken

                        String name=dataSnapshot.child("name").getValue().toString();
                        String phone=dataSnapshot.child("phone").getValue().toString();
                       // Rider rider= dataSnapshot.getValue(Rider.class);
                        //add driver to map
                      //  String name=rider.getName().toUpperCase();
                      //  Log.e("NAME",name);
                        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.latitude,location.longitude))
                        .flat(true)
                        .title(name)
                                .snippet(phone)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(distance<=LIMIT){
                    distance++;
                    loadAllAvailableDriver(location);

                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void createLocationRequest() {
        mLocationRequest =new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);


    }
    private void buildGoogleApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!= ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RES_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(item.getItemId()==R.id.action_logout)
            sendToStart();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(checkPlayServices()){
                        buildGoogleApiClient();
                        createLocationRequest();
                       // if(location_switch.isChecked())
                            displayLocation();

                    }
                }
        }
    }
  /*  @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser mCurrentUser=mAuth.getCurrentUser();
        if(mCurrentUser==null){
            sendToStart();
        }
    }
*/
    private void sendToStart() {
        Paper.book().destroy();
        Intent intent=new Intent(Home.this,MainActivity.class);
        startActivity(intent);
        finish();
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }
    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;
        displayLocation();
    }
}
