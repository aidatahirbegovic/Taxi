package com.example.taksi54;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {


    private String rideId, currentUserId,customerId, driverId, userDriverOrCustomer;

    private TextView rideLocation;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView kullaniciAdi;
    private TextView kullaniciNumarasi;

    private ImageView kullaniciImg;

    private RatingBar mRatingBar;

    private Button mTamamla;

    private DatabaseReference historyRideInfoDb;

    private LatLng destinationLtLng, pickupLatLng;

    private String distance;
    private Double ridePrice;
    private Boolean customerPaid = false;

    boolean isUserCustomer = false;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        polylines = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId");

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        rideLocation = findViewById(R.id.rideLocation);
        rideDistance = findViewById(R.id.rideDistance);
        rideDate = findViewById(R.id.rideDate);
        kullaniciAdi = findViewById(R.id.userName);
        kullaniciNumarasi = findViewById(R.id.userPhone);

        kullaniciImg = findViewById(R.id.userImage);
        mRatingBar = findViewById(R.id.ratingBar);
        mTamamla = findViewById(R.id.tamamla);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);

        mTamamla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistorySingleActivity.this, CustomerMapActivity.class);
                startActivity(intent);
                return;
            }
        });
    
        getRideInformation();
    
    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child: dataSnapshot.getChildren()){
                        /*if(child.getKey().equals("customer")){
                           customerId = child.getValue().toString();
                           if(!customerId.equals(currentUserId)){ //if id of current user its not the same with customer, than its drivers hirstory
                               userDriverOrCustomer = "Drivers";
                               getUserInformation("Customers", customerId);
                           }
                        }*/
                        if(child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                            if(!driverId.equals(currentUserId)){ //if id of current user its not the same with customer, than its drivers hirstory
                                userDriverOrCustomer = "Customers";
                                getUserInformation("Drivers", driverId);
                                displayCustomerRelatedObjects();
                            }
                        }
                        if(child.getKey().equals("timestamp")){
                            rideDate.setText(getDate(Long.valueOf(child.getValue().toString())));

                        }
                        if(child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                       /* if (child.getKey().equals("customerPaid")){
                            customerPaid =true;
                        }*/
                        if(child.getKey().equals("distance")){
                            distance = child.getValue().toString();//all the value, previse posle zareza
                            rideDistance.setText(distance.substring(0,Math.min(distance.length(),5))+"km"); //5 is// number of characters
                            ridePrice = Double.valueOf(distance)*0.5; //racuna se cena kada se km podele sa dva
                        }
                        if(child.getKey().equals("destination")){
                            rideLocation.setText(child.getValue().toString());

                        }
                        if(child.getKey().equals("location")){
                            //if(pickupLatLng != null)
                                pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()),Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLtLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()),Double.valueOf(child.child("to").child("lng").getValue().toString()));

                            if(destinationLtLng != new LatLng(0,0)){
                                getRouteToMarker();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRelatedObjects() {

        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyRideInfoDb.child("rating").setValue(rating);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers"). child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(rating);
            }
        });

    }


    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {

        DatabaseReference mOtherUserDB =FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        kullaniciAdi.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        kullaniciNumarasi.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(kullaniciImg);
                  }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        String date = df.format("MM-dd-yyyy hh:mm",cal).toString();
        return date;
    }
    private void getRouteToMarker() {
        if (pickupLatLng != null && destinationLtLng != null) {

            Routing routing = new Routing.Builder()
                    .key("AIzaSyC6mIQdANJdf4uBhiZ8pCV32AsEcH2irDw")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(pickupLatLng, destinationLtLng)
                    .build();
            routing.execute();

        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }


    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {

        if(e != null) {
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Tekrar Deneyiniz", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }
    boolean cameraSet = false;
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if(destinationLtLng != new LatLng(0,0)) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLatLng);
            builder.include(destinationLtLng);

            LatLngBounds bounds = builder.build();

            int width = getResources().getDisplayMetrics().widthPixels;
            int padding = (int) (width * 0.2);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cameraUpdate);
            mMap.addMarker(new MarkerOptions().position(destinationLtLng).title("hedef"));
        }
        else{
            float zoomLevel = 15.0f; //This goes up to 21
            if(!cameraSet) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, zoomLevel));
                cameraSet = true;
            }
        }


        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Müşteri Lokasyonu").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker)));

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            //Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRoutingCancelled() {

    }

}