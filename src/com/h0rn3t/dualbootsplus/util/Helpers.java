package com.h0rn3t.dualbootsplus.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.util.Log;


import java.io.*;

public class Helpers implements Constants {

    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists() && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "su does not exist!!!");
            return false; // tell caller to bail...
        }
        try {
            //if ((new CMDProcessor().su.runWaitFor("ls /data/app-private")).success()) {
            if ((new CMDProcessor().su.runWaitFor("su -c id")).success()) {
                Log.i(TAG, " SU exists and we have permission");
                return true;
            } else {
                Log.i(TAG, " SU exists but we dont have permission");
                return false;
            }
        }
        catch (final NullPointerException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public static boolean checkBusybox() {
        if (!new File("/system/bin/busybox").exists() && !new File("/system/xbin/busybox").exists()) {
            Log.e(TAG, "Busybox not in xbin or bin!");
            return false;
        }
        try {
            if (!new CMDProcessor().su.runWaitFor("busybox mount").success()) {
                Log.e(TAG, " Busybox is there but it is borked! ");
                return false;
            }
        }
        catch (final NullPointerException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }


    /**
     * Return mount points
     *
     * @param path
     * @return line if present
     */
    public static String[] getMounts(final String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException e) {
            Log.d(TAG, "Error reading /proc/mounts");
        }
        return null;
    }

    /**
     * Get mounts
     *
     * @param mount
     * @return success or failure
     */
    public static boolean getMount(final String mount) {
        final CMDProcessor cmd = new CMDProcessor();
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            if (cmd.su.runWaitFor("mount -o " + mount + ",remount -t " + point + " " + device+ " " + path).success()) {
                return true;
            }
        }
        return (cmd.su.runWaitFor("busybox mount -o remount," + mount + " /system").success());
    }

    /**
     * Read one line from file
     *
     * @param fname
     * @return line
     */
    public static String readOneLine(String fname) {
        String line = null;
        if (new File(fname).exists()) {
        	BufferedReader br;
	        try {
	            br = new BufferedReader(new FileReader(fname), 512);
	            try {
	                line = br.readLine();
	            } finally {
	                br.close();
	            }
	        } catch (Exception e) {
	            Log.e(TAG, "IO Exception when reading sys file", e);
	            // attempt to do magic!
	            return readFileViaShell(fname, true);
	        }
        }
        return line;
    }

    /**
     * Read file via shell
     *
     * @param filePath
     * @param useSu
     * @return file output
     */
    public static String readFileViaShell(String filePath, boolean useSu) {
        CMDProcessor.CommandResult cr = null;
        if (useSu) {
            cr = new CMDProcessor().su.runWaitFor("cat " + filePath);
        } else {
            cr = new CMDProcessor().sh.runWaitFor("cat " + filePath);
        }
        if (cr.success())
            return cr.stdout;
        return null;
    }

    /**
     * Write one line to a file
     *
     * @param fname
     * @param value
     * @return if line was written
     */
    public static boolean writeOneLine(String fname, String value) {
    	if (!new File(fname).exists()) {return false;}
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }

    /**
     * Reads string array from file
     *
     * @param fname
     * @return string array
     */
    private static String[] readStringArray(String fname) {
        String line = readOneLine(fname);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    public static String binExist(String b) {
        CMDProcessor.CommandResult cr = null;
        cr = new CMDProcessor().sh.runWaitFor("busybox which " + b);
        if (cr.success()){ return  cr.stdout; }
        else{ return null;}
    }


	public static String shExec(String sh){
		if (new File(sh).exists()) {
            new CMDProcessor().sh.runWaitFor("busybox chmod 750 "+sh );
            CMDProcessor.CommandResult cr;
			cr=new CMDProcessor().su.runWaitFor(sh);
            if(cr.success()){return cr.stdout;}
            else{Log.d(TAG, "execute: "+cr.stderr);return "";}
		}
		else{
			Log.d(TAG, "missing file: "+sh);
            return "";
		}
	}

    public static void get_assetsScript(String fn,Context c,String prefix,String postfix){
        byte[] buffer;
        final AssetManager assetManager = c.getAssets();
        try {
            InputStream f =assetManager.open(fn);
            buffer = new byte[f.available()];
            f.read(buffer);
            f.close();
            final String s = new String(buffer);
            final StringBuffer sb = new StringBuffer(s);
            if(!postfix.equals("")){ sb.append("\n\n"+postfix); }
            if(!prefix.equals("")){ sb.insert(0,prefix+"\n"); }
            sb.insert(0,"#!"+Helpers.binExist("sh")+"\n\n");
            try {
                FileOutputStream fos;
                fos = c.openFileOutput(fn, Context.MODE_PRIVATE);
                fos.write(sb.toString().getBytes());
                fos.close();

            } catch (IOException e) {
                Log.d(TAG, "error write "+fn+" file");
                e.printStackTrace();
            }

        }
        catch (IOException e) {
            Log.d(TAG, "error read "+fn+" file");
            e.printStackTrace();
        }
    }
    public static void get_assetsBinary(String fn,Context c){
        byte[] buffer;
        final AssetManager assetManager = c.getAssets();
        try {
            InputStream f =assetManager.open(fn);
            buffer = new byte[f.available()];
            f.read(buffer);
            f.close();
            try {
                FileOutputStream fos;
                fos = c.openFileOutput(fn, Context.MODE_PRIVATE);
                fos.write(buffer);
                fos.close();

            } catch (IOException e) {
                Log.d(TAG, "error write "+fn+" file");
                e.printStackTrace();
            }

        }
        catch (IOException e) {
            Log.d(TAG, "error read "+fn+" file");
            e.printStackTrace();
        }
    }
    public static String ReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = String.valueOf("KMGTPE".charAt(exp-1));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static Byte getActiveRom() {
        CMDProcessor.CommandResult cr = null;
        cr = new CMDProcessor().sh.runWaitFor("busybox echo `busybox mount | busybox grep system | busybox cut -d' ' -f1`");
        if(cr.success()&& !cr.stdout.equals("") ){
            if(cr.stdout.equalsIgnoreCase(SYSPART1)){
                return 1;
            }
            else if(cr.stdout.equalsIgnoreCase(SYSPART2)){
                return 2;
            }
            else{
                return 0;
            }
        }
        else{
            return 0;
        }
    }
    public static int getPartsNo(String s) {
        if(s.equals("")||s==null) return 0;
        final String ln[]=s.split(";");
        return ln.length-2;//nr.de partitii
    }
    public static String testSys(Context c,String v) {
        get_assetsScript("run", c, "", "get_build "+DUALBOOTFOLDER+" "+SYSPART2+" "+v+";\n");
        return shExec(c.getFilesDir() + "/run");
    }
    public static Boolean testPart(String s){
        if(s.equals("")||s==null) return false;
        final String ln[]=s.split(";");
        final int nl=ln.length-2;//nr.de partitii
        Log.d(TAG,"partitions: "+nl);
        if(nl<3) return false;
        if(!ln[2].split(":")[4].equals("fat32")||!ln[3].split(":")[4].equals("ext4")||!ln[4].split(":")[4].equals("ext4") ) return false;
        if(ln[3].split(":")[3].contains("GB")){
            float fv=Float.valueOf(ln[3].split(":")[3].replace("GB",""));
            if((int)(fv*1024) < min_sys){
                return false;
            }
        }
        else{
            if(Integer.valueOf(ln[3].split(":")[3].replace("MB",""))<min_sys){
                return false;
            }
        }
        if(ln[4].split(":")[3].contains("GB")){
            float fv=Float.valueOf(ln[4].split(":")[3].replace("GB",""));
            if((int)(fv*1024) < min_data){
                return false;
            }
        }
        else{
            if(Integer.valueOf(ln[4].split(":")[3].replace("MB",""))<min_data){
                return false;
            }
        }
        return true;
    }

    public static void infoDialog(Context c,String t,String m) {
        new AlertDialog.Builder(c).setTitle(t).setMessage(m)
        .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                }).create().show();
    }
    public static String getExtSD() {
        String externalsd="";
        final String externalstorage[]=System.getenv("SECONDARY_STORAGE").split(":");
        for ( final String dirs : externalstorage ) {
            final File dir= new File(dirs);
            if ( dir.isDirectory() && dir.canRead() && (dir.listFiles().length > 0) ) {
                externalsd=dirs;
                break;
            }
        }
        return externalsd;
    }
}
