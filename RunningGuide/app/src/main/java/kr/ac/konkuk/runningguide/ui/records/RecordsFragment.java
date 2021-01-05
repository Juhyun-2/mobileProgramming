package kr.ac.konkuk.runningguide.ui.records;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kr.ac.konkuk.runningguide.R;

import static android.view.View.GONE;

public class RecordsFragment extends Fragment {

    View root;
    String pathName;
    File dir;
    File[] recordFiles;
    TextView noRecordsTextView;
    ListView listview;
    List<Record> records;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_records, container, false);
        noRecordsTextView = root.findViewById(R.id.noRecords);


        showRecords();

        return root;
    }

    public void showRecords(){

        noRecordsTextView.setVisibility(View.VISIBLE);

        pathName = getActivity().getApplicationContext().getFilesDir() + "/records/";
        dir = new File(pathName);
        if(!dir.exists()){
            dir.mkdir();
        }

        // /data/data/kr.ac.konkuk.runningguide/files/records 아래에 파일이 있다면
        if(dir.list().length > 0) {

            noRecordsTextView.setVisibility(GONE);
            recordFiles = dir.listFiles();

            listview = (ListView) root.findViewById(android.R.id.list);

            records = new ArrayList<>();

            for (int i = 0; i < recordFiles.length; i++) {
                try {
                    // 디바이스에 저장되는 파일은 파일 이름 기준 오름차순으로 정렬
                    // 가장 최근 파일을 상단에 출력하고 싶어서 length - i -1
                    FileInputStream fis = new FileInputStream(recordFiles[recordFiles.length - 1 - i]);
                    byte[] buffer = new byte[fis.available()];
                    fis.read(buffer);
                    String string = new String(buffer);

                    /*
                    token 총 9개
                    0 : boolean Long-Term Goal 설정으로 뛰었나
                        true : Long-Term Goal, false : 자유 목표
                    1 : String 러닝 날짜 (yyyy-MM-dd)
                    2 : String 러닝 시작 시간 (HH:mm:ss)
                    3 : String 러닝 종료 시간 (HH:mm:ss)
                    4 : double targetDistance
                    5 : int targetTime
                    6 : double distance (실제로 뛴 거리)
                    7 : int time (실제 뛴 시간 (초))
                    8 : int pace (1km를 몇 초에 뛰는지)
                    */
                    String[] token = string.split("\n");

                    Record record = new Record(null
                            , token[1], token[2], token[3]
                            ,Double.parseDouble(token[4]), Integer.parseInt(token[5])
                            , Double.parseDouble(token[6]), Integer.parseInt(token[7]), Integer.parseInt(token[8]));


                    records.add(record);

                    //사용자가 러닝을 Long-Term Goal 목표로 설정하고 뛰었는지, 자유 목표로 뛰었는지,
                    //설정한 거리를 완주했는지, 목표 시간을 달성했는지에 따라 listview에 출력되는 각 record의 이미지가 다름.

                    //Long-Term Goal 목표로 뛰었을 때
                    if (token[0].equals("true")) {

                        //Long-Term Goal 목표로 뛰었으나, 중간에 그만 둠.
                        if (Double.parseDouble(token[4]) > Double.parseDouble(token[6])) {
                            record.setImage(getResources().getDrawable(R.drawable.ic_tired_regular_color));
                    }
                        //Long-Term Goal Distance 완주, TargetTime 도 달성.
                        else if (Integer.parseInt(token[5]) > Integer.parseInt(token[7])) {
                            record.setImage(getResources().getDrawable(R.drawable.ic_grin_stars_regular));
                        }
                        //Long-Term Goal Distance 완주, Time은 달성 못함.
                        else {
                            record.setImage(getResources().getDrawable(R.drawable.ic_grin_regular_color));
                        }
                    }
                    //자유 목표로 뛰었을 때
                    else {

                        //자유 목표로 뛰다 그만 둠
                        if (Double.parseDouble(token[4]) > Double.parseDouble(token[6])) {
                            record.setImage(getResources().getDrawable(R.drawable.ic_grin_beam_sweat_regular));
                        }

                        //자유 목표 완주
                        else {
                            record.setImage(getResources().getDrawable(R.drawable.ic_grin_regular));
                        }
                    }

                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //listview에 Adapter 등록
            RecordsAdapter recordsAdapter = new RecordsAdapter(getContext(),records);
            listview.setAdapter(recordsAdapter);

            //사용자가 listview에 출력되는 record를 길게 눌렀을 때 러닝 세부 정보 포함한 대화상자 출력
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                    final Record record = (Record) listview.getAdapter().getItem(position);
                    String content = record.getDate()+"\n\n목표 거리: "
                            + record.getTargetDistance()+"km\n목표 시간: "
                            + record.getTargetTime()/60 +"분 " + record.getTargetTime()%60+"초\n목표 평균 페이스: "
                            + ((int) Math.round((1/record.getTargetDistance()) * record.getTargetTime()))/60+"' " +((int) Math.round((1/record.getTargetDistance()) * record.getTargetTime()))%60+"\"\n\n러닝 거리: "
                            + record.getDistance()+"km\n러닝 시간: "
                            + record.getTime()/60 +"분 " + record.getTime()%60+"초\n평균 페이스: "
                            + record.getPace()/60+"' " + record.getPace()/60 +"\"\n";
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                    alertDialogBuilder.setMessage(content);

                    alertDialogBuilder.setPositiveButton("닫기" , null);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });

            //사용자가 listview에 출력되는 record를 길게 눌렀을 때 대화상자 출력, 삭제 가능
            listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {

                    //사용자가 길게 터치한 record 반환. getItem은 RecordAdapter.java에 선언되어 있음.
                    final Record record = (Record) listview.getAdapter().getItem(position);

                    //대화 상자에 출력될 내용(날짜, 거리, 시간, 페이스)
                    String content = record.getDate()+"\n\n목표 거리: "
                            + record.getTargetDistance()+"km\n목표 시간: "
                            + record.getTargetTime()/60 +"분 " + record.getTargetTime()%60+"초\n목표 평균 페이스: "
                            + ((int) Math.round((1/record.getTargetDistance()) * record.getTargetTime()))/60+"' " +((int) Math.round((1/record.getTargetDistance()) * record.getTargetTime()))%60+"\"\n\n러닝 시간: "
                            + record.getDistance()+"km\n러닝 시간: "
                            + record.getTime()/60 +"분 " + record.getTime()%60+"초\n평균 페이스: "
                            + record.getPace()/60+"' " + record.getPace()/60 +"\"\n";

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                    alertDialogBuilder.setMessage(content+"\n 이 기록을 삭제하시겠습니까?");

                    //삭제 버튼 클릭 시 files/record/ 내에 있는 해당 파일 삭제
                    alertDialogBuilder.setPositiveButton("삭제", new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            File recordFile = new File(pathName+record.getDate()+" "+record.getStartTime()+".txt");
                            recordFile.delete();

                            showRecords();
                        }
                    });

                    alertDialogBuilder.setNegativeButton("취소" ,new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    //false 반환하면 그냥 터치와 길게 터치를 구분하지 못함.
                    return true;
                }
            });
        }
    }

}
