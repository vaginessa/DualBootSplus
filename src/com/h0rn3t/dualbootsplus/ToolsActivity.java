package com.h0rn3t.dualbootsplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.Helpers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by h0rn3t on 07.11.2013.
 */
public class ToolsActivity extends Activity implements Constants {
    SharedPreferences mPreferences;
    private Context context;
    private byte WhatRom;
    private String bkupdir;
    private ProgressDialog progressDialog;
    private String sys_point,data_point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.tools);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        TextView deviceModel = (TextView) findViewById(R.id.model);
        TextView deviceBoard = (TextView) findViewById(R.id.board);

        Button flashromBtn = (Button) findViewById(R.id.flashrom);
        Button backupBtn = (Button) findViewById(R.id.backup);
        Button restoreBtn = (Button) findViewById(R.id.restore);
        Button recoveryBtn = (Button) findViewById(R.id.recovery);

        final String model= Build.MODEL;
        deviceModel.setText(model);
        deviceBoard.setText(Build.MANUFACTURER);
        WhatRom= Helpers.getActiveRom();

        if(WhatRom==1){
            sys_point=SYSPART2;
            data_point=DATAPART2;
        }
        else{
            sys_point="/system";
            data_point="/data";
        }
        // daca nu exista tar nu se poate face backup/restore
        if(Helpers.binExist("tar")==null){
            restoreBtn.setEnabled(false);
            backupBtn.setEnabled(false);
        }

        flashromBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(ToolsActivity.this, FileChooser.class);
                intent.putExtra("rom", true);
                startActivity(intent);
            }
        });
        backupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");
                Date now = new Date();
                bkupdir=formatter.format(now);
                final LayoutInflater factory = LayoutInflater.from(context);
                final View optDialog = factory.inflate(R.layout.backup_dialog, null);
                final EditText ebkup = (EditText) optDialog.findViewById(R.id.bkname);
                final RadioButton obkup = (RadioButton) optDialog.findViewById(R.id.intSD);
                ebkup.setText(bkupdir);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(getString(R.string.btn_backup))
                        .setView(optDialog)
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton(getString(R.string.start),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Boolean isok=true;
                                        dialog.cancel();

                                        if(!ebkup.getText().toString().equals("")) bkupdir=ebkup.getText().toString();
                                        if(obkup.isChecked()){
                                            bkupdir=Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+DUALBOOTFOLDER+"/backup/"+bkupdir;
                                        }
                                        else{
                                            final String externalsd=Helpers.getExtSD();
                                            if(!externalsd.equals("")){
                                                bkupdir=externalsd+"/"+DUALBOOTFOLDER+"/backup/"+bkupdir;
                                            }
                                            else{
                                                isok=false;
                                            }

                                        }
                                        if(isok){
                                            if(new File(bkupdir).exists()){
                                                Toast.makeText(context, getString(R.string.bkup_duplicat), Toast.LENGTH_LONG).show();
                                            }
                                            else{
                                                new BackupOperation().execute();
                                            }
                                        }
                                        else
                                            Toast.makeText(context, getString(R.string.no_extsd), Toast.LENGTH_LONG).show();
                                    }
                                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
        restoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(ToolsActivity.this, RestoreActivity.class);
                startActivity(intent);
            }
        });
        recoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(getString(R.string.reboot))
                        .setMessage(getString(R.string.reboot_message))
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
                                        new CMDProcessor().su.runWaitFor("reboot recovery" );
                                    }
                                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }


    private class BackupOperation extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... params) {
            StringBuilder sb;
            if(WhatRom==1){
                sb=new StringBuilder();
                Helpers.get_assetsBinary("parted", context);
                sb.append("busybox chmod 750 ").append(getFilesDir()).append("/parted;\n");
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");
                Helpers.get_assetsScript("run",context,"",sb.toString());
                String r= Helpers.shExec(getFilesDir()+"/run");
                if(!Helpers.testPart(r)){
                    return "nopart";
                }
                else if(Helpers.testSys(context).equals("")){
                    return "nosys";
                }

            }
            sb=new StringBuilder();
            sb.append("busybox mkdir -p ").append(bkupdir).append(";\n");

            sb.append("busybox mount -o remount,rw /;\n");
            sb.append("busybox mkdir -p /mnt/"+DUALBOOTFOLDER+"/system;\n");
            sb.append("busybox mkdir -p /mnt/"+DUALBOOTFOLDER+"/data;\n");
            sb.append("busybox mount -o remount,ro /;\n");

            sb.append("busybox mount ").append(sys_point).append(" /mnt/").append(DUALBOOTFOLDER).append("/system;\n");
            sb.append("busybox mount ").append(data_point).append(" /mnt/").append(DUALBOOTFOLDER).append("/data;\n");

            sb.append("cd /mnt/"+DUALBOOTFOLDER+"/system;\n");
            sb.append("busybox tar -cf \"").append(bkupdir).append("/system.tar\" *;\n");
            sb.append("cd ../../;\n");

            sb.append("cd /mnt/"+DUALBOOTFOLDER+"/data;\n");
            sb.append("busybox tar -cf \"").append(bkupdir).append("/data.tar\" *;\n");
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
            else if( result.equals("nosys")){
                Toast.makeText(context, getString(R.string.error)+" "+getString(R.string.no_system), Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ToolsActivity.this, null, getString(R.string.progress) + " " + getString(R.string.wait));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }
    }


}
