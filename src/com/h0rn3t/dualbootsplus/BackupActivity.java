package com.h0rn3t.dualbootsplus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
 * Created by h0rn3t on 14.12.2013.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class BackupActivity extends Activity implements Constants {
    SharedPreferences mPreferences;
    private Context context;
    private byte WhatRom;
    private String bkupdir;
    private ProgressDialog progressDialog;
    private String sys_point,data_point;
    private String build="";
    private TextView buildtxt;
    private Button backupBtn;
    private LinearLayout lwait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.backup);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        backupBtn = (Button) findViewById(R.id.startbk);
        final EditText ebkup = (EditText) findViewById(R.id.bkname);
        buildtxt = (TextView) findViewById(R.id.buildtxt);
        final RadioButton obkup = (RadioButton) findViewById(R.id.intSD);
        lwait = (LinearLayout) findViewById(R.id.layout_wait);


        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");
        Date now = new Date();
        bkupdir=formatter.format(now);
        ebkup.setText(bkupdir);

        new InitOperation().execute();

        backupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ebkup.clearFocus();
                if(!ebkup.getText().toString().equals("")) bkupdir=ebkup.getText().toString();
                else return;
                if(obkup.isChecked()){
                    bkupdir= Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+DUALBOOTFOLDER+"/backup/"+bkupdir;
                }
                else{
                    final String externalsd=Helpers.getExtSD();
                    if(!externalsd.equals("")){
                        bkupdir=externalsd+"/"+DUALBOOTFOLDER+"/backup/"+bkupdir;
                    }
                    else{
                        Toast.makeText(context, getString(R.string.no_extsd), Toast.LENGTH_LONG).show();
                        return;
                    }

                }
                if(new File(bkupdir).exists()){
                    Toast.makeText(context, getString(R.string.bkup_duplicat), Toast.LENGTH_LONG).show();
                }
                else{
                    new BackupOperation().execute();
                }
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
                else if(build.equals("")){
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
            progressDialog = ProgressDialog.show(BackupActivity.this, null, getString(R.string.progress) + " " + getString(R.string.wait));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }
    }


    private class InitOperation extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... params) {
            WhatRom= Helpers.getActiveRom();
            if(WhatRom==1){
                sys_point=SYSPART2;
                data_point=DATAPART2;
                build=Helpers.testSys(context,"ro.build.display.id");
            }
            else{
                sys_point="/system";
                data_point="/data";
                CMDProcessor.CommandResult cr = new CMDProcessor().sh.runWaitFor("busybox echo `busybox cat /system/build.prop | busybox grep ro.build.display.id | busybox cut -d'=' -f2`");
                if(cr.success()){build=cr.stdout;}
            }

            return build;

        }

        @Override
        protected void onPostExecute(String result) {
            if(build.equals("")){
                // no system2 detected
                buildtxt.setText(getString(R.string.no_system));
                backupBtn.setEnabled(false);
            }
            else{
                buildtxt.setText(build);
            }
            // no tar no backup/restore
            if(Helpers.binExist("tar")==null){
                backupBtn.setEnabled(false);
            }
            lwait.setVisibility(LinearLayout.GONE);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }
    }

}
