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
