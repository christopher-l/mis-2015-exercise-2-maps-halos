package com.example.mmbuw.hellomaps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private View mapView;
    private Projection projection;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Marker currentLocation;
    private Markers markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        markers = new Markers(this);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        markers.saveMarkers();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mapView = findViewById(R.id.map);
        projection = mMap.getProjection();
        currentLocation = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Current Location"));
        MapListener mapListener = new MapListener();
        mMap.setOnMapLongClickListener(mapListener);
        mMap.setOnCameraChangeListener(mapListener);
        markers.loadMarkers();
    }

    private void makeUseOfNewLocation(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentLocation.setPosition(latLng);
    }


    private class Markers {

        private class Marker {
            LatLng point;
            String title;
            Circle circle;
            final static private int overlap = 20;

            public Marker (LatLng newPoint, String newTitle) {
                point = newPoint;
                title = newTitle;
            }

            public Marker (String json) throws JSONException {
                JSONObject marker = new JSONObject(json);
                point = new LatLng (marker.getDouble("lat"), marker.getDouble("lng"));
                title = marker.getString("title");
            }

            public String toJSON() throws JSONException {
                JSONObject json = new JSONObject();
                json.put("lat", point.latitude);
                json.put("lng", point.longitude);
                json.put("title", title);
                return json.toString();
            }

            public void drawCircle() {
                if (isVisible()) {
                    if (circle != null) {
                        circle.remove();
                        circle = null;
                    }
                } else {
                    if (circle == null) {
                        circle = mMap.addCircle(new CircleOptions()
                                .center(point)
                                .radius(calcCircRadius())
                                .strokeColor(Color.RED));
                    } else {
                        circle.setRadius(calcCircRadius());
                    }
                }
            }

            private boolean isVisible() {
                Point screenPoint = projection.toScreenLocation(point);
                return screenPoint.x > 0 &&
                        screenPoint.x < mapView.getWidth() &&
                        screenPoint.y > 0 &&
                        screenPoint.y < mapView.getHeight();
            }

            private Point calcNearestScreenPoint() {
                Point screenPoint = projection.toScreenLocation(point);
                if (screenPoint.x < 0) {
                    if (screenPoint.y < 0) {
                        return new Point(overlap, overlap);
                    } else if (screenPoint.y > mapView.getHeight()) {
                        return new Point(overlap, mapView.getHeight() - overlap);
                    } else {
                        return new Point(overlap, screenPoint.y);
                    }
                } else if (screenPoint.x > mapView.getWidth()) {
                    if (screenPoint.y < 0) {
                        return new Point(mapView.getWidth() - overlap, overlap);
                    } else if (screenPoint.y > mapView.getHeight()) {
                        return new Point(mapView.getWidth() - overlap, mapView.getHeight() - overlap);
                    } else {
                        return new Point(mapView.getWidth() - overlap, screenPoint.y);
                    }
                } else {
                    if (screenPoint.y < 0) {
                        return new Point(screenPoint.x, overlap);
                    } else if (screenPoint.y > mapView.getHeight()) {
                        return new Point(screenPoint.x, mapView.getHeight() - overlap);
                    } else {
                        return screenPoint; // Point is visible on the screen.
                                            // This should not happen.
                    }
                }
            }

            private double calcCircRadius() {
                LatLng nearestScreenPoint = projection.fromScreenLocation(calcNearestScreenPoint());
                float[] results = new float[]{0};
                Location.distanceBetween(point.latitude, point.longitude,
                        nearestScreenPoint.latitude, nearestScreenPoint.longitude, results);
                return (double) results[0];
            }
        }

        private Set<Marker> markers = new HashSet<Marker>();
        private SharedPreferences sharedPref;

        public Markers(FragmentActivity activity) {
            sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        }

        public void addMarker(Marker marker) {
            markers.add(marker);
            mMap.addMarker(new MarkerOptions()
                            .position(marker.point)
                            .title(marker.title)
            );
        }

        public void addMarker(LatLng point, String title) {
            Marker marker = new Marker(point, title);
            addMarker(marker);
        }

        public void saveMarkers() {
            Set<String> savedMarkers = new HashSet<String>();
            for (Marker marker : markers) {
                try {
                    String json = marker.toJSON();
                    savedMarkers.add(json);
                } catch (JSONException e) {}
            }
            sharedPref.edit()
                    .putStringSet("markers", savedMarkers)
                    .apply();
        }

        public void loadMarkers() {
            Set<String> savedMarkers =
                    new HashSet<String>(sharedPref.getStringSet("markers", new HashSet<String>()));
            for (String s : savedMarkers){
                try {
                    Marker marker = new Marker(s);
                    addMarker(marker);
                } catch (JSONException e) {}
            }
        }

        public void updateCircles() {
            for (Marker marker : markers) {
                marker.drawCircle();
            }
        }
    }



    private class MapListener implements
            GoogleMap.OnMapLongClickListener,
            GoogleMap.OnCameraChangeListener {

        @Override
        public void onMapLongClick(LatLng point){
            EditText editText = (EditText)findViewById(R.id.editText);
            markers.addMarker(point, editText.getText().toString());
            editText.setText("");
        }

        @Override
        public void onCameraChange(CameraPosition position){
            projection = mMap.getProjection();
            markers.updateCircles();
        }
    }


}
