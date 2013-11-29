DualBootSplus
=============
Dual boot Manager for Samsung Galaxy S plus

More details: http://forum.xda-developers.com/showthread.php?t=2462783

Requirements:
Root
Busybox
CM or other AOSP like ROM based on 2.3.6+
Minimum ~350MB internal storage free space
External SD (min 4GB, the higher class, the better)


Description:

The process is fairly simple. The application will format your external-sd (we recommend to back up your personal data), then create 3 partitions as follows:
/dev/block/mmcblk1p1 -> storage (formatted VFAT)
/dev/block/mmcblk1p2 -> /system (formatted EXT4)
/dev/block/mmcblk1p3 -> /data (formatted EXT4)

After that you'll be given the possibility to flash a secondary ROM on your EXT-SD (currently only 4.1.2+ ROM's are supported as secondary and 2.3.6+ as primary).
At last, you'll be able to switch between ROM's within the app.


Process:

WARNING: Partitioning and flashing operations could take up to 5 minutes each to complete !
Download the app and kernel pack from the Downloads section
If you want the app to be automatically installed on the secondary ROM, push the app to /system/app and give proper permissions (rw-r-r or 644). Otherwise, just install it normally.
Backup your personal data from the external sd card.
Run the app and partition your external-sd.
In app, choose flashing then choose your preferred secondary ROM from your internal SD card (must be 4.1.2 or higher).
Your phone will reboot into recovery and automatically flash the secondary ROM. (in CWM, if you get "Signature Verification Failed", continue flashing, it is normal)
After the flashing process is done, you'll be redirected to your primary ROM.
On your internal SD-Card, you'll have a folder called DualBootSplus, open it up.
Copy the kernels from the kernels pack into their appropriate subfolder. (eg CM-10 kernel (boot2.img) goes into 4.1.2 subfolder -> you'll have to create the subfolder if it doesn't exit)
Now you can switch between ROM's or flash secondary gapps within the app.

Additional details:
The kernel pack contains 2 kernel types for each ROM, Stock and OPT. OPT means that is has all the features from Phenom Kernel.
You must put only 1 boot2.img type (stock or OPT) in the DualBootSplus subfolders.
The kernel will determinate which ROM will be booted. So, if you have a primary and secondary ROM, you can quickly switch between them within the app (make sure you have the kernels in the SD-Card subfolders), or manually by flashing the correspondent kernel.
The boot2.img can be flashed separately in ADB.

