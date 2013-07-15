package com.t2.dataouthandlertest;

import android.content.Context;

import com.t2.dataouthandler.DataOutHandler;

public class Global {

	public static DataOutHandler sDataOutHandler;
	
	public static DataOutHandler InitDatabase (Context context, String userId, String sessionDate, String appName, String dataType, long sessionId) {
		sDataOutHandler = DataOutHandler.getInstance(context, userId, sessionDate, appName, dataType, sessionId);
		return sDataOutHandler;
		
	}
	
	
		
	
	
	
}
