package kr.ac.konkuk.timerservice_201812327;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;


public class TimerService extends Service {

    String TAG = "TimerService";
    TestTimer testTimer;
    TestTimer.TaskToDo taskToDo;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        System.out.println("onCreate");
        testTimer = new TestTimer();
        testTimer.totalTime = 0;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        Toast.makeText(this, "Timer Service 중지, 경과시간 : " + testTimer.totalTime + "초", Toast.LENGTH_LONG)
                .show();
        taskToDo.cancel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG,"onStartCommand");
        Toast.makeText(this, "Timer Service 시작", Toast.LENGTH_LONG)
                .show();

        testTimer.setTimer(0,1000);

        return super.onStartCommand(intent, flags, startId);

    }

    class TestTimer{

        private Timer timer;
        int totalTime;



        public class TaskToDo extends TimerTask {
            public void run(){

                totalTime = totalTime + 1;

            }
        }

        public void setTimer(long delay,long period){
            timer = new Timer();



            taskToDo = new TestTimer.TaskToDo();
            timer.schedule(taskToDo,delay,period);
        }

    }
}

