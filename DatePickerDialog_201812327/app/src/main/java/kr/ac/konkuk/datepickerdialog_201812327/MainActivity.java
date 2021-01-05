package kr.ac.konkuk.datepickerdialog_201812327;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button setDateButton, setTimeButton, setNumberButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 날짜선택, 시간선택, 인원수 선택 버튼
        setDateButton = (Button)findViewById(R.id.setDateButton);
        setTimeButton = (Button)findViewById(R.id.setTimeButton);
        setNumberButton = (Button)findViewById(R.id.setNumberButton);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(final View v){
        if(v == setDateButton)//날짜 선택 버튼 클릭 시
        {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    setDateButton.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                }
            }, year, month, day);
            datePickerDialog.show();
        }

        if(v == setTimeButton)//시간 선택 버튼 클릭 시
        {
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    setTimeButton.setText(hourOfDay + ":" + minute);
                }
            }, hour, minute, false);
            timePickerDialog.show();
        }

        if(v == setNumberButton)//인원 수 선택 버튼 클릭 시
        {
            //인원 수 입력 커스텀 다이얼로그
            final Dialog numberDialog = new Dialog(this);
            numberDialog.setContentView(R.layout.custom_dialog);

            //커스텀 다이얼로그 내 인원 수 입력 EditText, 확인 버튼, 취소 버튼
            final EditText numberTextView = (EditText) numberDialog.findViewById(R.id.numberOfPeople);
            Button okButton = (Button) numberDialog.findViewById(R.id.ok);
            Button cancelButton = (Button) numberDialog.findViewById(R.id.cancel);

            //확인 버튼 onClickListener 등록
            okButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){

                    //다이얼로그 창 닫은 뒤 사용자가 입력한 인원 수 출력
                    numberDialog.dismiss();
                    setNumberButton.setText("인원 수: " + numberTextView.getText());
                }
            });

            //취소 버튼 onClickListener 등록
            cancelButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {

                    //다이얼로그 창 닫음
                    numberDialog.dismiss();
                }
            });

            //다이얼로그 창 출력
            numberDialog.show();

        }

    }
}
