package kr.ac.konkuk.runningguide;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback,LocationListener {

    private boolean mLocationPermissionGranted = false;
    private int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    Location location;
    int UPDATE_INTERVAL_MS = 1000;
    int FASTEST_UPDATE_INTERVAL_MS = 500;

    GoogleMap gMap;
    Marker currentMarker = null;
    Location currentLocation;
    LatLng currentPosition = null;
    LatLng startPosition = null;
    LatLng lastPosition = null;

    boolean isRunning;
    boolean canPressBackButton;
    boolean isRunForLongTermGoal;
    double totalDistance = 0;
    double totalTime = 0;
    double targetDistance;
    int targetTime;

    RunningTimer runningTimer;
    RunningTimer.TaskToDo taskToDo;

    TextView targetDistanceView;
    TextView targetTimeView;
    TextView targetPaceView;
    TextView runningDistanceView;
    TextView runningTimeView;
    TextView runningPaceView;

    long now;
    SimpleDateFormat dateFormat;
    SimpleDateFormat timeFormat;
    String runningDate;
    String startTime;
    String finishTime;
    int pace;

    static final int saveRecord = 1;

    //위치 정보 요청 LocationRequest 객체
    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    //목표 거리 완료 시 진동으로 알림을 주기 위함
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        //위치 정보를 받아오기 위한 FusedLocationProviderClient
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        MapFragment mapFragment = (MapFragment) getFragmentManager() .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));     //yyyy-MM-dd
        timeFormat = new SimpleDateFormat(getString(R.string.timeFormat));     //HH:mm:ss

        isRunning = false;
        canPressBackButton = true;

        //MainActivity의 homeFragment에서 넘긴 인텐트에서 값을 받아옴. 사용자가 Long-Term Goal 목표로 러닝을 하는지, 목표 거리, 시간
        //자유 목표로 러닝할 경우, 목표 시간은 주어지지 않음.
        isRunForLongTermGoal = getIntent().getBooleanExtra("Long-TermGoal",false);
        targetDistance = getIntent().getDoubleExtra("TARGET_DISTANCE",1);
        targetTime = getIntent().getIntExtra("TARGET_TIME",0);

        //목표 거리 화면에 출력
        targetDistanceView = (TextView) findViewById(R.id.targetDistance);
        targetDistanceView.setText(targetDistance+"km");

        targetTimeView = (TextView) findViewById(R.id.targetTime);
        targetPaceView = (TextView) findViewById(R.id.targetPace);

        //사용자가 Long-Term Goal 목표로 러닝할 경우, 목표 거리, 시간 모두 주어지므로
        //목표 평균 페이스를 구할 수 있음.
        if(isRunForLongTermGoal){

            int targetPace = (int) Math.round((1/targetDistance) * targetTime);

            targetTimeView.setText(targetTime/60 +"분 "+ targetTime%60 + "초");
            targetPaceView.setText(targetPace/60 + "' " + targetPace%60 + "\"");
        }

        //사용자가 현재 뛴 거리, 러닝 시간, 현재 평균 페이스를 출력하기 위한 TextView
        runningDistanceView = (TextView) findViewById(R.id.runningDistance);
        runningTimeView = (TextView) findViewById(R.id.runningTime);
        runningPaceView = (TextView) findViewById(R.id.runningPace);

        //일시정지, 정지, 시작 버튼
        final Button pauseButton = (Button) findViewById(R.id.runningPauseButton);
        final Button stopButton = (Button) findViewById(R.id.runningStopButton);
        final Button resumeButton = (Button) findViewById(R.id.runningResumeButton);

        //일시 정지 버튼을 누르면 일시정지 버튼은 사라지고 다시시작 버튼과 정지 버튼이 나타남.
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);

                isRunning = false;
                taskToDo.cancel();
            }
        });

        //다시시작 버튼을 누르면 다시시작 버튼, 정지버튼은 사라지고 일시정지 버튼 나타남.
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);

                isRunning = true;

                //RunningActivity에서 resumeButton을 처음 누르는 경우
                //(측정 시작)
                if(canPressBackButton == true)
                {
                    canPressBackButton = false;
                    now = System.currentTimeMillis();
                    runningDate = dateFormat.format(new Date(now));
                    startTime = timeFormat.format(new Date(now));
                }

                runningTimer = new RunningTimer();
                runningTimer.setTimer(0,100);

                if(startPosition == null)
                {
                    startPosition = currentPosition;
                }
            }
        });

        //정지 버튼을 누르면 기록을 파일로 저장.
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveRecord();
            }
        });

    }

    //기록 측정 중에는 뒤로가기 버튼을 사용할 수 없도록 막는다.
    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        if (canPressBackButton){
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
            super.onBackPressed();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        gMap = googleMap;

        setDefaultLocation();

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        else{
            checkPermission();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentPosition = new LatLng( location.getLatitude(), location.getLongitude());
        setCurrentLocation(location);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getApplicationContext());
            alertDialogBuilder.setMessage(getString(R.string.userDeniedPermission));

            alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        return mLocationPermissionGranted;
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

    //현재 위치에 구글지도 마커 표시
    public void setCurrentLocation(Location location){

        if(currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();

        //location 객체는 locationCallBack에서 업데이트 해줌.
        currentPosition = new LatLng(location.getLatitude(),location.getLongitude());
        markerOptions.position(currentPosition);
        currentMarker = gMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentPosition,16);
        gMap.moveCamera(cameraUpdate);

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //사용자의 위치가 변경되면 호출됨.
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {

                //location = locationList.get(0);
                location = locationList.get(locationList.size() - 1);

                //바로 직전 좌표와 현재 좌표간의 거리 차이를 계산하기 위해
                //직전 좌표를 lastPosition에 저장하고 currentPosition을 받아옴.
                lastPosition = currentPosition;
                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                //러닝 중이라면 거리, 페이스 갱신하여 출력
                if(lastPosition != null && isRunning == true) {

                    //distance 메소드는 두 좌표간 거리를 구해줌.
                    totalDistance = totalDistance +
                            distance(lastPosition.latitude, lastPosition.longitude, currentPosition.latitude, currentPosition.longitude);

                    runningDistanceView.setText(Math.floor(totalDistance * 100) / 100.0+ "km");

                    //페이스: 1km를 몇 분에 뛰는지
                    pace = (int) Math.round((1/totalDistance) * totalTime);
                    runningPaceView.setText(pace/60 + "' " + pace%60 + "\"");

                    //목표 거리를 달성했을 때
                    if(totalDistance >= targetDistance){

                        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

                        //0.5초 동안 진동 이후 0.05초 대기, 0.5초 진동, 반복 안함.
                        long[] pattern = {500,50,500};
                        vibrator.vibrate(pattern,-1);
                        saveRecord();
                    }

                }

                //현재 위치에 마커 생성하고 이동
                setCurrentLocation(location);

                currentLocation = location;
            }


        }

    };

    //위치 정보 업데이트 시작
    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {
            checkPermission();
        }
        else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ) {
                return;
            }

            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                gMap.setMyLocationEnabled(true);

        }

    }

    //두 좌표간 거리 구하는 메소드
    private double distance(double lat1, double lon1, double lat2, double lon2) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;
        return (dist);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    //runningTimeView에 사용자가 뛴 시간을 1초마다 갱신해 주어야 함.
    public class RunningTimer{
        private Timer timer;

        public class TaskToDo extends TimerTask {

            //타이머가 실행할 메소드
            @Override
            public void run(){
                //Main Thread 에서만 UI를 조작할 수 있기 때문에
                //Timer Thread 내에서 View를 조작하려 하면 CalledFromWrongThreadException이 발생하여
                //runOnUiThread 에서 setText 실행.
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        totalTime = totalTime + 0.1;
                        int sec = (int) Math.floor(totalTime);
                        runningTimeView.setText(sec/60 +"분 "+ sec%60 + "초");
                    }
                });
            }
        }

        public void setTimer(long delay,long period){
            timer = new Timer();

            taskToDo = new TaskToDo();
            timer.schedule(taskToDo,delay,period);
        }
    }

    //사용자의 러닝 기록을 .txt파일 형태로 /data/data/kr.ac.konkuk.runningguide/files/records/에 저장
    //.txt 파일은 러닝 날짜, 시작 시간, 종료 시간, 목표 거리, 목표 시간, 러닝 거리, 러닝 시간, 평균 페이스를 포함.
    public void saveRecord(){
        now = System.currentTimeMillis();
        finishTime = timeFormat.format(new Date(now));

        //저장되는 파일의 이름은 러닝 날짜 + 시작시간.txt임.
        //정상적으로 어플 내에서 기록을 저장했다면 파일의 이름이 중복될 수 없음.
        String fileName = runningDate + " " + startTime + ".txt";
        String pathName = getApplicationContext().getFilesDir() + "/records/";
        File dir = new File(pathName);

        // 내부 저장소 /data/data/패키지 이름/files에 records 디렉토리가 없다면 새로 생성.
        if(!dir.exists()){
            dir.mkdir();
        }

        //.txt 파일에 포함할 내용
        String content = isRunForLongTermGoal + "\n"
                + runningDate + "\n"
                + startTime + "\n"
                + finishTime + "\n"
                + targetDistance + "\n"
                + targetTime + "\n"
                + Math.floor(totalDistance * 100) / 100.0 + "\n"
                + (int) Math.floor(totalTime) + "\n"
                + pace;

        try {

            //openFileOuput은 파일 이름에 /(path separator) 사용할 수 없어서 new FileOutputStream 사용
            FileOutputStream fos = new FileOutputStream(pathName+fileName);
            fos.write(content.getBytes());
            fos.close();

        }
        catch (IOException e){
            e.printStackTrace();
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(runningDate+"\n\n목표 거리: " + targetDistanceView.getText() +"\n목표 시간: "
                + targetTimeView.getText() + "\n목표 평균 페이스: " + targetPaceView.getText() +"\n\n러닝 거리: "
                + runningDistanceView.getText() + "\n러닝 시간: " + runningTimeView.getText() + "\n평균 페이스: " + runningPaceView.getText() +"\n\n저장되었습니다.");

        alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}




