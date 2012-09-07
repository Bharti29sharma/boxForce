package com.metacube.boxforce;

import java.util.ArrayList;

import com.metacube.boxforce.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FilesAdapter extends ArrayAdapter<String>{

    Context context; 
    int layoutResourceId;    
    ArrayList<String> data = null;
    
    public FilesAdapter(Context context, int layoutResourceId, ArrayList<String> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
       // WeatherHolder holder = null;
        
        //if(row == null)
        //{
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, null);
            
          //  holder = new WeatherHolder();
           // holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
           TextView txtTitle = (TextView)row.findViewById(R.id.list_item_main_text);
           txtTitle.setText(data.get(position)) ;
           
          //  row.setTag(holder);
        //}
       // else
       // {
          //  holder = (WeatherHolder)row.getTag();
       // }
        
       /* Weather weather = data[position];
        holder.txtTitle.setText(weather.title);
        holder.imgIcon.setImageResource(weather.icon);
        */
        return row;
    }
    
   
}