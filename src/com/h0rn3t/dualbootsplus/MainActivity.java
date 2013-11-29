package com.h0rn3t.dualbootsplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.Helpers;

import java.io.File;


public class MainActivity extends Activity implements Constants {
    SharedPreferences mPreferences;
    private Context context;
    private ProgressDialog progressDialog;
    private byte WhatRom;
    private CheckBox chkdata,chkcache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.main);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        TextView deviceName = (TextView) findViewById(R.id.name);
        TextView deviceModel = (TextView) findViewById(R.id.model);
        TextView deviceBoard = (TextView) findViewById(R.id.board);
        TextView flasherInfo = (TextView) findViewById(R.id.flashinfo);

        Button partBtn = (Button) findViewById(R.id.partBtn);
        Button flashBtn = (Button) findViewById(R.id.flashBtn);
        Button switchBtn = (Button) findViewById(R.id.switchBtn);

        LinearLayout lwait = (LinearLayout) findViewById(R.id.layout_wait);
        LinearLayout tools = (LinearLayout) findViewById(R.id.tools);

        final String model= Build.MODEL;
        deviceModel.setText(model);
        deviceBoard.setText(Build.MANUFACTURER);
        WhatRom=Helpers.getActiveRom();
        //new CMDProcessor().sh.runWaitFor("busybox echo `busybox cat /system/build.prop | busybox grep ro.build.display.id | busybox cut -d'=' -f2");
        if(WhatRom==1){
            deviceName.setText(getString(R.string.rom1)+"\n"+Build.DISPLAY);
            partBtn.setEnabled(true);
            partBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent intent = new Intent(MainActivity.this, PartActivity.class);
                    startActivity(intent);
                }
            });
        }
        else{
            deviceName.setText(getString(R.string.rom2)+"\n"+Build.DISPLAY);
            partBtn.setEnabled(false);
        }

        lwait.setVisibility(LinearLayout.GONE);

        if(model.equalsIgnoreCase(MODEL)){
            flasherInfo.setVisibility(TextView.GONE);
            tools.setVisibility(LinearLayout.VISIBLE);
            checkForSu();
        }
        else{
            flasherInfo.setVisibility(TextView.VISIBLE);
            tools.setVisibility(LinearLayout.GONE);
        }

        flashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
                startActivity(intent);
            }
        });

        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(WhatRom==1){
                    makeOptdialog(getString(R.string.btn_switch),getString(R.string.switchrom2));
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(getString(R.string.btn_switch))
                            .setCancelable(false)
                            .setMessage(getString(R.string.switchrom1))
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
                                        }
                                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    theButton.setOnClickListener(new StartFlashListener(alertDialog));
                }
            }
        });

        new CMDProcessor().sh.runWaitFor("busybox mkdir "+Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+DUALBOOTFOLDER);
    }

    class StartFlashListener implements View.OnClickListener {
        private final Dialog dialog;
        public StartFlashListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
            dialog.cancel();
            new SwitchRom().execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.about:
                Intent intent = new Intent(context, AboutActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void checkForSu() {
        boolean firstrun = mPreferences.getBoolean("firstrun", true);
        boolean rootWasCanceled = mPreferences.getBoolean("rootcanceled", false);
        if (firstrun || rootWasCanceled) {
            SharedPreferences.Editor e = mPreferences.edit();
            e.putBoolean("firstrun", false).commit();
            launchFirstRunDialog();
        }
    }

    private void launchFirstRunDialog() {
        String title = getString(R.string.first_run_title);
        final String failedTitle = getString(R.string.su_failed_title);
        LayoutInflater factory = LayoutInflater.from(this);
        final View firstRunDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) firstRunDialog.findViewById(R.id.message);
        tv.setText(R.string.first_run_message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(firstRunDialog)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean canSu = Helpers.checkSu();
                        boolean canBb = Helpers.checkBusybox();
                        if (canSu && canBb) {
                            String title = getString(R.string.su_success_title);
                            String message = getString(R.string.su_success_message);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", false).commit();
                            suResultDialog(title, message);
                        }
                        if (!canSu || !canBb) {
                            String message = getString(R.string.su_failed_su_or_busybox);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", true).commit();
                            suResultDialog(failedTitle, message);
                            finish();
                        }
                    }
                }).create().show();
    }

    private void suResultDialog(String title, String message) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View suResultDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) suResultDialog.findViewById(R.id.message);
        tv.setText(message);
        new AlertDialog.Builder(this).setTitle(title).setView(suResultDialog)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }



    private class SwitchRom extends AsyncTask<String, Void, String> {
        String build="";
        @Override
        protected String doInBackground(String... params) {
            final String dn= Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+DUALBOOTFOLDER;
            final StringBuilder sb=new StringBuilder();

            if(WhatRom==1){
                Helpers.get_assetsBinary("parted", context);
                sb.append("busybox chmod 750 ").append(getFilesDir()).append("/parted;\n");
                sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");
                Helpers.get_assetsScript("run",context,"",sb.toString());
                final String r= Helpers.shExec(getFilesDir()+"/run");
                if(!Helpers.testPart(r)){
                    return "cancel";
                }
                build=Helpers.testSys(context);
                if(build.equals("") || build==null){
                    return "nosys";
                }
                File destDir = new File(dn+"/"+build);
                if (!destDir.exists()) {destDir.mkdir();}
                if(!new File(dn+"/"+build+"/boot2.img").exists()){
                    //if(!build.equals("4.2.2")){
                        return "noboot";
                    //}
                    //Helpers.get_assetsBinary("boot.img", context);
                    //sb.append("busybox cp ").append(getFilesDir()).append("/boot.img ").append(dn).append("/").append(build).append("/boot2.img;\n");
                }
                sb.append("dd if=").append(BOOT).append(" of=").append(dn).append("/boot1.img;\n");
                if(chkcache.isChecked()){
                    sb.append("busybox mkdir /sdcard/"+DUALBOOTFOLDER+";\n");
                    sb.append("busybox mkdir /sdcard/"+DUALBOOTFOLDER+"/tmp;\n");
                    sb.append("busybox mount "+DATAPART2+" /sdcard/"+DUALBOOTFOLDER+"/tmp;\n");
                    sb.append("busybox rm -rf /sdcard/"+DUALBOOTFOLDER+"/tmp/dalvik-cache/*;\n");
                    sb.append("busybox rm -rf /cache/dalvik-cache/*;\n");
                    sb.append("busybox umount /sdcard/"+DUALBOOTFOLDER+"/tmp;\n");
                    sb.append("busybox rm -r /sdcard/"+DUALBOOTFOLDER+"/tmp;");
                }
                sb.append("dd if=").append(dn).append("/").append(build).append("/boot2.img").append(" of=").append(BOOT).append(";\n");
            }
            else{
                sb.append("dd if=").append(dn).append("/boot1.img").append(" of=").append(BOOT).append(";\n");
            }
            //sb.append("reboot;\n");
            Helpers.get_assetsScript("run", context, "",sb.toString());
            Helpers.shExec(getFilesDir()+"/run");
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if(result.equals("cancel")){
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.no_partitions), Toast.LENGTH_LONG).show();
            }
            else if(result.equals("nosys")){
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.no_system), Toast.LENGTH_LONG).show();
            }
            else if(result.equals("noboot")){
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.no_boot,build), Toast.LENGTH_LONG).show();
            }
            else{
                new CMDProcessor().su.runWaitFor("reboot");
            }
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, getString(R.string.wait));

        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private void makeOptdialog(String t,String m){
        final LayoutInflater factory = LayoutInflater.from(context);
        final View optDialog = factory.inflate(R.layout.flash_dialog, null);
        final TextView info=(TextView) optDialog.findViewById(R.id.flashinfo);
        chkdata=(CheckBox) optDialog.findViewById(R.id.wipedata);
        chkcache=(CheckBox) optDialog.findViewById(R.id.wipecache);
        chkcache.setChecked(false);
        chkdata.setVisibility(CheckBox.GONE);
        info.setText(m);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(t)
                .setView(optDialog)
                .setNegativeButton(getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                //finish();
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
        //alertDialog.setCancelable(false);
        Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (theButton != null) {
            theButton.setOnClickListener(new StartFlashListener(alertDialog));
        }
    }
}