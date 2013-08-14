/*****************************************************************
AltEntryActivity

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

import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRDictionary;
import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandler.T2AuthDelegate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class AltEntryActivity extends Activity implements T2AuthDelegate{

	private DataOutHandler mDataOutHandler;	
	private static final String TAG = AltEntryActivity.class.getSimpleName();
	
	
	
	public AltEntryActivity() {

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.alt_entry_activity);		
		
        Button altActivityButton = (Button) findViewById(R.id.buttonAddEntry);
        altActivityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				DataOutPacket packet = generateTestPacketMinimalVersionOnly();
				SendPacket(packet);			
			}
		});        
        		
        
        try {
			mDataOutHandler = DataOutHandler.getInstance();
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}       
        
        
		
	}

	DataOutPacket generateTestPacketMinimalVersionOnly() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacket1");
		return packet;	
	}
	
	void SendPacket(DataOutPacket packet) {
		try {
			mDataOutHandler.handleDataOut(packet);
		} catch (DataOutHandlerException e) {
			e.printStackTrace();
		}		
	}	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void T2AuthSuccess(JRDictionary auth_info, String provider,
			HttpResponseHeaders responseHeaders, String responsePayload) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void T2AuthFail(JREngageError error, String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void T2AuthNotCompleted() {
		// TODO Auto-generated method stub
		
	}

}
