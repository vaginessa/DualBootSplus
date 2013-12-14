package com.h0rn3t.dualbootsplus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.h0rn3t.dualbootsplus.util.CMDProcessor;
import com.h0rn3t.dualbootsplus.util.Constants;
import com.h0rn3t.dualbootsplus.util.Helpers;

/**
 * Created by h0rn3t on 07.11.2013.
 */
public class ToolsActivity extends Activity implements Constants {
    SharedPreferences mPreferences;
    private Context context;

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
        byte whatRom = Helpers.getActiveRom();

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
                Intent intent = new Intent(ToolsActivity.this, BackupActivity.class);
                startActivity(intent);
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



}
