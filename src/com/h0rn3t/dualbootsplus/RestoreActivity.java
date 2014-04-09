package com.h0rn3t.dualbootsplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.FileArrayAdapter;
import com.h0rn3t.dualbootsplus.util.Helpers;
import com.h0rn3t.dualbootsplus.util.Item;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by h0rn3t on 09.11.2013.
 */
public class RestoreActivity  extends Activity implements Constants,AdapterView.OnItemClickListener   {
    final Context context = this;
    private FileArrayAdapter adapter;
    private ProgressDialog progressDialog;
    private String bkupdir="";
    List<Item> dir = new ArrayList<Item>();
    private LinearLayout nodata;
    private byte WhatRom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.files);
        WhatRom= Helpers.getActiveRom();
        ListView packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        packList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Item o = adapter.getItem(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(getString(R.string.bkup_del_title))
                        .setMessage(getString(R.string.del_backup))
                        .setNegativeButton(getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        CMDProcessor.CommandResult cr = new CMDProcessor().sh.runWaitFor("busybox rm -r " + o.getPart() + "/" + o.getName());
                                        if (cr.success()) {
                                            adapter.remove(o);
                                            adapter.notifyDataSetChanged();
                                            if(adapter.getCount()<=0) nodata.setVisibility(LinearLayout.VISIBLE);
                                        }
                                    }
                                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            }
        });
        TextView titlu = (TextView) findViewById(R.id.titlu);
        titlu.setText(getString(R.string.backup_info));
        //titlu.setVisibility(TextView.GONE);

        nodata=(LinearLayout) findViewById(R.id.nofiles);
        TextView tinfo=(TextView) findViewById(R.id.info);
        tinfo.setText(getString(R.string.no_bkup_info));

        File currentDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DUALBOOTFOLDER + "/backup/");
        fill(currentDir);

        final String externalsd=Helpers.getExtSD();
        if(!externalsd.equals("")){
            currentDir =new File(externalsd+"/"+DUALBOOTFOLDER+"/backup/");
            fill(currentDir);
        }

        adapter = new FileArrayAdapter(RestoreActivity.this,R.layout.tar_item, dir);
        packList.setAdapter(adapter);
        if(adapter.getCount()<=0) nodata.setVisibility(LinearLayout.VISIBLE);
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // daca nu exista tar nu se poate face backup/restore
        if(Helpers.binExist("tar")!=null){
            MenuInflater inflater=getMenuInflater();
            inflater.inflate(R.menu.backup_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.backup_rom:
                Intent intent = new Intent(RestoreActivity.this, BackupActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void fill(File f){
        File[]dirs = f.listFiles();

        try{
            assert dirs != null;
            for(File ff: dirs){
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){
                    dir.add(new Item(ff.getName(), Helpers.ReadableByteCount(getFolderSize(ff.getAbsoluteFile())), date_modify, ff.getAbsolutePath(), ff.getParent(), "dir"));
                }
            }
        }
        catch(Exception e){
        }

        Collections.sort(dir);
    }

    public static long getFolderSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (File aFileList : fileList) {
                if (aFileList.isDirectory()) {
                    result += getFolderSize(aFileList);
                } else {
                    result += aFileList.length();
                }
            }
            return result;
        }
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        if(WhatRom==1){
            Item o = adapter.getItem(position);
            bkupdir=o.getPart()+"/"+o.getName();
            if(!new File(bkupdir+"/system.tar").exists() || !new File(bkupdir+"/data.tar").exists()) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.btn_restore))
                    .setMessage(getString(R.string.restore_message,bkupdir))
                    .setNegativeButton(getString(R.string.no),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setPositiveButton(getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    new RestoreOperation().execute();
                                }
                            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }


    private class RestoreOperation extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb;

            sb=new StringBuilder();
            Helpers.get_assetsBinary("parted", context);
            sb.append("busybox chmod 750 ").append(getFilesDir()).append("/parted;\n");
            sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");
            Helpers.get_assetsScript("run",context,"",sb.toString());
            String r= Helpers.shExec(getFilesDir()+"/run");
            if(!Helpers.testPart(r)){
                return "nopart";
            }

            sb=new StringBuilder();
            sb.append("busybox mount -o remount,rw /;\n");
            sb.append("busybox mkdir -p /mnt/"+DUALBOOTFOLDER+"/system;\n");
            sb.append("busybox mkdir -p /mnt/"+DUALBOOTFOLDER+"/data;\n");
            sb.append("busybox mount -o remount,ro /;\n");

            sb.append("busybox mount ").append(SYSPART2).append(" /mnt/").append(DUALBOOTFOLDER).append("/system;\n");
            sb.append("busybox mount ").append(DATAPART2).append(" /mnt/").append(DUALBOOTFOLDER).append("/data;\n");
            sb.append("cd /mnt/"+DUALBOOTFOLDER+"/system;\n");
            sb.append("busybox rm -rf *;\n");
            sb.append("busybox tar -xf \"").append(bkupdir).append("/system.tar\" 2>/dev/null;\n");
            sb.append("cd ../../;\n");
            sb.append("cd /mnt/"+DUALBOOTFOLDER+"/data;\n");
            sb.append("busybox rm -rf *;\n");
            sb.append("busybox tar -xf \"").append(bkupdir).append("/data.tar\" 2>/dev/null;\n");
            sb.append("cd ../../;\n");
            sb.append("busybox umount /mnt/"+DUALBOOTFOLDER+"/system;\n");
            sb.append("busybox umount /mnt/"+DUALBOOTFOLDER+"/data;\n");

            sb.append("busybox mount -o remount,rw /;\n");
            sb.append("busybox rm -r /mnt/"+DUALBOOTFOLDER+";\n");
            sb.append("busybox mount -o remount,ro /;\n");
            Helpers.get_assetsScript("run",context,"",sb.toString());
            Helpers.shExec(getFilesDir() + "/run");
            return "ok";

        }

        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(vb!=null) vb.vibrate(500);
            if( result.equals("nopart")){
                Toast.makeText(context, getString(R.string.error)+" "+getString(R.string.no_partitions), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(RestoreActivity.this, null, getString(R.string.progress) + " " + getString(R.string.wait));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }
    }


}
