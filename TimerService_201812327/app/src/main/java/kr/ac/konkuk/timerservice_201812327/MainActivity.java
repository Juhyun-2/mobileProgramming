package kr.ac.konkuk.timerservice_201812327;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button timerStartButton = findViewById(R.id.TimerStartButton);
        Button timerStopButton = findViewById(R.id.TimerStopButton);

        timerStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("startButton clicked");
                startService(new Intent(MainActivity.this,TimerService.class));
            }
        });

        timerStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this,TimerService.class));
            }
        });
    }
}