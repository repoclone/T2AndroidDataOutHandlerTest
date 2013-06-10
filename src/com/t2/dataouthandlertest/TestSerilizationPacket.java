package com.t2.dataouthandlertest;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.amazonaws.services.dynamodb.model.AttributeValue;

public class TestSerilizationPacket implements Serializable {

	String s1;
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public String mStr = "";
//	public ObjectNode mItem;		
//	public ObjectNode mData;		
	HashMap<String, AttributeValue> hashMap = new HashMap<String, AttributeValue>();			
	HashMap<String, String> mItemsMap = new HashMap<String, String>();	

	
	public TestSerilizationPacket() {
		UUID uuid = UUID.randomUUID();
		Calendar calendar = GregorianCalendar.getInstance();
		long currentTime = calendar.getTimeInMillis();
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	    String currentTimeString = dateFormatter.format(calendar.getTime());
		String id = currentTime + "-" + uuid.toString();
//		
//		if (mLogFormat == LOG_FORMAT_JSON) {
//			mStr = "{" + SHORT_TIME_STAMP + ":" + currentTime + ",";			
//		}
//		else {
//			mStr = SHORT_TIME_STAMP + ",";			
//		}
//		
//    	mData = JsonNodeFactory.instance.objectNode();		
//    	mItem = JsonNodeFactory.instance.objectNode();		
    			
		
		
		
	}
	public String toString() {
		String result = "";
		   Iterator it = mItemsMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        result += pairs.getKey() + " = " + pairs.getValue() + ", ";
		       // it.remove(); // avoids a ConcurrentModificationException
		    }		
		

		return result;
	}	
//	public String toString() {
//		return dateFormatter.toString();
//		
//	}

}
