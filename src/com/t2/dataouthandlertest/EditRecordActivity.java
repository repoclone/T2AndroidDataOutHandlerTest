/*****************************************************************
EditRecordActivity

Copyright (C) 2011-2013 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.dataouthandlertest;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandlertest.MainActivity.DataOutPacketArrayAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class EditRecordActivity extends Activity  {
	private static final String TAG = EditRecordActivity.class.getSimpleName();
	private Context mContext;
	
	
	private ListView listview;	
	
	DataOutPacket mCurrentPacket;	
	

	public class KeyValue {
		String mKey;
		String mValue;
		
		public KeyValue(String key, String value) {
			mKey = key;
			mValue = value;
			
		}
		
	}
	
	
    public class KeyValuePacketArrayAdapter extends ArrayAdapter<KeyValue> {
    	  private final Context context;

    	  public KeyValuePacketArrayAdapter(Context context, List<KeyValue> values) {
    	    super(context, R.layout.record_edit_row_layout, values);
    	    this.context = context;
    	  }

    	  @Override
    	  public View getView(final int position, View convertView, ViewGroup parent) {
    	    LayoutInflater inflater = (LayoutInflater) context
    	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	    final int buttonposition = position;	  	    
    	    
    	    View rowView = inflater.inflate(R.layout.record_edit_row_layout, parent, false);
    	    TextView textViewKey = (TextView) rowView.findViewById(R.id.key);
    	    TextView textViewValue = (TextView) rowView.findViewById(R.id.value);
    	    
    	    final KeyValue item = this.getItem(position);  	    
    	    textViewKey.setText(item.mKey);
    	    //textViewKey.setText(item.mKey + " : " + item.mValue);
    	    textViewValue.setText(item.mValue);

      	    Button editButton = (Button) rowView.findViewById(R.id.buttonEdit1);
      	    editButton.setOnClickListener(new View.OnClickListener() {
      	         public void onClick(View v) {
      	        	 Log.e(TAG, "Edit Button " + buttonposition);
      	        	 
     				AlertDialog.Builder alert1 = new AlertDialog.Builder(mContext);

    				alert1.setMessage(item.mKey);

    				// Set an EditText view to get user input 
    				final EditText input = new EditText(mContext);
    				input.setText(item.mValue);
    				alert1.setView(input);

    				alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int whichButton) {
    					
    					// Handle special case where value is a nested vector
    					String newValue = input.getText().toString();
    					if (newValue.startsWith("[") && newValue.endsWith("]")) {
    						// Create a vector from the list of values
    						newValue = newValue.substring(1, newValue.length() - 1);
    						List<String> items = Arrays.asList(newValue.split("\\s*,\\s*"));
    						Vector v = new Vector(items);
    						Log.e(TAG, mCurrentPacket.mItemsMap.toString());
    						mCurrentPacket.mItemsMap.put(item.mKey, v);
    						Log.e(TAG, mCurrentPacket.mItemsMap.toString());
    					}
    					else {
        					mCurrentPacket.mItemsMap.put(item.mKey, newValue);
    					}
    					
    					
    					
    			        KeyValuePacketArrayAdapter adapter2 = new KeyValuePacketArrayAdapter(mContext, PacketToArrayOfValues(mCurrentPacket));
    			        listview.setAdapter(adapter2);     					
    					
    					
    					
    				  }
    				});

    				alert1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    				  public void onClick(DialogInterface dialog, int whichButton) {
    				  }
    				});

    				alert1.show();	      	        	 
      	         }
      	    });    	    
    	    
    	    
    	    
    	    
    	    return rowView;
    	  }
    	  
    	  
    	  
    	  
    	}  	
	
	public EditRecordActivity() {
	}

	private List<KeyValue> PacketToArrayOfValues(DataOutPacket packet) {
		List<KeyValue> list = new ArrayList<KeyValue>();
		
		Set<String> set = packet.mItemsMap.keySet();
		
		// Note: Need to handle the case of a nested vector manually
		for(String key : set) {
			
			if (packet.mItemsMap.get(key) instanceof String) {
				KeyValue kv = new KeyValue(key, (String) packet.mItemsMap.get(key));
				list.add(kv);
				
			}
			else if (packet.mItemsMap.get(key) instanceof Vector) {
				Vector vector = (Vector) packet.mItemsMap.get(key);
				String str = vector.toString();
				KeyValue kv = new KeyValue(key, str);
				list.add(kv);
				
			}
		}
//		KeyValue kv = new KeyValue("1", "2");
//		list.add(kv);
//		kv = new KeyValue("3", "4");
//		list.add(kv);
		
		return list;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_record_layout);	
        
        mContext = this;
        
  	    Button editButton = (Button) findViewById(R.id.buttonCancel);
  	    editButton.setOnClickListener(new View.OnClickListener() {
  	         public void onClick(View v) {
  	        	 finish();
  	         }
  	    });
  	            
  	    Button cancelButton = (Button) findViewById(R.id.buttonSave);
  	    cancelButton.setOnClickListener(new View.OnClickListener() {
  	         public void onClick(View v) {
  	        	 
  	        	Intent intent = getIntent();
  	        	
				Bundle args = new Bundle();
				args.putSerializable("EXISTINGITEM", mCurrentPacket);
				intent.putExtras(args);  	        	
  	        	((Activity) mContext).setResult(RESULT_OK, intent);
  	        	 finish();
  	         }
  	    });
  	            
        
        
        
		Bundle bundle = getIntent().getExtras();
		String articleNodeId = bundle.getString("article_id");        
		
		
		try
		{
			Bundle extras = getIntent().getExtras();
			mCurrentPacket = (DataOutPacket) extras.getSerializable("EXISTINGITEM");
			Log.e(TAG, mCurrentPacket.toString());

	        listview = (ListView) findViewById(R.id.listView_list);		
	        KeyValuePacketArrayAdapter adapter2 = new KeyValuePacketArrayAdapter(this, PacketToArrayOfValues(mCurrentPacket));
	        listview.setAdapter(adapter2);        
		
		}
		catch(Exception ex){

		}		
        
        		
		
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	
}
