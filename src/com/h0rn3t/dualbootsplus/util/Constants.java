package com.h0rn3t.dualbootsplus.util;

public interface Constants {

    public static final String TAG = "DualBootSplus";
    public static final String DUALBOOTFOLDER = "DualBootSplus";
    public static final String VERSION_NUM = "1.9";
    public static final String MODEL = "GT-I9001";
    public static final String SYSPART1 = "/dev/block/mmcblk0p15";
    public static final String SYSPART2 = "/dev/block/mmcblk1p2";
    public static final String DATAPART1 = "/dev/block/mmcblk0p17";
    public static final String DATAPART2 = "/dev/block/mmcblk1p3";
    public static final String PARTEXT = "/dev/block/mmcblk1";
    public static final String BOOT="/dev/block/mmcblk0p8";

    public static final int min_sys=512;
    public static final int min_data=1024;
    public static final int min_storage=512;
}


