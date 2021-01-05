package kr.ac.konkuk.runningguide.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kr.ac.konkuk.runningguide.R;
import kr.ac.konkuk.runningguide.RunningActivity;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,LocationListener {

    private Context context;
    private View mLayout;

    MapView mapView;
    GoogleMap gMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    LatLng currentPosition = null;     //현재 좌표
    Marker currentMarker = null;       //현재 위치 표시
    Location currentLocation;
    Location location;
    int UPDATE_INTERVAL_MS = 2000;
    int FASTEST_UPDATE_INTERVAL_MS = 1000;

    boolean isUserDeniedPermission = false;
    private boolean mLocationPermissionGranted;
    String[] REQUIRED_PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION};
    private static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;

    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    NumberPicker freeRunningTargetPicker;
    boolean isRunForLongTermGoal = true;

    String DistanceValues[] = {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5",
            "5.5","6","6.5","7","7.5","8","8.5","9","9.5","10"};

    String longTermGoalStartDate;
    String longTermGoalFinishDate;
    SimpleDateFormat dateFormat;

    double longTermGoalTargetDistance;
    double targetDistance;      //단위: km

    int longTermGoalTargetTime;
    int targetTime = 0;         //단위: 초
    long monthPassed;

    SharedPreferences pref;
    TextView longTermGoalTarget;

    static final int saveRecord = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        context = container.getContext();
        mLayout = getActivity().findViewById(R.id.container);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        longTermGoalTarget = root.findViewById(R.id.longTermGoalTarget);
        mapView = (MapView) root.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setLongTermGoal();

        //러닝 타입 선택 토글 버튼(Long-term Goal 기반 목표 or 자유 러닝)
        ToggleButton runningTypeToggleButton = (ToggleButton) root.findViewById(R.id.runningTypeToggleButton);
        runningTypeToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getActivity().findViewById(R.id.longTermGoalTarget).setVisibility(View.VISIBLE);
                    getActivity().findViewById(R.id.freeRunningTarget).setVisibility(View.GONE);
                    getActivity().findViewById(R.id.freeRunning_targetkm).setVisibility(View.GONE);

                    setLongTermGoal();
                    isRunForLongTermGoal = true;
                } else {
                    getActivity().findViewById(R.id.longTermGoalTarget).setVisibility(View.GONE);
                    getActivity().findViewById(R.id.freeRunningTarget).setVisibility(View.VISIBLE);
                    freeRunningTargetPicker = getActivity().findViewById(R.id.freeRunningTarget);
                    freeRunningTargetPicker.setMinValue(0);
                    freeRunningTargetPicker.setMaxValue(19);
                    freeRunningTargetPicker.setDisplayedValues(DistanceValues);
                    getActivity().findViewById(R.id.freeRunning_targetkm).setVisibility(View.VISIBLE);

                    isRunForLongTermGoal = false;
                }
            }
        });

        //러닝 시작 버튼 클릭 시 RunningActivity 호출.
        Button runningStartButton = (Button) root.findViewById(R.id.runningStartButton);
        runningStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), RunningActivity.class);

                //toggleButton on (Long-Term Goal)
                if(isRunForLongTermGoal == true)
                {
                    targetDistance = longTermGoalTargetDistance;

                    longTermGoalStartDate = pref.getString(getString(R.string.longTermGoalStartDate),"");
                    dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));     //yyyy-MM-dd
                    try {
                        long diff = dateFormat.parse(longTermGoalStartDate).getTime() - System.currentTimeMillis();

                        //diff는 Long-Term Goal을 설정한 날짜부터 오늘까지 며칠이 흘렀는지를 나타냄(0~179)
                        diff = TimeUnit.DAYS.convert(diff,TimeUnit.MILLISECONDS);

                        //Long-Term Goal을 설정하고 몇 개월이 지났는지(0, 1,...,5개월)에 따라 목표 시간을 다르게
                        monthPassed = diff/30;

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //1개월 째 : Long-Term Goal Target Time * 155% 시간 내에 완주가 목표
                    //2개월 째 : 144%
                    //3개월 째 : 133% ... 6개월 째에 targetTime = longTermGoalTargetTime
                    targetTime = (int) Math.floor(longTermGoalTargetTime * ((15.5 - monthPassed)/10.0));
                    System.out.println("targetTime = "+targetTime);
                }

                //toggleButton off (자유 목표)
                else {
                    //목표 거리는 0.5km 단위이지만 numberPicker는 int 값만 표현 할 수 있기 때문에
                    //DistanceValues String에 거리 값들을 담아 인덱스로 접근
                    targetDistance = Double.parseDouble(DistanceValues[freeRunningTargetPicker.getValue()]);
                }


                //목표 거리, 시간, Long-Term Goal 목표로 러닝을 시작하는지
                intent.putExtra("TARGET_DISTANCE",targetDistance);
                intent.putExtra("TARGET_TIME",targetTime);
                intent.putExtra("Long-TermGoal",isRunForLongTermGoal);
                startActivityForResult(intent,saveRecord);
            }
        });

        return root;
    }

    //onResume, onDestroy, onLowMemory 삭제하면 구글지도 표시 안됨
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();

        //onResume 호출된 경우 위치 정보 업데이트 다시 시작함.
        //사용자가 위치 정보 접근 권한을 거부해서 대화상자 출력된 이후 onResume 호출된 경우 위치 정보 업데이트 시작 x
        if(!isUserDeniedPermission) {
            checkPermission();
            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    @Override
    public void onStop(){
        super.onStop();

        //RunningActivity나 다른 fragment로 이동할 때 HomeFragment에서 위치 정보 업데이트를 멈추지 않으면
        //RunningActivity, HomeFragment 동시에 위치 정보를 받아오므로
        //데이터, 배터리 소모가 커질 수 있음
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void setLongTermGoal(){

        // 저장된 Long-Term Goal 가져오기 위해서 Shared Preference 생성
        pref = getActivity().getSharedPreferences(getString(R.string.SharedPreferences_LongTermGoal), MODE_PRIVATE);
        longTermGoalTargetDistance = Double.longBitsToDouble(pref.getLong(getString(R.string.longTermGoalTargetDistance),0));
        System.out.println("longTermGoalTargetDistance: "+longTermGoalTargetDistance);

        if(longTermGoalTargetDistance == 0){
            longTermGoalTarget.setText(getString(R.string.ifNoLongTermGoal));
        }
        else{
            longTermGoalStartDate = pref.getString(getString(R.string.longTermGoalStartDate),"");
            longTermGoalFinishDate = pref.getString(getString(R.string.longTermGoalFinishDate),"");
            longTermGoalTargetTime = pref.getInt(getString(R.string.longTermGoalTargetTime),0);
            longTermGoalTarget.setText(longTermGoalStartDate + " ~ " + longTermGoalFinishDate + "\n\n"
                    + longTermGoalTargetDistance+"km\t\t"+longTermGoalTargetTime/60 +"분 "+longTermGoalTargetTime%60+"초");
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(this.getActivity());

        gMap = googleMap;

        //구글 맵이 준비 되면 일단 default 위치를 화면에 보여주고
        //권한 설정을 확인 후, 위치 정보에 접근이 가능하면 현재 위치 출력
        setDefaultLocation();

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        else{
            checkPermission();
        }
    }

    //사용자의 현재 위치가 변경되었을 때 자동으로 호출됨
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                //현재 위치에 마커 생성하고 이동
                setCurrentLocation(location);
                currentLocation = location;
            }

        }

    };

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentPosition = new LatLng( location.getLatitude(), location.getLongitude());
        setCurrentLocation(location);
    }

    public void setCurrentLocation(Location location){

        if(currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        currentPosition = new LatLng(location.getLatitude(),location.getLongitude());
        markerOptions.position(currentPosition);
        currentMarker = gMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentPosition,16);
        gMap.moveCamera(cameraUpdate);

    }

    public void setDefaultLocation() {


        //디폴트 위치 건국대학교,
        LatLng DEFAULT_LOCATION = new LatLng(37.5408, 127.0793);

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);

        currentMarker = gMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 16);
        gMap.moveCamera(cameraUpdate);

    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

        } else {

            //사용자에게 권한 설정을 요청함.
            //사용자가 위치 정보 접근 허용을 했는지 하지 않았는지에 대한 결과 값은
            //onRequestPermissionsResult 에서 처리됨.
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        return mLocationPermissionGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {

        // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
        if ( requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grandResults.length == REQUIRED_PERMISSIONS.length) {

            boolean check_result = true;

            // 모든 퍼미션을 허용했는지 체크.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if ( check_result ) {
                startLocationUpdates();
            }

            // 거부한 퍼미션이 있다면 앱을 종료.
            else {
                isUserDeniedPermission = true;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

                // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있음
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), REQUIRED_PERMISSIONS[0])) {

                    alertDialogBuilder.setMessage(getString(R.string.userDeniedPermission));
                }

                //사용자가 Don't ask again 선택
                else {
                    alertDialogBuilder.setMessage(R.string.userCheckedDontAskAgain);
                }

                alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getActivity().finish();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

        }
    }


    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {
            checkPermission();
        }
        else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ) {
                return;
            }

            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                gMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //RunningActivity에서 러닝을 마치고 결과를 정상적으로 저장했을 때
        //MainActivity로 돌아오는데, home Fragment 대신 records Fragment를 출력하기 위함.
        if (requestCode == saveRecord) {
            if (resultCode == RESULT_OK) {
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_home, true)
                        .build();

                //action_navigation_home_to_navigation_records는 nav/mobile_navigation.xml에 선언되어 있음
                //MainActivity로 돌아갔을 때 default로 출력되는 fragment는 homeFragment 이지만, 해당 명령어를 수행하면 recordsFragment가 출력됨.
                Navigation.findNavController(getActivity(),R.id.nav_host_fragment).navigate(R.id.action_navigation_home_to_navigation_records, null, navOptions);
            }
        }
    }

}

