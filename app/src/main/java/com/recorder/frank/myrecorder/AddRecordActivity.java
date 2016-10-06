package com.recorder.frank.myrecorder;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import jxl.*;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class AddRecordActivity extends AppCompatActivity {

    Spinner spinner_c;
    Spinner spinner_2;
    private ArrayAdapter<String> aspnCountries;
    private List<String> allcountries;

    private EditText showDate = null;
    private Button pickDate = null;
    private EditText showTime = null;
    private Button pickTime = null;

    private static final int SHOW_DATAPICK = 0;
    private static final int DATE_DIALOG_ID = 1;
    private static final int SHOW_TIMEPICK = 2;
    private static final int TIME_DIALOG_ID = 3;

    public static final String MY_RECORD_FILE_HEADER="/sdcard/easy_recorder/";

    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;
    private int status;

    //modify:
    private String date;
    private String time;
    private String type;
    private String what_to_do;
    private String how_much;

    private int mod_row;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mod_row = -1; //default -1
        final Calendar c = Calendar.getInstance();

        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        Intent it = this.getIntent();
        setContentView(R.layout.add_record);
        if((it.getSerializableExtra("activityMain")).equals("modify")) {
            status = 1; //modify
            mod_row = (int)it.getSerializableExtra("row");
            parseModifyRow(mod_row);
        }
        else
            status = 0; //add
        find_and_modify_view(type);
        initializeViews_date_time();

        setDateTime(status);
        setTimeOfDay(status);
        if(status==1)
        {
            TextView tv_wtd = (TextView)findViewById(R.id.text_what_to_do);
            tv_wtd.setText(what_to_do);
            TextView tv_hm = (TextView)findViewById(R.id.text_how_much);
            tv_hm.setText(how_much);
        }

        Button button_ok = (Button)findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 如果没有创建或打开Excel文件
                // excel文件名格式为 MY_RECORD_FILE_HEADER+ 2016.9.xls
                String MY_RECORD_FILE = String.format("%s%d.%d.xls", MY_RECORD_FILE_HEADER, mYear, mMonth + 1);
                File f = new File(MY_RECORD_FILE);
                if(!f.exists())
                {
                    createExcel(MY_RECORD_FILE);
                }
                if(((Spinner)findViewById(R.id.spinner_type)).getSelectedItem().toString().isEmpty()
            ||((EditText)findViewById(R.id.text_what_to_do)).getText().toString().isEmpty()
                        ||((EditText)findViewById(R.id.text_how_much)).getText().toString().isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "记录不完全，无法保存",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                updateExcel(
                        MY_RECORD_FILE,
                        ((EditText)findViewById(R.id.editText_date)).getText().toString(),
                        ((EditText)findViewById(R.id.editText_time)).getText().toString(),
                        ((Spinner)findViewById(R.id.spinner_type)).getSelectedItem().toString(),
                        ((EditText)findViewById(R.id.text_what_to_do)).getText().toString(),
                        ((EditText)findViewById(R.id.text_how_much)).getText().toString(),
                        mod_row
                );
                Intent intent1 = new Intent(AddRecordActivity.this, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent1, 1);
                finish();
            }
        });

        Button button_cancel = (Button)findViewById(R.id.button_cancel);
        button_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void parseModifyRow(int position)
    {
        String filename = String.format("%s%d.%d.xls", MY_RECORD_FILE_HEADER, mYear, mMonth + 1);
        try {
            Workbook rwb = Workbook.getWorkbook(new File(filename));
            Sheet st = rwb.getSheet(0);

            Cell cell = st.getCell(0,position);

            date = cell.getContents();
            cell = st.getCell(1,position);
            time = cell.getContents();
            cell = st.getCell(2,position);
            type = cell.getContents();
            cell = st.getCell(3,position);
            what_to_do = cell.getContents();
            cell = st.getCell(4,position);
            how_much = cell.getContents();
            rwb.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "导入修改记录失败",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void createExcel(String filename) {
        try {
            //创建文件夹：
            Runtime.getRuntime().exec("mkdir -p " + MY_RECORD_FILE_HEADER);
            Runtime.getRuntime().exec("sync");
            WritableWorkbook book = Workbook.createWorkbook(new File(
                    filename));
            // 生成名为“第一页”的工作表,参数0表示这是第一页
            book.createSheet("第一页", 0);

            // 写入数据并关闭文件
            book.write();
            book.close();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "创建记录文件失败。。。",
                    Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * jxl暂时不提供修改已经存在的数据表,这里通过一个小办法来达到这个目的,不适合大型数据更新! 这里是通过覆盖原文件来更新的.
     *
     *
     */
    public void updateExcel(String filename, String date, String time, String type, String what, String how_much,int mod_row) {
        //添加一行
        try {
            String MY_RECORD_FILE_BAK = filename + "bak";
            Workbook rwb = Workbook.getWorkbook(new File(filename));
            WritableWorkbook wwb = Workbook.createWorkbook(new File(
                    MY_RECORD_FILE_BAK), rwb);// copy
            WritableSheet ws = wwb.getSheet(0);
            int row = mod_row>=0?mod_row:ws.getRows();
            Label label_date = new Label(0, row, date);
            ws.addCell(label_date);
            Label label_time = new Label(1, row, time);
            ws.addCell(label_time);
            Label label_type = new Label(2, row, type);
            ws.addCell(label_type);
            Label label_what_to_do = new Label(3, row, what);
            ws.addCell(label_what_to_do);
            Label label_how_much = new Label(4, row, how_much);
            ws.addCell(label_how_much);

            wwb.write();
            wwb.close();
            rwb.close();
            Runtime.getRuntime().exec("rm " + filename);
            Runtime.getRuntime().exec("mv " + MY_RECORD_FILE_BAK+ " " + filename);
            Toast.makeText(getApplicationContext(), "添加纪录成功",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "创建记录失败。。。",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private final String[] mCountries = {"","衣", "食", "住", "行", "其他"};

    private void find_and_modify_view(String type) {
        spinner_c = (Spinner) findViewById(R.id.spinner_type);
        int position = 0;
        allcountries = new ArrayList<String>();
        for (int i = 0; i < mCountries.length; i++) {
            allcountries.add(mCountries[i]);
            if(mCountries[i].equals(type))
                position = i;
        }

        aspnCountries = new ArrayAdapter<String>(this,
                R.layout.my_spinner, allcountries);
        aspnCountries
                .setDropDownViewResource(R.layout.my_spinner_down);
        spinner_c.setAdapter(aspnCountries);
        spinner_c.setSelection(position, true);
    }

    /**
     * 初始化控件和UI视图
     */
    private void initializeViews_date_time(){
        showDate = (EditText) findViewById(R.id.editText_date);
        //pickDate = (Button) findViewById(R.id.pickdate);
        showTime = (EditText)findViewById(R.id.editText_time);
        //pickTime = (Button)findViewById(R.id.picktime);

        showDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Message msg = new Message();
                if (showDate.equals((EditText) v)) {
                    msg.what = AddRecordActivity.SHOW_DATAPICK;
                }
                AddRecordActivity.this.dateandtimeHandler.sendMessage(msg);
            }
        });

        showTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Message msg = new Message();
                if (showTime.equals((EditText) v)) {
                    msg.what = AddRecordActivity.SHOW_TIMEPICK;
                }
                AddRecordActivity.this.dateandtimeHandler.sendMessage(msg);
            }
        });
    }

    /**
     * 设置日期
     */
    private void setDateTime(int status){
        final Calendar c = Calendar.getInstance();

        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        updateDateDisplay(status);
    }

    /**
     * 更新日期显示
     */
    private void updateDateDisplay(int status){
        if(status == 1)
            showDate.setText(date);
        else
            showDate.setText(new StringBuilder().append(mYear).append("-")
                .append((mMonth + 1) < 10 ? "0" + (mMonth + 1) : (mMonth + 1)).append("-")
                .append((mDay < 10) ? "0" + mDay : mDay));
    }

    /**
     * 日期控件的事件
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;

            updateDateDisplay(0);
        }
    };

    /**
     * 设置时间
     */
    private void setTimeOfDay(int status){
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        updateTimeDisplay(status);
    }

    /**
     * 更新时间显示
     */
    private void updateTimeDisplay(int status){
        if(status==1)
            showTime.setText(time);
        else
            showTime.setText(new StringBuilder().append(mHour).append(":")
                .append((mMinute < 10) ? "0" + mMinute : mMinute));
    }

    /**
     * 时间控件事件
     */
    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mHour = hourOfDay;
            mMinute = minute;

            updateTimeDisplay(0);
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
                        mDay);
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, true);
        }

        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
                break;
            case TIME_DIALOG_ID:
                ((TimePickerDialog) dialog).updateTime(mHour, mMinute);
                break;
        }
    }

    /**
     * 处理日期和时间控件的Handler
     */
    Handler
            dateandtimeHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AddRecordActivity.SHOW_DATAPICK:
                    showDialog(DATE_DIALOG_ID);
                    break;
                case AddRecordActivity.SHOW_TIMEPICK:
                    showDialog(TIME_DIALOG_ID);
                    break;
            }
        }

    };
}
