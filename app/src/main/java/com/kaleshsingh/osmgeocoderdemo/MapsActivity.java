package com.kaleshsingh.osmgeocoderdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    EditText addressEditText;
    ArrayList<String> addresses;
    ArrayList<Double> latitudes;
    ArrayList<Double> longitudes;
    ListView addressListView;
    ArrayAdapter<String> arrayAdapter;
    Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        addressEditText = findViewById(R.id.addressEditText);
        searchButton = findViewById(R.id.searchButton);

        addressListView = findViewById(R.id.addressListView);
        addressListView.setAlpha(0.5f);

        addressListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (addresses.size() == latitudes.size() && latitudes.size() == longitudes.size()) {
                    LatLng location = new LatLng(latitudes.get(i), longitudes.get(i));
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(location));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 20));
                }

                addressListView.setVisibility(View.INVISIBLE);
                latitudes.clear();
                longitudes.clear();
                addresses.clear();
                arrayAdapter.notifyDataSetChanged();
                Utility.setListViewHeightBasedOnChildren(addressListView);
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Do something
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };


    }


    @Override
    public void onMapClick(LatLng latLng) {
        if (getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }

        if (addressEditText.isFocused()) {
               addressEditText.clearFocus();
        }
    }

    private class DownloadAddress extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while (data != -1) {
                    char currentChar = (char) data;
                    result += currentChar;
                    data = inputStreamReader.read();
                }

                Log.i("URL Content", result);

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                try {
                    String address = "";
                    JSONObject apiResult = new JSONObject(result);
                    Log.i("API Result", apiResult.toString());
                    JSONObject addressInfo = new JSONObject(apiResult.getString("address"));
                    Log.i("Address Info", addressInfo.toString());

                    if (addressInfo.has("house_number")) {
                        String houseNumber = addressInfo.getString("house_number");
                        Log.i("House Number", houseNumber);
                        address += houseNumber + " ";
                    }
                    if (addressInfo.has("road")) {
                        String street = addressInfo.getString("road");
                        Log.i("Street", street);
                        address += street;
                    }
                    if (addressInfo.has("village")) {
                        String village = addressInfo.getString("village");
                        Log.i("Village", village);
                        if (addressInfo.has("road")) {
                            address += ", ";
                        }
                        address += village;
                    }
                    if (addressInfo.has("state")) {
                        String state = addressInfo.getString("state");
                        Log.i("County", state);
                        if (addressInfo.has("village")) {
                            address += ", ";
                        }
                        address += state;
                    }
                    if (addressInfo.has("country")) {
                        String country = addressInfo.getString("country");
                        Log.i("Country", country);
                        if (addressInfo.has("state")) {
                            address += ", ";
                        }
                        address += country;
                    }
                    if (address.equals("")) {
                        address = "Unknown Location";
                    }

                    Log.i("Address", address);

                    latitudes.add(Double.parseDouble(apiResult.getString("lat")));
                    longitudes.add(Double.parseDouble(apiResult.getString("lon")));
                    addresses.add(address);
                    arrayAdapter.notifyDataSetChanged();
                    Utility.setListViewHeightBasedOnChildren(addressListView);
                    addressListView.setVisibility(View.VISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class DownloadNominatimLocation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while (data != -1) {
                    char currentChar = (char) data;
                    result += currentChar;
                    data = inputStreamReader.read();
                }

                Log.i("Nominatim Address", result);

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                try {

                    JSONArray apiResult = new JSONArray(result);
                    Log.i("API Result", apiResult.toString());
                    JSONObject jsonObject = (JSONObject) apiResult.get(0);
                    Double latitude = Double.parseDouble(jsonObject.getString("lat"));
                    Double longitude = Double.parseDouble(jsonObject.getString("lon"));
                   String address = jsonObject.getString("display_name");
                    /*JSONObject addressInfo = jsonObject.getJSONObject("address");
                    String address = "";
                    if (addressInfo.has("house_number")){
                        address += addressInfo.getString("house_number");
                    }

                    Boolean hasStreetOrRoad = false;
                    if(addressInfo.has("street")){
                        hasStreetOrRoad = true;
                        if(addressInfo.has("house_number")){
                            address += " ";
                        }
                        address += addressInfo.getString("street");
                    }
                    else if(addressInfo.has("road")){
                        hasStreetOrRoad = true;
                        if(addressInfo.has("house_number")){
                            address += " ";
                        }
                        address += addressInfo.getString("road");
                    }

                    Boolean hasVillageOrCity = false;
                    if(addressInfo.has("village")){
                        hasVillageOrCity = true;
                        if(hasStreetOrRoad){
                            address += ", ";
                        }
                        address += addressInfo.getString("village");
                    }
                    else if (addressInfo.has("city")){
                        hasVillageOrCity = true;
                        if(hasStreetOrRoad){
                            address += ", ";
                        }
                        address += addressInfo.getString("city");
                    }
                    if(addressInfo.has("suburb")){
                        if(hasVillageOrCity){
                            address += ", ";
                        }
                        address += addressInfo.getString("suburb");
                    }
                    if(addressInfo.has("state")){
                        if(addressInfo.has("suburb")){
                            address += ", ";
                        }
                        address += addressInfo.getString("state");
                    }
                    if(addressInfo.has("country")){
                        if(addressInfo.has("state")){
                            address += ", ";
                        }
                        address += addressInfo.getString("country");
                    }
                    if(addressInfo.has("postcode")){
                        if(addressInfo.has("country")){
                            address += ", ";
                        }
                        address += addressInfo.getString("postcode");
                    }*/
                    addresses.add(address);
                    arrayAdapter.notifyDataSetChanged();
                    Utility.setListViewHeightBasedOnChildren(addressListView);
                    latitudes.add(latitude);
                    longitudes.add(longitude);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class DownloadGoogleLocation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while (data != -1) {
                    char currentChar = (char) data;
                    result += currentChar;
                    data = inputStreamReader.read();
                }

                Log.i("Google Address", result);

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                try {

                    JSONObject apiResult = new JSONObject(result);
                    JSONArray results = apiResult.getJSONArray("results");
                    for (int i = 0; i < results.length(); ++i) {
                        JSONObject addressInfo = (JSONObject) results.get(i);
                        String address = addressInfo.getString("formatted_address");
                        addresses.add(address);
                        arrayAdapter.notifyDataSetChanged();
                        Utility.setListViewHeightBasedOnChildren(addressListView);
                        JSONObject geometryInfo = addressInfo.getJSONObject("geometry");
                        JSONObject latitudeLongitude = geometryInfo.getJSONObject("location");
                        Double latitude = (Double) latitudeLongitude.get("lat");
                        Double longitude = (Double) latitudeLongitude.get("lng");
                        latitudes.add(latitude);
                        longitudes.add(longitude);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(addresses.size() == 0){
                addressEditText.setText("No results found");
            }
            searchButton.setClickable(true);
        }
    }


    private String getAddress(LatLng latLng) {

        latitudes.clear();
        longitudes.clear();
        addresses.clear();
        arrayAdapter.notifyDataSetChanged();
        Utility.setListViewHeightBasedOnChildren(addressListView);
        addressListView.setVisibility(View.VISIBLE);

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            String address = "";
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addressList != null && addressList.size() > 0) {
                Log.i("Place Info", addressList.get(0).toString());

                String lotNumber = addressList.get(0).getSubThoroughfare();
                if (lotNumber != null) {
                    address += lotNumber;
                }
                String street = addressList.get(0).getThoroughfare();
                if (street != null) {
                    if (lotNumber != null) {
                        address += ", ";
                    }
                    address += street;
                }
                String village = addressList.get(0).getLocality();
                if (village != null) {
                    if (street != null) {
                        address += ", ";
                    }
                    address += village;
                }
                String country = addressList.get(0).getCountryName();
                if (country != null) {
                    if (village != null) {
                        address += ", ";
                    }
                    address += country;
                }
                addresses.add(address);
                latitudes.add(latLng.latitude);
                longitudes.add(latLng.longitude);
                arrayAdapter.notifyDataSetChanged();
                Utility.setListViewHeightBasedOnChildren(addressListView);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            DownloadAddress task = new DownloadAddress();
            URI uri = new URI(
                    "http",
                    "nominatim.openstreetmap.org",
                    "/reverse/",
                    "email=kaleshsingh96@gmail.com&format=json&lat="
                            + latLng.latitude + "&lon="
                            + latLng.longitude
                            + "&zoom=18&addressdetails=1",
                    null);
            URL url = uri.toURL();
            Log.i("URL Encoded", url.toString());

            task.execute(url.toString());

        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getLocation(View view) {
        if (getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        addressListView.setVisibility(View.VISIBLE);
        latitudes.clear();
        longitudes.clear();
        addresses.clear();
        arrayAdapter.notifyDataSetChanged();
        Utility.setListViewHeightBasedOnChildren(addressListView);

        searchButton.setClickable(false);

        String address = addressEditText.getText().toString();

        if (address.trim().isEmpty() || address.trim().length() == 0 || address.trim().equals("")) {
            Toast.makeText(this, "Address cannot be empty!", Toast.LENGTH_SHORT).show();
        } else {
            try {
                DownloadNominatimLocation task = new DownloadNominatimLocation();
                URI uri = new URI(
                        "http",
                        "nominatim.openstreetmap.org",
                        "/search/" + address,
                        "format=json&addressdetails=1&limit=1&polygon_svg=1",
                        null);

                URL url = uri.toURL();
                Log.i("URL Encoded", url.toString());
                task.execute(url.toString());

            } catch (URISyntaxException | MalformedURLException e) {
                e.printStackTrace();
            }


            try {
                DownloadGoogleLocation task = new DownloadGoogleLocation();
                URI uri = new URI(
                        "http",
                        "maps.google.com",
                        "/maps/api/geocode/json",
                        "address=" + address,
                        null);

                URL url = uri.toURL();
                Log.i("URL Encoded", url.toString());
                task.execute(url.toString());

            } catch (URISyntaxException | MalformedURLException e) {
                e.printStackTrace();
            }


        }
    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        latitudes.clear();
        longitudes.clear();
        addresses.clear();
        arrayAdapter.notifyDataSetChanged();
        Utility.setListViewHeightBasedOnChildren(addressListView);

        mMap.addMarker(new MarkerOptions().position(latLng));
        getAddress(latLng);
    }

    public void centerMap(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);

        latitudes = new ArrayList<>();
        longitudes = new ArrayList<>();
        addresses = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, addresses) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = view.findViewById(android.R.id.text1);

                // Your choice of color
                textView.setTextColor(Color.BLACK);
                textView.setBackgroundColor(Color.WHITE);

                return view;
            }
        };
        addressListView.setAdapter(arrayAdapter);

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                centerMap(lastKnownLocation);
            }
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                centerMap(lastKnownLocation);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        centerMap(lastKnownLocation);
                    }
                }
            }
        }
    }
}


