package com.yourapp.myapplication;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private BottomSheetBehavior<View> behavior;
    private DrawerLayout drawerLayout;
    private GeoPoint userLocation = new GeoPoint(20.5937, 78.9629); // Default (India)
    private GeoPoint selectedLocation;
    private Polyline currentRoute;
    
    // Bottom Sheet elements
    private View weatherLayout;
    private TextView etaText;
    private TextView destTempText;
    private ImageView destWeatherIcon;

    // Main UI elements (Bottom Left)
    private View mainDestWeatherCard;
    private TextView mainDestTempText;
    private ImageView mainDestWeatherIcon;

    // Search elements
    private AutoCompleteTextView searchEditText;
    private final Map<String, GeoPoint> famousCities = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_main);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initFamousCities();
        initMap();
        initUI();
        setupSearch();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bookmarks) {
                Toast.makeText(this, "Traveling Bookmarks clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_contribution) {
                Toast.makeText(this, "Contribution clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_help) {
                Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_about) {
                Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void initFamousCities() {
        famousCities.put("New York", new GeoPoint(40.7128, -74.0060));
        famousCities.put("London", new GeoPoint(51.5074, -0.1278));
        famousCities.put("Paris", new GeoPoint(48.8566, 2.3522));
        famousCities.put("Tokyo", new GeoPoint(35.6762, 139.6503));
        famousCities.put("Mumbai", new GeoPoint(19.0760, 72.8777));
        famousCities.put("Sydney", new GeoPoint(-33.8688, 151.2093));
        famousCities.put("Dubai", new GeoPoint(25.2048, 55.2708));
        famousCities.put("Rome", new GeoPoint(41.9028, 12.4964));
        famousCities.put("New Delhi", new GeoPoint(28.6139, 77.2090));
        famousCities.put("Berlin", new GeoPoint(52.5200, 13.4050));
    }

    private void initMap() {
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        
        map.getController().setZoom(5.0);
        map.getController().setCenter(userLocation);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(userLocation);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Your Location");
        map.getOverlays().add(startMarker);
        
        map.getOverlayManager().add(new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                String placeName = getAddressFromLatLng(p);
                handleLocationSelection(p, placeName != null ? placeName : "Selected Location");
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }

    private void initUI() {
        View bottomSheet = findViewById(R.id.explore_bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight(80); // iOS style peek
        behavior.setHideable(true);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        weatherLayout = findViewById(R.id.destination_weather_layout);
        etaText = findViewById(R.id.eta_text);
        destTempText = findViewById(R.id.dest_temp_text);
        destWeatherIcon = findViewById(R.id.dest_weather_icon);

        mainDestWeatherCard = findViewById(R.id.main_dest_weather_card);
        mainDestTempText = findViewById(R.id.main_dest_temp_text);
        mainDestWeatherIcon = findViewById(R.id.main_dest_weather_icon);

        findViewById(R.id.btn_directions).setOnClickListener(v -> {
            if (selectedLocation != null) {
                drawRoute(userLocation, selectedLocation);
                updateDestinationWeather();
                Toast.makeText(this, "Calculating route...", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_start).setOnClickListener(v -> {
            if (currentRoute != null) {
                Toast.makeText(this, "Navigation started!", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.fab_location).setOnClickListener(v -> {
            map.getController().animateTo(userLocation, 15.0, 1000L);
        });

        findViewById(R.id.fab_layers).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Standard");
            popup.getMenu().add("Transportation");
            popup.getMenu().add("Topographical");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Standard")) map.setTileSource(TileSourceFactory.MAPNIK);
                else if (item.getTitle().equals("Transportation")) map.setTileSource(TileSourceFactory.OpenTopo);
                else map.setTileSource(TileSourceFactory.USGS_SAT);
                return true;
            });
            popup.show();
        });

        findViewById(R.id.img_btn_menu).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.search_edit_text);
        String[] cities = famousCities.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        searchEditText.setAdapter(adapter);

        searchEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            GeoPoint location = famousCities.get(selectedCity);
            if (location != null) {
                handleLocationSelection(location, selectedCity);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        });
    }

    private String getAddressFromLatLng(GeoPoint p) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder fullAddress = new StringBuilder();

                if (address.getFeatureName() != null && !address.getFeatureName().equals(address.getSubThoroughfare())) {
                    fullAddress.append(address.getFeatureName());
                } else if (address.getThoroughfare() != null) {
                    fullAddress.append(address.getThoroughfare());
                }

                String locality = address.getLocality() != null ? address.getLocality() : address.getSubAdminArea();
                if (locality != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(locality);
                }

                return fullAddress.length() > 0 ? fullAddress.toString() : address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void handleLocationSelection(GeoPoint p, String title) {
        selectedLocation = p;
        map.getOverlays().removeIf(overlay -> overlay instanceof Marker && !((Marker)overlay).getTitle().equals("Your Location"));

        Marker marker = new Marker(map);
        marker.setPosition(p);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        map.getOverlays().add(marker);

        map.getController().animateTo(p, 15.0, 1000L);

        ((TextView)findViewById(R.id.sheet_title)).setText(title);
        String coordsText = String.format(Locale.getDefault(), "%.4f, %.4f", p.getLatitude(), p.getLongitude());
        ((TextView)findViewById(R.id.sheet_desc)).setText(coordsText);

        weatherLayout.setVisibility(View.GONE);
        mainDestWeatherCard.setVisibility(View.GONE);

        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        currentRoute = null;
        map.invalidate();
    }

    private void updateDestinationWeather() {
        Random random = new Random();
        int hours = random.nextInt(5) + 1;
        int minutes = random.nextInt(60);
        int temp = random.nextInt(15) + 15;

        String etaStr = String.format(Locale.getDefault(), "ETA: %dh %dm", hours, minutes);
        String tempStr = String.format(Locale.getDefault(), "%d°", temp);

        etaText.setText(etaStr);
        destTempText.setText(tempStr);
        mainDestTempText.setText(tempStr);

        weatherLayout.setAlpha(0f);
        weatherLayout.setVisibility(View.VISIBLE);
        weatherLayout.animate().alpha(1f).setDuration(500).setInterpolator(new AnticipateOvershootInterpolator()).start();

        mainDestWeatherCard.setAlpha(0f);
        mainDestWeatherCard.setVisibility(View.VISIBLE);
        mainDestWeatherCard.animate().alpha(1f).setDuration(500).start();
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        if (currentRoute != null) map.getOverlays().remove(currentRoute);

        currentRoute = new Polyline();
        currentRoute.setColor(Color.parseColor("#007AFF"));
        currentRoute.setWidth(12f);
        List<GeoPoint> pts = new ArrayList<>();
        pts.add(start);
        pts.add(end);
        currentRoute.setPoints(pts);

        map.getOverlays().add(currentRoute);
        map.getController().animateTo(end, 8.0, 1000L);
        map.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
