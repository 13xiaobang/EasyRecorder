package com.recorder.frank.myrecorder;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tools.titlepopup.TitlePopup;
import com.tools.titlepopup.ActionItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import static com.tools.titlepopup.Util.getDateString;


public class MainActivity extends AppCompatActivity {
    private TitlePopup titlePopup;
    private ArrayList<String> arr;
    private Map record_map;
    private String filename;
    ListView content_list;
    ArrayAdapter<String> arrayAdapter;
    int row_ready_tobe_del;
    int cur_sel;

    public class AddRecord implements TitlePopup.OnItemOnClickListener {
       @Override
        public void onItemClick(ActionItem item, int position) {
            if(item.mTitle.toString().equals("添加纪录"))
            {
                Intent intent1 = new Intent(MainActivity.this, AddRecordActivity.class);
                intent1.putExtra("activityMain", "add");
                intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent1, 1);
            }
           else if(item.mTitle.toString().equals("修改纪录"))
           {
               Intent intent1 = new Intent(MainActivity.this, AddRecordActivity.class);
               intent1.putExtra("activityMain", "modify");
               intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
               if(record_map.isEmpty() || cur_sel<0)
               {
                   Toast.makeText(getApplicationContext(), "未选中任何记录，无法修改。",
                           Toast.LENGTH_SHORT).show();
                   return;
               }
               Log.e("position", "position="+ cur_sel);
               intent1.putExtra("row", (int)record_map.get(cur_sel));
               startActivityForResult(intent1, 1);
           }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        setContentView(R.layout.activity_main);
        (findViewById(R.id.imageButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                titlePopup.show(v);
            }
        });
        content_list = (ListView)findViewById(R.id.history_view);
        //定义一个数组
        //格式： 9月24日|16:12|吃完饭|5.5元
        arr = new ArrayList<String>();
        record_map = new HashMap();
        load_record_to_arr(); // init arr
        //将数组包装ArrayAdapter
        arrayAdapter = new ArrayAdapter<String>(
                this , android.R.layout.simple_list_item_1, arr);

        cur_sel = -1;
        //为ListView设置Adapter
        content_list.setAdapter(arrayAdapter);

        content_list.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                  cur_sel = (int)arg3;
            }
        });

        content_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int arg2, long arg3) {
                row_ready_tobe_del = (int)arg3;
                // TODO Auto-generated method stub
                new AlertDialog.Builder(MainActivity.this).setTitle("确认删除该项吗？")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeRowExcel(filename, (int)record_map.get(row_ready_tobe_del));
                                reflash_mainactivity();

                            }
                        })
                        .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击“返回”后的操作,这里不设置没有任何操作
                            }
                        }).show();

                return true;
            }
        });
        TextView tv = (TextView)findViewById(R.id.textView_title);
        tv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent2= new Intent(MainActivity.this, IntroductionActivity.class);
                startActivityForResult(intent2, 1);

            }
        });
    }
    private void reflash_mainactivity()
    {
        arr.clear();
        record_map.clear();
        load_record_to_arr();
        arrayAdapter.notifyDataSetChanged();
    }
    private void removeRowExcel(String filename, int row)
    {
        try{
            Workbook book  =  Workbook.getWorkbook( new  File( filename ));
            WritableWorkbook wbook = Workbook.createWorkbook(new File(filename), book);//
            //  获得第一个工作表对象
            WritableSheet sheet  =  wbook.getSheet( 0 );
            sheet.removeRow(row);
            wbook.write();
            wbook.close();
            book.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "删除成功！",
                Toast.LENGTH_SHORT).show();
    }
    private ArrayList<String>  load_record_to_arr()
    {
        String record;
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month  = c.get(Calendar.MONTH)+1;

        filename = AddRecordActivity.MY_RECORD_FILE_HEADER +  year + "." + month + ".xls";
        File f = new File(filename);
        if(!f.exists())
            return arr;
        try {
            Workbook rwb = Workbook.getWorkbook(new File(filename));
            String dateStr = getDateString().toString();

            Sheet st = rwb.getSheet(0);
            int row = st.getRows();
            for(int i = 0, record_row = 0; i < row; i++)
            {
                Cell cell = st.getCell(0,i);
                if(!dateStr.equals(cell.getContents()))
                    continue;
                record = cell.getContents() + "|";
                cell = st.getCell(1,i);
                record = record + cell.getContents() + "|";
                cell = st.getCell(2,i);
                record = record + cell.getContents() + "|";
                cell = st.getCell(3,i);
                record = record + cell.getContents() + "|";
                cell = st.getCell(4,i);
                record = record + cell.getContents() + "元";
                arr.add(record);
                record_map.put(record_row++, i);
            }
            if(arr.isEmpty())
                arr.add("今天还没有记录哦，添加一个吧？");
            rwb.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "加载记录失败。。。",
                    Toast.LENGTH_SHORT).show();
        }
        return arr;
    }

    private void init(){
        titlePopup = new TitlePopup(this, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titlePopup.addAction(new ActionItem(this, "添加纪录", R.drawable.mm_title_btn_receiver_normal));
        titlePopup.addAction(new ActionItem(this, "修改纪录", R.drawable.mm_title_btn_set_normal));
        titlePopup.setItemOnClickListener(new AddRecord());
    }
}
