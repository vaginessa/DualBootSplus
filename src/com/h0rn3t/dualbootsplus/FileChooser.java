package com.h0rn3t.dualbootsplus;
/**
 * Created by h0rn3t on 24.09.2013.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
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
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.FileArrayAdapter;
import com.h0rn3t.dualbootsplus.util.Helpers;
import com.h0rn3t.dualbootsplus.util.Item;
import com.h0rn3t.dualbootsplus.util.UnzipUtility;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileChooser extends Activity implements Constants,AdapterView.OnItemClickListener {
    final Context context = this;
    private File currentDir;
    private FileArrayAdapter adapter;
    private ProgressDialog progressDialog;
    private String nFile;
    private int nbk=1;
    private boolean rom;
    private CheckBox chkdata;
    private CheckBox chkcache;
    private String tmpzip;
    private ListView packList;
    private TextView titlu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.files);
        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        titlu=(TextView) findViewById(R.id.titlu);
        titlu.setText(getString(R.string.pt_other));
        Intent intent=getIntent();
        rom=intent.getBooleanExtra("rom",true);
        if(rom) titlu.setText(getString(R.string.pt_rom));

        currentDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        fill(currentDir);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filechooser_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.close:
                finish();
                break;
            case R.id.rom:
                rom=true;
                titlu.setText(getString(R.string.pt_rom));
                break;
            case R.id.other:
                rom=false;
                titlu.setText(getString(R.string.pt_other));
                break;
        }

        return true;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    private void fill(File f){
        File[]dirs = f.listFiles();
        List<Item> dir = new ArrayList<Item>();
        try{
            assert dirs != null;
            for(File ff: dirs){
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){
                    dir.add(new Item(ff.getName(),getString(R.string.dir),date_modify,ff.getAbsolutePath(),null,"dir"));
                }
                else{
                    if(ff.getName().toLowerCase().endsWith(".zip"))
                        dir.add(new Item(ff.getName(), Helpers.ReadableByteCount(ff.length()), date_modify, ff.getAbsolutePath(),null,"file"));
                }
            }
        }
        catch(Exception e){
        }
        Collections.sort(dir);

        if(!f.getName().equalsIgnoreCase(""))
            dir.add(0,new Item("..",getString(R.string.dir_parent),"",f.getParent(),null,"dir"));
        adapter = new FileArrayAdapter(FileChooser.this,R.layout.file_item, dir);
        packList.setAdapter(adapter);
    }

    @Override
    public void onBackPressed(){
        if(adapter.getItem(0).getName().equalsIgnoreCase("..")){
            currentDir=currentDir.getParentFile();
            fill(currentDir);
            nbk=1;
        }
        else{
            if(nbk==2){finish();}
            else{
                nbk++;
                Toast.makeText(getApplicationContext(), getString(R.string.bkexit), Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        Item o = adapter.getItem(position);
        if(o.getImage().equalsIgnoreCase("dir")){
            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else{
            nFile=currentDir.getAbsolutePath()+"/"+o.getName();
            if(o.getName().toLowerCase().endsWith(".zip")){
                new TestZipOperation().execute();
            }
        }

    }

    private class TestZipOperation extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            final UnzipUtility unzipper = new UnzipUtility();
            try{
                return unzipper.testZip(nFile,"META-INF/com/google/android/updater-script");
            }
            catch (Exception e) {
                Log.d(TAG,"test ZIP error: "+nFile);
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if(!result){
                Toast.makeText(context, getString(R.string.bad_zip,nFile), Toast.LENGTH_SHORT).show();
                return;
            }
            if(rom){
                makeOptdialog(getString(R.string.rom2), getString(R.string.flash_rom, nFile),(byte)0);
            }
            else{
                makedialog(getString(R.string.flash_btn),getString(R.string.flash_gapps,nFile),getString(R.string.yes),getString(R.string.no),(byte)0);
            }
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(FileChooser.this, null, getString(R.string.verify));
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class FlashOperation extends AsyncTask<String,Integer,String> {
        long freetotal=0;
        int tot=0;
        final String dn=Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+DUALBOOTFOLDER;
        final String script="META-INF/com/google/android/updater-script";

        @Override
        protected String doInBackground(String... params) {
            freetotal=Freebytes(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            long cursize=new File(nFile).length();

            if(cursize>=freetotal){return "nospace";}
            if(rom){
               tmpzip=dn+"/rom2.zip";
            }
            else{
                tmpzip=dn+"/other2.zip";
            }
            StringBuilder sb=new StringBuilder();

            Helpers.get_assetsBinary("parted", context);
            sb.append("busybox chmod 750 ").append(getFilesDir()).append("/parted;\n");
            sb.append(getFilesDir()).append("/parted -sm ").append(PARTEXT).append(" print;\n");
            Helpers.get_assetsScript("run",context,"",sb.toString());
            String r= Helpers.shExec(getFilesDir()+"/run");
            if(!Helpers.testPart(r)){
                return "cancel";
            }
            try{
                new UnzipUtility().unzipfile(nFile,dn,script);

                File file = new File(dn+"/"+script);
                String s = readTextFile(file);
                writeTextFile(file,s);

                sb=new StringBuilder();
                sb.append(script+"\n");
                if(new File("/system/app/"+TAG+".apk").exists() && Helpers.getActiveRom()==1 && rom){
                    new CMDProcessor().sh.runWaitFor("busybox mkdir -p "+dn+"/system/app/");
                    new CMDProcessor().su.runWaitFor("busybox cp /system/app/"+TAG+".apk "+dn+"/system/app/"+TAG+".apk" );
                    sb.append("system/app/"+TAG+".apk");
                }
                String[] files=sb.toString().split("\n");
                byte[] buffer = new byte[8192];

                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmpzip));
                for (String file1 : files) {
                    if (new File(dn + "/" + file1).exists()) {
                        InputStream in = new FileInputStream(dn + "/" + file1);
                        out.putNextEntry(new ZipEntry(file1));
                        for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
                            out.write(buffer, 0, read);
                        }
                        out.closeEntry();
                        in.close();
                    }
                }
                ZipInputStream zin = new ZipInputStream(new FileInputStream(nFile));
                ZipFile z=new ZipFile(nFile);
                tot=z.size();

                ZipEntry ze = zin.getNextEntry();
                int i=0;
                while (ze != null) {
                    if(!ze.isDirectory()){
                    if(!zipEntryMatch(ze.getName(), files)){
                        out.putNextEntry(new ZipEntry(ze.getName()));
                        for(int read = zin.read(buffer); read > -1; read = zin.read(buffer)){
                            out.write(buffer, 0, read);
                        }
                        out.closeEntry();
                    }
                    }
                    publishProgress(i);
                    i++;
                    ze = zin.getNextEntry();
                }
                out.close();

                tmpzip=tmpzip.replace("/storage/sdcard0/","/sdcard/").replace("/mnt/sdcard/","/sdcard/").replace("/storage/sdcard1/","/external_sd/").replace("/mnt/external_sd/","/external_sd/");
                new CMDProcessor().sh.runWaitFor("busybox rm -r "+dn+"/system");
                new CMDProcessor().sh.runWaitFor("busybox rm -r "+dn+"/META-INF");
                return "ok";

            }
            catch (ZipException e) {
                return "";
            }
            catch (IOException e) {
                return "";
            }
            catch (Exception e) {
                return "";
            }
        }
        private boolean zipEntryMatch(String zeName, String[] files){
            for (String file : files) {
                if (zeName.contains(file)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(vb!=null) vb.vibrate(500);
            if(result.equals("")){
                new CMDProcessor().su.runWaitFor("busybox rm -f "+tmpzip);
                new CMDProcessor().sh.runWaitFor("busybox rm -r "+dn+"/system");
                new CMDProcessor().sh.runWaitFor("busybox rm -r "+dn+"/META-INF");
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.err_zip), Toast.LENGTH_LONG).show();
            }
            else if(result.equals("cancel")){
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.no_partitions), Toast.LENGTH_LONG).show();
            }
            else if(result.equals("nospace")){
                Toast.makeText(context, getString(R.string.error)+". "+getString(R.string.no_space), Toast.LENGTH_LONG).show();
            }
            else{
                makedialog(getString(R.string.ready),getString(R.string.ready_nessage,tmpzip),getString(R.string.btn_flashnow),getString(R.string.btn_later),(byte)1);
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(FileChooser.this, null, getString(R.string.prepare_zip)+" "+getString(R.string.wait));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage( getString(R.string.prepare_zip)+" "+progress[0]*100/tot+"%");
        }
    }

    class CustomListener implements View.OnClickListener {
        private final Dialog dialog;
        private final byte sw;
        public CustomListener(Dialog dialog,byte sw) {
            this.dialog = dialog;
            this.sw=sw;
        }
        @Override
        public void onClick(View v) {
            dialog.cancel();
            switch (sw){
                case 0:
                    new FlashOperation().execute();
                    break;
                case 1:
                    StringBuilder sb=new StringBuilder();
                    sb.append("busybox mkdir /cache/recovery;\n");
                    sb.append("busybox echo \"--update_package=").append(tmpzip).append("\" > /cache/recovery/command;\n");
                    sb.append("reboot recovery;\n");
                    Helpers.get_assetsScript("run", context, "",sb.toString());
                    Helpers.shExec(getFilesDir()+"/run");
                    break;
            }

        }
    }

    private void makedialog(String t,String m,String bok,String bno,byte sw){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(t)
                .setMessage(m)
                .setNegativeButton(bno,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                                //finish();
                            }
                        })
                .setPositiveButton(bok,
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
            theButton.setOnClickListener(new CustomListener(alertDialog,sw));
        }
    }
    private void makeOptdialog(String t,String m,byte sw){
        final LayoutInflater factory = LayoutInflater.from(context);
        final View optDialog = factory.inflate(R.layout.flash_dialog, null);
        final TextView info=(TextView) optDialog.findViewById(R.id.flashinfo);
        chkdata=(CheckBox) optDialog.findViewById(R.id.wipedata);
        chkcache=(CheckBox) optDialog.findViewById(R.id.wipecache);
        chkcache.setChecked(false);
        chkdata.setChecked(false);
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
            theButton.setOnClickListener(new CustomListener(alertDialog,sw));
        }
    }
    private void writeTextFile(File file, String text) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(text);
        writer.close();
    }

    private String readTextFile(File file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder text = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            line=line.replace(SYSPART1,SYSPART2);
            line=line.replace(DATAPART1,DATAPART2);
            if(line.contains("run_program")){
                if(line.contains("umount")){
                    ;
                }
                else if(line.contains("mount")){
                    if(line.contains("/system")){
                        line="mount(\"ext4\", \"EMMC\", \""+SYSPART2+"\", \"/system\");";
                    }
                    else if(line.contains("/data")){
                        line="mount(\"ext4\", \"EMMC\", \""+DATAPART2+"\", \"/data\");";
                    }
                }
            }

            if(!line.contains("boot.img")){
                text.append(line);
                text.append("\n");
                if(line.contains("format") && line.contains("/system")){
                    if(chkdata.isChecked()){
                        text.append("ui_print(\"Formating data...\");\n");
                        text.append("format(\"ext4\", \"EMMC\", \""+DATAPART2+"\", \"0\", \"/data\");\n");
                        text.append("ui_print(\"Resume installing...\");\n");
                    }
                    if(chkcache.isChecked()){
                        text.append("ui_print(\"Mounting data...\");\n");
                        text.append("mount(\"ext4\", \"EMMC\", \""+DATAPART2+"\", \"/data\");\n");
                        text.append("ui_print(\"Wipe cache and dalvik-cache...\");\n");
                        text.append("delete_recursive(\"/cache\");\n");
                        text.append("delete_recursive(\"/data/dalvik-cache\");\n");
                        text.append("ui_print(\"Unmounting data...\");\n");
                        text.append("unmount(\"/data\");\n");
                        text.append("ui_print(\"Resume installing...\");\n");
                    }
                }
            }
        }
        reader.close();
        if(rom){
            text.insert(0,"ui_print(\"Installing Secondary ROM...\");\n");
        }
        else{
            text.insert(0,"ui_print(\"Installing on Secondary ROM...\");\n");
        }
        return text.toString();
    }
    public static long Freebytes(File f) {
        StatFs stat = new StatFs(f.getPath());
        return (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
    }
}
