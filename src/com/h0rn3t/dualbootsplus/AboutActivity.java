package com.h0rn3t.dualbootsplus;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.h0rn3t.dualbootsplus.util.Constants;

/**
 * Created by h0rn3t on 04.10.2013.
 */
public class AboutActivity extends Activity implements Constants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView ver=(TextView) findViewById(R.id.ver);
        ver.setText("Version: "+ VERSION_NUM);
    }
}