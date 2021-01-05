package kr.ac.konkuk.runningguide.ui.records;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import kr.ac.konkuk.runningguide.R;


public class RecordsAdapter extends ArrayAdapter<Record> {

    private List list;


    public RecordsAdapter(@NonNull Context context, List<Record> list) {
        super(context, 0, list);
        this.list = list;

    }

    class UserViewHolder{
        ImageView list_item_image;
        TextView list_item_date;
        TextView list_item_runningTime;
        TextView list_item_distance;
        TextView list_item_time;
        TextView list_item_pace;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View rowView = convertView;
        UserViewHolder viewHolder;
        String Status;


        // 기존에 생성했던 뷰라면 재사용하고 그렇지 않으면 XML 파일을 새로 view 객체로 변환
        // 재사용을 하지 않으면, 화면에 출력될 수 있는 최대 record가 5개이더라도 스크롤을 내려 새 record를 출력할 때 마다
        // 6개 이상으로 view 객체를 만들기 때문에 메모리가 낭비됨.
        if (rowView == null) {

            // 레이아웃을 정의한 XML 파일(R.layout.listview_item)을 읽어서 계층 구조의 뷰 객체(rowView)로 변환
            rowView = LayoutInflater.from(getContext()).inflate(R.layout.listview_item, parent, false);


            // view holder의 구성 요소의 값과 한 줄을 구성하는 레이아웃을 연결함.
            viewHolder = new UserViewHolder();

            viewHolder.list_item_image = (ImageView) rowView.findViewById(R.id.list_item_image);
            viewHolder.list_item_date = (TextView) rowView.findViewById(R.id.list_item_date);
            viewHolder.list_item_runningTime = (TextView) rowView.findViewById(R.id.list_item_runningTime);
            viewHolder.list_item_distance = (TextView) rowView.findViewById(R.id.list_item_distance);
            viewHolder.list_item_time = (TextView) rowView.findViewById(R.id.list_item_time);
            viewHolder.list_item_pace = (TextView) rowView.findViewById(R.id.list_item_pace);


            rowView.setTag(viewHolder);

            Status = "created";
        }
        else {

            viewHolder = (UserViewHolder) rowView.getTag();

            Status = "reused";
        }

        //record 객체 리스트의 position 위치에 있는  객체를 가져옴.
        Record record = (Record) list.get(position);

        //현재 선택된 Record 객체를 화면에 보여주기 위해서 앞에서 미리 찾아 놓은 뷰에 데이터를 집어 넣음.
        viewHolder.list_item_image.setImageDrawable(record.getImage());
        viewHolder.list_item_date.setText(record.getDate());
        viewHolder.list_item_runningTime.setText(record.getStartTime()+" ~ "+record.getFinishTime());
        viewHolder.list_item_distance.setText(record.getDistance() + "km");
        viewHolder.list_item_time.setText(record.getTime()/60+"분 "+record.getTime()%60+"초");
        viewHolder.list_item_pace.setText(record.getPace()/60+"' "+record.getPace()%60+"\"");

        return rowView;
    }

    //position에 해당하는 Record 객체 반환
    @Nullable
    @Override
    public Record getItem(int position) {
        return (Record) list.get(position);
    }
}
