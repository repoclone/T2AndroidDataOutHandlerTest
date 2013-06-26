/*****************************************************************
dataouthandlertest

Copyright (C) 2011 The National Center for Telehealth and 
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
