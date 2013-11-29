package com.h0rn3t.dualbootsplus.util;

/**
 * Created by h0rn3t on 22.07.2013.
 */

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.h0rn3t.dualbootsplus.R;


public class FileArrayAdapter extends ArrayAdapter<Item>{

    private Context c;
    private int id;
    private List<Item>items;

    public FileArrayAdapter(Context context, int textViewResourceId,List<Item> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }
    public Item getItem(int i){
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Item o = items.get(position);
        if (o != null) {
            TextView t1 = (TextView) v.findViewById(R.id.TextView01);
            TextView t2 = (TextView) v.findViewById(R.id.TextView02);
            TextView t3 = (TextView) v.findViewById(R.id.TextViewDate);
            TextView t4 = (TextView) v.findViewById(R.id.textView);

            if(t1!=null){
                if(o.getImage().equalsIgnoreCase("dir")){t1.setTypeface(null, Typeface.BOLD);}
                else{t1.setTypeface(null, Typeface.NORMAL);}
                t1.setText(o.getName());
            }
            if(t2!=null){
                if(o.getData()==null){
                    t2.setText("");
                }
                else{
                    t2.setText(o.getData());
                }
            }
            if(t3!=null){
                if(o.getDate()==null){
                    t3.setText("");
                }
                else{
                    t3.setText(o.getDate());
                }
            }
            if(t4!=null){
                if(o.getPart()==null){
                    t4.setText("");
                }
                else{
                    t4.setText(o.getPart());
                }
            }
        }
        return v;
    }



}
