package kr.ac.konkuk.runningguide.ui.setting;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import kr.ac.konkuk.runningguide.AlarmReceiver;
import kr.ac.konkuk.runningguide.DeviceBootReceiver;
import kr.ac.konkuk.runningguide.R;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends Fragment {

    private final static String TAG = "SettingFragment";

    String DistanceValues[] = {"0.5","1","1.5","2","2.5","3","3.5","4","4.5","5",
            "5.5","6","6.5","7","7.5","8","8.5","9","9.5","10"};
    String SecValues[] = {"0","5","10","15","20","25","30","35","40","45","50","55"};

    TextView currentLongTermGoal;
    NumberPicker longTermTargetDistancePicker;
    NumberPicker longTermTargetMinPicker;
    NumberPicker longTermTargetSecPicker;
    Button saveLongTermGoalButton;

    ToggleButton[] toggleButtons;
    ToggleButton notification_Mon_toggleButton;
    ToggleButton notification_Tue_toggleButton;
    ToggleButton notification_Wed_toggleButton;
    ToggleButton notification_Thu_toggleButton;
    ToggleButton notification_Fri_toggleButton;
    ToggleButton notification_Sat_toggleButton;
    ToggleButton notification_Sun_toggleButton;

    CheckBox notification_checkbox;
    Button saveNotificationButton;

    int longTermGoalTargetTime;
    double longTermGoalTargetDistance;

    SimpleDateFormat dateFormat;
    String longTermGoalStartDate;
    String longTermGoalFinishDate;

    SharedPreferences pref_longTermGoal;
    SharedPreferences.Editor editor_longTermGoal;

    SharedPreferences pref_notification;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_setting, container, false);

        currentLongTermGoal = root.findViewById(R.id.currentLongTermGoal);
        dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));     //yyyy-MM-dd

        //shared preference에서 Long-Term Goal 가져옴
        pref_longTermGoal = getContext().getSharedPreferences(getString(R.string.SharedPreferences_LongTermGoal), MODE_PRIVATE);
        editor_longTermGoal = pref_longTermGoal.edit();
        longTermGoalFinishDate = pref_longTermGoal.getString(getString(R.string.longTermGoalFinishDate),"");

        //설정된 Long-Term Goal 없음.
        if(longTermGoalFinishDate.equals("")){
            currentLongTermGoal.setText(getString(R.string.ifNoLongTermGoal));
        }
        //Lont-Term Goal 설정되어 있음.
        else{
            dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));
            try {
                long now = System.currentTimeMillis();

                //설정된 Long-Term Goal의 Finish Date 를 지났을 때
                //Shared Preferences 삭제.
                if((dateFormat.parse(longTermGoalFinishDate).getTime() - now) < 0){
                    editor_longTermGoal.clear();
                    editor_longTermGoal.commit();
                    currentLongTermGoal.setText(getString(R.string.ifNoLongTermGoal));
                }
                //설정된 Long-Term Goal의 Finish Date 를 지나지 않았을 때
                else {
                    longTermGoalStartDate = pref_longTermGoal.getString(getString(R.string.longTermGoalStartDate),"");
                    longTermGoalTargetDistance = Double.longBitsToDouble(pref_longTermGoal.getLong(getString(R.string.longTermGoalTargetDistance),0));
                    longTermGoalTargetTime = pref_longTermGoal.getInt(getString(R.string.longTermGoalTargetTime),0);

                    currentLongTermGoal.setText(longTermGoalStartDate+" ~ "+ longTermGoalFinishDate +"\n\n"
                            +"목표 거리: "+ longTermGoalTargetDistance +"km\n" + "목표 시간: " + longTermGoalTargetTime /60+"분 "+ longTermGoalTargetTime %60+"초\n");

                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }




        //목표 거리 설정 NumberPicker
        longTermTargetDistancePicker = root.findViewById(R.id.longTermTargetDistance);
        longTermTargetDistancePicker.setMinValue(0);
        longTermTargetDistancePicker.setMaxValue(19);
        longTermTargetDistancePicker.setDisplayedValues(DistanceValues);

        //목표 시간(분) NumberPicker
        longTermTargetMinPicker = root.findViewById(R.id.longTermTargetMin);
        longTermTargetMinPicker.setMinValue(2);
        longTermTargetMinPicker.setMaxValue(90);

        //목표 시간(초) NumberPicker
        longTermTargetSecPicker = root.findViewById(R.id.longTermTargetSec);
        longTermTargetSecPicker.setMinValue(0);
        longTermTargetSecPicker.setMaxValue(11);
        longTermTargetSecPicker.setDisplayedValues(SecValues);

        //Long-Term Gaol 저장 버튼
        saveLongTermGoalButton = root.findViewById(R.id.saveLongTermGoalButton);
        saveLongTermGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar cal = Calendar.getInstance();

                //목표 설정 날짜.
                longTermGoalStartDate = dateFormat.format(cal.getTime());

                //목표 달성 날짜 : 설정한 날 + 6개월
                //Date 연산을 위해 Calendar 객체 사용
                cal.add(Calendar.MONTH,6);
                longTermGoalFinishDate = dateFormat.format(cal.getTime());

                //사용자는 0.5km 단위로 목표거리 설정가능하나 NumberPicker에는 int 형만 담기기 때문에
                //사용자가 선택한 값을 인덱스로 사용, 상단에 선언했던 DistanceValue 배열에서 값 가져옴.
                longTermGoalTargetDistance = Double.parseDouble(DistanceValues[longTermTargetDistancePicker.getValue()]);

                //목표 시간은 int형으로 선언했고, 사용자가 5분 30초를 선택했다면 longTermTargetTime에는 330이 저장됨.
                longTermGoalTargetTime = longTermTargetMinPicker.getValue() * 60 + Integer.parseInt(SecValues[longTermTargetSecPicker.getValue()]);

                // 저장 버튼 눌렀을 때 호출될 AlertDialog 선언
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setMessage(longTermGoalStartDate+" ~ "+ longTermGoalFinishDate +"\n\n"
                        +"목표 거리: "+ longTermGoalTargetDistance +"km\n" + "목표 시간: " + longTermGoalTargetTime /60+"분 "+ longTermGoalTargetTime %60+"초\n\n"
                +"저장하시겠습니까?\n현재 설정된 Long-Term Goal은 삭제됩니다.");

                //yes 선택 시 Shared Preferences 에 저장됨
                alertDialogBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        editor_longTermGoal.putString(getString(R.string.longTermGoalStartDate), longTermGoalStartDate);
                        editor_longTermGoal.putString(getString(R.string.longTermGoalFinishDate),longTermGoalFinishDate);
                        editor_longTermGoal.putLong(getString(R.string.longTermGoalTargetDistance),Double.doubleToLongBits(longTermGoalTargetDistance));
                        editor_longTermGoal.putInt(getString(R.string.longTermGoalTargetTime), longTermGoalTargetTime);
                        editor_longTermGoal.commit();

                        currentLongTermGoal.setText(longTermGoalStartDate+" ~ "+ longTermGoalFinishDate +"\n\n"
                                +"목표 거리: "+ longTermGoalTargetDistance +"km\n" + "목표 시간" + longTermGoalTargetTime /60+"분 "+ longTermGoalTargetTime %60+"초\n");
                    }
                });

                alertDialogBuilder.setNegativeButton("no", null);

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

            }       //여기까지 saveLongTermGoalButton OnClickListener
        });

        notification_checkbox = root.findViewById(R.id.notification_checkbox);

        notification_Mon_toggleButton = root.findViewById(R.id.notification_Mon_toggleButton);
        notification_Tue_toggleButton = root.findViewById(R.id.notification_Tue_toggleButton);
        notification_Wed_toggleButton = root.findViewById(R.id.notification_Wed_toggleButton);
        notification_Thu_toggleButton = root.findViewById(R.id.notification_Thu_toggleButton);
        notification_Fri_toggleButton = root.findViewById(R.id.notification_Fri_toggleButton);
        notification_Sat_toggleButton = root.findViewById(R.id.notification_Sat_toggleButton);
        notification_Sun_toggleButton = root.findViewById(R.id.notification_Sun_toggleButton);
        
        toggleButtons = new ToggleButton[]{notification_Mon_toggleButton,notification_Tue_toggleButton,notification_Wed_toggleButton,
                notification_Thu_toggleButton,notification_Fri_toggleButton,notification_Sat_toggleButton,notification_Sun_toggleButton};

        saveNotificationButton = root.findViewById(R.id.saveNotificationButton);


        //알림 설정 월화수목금토일 토글 버튼
        //예를 들어 사용자가 화 목 토 알림을 설정했다면 0101010 저장
        //월 화 수 알림 설정했다면 1110000
        SharedPreferences pref_notification = getContext().getSharedPreferences(getString(R.string.SharedPreferences_Notification),MODE_PRIVATE);

        String turnOnToggleButton = pref_notification.getString(getString(R.string.notificationDay),"0000000");
        final SharedPreferences.Editor editor_notification = pref_notification.edit();

        System.out.println("turnOnToggleButton = " + turnOnToggleButton);

        if(!turnOnToggleButton.equals("0000000")){
            for(int i=0; i<7 ;i++){
                if (turnOnToggleButton.charAt(i) == '1'){
                    toggleButtons[i].setChecked(true);
                }
            }
            notification_checkbox.setChecked(true);
        }


        saveNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (notification_checkbox.isChecked() == false){
                    editor_notification.putString(getString(R.string.notificationDay),"0000000");
                    editor_notification.commit();

                    return;
                }

                int sum = 0;
                for (int i = 0; i <7; i++){
                    if(toggleButtons[i].isChecked()){
                        sum = sum + (int) Math.pow(2,6-i);
                    }
                }

                String turnedOnToggleButton = String.format("%07d",Integer.parseInt(Integer.toBinaryString(sum)));

                System.out.println("turnedOntoggleButton = "+turnedOnToggleButton);

                editor_notification.putString(getString(R.string.notificationDay),turnedOnToggleButton);
                editor_notification.commit();

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 12);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                // 이미 정오가 넘었다면 다음날 호출되도록 함
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                Toast.makeText(getContext(),"설정하신 요일로 알람이 설정되었습니다!", Toast.LENGTH_SHORT).show();

                editor_notification.putLong("nextNotifyTime", (long)calendar.getTimeInMillis());
                editor_notification.apply();
                diaryNotification(calendar);
            }
        });

        return root;
    }

    void diaryNotification(Calendar calendar)
    {

        PackageManager pm = getContext().getPackageManager();
        ComponentName receiver = new ComponentName(getContext(), DeviceBootReceiver.class);

        Intent alarmIntent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);



        if (alarmManager != null) {

            //알림의 타입에는 ELAPSED_REALTIME, RTC , ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP 이 있다.
            //running guide에서는 알림을 정오에 발생시키므로 RTC가 맞고, 화면이 꺼져있는 상태에서 켜지면서 알림이 가도록 하고 싶어서
            //WAKEUP을 선택했다. 하지만 사용자가 디바이스의 시간을 임의로 설정하면 알림이 제대로 발생하지 않을 수 있다.
            //알림은 매일 발생하도록 설정하고, AlarmManager에서 push 알림을 설정한 요일이 맞으면 notification을 발생시킨다.
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pendingIntent);

            //VERSION_CODES.M(Marshmallow) 버전 부터 DOZE 모드가 생겨 DOZE 상태에서는 알림이 울리지 않는다.
            //setExactAndAllowWhileIdle 을 해주면 DOZE 모드에서도 알림이 울린다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }

        // 부팅 후 실행되는 리시버 사용가능하게 설정
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
}