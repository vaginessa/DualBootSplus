package com.h0rn3t.dualbootsplus;
/**
 * Created by h0rn3t on 17.09.2013.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.FileArrayAdapter;
import com.h0rn3t.dualbootsplus.util.Helpers;
import com.h0rn3t.dualbootsplus.util.Item;

import java.util.ArrayList;
import java.util.List;


public class PartActivity extends Activity implements Constants,AdapterView.OnItemClickListener {
    Context context=this;
    private LinearLayout linwait,footer;
    private TextView textinfo;
    private int sdsize=0;

    private int sys_size=1024;
    private int data_size=2048;
    private int storage_size=0;
    private TextView pttable,pstable,ttotal;
    ProgressDialog progressDialog;
    private Boolean canclick=true;
    private String table;
    private ListView packList;
    private FileArrayAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.part);

        linwait = (LinearLayout) findViewById(R.id.layout_wait);
        footer = (LinearLayout) findViewById(R.id.footer);

        textinfo=(TextView) findViewById(R.id.textinfo);
        pttable=(TextView) findViewById(R.id.pttable);
        pstable=(TextView) findViewById(R.id.pstable);
        ttotal=(TextView) findViewById(R.id.ttotal);


        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);

        footer.setVisibility(LinearLayout.GONE);
        Button startBtn=(Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            final String externalsd=Helpers.getExtSD();
            if(!externalsd.equals("")){
                Helpers.infoDialog(context,getString(R.string.error),getString(R.string.no_unmounted));
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.btn_part))
                    .setCancelable(false)
                    .setMessage(getString(R.string.total_size,(storage_size+sys_size+data_size))+"MB\n\n"+getString(R.string.partition_info))
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setPositiveButton(getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new StartPartListener(alertDialog));

            }
        });

        new PartInit().execute();
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.part_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.setting_table:
                showSetablePartitions(table);
                break;
            case R.id.current_table:
                showCurrentPartitions(table);
                break;
            case R.id.revert_table:
                revertPartitions(table);
                break;
        }

        return true;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final Item o = adapter.getItem(position);
        if(!canclick) return;

        editRunDialog(adapter,position);

    }
    private class PartInit extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Helpers.get_assetsBinary("parted", context);
            Helpers.get_assetsBinary("tune2fs", context);

            final StringBuilder sb=new StringBuilder();
            sb.append("busybox chmod 750 ").append(getFilesDir()).append("/parted;\n");
            sb.append("busybox chmod 750 ").append(getFilesDir()).append("/tune2fs;\n");
            sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");
            Helpers.get_assetsScript("run",context,"",sb.toString());
            return Helpers.shExec(getFilesDir()+"/run");
        }

        @Override
        protected void onPostExecute(String result) {
            table=result;
            showSetablePartitions(table);
            linwait.setVisibility(LinearLayout.GONE);
            footer.setVisibility(LinearLayout.VISIBLE);
            Log.d(TAG, "SDCard Size: " + sdsize + "MB");
        }

        @Override
        protected void onPreExecute() {
            footer.setVisibility(LinearLayout.GONE);
            linwait.setVisibility(LinearLayout.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    class StartPartListener implements View.OnClickListener {
        private final Dialog dialog;
        public StartPartListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
            dialog.cancel();
            new Partition().execute();
        }
    }

    private void editRunDialog(final FileArrayAdapter a,int id) {
        final int tid=id;
        LayoutInflater factory = LayoutInflater.from(this);
        final View editDialog = factory.inflate(R.layout.partsize_dialog, null);
        final TextView tv = (TextView) editDialog.findViewById(R.id.editText);
        final TextView tt = (TextView) editDialog.findViewById(R.id.textView2);
        tv.setText(a.getItem(id).getPart().replace("MB",""));
        tt.setText(a.getItem(id).getName());
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.size_title))
                .setView(editDialog)
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int test = 0;
                        if (tv.getText() != null && tv.getText().length()>0) {
                                test = Integer.parseInt(tv.getText().toString());
                        }
                        switch (tid) {
                            case 1:
                                if (reCalcSize(test, 1)) {
                                    sys_size = test;
                                    ttotal.setText(Integer.toString(sys_size+data_size+storage_size)+"MB");
                                    for(int i=0;i<adapter.getCount();i++){
                                        switch (i){
                                            case 0:
                                                a.getItem(0).setPart(storage_size + "MB");
                                                break;
                                            case 1:
                                                a.getItem(1).setPart(sys_size + "MB");
                                                break;
                                            case 2:
                                                a.getItem(2).setPart(data_size + "MB");
                                                break;
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                break;
                            case 2:
                                if (reCalcSize(test,2)) {
                                    data_size = test;
                                    ttotal.setText(Integer.toString(sys_size+data_size+storage_size)+"MB");
                                    for(int i=0;i<adapter.getCount();i++){
                                        switch (i){
                                            case 0:
                                                a.getItem(0).setPart(storage_size + "MB");
                                                break;
                                            case 1:
                                                a.getItem(1).setPart(sys_size + "MB");
                                                break;
                                            case 2:
                                                a.getItem(2).setPart(data_size + "MB");
                                                break;
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                break;
                            case 0:
                                if (reCalcSize(test, 0)) {
                                    storage_size = test;
                                    ttotal.setText(Integer.toString(sys_size+data_size+storage_size)+"MB");
                                    for(int i=0;i<adapter.getCount();i++){
                                        switch (i){
                                            case 0:
                                                a.getItem(0).setPart(storage_size + "MB");
                                                break;
                                            case 1:
                                                a.getItem(1).setPart(sys_size + "MB");
                                                break;
                                            case 2:
                                                a.getItem(2).setPart(data_size + "MB");
                                                break;
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                break;
                        }
                    }
                }).create().show();
    }

    public Boolean reCalcSize(int v, int t){
        Boolean flag=true;
        switch (t){
            case 1:
                if(((v+storage_size+data_size)>sdsize) || (v<min_sys)){
                    flag= false;
                }
                break;
            case 2:
                if(((v+storage_size+sys_size)>sdsize) || (v<min_data)){
                    flag= false;
                }
                break;
            case 0:
                if(((v+data_size+sys_size)>sdsize) || (v<min_storage)){
                    flag= false;
                }
                break;
        }
        return flag;
    }

    private class Partition extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final StringBuilder sb=new StringBuilder();
            final int np=Helpers.getPartsNo(table);

            for (byte i=1;i<=np;i++)
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" rm ").append(Integer.toString(i)).append(";\n");
            //create partitions
            if(adapter.getCount()>1){
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" mkpartfs primary fat32 0 ").append(Integer.valueOf(storage_size)).append(";\n");
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" mkpartfs primary ext2 ").append(Integer.valueOf(storage_size)).append(" ").append(Integer.valueOf(sys_size+storage_size)).append(";\n");
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" mkpartfs primary ext2 ").append(Integer.valueOf(sys_size+storage_size)).append(" ").append(Integer.valueOf(sys_size+storage_size+data_size)).append(";\n");
                //convert ext2 to ext3
                sb.append(getFilesDir()).append("/tune2fs -j ").append(SYSPART2).append(";\n");
                sb.append(getFilesDir()).append("/tune2fs -j ").append(DATAPART2).append(";\n");
                //convert to ext4
                sb.append(getFilesDir()).append("/tune2fs -O extents,uninit_bg,dir_index ").append(SYSPART2).append(";\n");
                sb.append(getFilesDir()).append("/tune2fs -O extents,uninit_bg,dir_index ").append(DATAPART2).append(";\n");
            }
            else{
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" mkpartfs primary fat32 0 ").append(Integer.valueOf(storage_size)).append(";\n");
            }
            sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");

            Helpers.get_assetsScript("run",context,"",sb.toString());
            table = Helpers.shExec(getFilesDir()+"/run");

            return table;
        }
        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(vb!=null) vb.vibrate(500);
            showCurrentPartitions(result);
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, null, getString(R.string.progress)+" "+getString(R.string.wait));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private void showSetablePartitions(String s){
        if((s==null)||(s.equals(""))) return;
        pttable.setText(getString(R.string.pt_table_proposal));
        pttable.setTextColor(getResources().getColor(R.color.pc_yellow));
        pstable.setText(getString(R.string.ps_table_proposal));
        final String ln[]=s.split(";");
        if(ln[1].split(":")[1].trim().contains("GB")){
            final float fv=Float.valueOf(ln[1].split(":")[1].trim().replace("GB",""));
            sdsize=(int)(fv*1000);
        }
        else{
            sdsize=Integer.valueOf(ln[1].split(":")[1].trim().replace("MB",""));
        }
        //Log.d(TAG,"sdcard size: "+sdsize);
        sys_size=1024;
        data_size=2048;
        storage_size=0;
        textinfo.setText(getString(R.string.model)+" "+ln[1].split(":")[6].trim()+" ("+ln[1].split(":")[2].trim()+")\n"+getString(R.string.disksize)+" "+ln[1].split(":")[1].trim());
        storage_size=sdsize-sys_size-data_size;
        if(storage_size<min_storage){
            storage_size=min_storage;
            data_size=sdsize-sys_size-storage_size;
        }
        final List<Item> dir = new ArrayList<Item>();
        dir.add(new Item("Storage", "/dev/block/mmcblk1p1","FAT32","", storage_size+"MB", "file"));
        dir.add(new Item("System", "/dev/block/mmcblk1p2","EXT4","", sys_size+"MB", "file"));
        dir.add(new Item("Data", "/dev/block/mmcblk1p3","EXT4","",data_size+"MB", "file"));
        adapter = new FileArrayAdapter(PartActivity.this,R.layout.file_item, dir);
        packList.setAdapter(adapter);
        footer.setVisibility(LinearLayout.VISIBLE);
        ttotal.setText(Integer.toString(sys_size+data_size+storage_size)+"MB");
        ttotal.setVisibility(TextView.VISIBLE);
        canclick=true;
    }
    private void showCurrentPartitions(String s){
        if((s==null)||(s.equals(""))) return;
        pttable.setText(getString(R.string.pt_table_current));
        pttable.setTextColor(getResources().getColor(R.color.pc_blue));
        pstable.setText("");
        final String ln[]=s.split(";");
        final List<Item> dir = new ArrayList<Item>();
        for(int i=0;i<Helpers.getPartsNo(table);i++){
            int j=i+1;
            dir.add(new Item(j+".", "/dev/block/mmcblk1p"+j,ln[2+i].split(":")[4].toUpperCase(),"", "["+ln[2+i].split(":")[1]+":"+ln[2+i].split(":")[2]+"] "+ln[2+i].split(":")[3], "file"));

        }
        adapter = new FileArrayAdapter(PartActivity.this,R.layout.file_item, dir);
        packList.setAdapter(adapter);
        footer.setVisibility(LinearLayout.GONE);
        ttotal.setVisibility(TextView.GONE);
        canclick=false;
    }

    private void revertPartitions(String s){
        if((s==null)||(s.equals(""))) return;
        pttable.setText(getString(R.string.pt_table_revert));
        pttable.setTextColor(getResources().getColor(R.color.pc_yellow));
        pstable.setText(getString(R.string.ps_table_proposal));
        final String ln[]=s.split(";");
        if(ln[1].split(":")[1].trim().contains("GB")){
            final float fv=Float.valueOf(ln[1].split(":")[1].trim().replace("GB",""));
            sdsize=(int)(fv*1000);
        }
        else{
            sdsize=Integer.valueOf(ln[1].split(":")[1].trim().replace("MB",""));
        }
        //Log.d(TAG,"sdcard size: "+sdsize);
        textinfo.setText(getString(R.string.model)+" "+ln[1].split(":")[6].trim()+" ("+ln[1].split(":")[2].trim()+")\n"+getString(R.string.disksize)+" "+ln[1].split(":")[1].trim());
        storage_size=sdsize;
        data_size=0;
        sys_size=0;

        final List<Item> dir = new ArrayList<Item>();
        dir.add(new Item("Storage", "/dev/block/mmcblk1p1","FAT32","", storage_size+"MB", "file"));
        adapter = new FileArrayAdapter(PartActivity.this,R.layout.file_item, dir);
        packList.setAdapter(adapter);
        footer.setVisibility(LinearLayout.VISIBLE);
        ttotal.setText(Integer.toString(sys_size+data_size+storage_size)+"MB");
        ttotal.setVisibility(TextView.VISIBLE);
        canclick=true;
    }
}
