/*****************************************************************
UnitTests

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
package com.t2.h2test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandler.GlobalH2;
import com.t2.dataouthandlertest.Global;
import com.t2.dataouthandlertest.R;

public class UnitTests {
	private static final String TAG = UnitTests.class.getSimpleName();
	
    private List<UnitTestParams> UnitTestQueue =
            Collections.synchronizedList(new ArrayList<UnitTestParams>());	
    
    String mDatabaseTypeString = "";    
    
	int mLargePacketLength;
	int mTooLargePacketLength;
    

	public UnitTests(Context context, String databaseTypeString) {
		mDatabaseTypeString = databaseTypeString;
		
		if (mDatabaseTypeString.equalsIgnoreCase(context.getString(R.string.database_type_drupal))) {
			mLargePacketLength = 20000;
			mTooLargePacketLength = 24000;
		}
		else if (mDatabaseTypeString.equalsIgnoreCase(context.getString(R.string.database_type_aws))) {
			mLargePacketLength = 64000;
			mTooLargePacketLength = 24001;
		}
		else {
			mLargePacketLength = 64000;
			mTooLargePacketLength = 24001;
		}		
	}
	
	
    
	// ------------------------------------------------------------------
	// Unit Test Cases
	// ------------------------------------------------------------------
	
	/**
	 * Performs unit tests on system by sending various forms of data packets to server
	 */
	public void performUnitTests() {
		try {
			
	
			UnitTestParams p1 = generatePacketFullGood(1, "sendFullPayload");
			UnitTestParams p2 = generateTestPacketNumericAsStrings(2, "sendTestPacketNumericAsStrings");
			UnitTestParams p3 = generateTestPacketEmpty(3, "sendTestPacketEmpty");
			UnitTestParams p4 = generateTestPacketMinimalVersionOnly(4, "sendTestPacketMinimalVersionOnly");
			UnitTestParams p5 = generateTestPacketLarge(5, "sendTestPacketLarge", mLargePacketLength);
			UnitTestParams p6 = generateTestPacketNull(6, "sendTestPAcketNull");
			UnitTestParams p7 = generateTestPacketRepeatedParameters(7, "sendTestPacketRepeatedParameters");
			UnitTestParams p8 = generateTestPacketEmptyJSONArray(8, "sendTestPacketEmptyJSONArray");
			UnitTestParams p9 = generateTestPacketJSONArrayTooManyLevels(9, "sendTestPacketJSONArrayTooManyLevels");
			UnitTestParams p10 = generateTestPacketTooLarge(10, "sendTestPacketTooLarge", mTooLargePacketLength );
			// 11 fails - see note in UnitTestParams
				UnitTestParams p11 = generateTestPacketUnknownInconsistentTags(11, "sendTestPacketUnknownInconsistentTags");
			UnitTestParams p12 = generateTestPacketParameterTypes(12, "sendTestPacketParameterTypes");
			UnitTestParams p13 = generateTestPacketInvalidCharacter(13, "sendgenerateTestPacketInvalidCharacter");
			UnitTestParams p14 = generateTestPacketParameterOutOfRange(14, "sendTestPacketParameterOutOfRange");

			UnitTestParams p15 = generateTestPacketHabit(15, "TestPacketHabit");
			UnitTestParams p16 = generateTestPacketCheckin(16, "TestPacketCheckin");
			
			// Now add the test cases to the queue and execute them
			new PacketTestTask().execute(p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16);		
			
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}			
	}

	// ------------------------------------------------------------------
	// Perform one unit test
	// ------------------------------------------------------------------
	/**
	 * Causes a number of test cases to be run
	 *   This is done in two parts.
	 *   Part 1 - Performed here
	 *   	The packet is sent to the remote database
	 *   	The packet is added to the UnitTestQueue for later processing
	 *   
	 *   Part 2 -
	 *   	When the client receives the remoteDatabaseSyncComplete() callback
	 *   	(This means that the remote database thinks it's up to date with the 
	 *   	local cache) we process the unit tests (Check for pass/fail) 
	 *   
	 * @author scott.coleman
	 *
	 */
	class PacketTestTask extends AsyncTask<UnitTestParams, Void, Boolean> {

	    private Exception exception;

	    protected Boolean doInBackground(UnitTestParams... unitTestParams) {
	    	UnitTestParams currentTestParams = unitTestParams[0];
			Boolean packetsAreEqual = false;
			
			for (UnitTestParams unitTestParam : unitTestParams) {
				// --------------------------------------------------
				// Step 1 - Send the packet
				// --------------------------------------------------
				try {
					Log.e(TAG, "Adding test case " + unitTestParam.mTestCase + " : " + unitTestParam.mDescription);
					Global.sDataOutHandler.handleDataOut(unitTestParam.mPacketUnderTest);
					unitTestParam.mStatus = Global.UNIT_TEST_EXECUTING;
					
					synchronized(UnitTestQueue) {
						UnitTestQueue.add(unitTestParam);
					}
					
					// Arbitrary delay between test cases
					Thread.sleep(1000);
				} catch (DataOutHandlerException e) {
					Log.e(TAG, e.toString());
					Log.e(TAG, "Test Case " + unitTestParam.mTestCase + "           FAILED");
					unitTestParam.mStatus = Global.UNIT_TEST_FAILED;					
				}	
				catch (InterruptedException e) {
					Log.e(TAG, e.toString());
					Log.e(TAG, "Test Case " + unitTestParam.mTestCase + "           FAILED");
					unitTestParam.mStatus = Global.UNIT_TEST_FAILED;					
				}
			}
			return packetsAreEqual;
	    }

	    protected void onPostExecute(Boolean packetsAreEqual) {
	    }
	 }		
	
	/**
	 * Processes unit tests which are in the UNIT_TEST_EXECUTING state of the UnitTestQueue
	 *   This should be called every time the dataOutHandler has completed synchronization
	 *   
	 * @param remoteContentsMap Hash map mapping all current record id's to drupal id's
	 */
	public void processUnitTests(HashMap<String, String> remoteContentsMap) {
		
		if (remoteContentsMap == null || Global.sDataOutHandler == null)
			return;

		synchronized(UnitTestQueue) {

			// Iterate through all unit tests in process. If a test is executing, check to see if it's packet status
			// is idle (sent correctly). If so then compute pass fail criteria for that test.
			for (UnitTestParams unitTestParam : UnitTestQueue) {
				if (unitTestParam.mStatus == Global.UNIT_TEST_EXECUTING) {
					
					DataOutPacket packetTestResult = Global.sDataOutHandler.getPacketByRecordId(unitTestParam.mPacketUnderTest.mRecordId);
					if (packetTestResult != null) {
						if (packetTestResult.mCacheStatus == GlobalH2.CACHE_IDLE) {
							
							// --------------------------------------------------
							// Step 3 - Compare the sent packet with the one from 
							//          the cache (remote database)
							// --------------------------------------------------
							Log.e(TAG, "Computing results for test case " + unitTestParam.mTestCase + " : " + unitTestParam.mDescription);
							
							Boolean passed;
	
							if (unitTestParam.mAlternateResultPacket != null) {
								if (unitTestParam.mIgnoreList != null)
									passed = packetTestResult.equalsIgnoreTag(unitTestParam.mAlternateResultPacket, unitTestParam.mIgnoreList);
								else
									passed = packetTestResult.equals(unitTestParam.mAlternateResultPacket);
							}
							else {
								if (unitTestParam.mIgnoreList != null)
									passed = packetTestResult.equalsIgnoreTag(unitTestParam.mPacketUnderTest, unitTestParam.mIgnoreList);
								else
									passed = packetTestResult.equals(unitTestParam.mPacketUnderTest);
							}				
							
							if (passed) {
								Log.d(TAG, "Test Case " + unitTestParam.mTestCase + "           PASSED");		
								unitTestParam.mStatus = Global.UNIT_TEST_PASSED;								
							}
							else {
								Log.e(TAG, "Test Case " + unitTestParam.mTestCase + "           FAILED");
								unitTestParam.mStatus = Global.UNIT_TEST_FAILED;								
							}								
						}				
					}
					else {
						unitTestParam.mStatus = Global.UNIT_TEST_INVALID;						
					}
				}
			}
		}
	}
	
	
	// ------------------------------------------------------------------
	// Generation of packets for test cases
	// ------------------------------------------------------------------
	
	public UnitTestParams generatePacketFullGood(int testCaseNum, String description) {

		DataOutPacket packet = new DataOutPacket();

		// Throw in some dummy location data
		packet.add(DataOutHandlerTags.version, "Test Version");
		packet.add(DataOutHandlerTags.ACCEL_Z, (double) 34.5678);
		packet.add(DataOutHandlerTags.ACCEL_Y, (double) 34.5678);
		packet.add(DataOutHandlerTags.ACCEL_X, (double) 34.5678);
		packet.add(DataOutHandlerTags.ORIENT_Z, (double) 34.5678);
		packet.add(DataOutHandlerTags.ORIENT_Y, (double) 34.5678);
		packet.add(DataOutHandlerTags.ORIENT_X, (double) 34.5678);
        packet.add(DataOutHandlerTags.LIGHT, (float) 1.0);
        packet.add(DataOutHandlerTags.PROXIMITY, (float) 1.0);
        packet.add(DataOutHandlerTags.BATTERY_LEVEL, (int) 1);
        packet.add(DataOutHandlerTags.BATTERY_STATUS, (int) 1);
	   	packet.add(DataOutHandlerTags.SCREEN, 1);
	   	packet.add(DataOutHandlerTags.MODEL, "Galaxy S3");
	   	packet.add(DataOutHandlerTags.LOCALE_LANGUAGE, "english");
	   	packet.add(DataOutHandlerTags.LOCALE_COUNTRY, "usa");
		packet.add(DataOutHandlerTags.TEL_CELLID, (int) 1);
		packet.add(DataOutHandlerTags.TEL_MDN, (long) 123);
		packet.add(DataOutHandlerTags.TEL_NETWORK, "verizon");
        packet.add(DataOutHandlerTags.GPS_LON, (double) 34.5678);
        packet.add(DataOutHandlerTags.GPS_LAT, (double) 34.5678);
        packet.add(DataOutHandlerTags.GPS_SPEED, (float) 34.5678);
        packet.add(DataOutHandlerTags.GPS_TIME, (long) 123456);
        packet.add(DataOutHandlerTags.KEYLOCKED, 1);        	

		packet.add(DataOutHandlerTags.BLUETOOTH_ENABLED, 1);			
		packet.add(DataOutHandlerTags.WIFI_ENABLED, 0);			
		packet.add(DataOutHandlerTags.WIFI_CONNECTED_AP, "fred");			
		packet.add(DataOutHandlerTags.CALL_DIR, "in");
		packet.add(DataOutHandlerTags.CALL_REMOTENUM, "2536779838");
		packet.add(DataOutHandlerTags.CALL_DURATION, (int) 1);
    	packet.add(DataOutHandlerTags.SMS_DIR, "out");
		packet.add(DataOutHandlerTags.SMS_REMOTENUM, "2536779838");
		packet.add(DataOutHandlerTags.SMS_LENGTH, (int) 1);
		packet.add(DataOutHandlerTags.MMS_DIR, "in");
		packet.add(DataOutHandlerTags.MMS_REMOTENUM, "2536779838");
		packet.add(DataOutHandlerTags.MMS_LENGTH, (int) 1);
    	packet.add(DataOutHandlerTags.WEBPAGE, "google.com");	    			


		Vector<String> taskVector = new Vector<String>();
		taskVector.add("one");
		taskVector.add("two");
        	
		packet.add(DataOutHandlerTags.TASKS, taskVector);

		Vector<String> bluetoothVector = new Vector<String>();
		bluetoothVector.add("four");
		bluetoothVector.add("five");
		bluetoothVector.add("six");
		
		packet.add(DataOutHandlerTags.BLUETOOTH_PAIREDDEVICES, bluetoothVector);

		Vector<String> wifiVector = new Vector<String>();
		wifiVector.add("seven");
		wifiVector.add("eight");
		wifiVector.add("nine");
		wifiVector.add("ten");
		
		packet.add(DataOutHandlerTags.WIFI_APSCAN, wifiVector);	
		
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);
		
		
		
		return unitTestParams;
	}
	
	
	public UnitTestParams generateTestPacketNumericAsStrings(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketNumericAsStrings");
		packet.add(DataOutHandlerTags.ACCEL_X, String.valueOf((double) 11.11111));
		packet.add(DataOutHandlerTags.ACCEL_Y, String.valueOf((double) 22.22222));
		packet.add(DataOutHandlerTags.ACCEL_Z, String.valueOf((double) 33.33333));
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}	
	
	// Check that record is saved (only header data in record)
	public UnitTestParams generateTestPacketEmpty(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketEmpty");

		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}

	public UnitTestParams generateTestPacketMinimalVersionOnly(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacket1");

		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}
	
	public UnitTestParams generateTestPacketLarge(int testCaseNum, String description, int largePacketLength) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketLarge");
		
		char[] array = new char[largePacketLength];
		
		for (int i = 0; i < largePacketLength; i++) {
			int ones = i % 9;
			array[i] = (char) (0x30 + ones);
		}
		
		packet.add("test_field", new String(array));

		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}	

	public UnitTestParams generateTestPacketNull(int testCaseNum, String description) {

		DataOutPacket packet = null;
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

//		Log.d(TAG, "Test case " + testCase + ": sendTestPacketNull");
//		packet = generateTestPacketNull();				// Should throw null pointer exception (but not crash)
//		try {
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//		} catch (Exception e) {
//			
//			if (e.toString().equalsIgnoreCase("java.lang.NullPointerException") ) {
//				Log.d(TAG, "Test Case " + testCase + "           PASSED");
//			}
//			Log.d(TAG, e.toString());
//		}			
//		testCase++;			
		
		return unitTestParams;
	}	
	
	public UnitTestParams generateTestPacketRepeatedParameters(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketRepeatedParameters");
		packet.add(DataOutHandlerTags.ACCEL_Z, (double) 11.11111);
		packet.add(DataOutHandlerTags.ACCEL_Z, (double) 22.22222);

		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}	

	public UnitTestParams generateTestPacketEmptyJSONArray(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketEmptyJSONArray");
		Vector<String> taskVector = new Vector<String>();
		packet.add(DataOutHandlerTags.TASKS, taskVector);

		// Now create pass/fail criteria
		List<String> ignoreList;	
		ignoreList = new ArrayList<String>();
		ignoreList.add(DataOutHandlerTags.TASKS);	
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, ignoreList, false, description);

		return unitTestParams;
	}		
	
	// Note, this will pass on Drupal but fail on AWS (which doesn't allow multiple levels
	public UnitTestParams generateTestPacketJSONArrayTooManyLevels(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketJSONArrayTooManyLevels");
		Vector<Vector> taskVector = new Vector<Vector>();
		Vector<String> innerVector = new Vector<String>();			// One too man levels of nesting
		innerVector.add("one");
		taskVector.add(innerVector);
		packet.add(DataOutHandlerTags.TASKS, taskVector);
		
		
//		// Now create pass/fail criteria
//		DataOutPacket expectedpacket = new DataOutPacket();
//		expectedpacket.add(DataOutHandlerTags.version, "TestPacketJSONArrayTooManyLevels");
//		Vector<String> expectedTaskVector = new Vector<String>();
//		expectedTaskVector.add("one");
//		expectedpacket.add(DataOutHandlerTags.TASKS, expectedTaskVector);			
		
		List<String> ignoreList;
		ignoreList = new ArrayList<String>();
		ignoreList.add("time_stamp");
		ignoreList.add("record_id");		
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, ignoreList, false, description);

		return unitTestParams;
	}		

	public UnitTestParams generateTestPacketTooLarge(int testCaseNum, String description, int tooLargePacketLength) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketTooLarge");
		
		char[] array = new char[tooLargePacketLength];
		
		for (int i = 0; i < tooLargePacketLength; i++) {
			int ones = i % 9;
			array[i] = (char) (0x30 + ones);
		}
		
		packet.add("test_field", new String(array));
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		
	

	// This one fails now because of the following
	// when we add a packet it goes to the cache, then is sent to the remote database
	// The database returns with the node id to tell us that it was successfully added.
	// In this case it reuturns the node id even though id doesn't save the unknown tags.
	// We then use the Cache as if it were good (the the remote database doesn't match it
	
	// The fix is, when we receive the node id from the remote database telling us of the 
	// success we need to actually grab the record and reconcile it with the cache.
	public UnitTestParams generateTestPacketUnknownInconsistentTags(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketUnknownInconsistentTags");
		packet.add("UnknownTag1", "Unknown1");
		packet.add("AIRFLOW", "test@gmail.com");
		packet.add("UnknownTag2", "Unknown2");
		
		// Now create pass/fail criteria
		DataOutPacket packetExpected = new DataOutPacket();
		packetExpected.add(DataOutHandlerTags.version, "TestPacketUnknownInconsistentTags");
		packetExpected.add("AIRFLOW", "test@gmail.com");
		
		List<String> ignoreList = new ArrayList<String>();
		ignoreList.add("time_stamp");
		ignoreList.add("record_id");		
		
		
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), packetExpected, ignoreList, false, description);

		return unitTestParams;
	}		
	
	public UnitTestParams generateTestPacketParameterTypes(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketParameterTypes");
		// Note that we have to hyjack some tags for this test
		packet.add(DataOutHandlerTags.ACCEL_X, (char) 'a');
		packet.add(DataOutHandlerTags.ACCEL_Y, (byte) 1);
		packet.add(DataOutHandlerTags.ACCEL_Z, (short) 2);
		packet.add(DataOutHandlerTags.ORIENT_X, (int) 3);
		packet.add(DataOutHandlerTags.ORIENT_Y, (long) 4);
		packet.add(DataOutHandlerTags.ORIENT_Y, (float) 5.5);
		packet.add(DataOutHandlerTags.ORIENT_X, (double) 6.6);
		packet.add(DataOutHandlerTags.ORIENT_X, (double) 6.6);
		packet.add(DataOutHandlerTags.GPS_LAT, (char) 7);
		packet.add(DataOutHandlerTags.GPS_LON, "eight");
		
		// Now create pass/fail criteria
		Vector<String> taskVector = new Vector<String>();
		taskVector.add("one");
		taskVector.add("two");
		packet.add(DataOutHandlerTags.TASKS, taskVector);	
		
		
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		

	public UnitTestParams generateTestPacketInvalidCharacter(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestTestPacketInvalidCharacter");
		// Note that we have to hyjack some tags for this test
		packet.add(DataOutHandlerTags.ACCEL_Y, "S\uFFFF Se\uFFFFFFFFor"); // This says si senior
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, true, description);

		return unitTestParams;
	}		


	// This passes because we are simply sending all numerics as strings
	public UnitTestParams generateTestPacketParameterOutOfRange(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketParameterOutOfRange");
		// Note that we have to hyjack some tags for this test
		packet.add(DataOutHandlerTags.ACCEL_X, "99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999");
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		

	public UnitTestParams generateTestPacketHabit(int testCaseNum, String description) {
		DataOutPacket packet = new DataOutPacket(DataOutHandlerTags.STRUCTURE_TYPE_HABIT);
		packet.mTitle = "Test habit 1 - name";		
		packet.add(DataOutHandlerTags.version, description);
		packet.add(DataOutHandlerTags.HABIT_NOTE, "Test habit 1 - note");
		
	    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    	Calendar calendar = GregorianCalendar.getInstance();
        String currentTimeString = dateFormatter.format(calendar.getTime());
		packet.add(DataOutHandlerTags.HABIT_REMINDER_TIME, currentTimeString);
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		

	public UnitTestParams generateTestPacketCheckin(int testCaseNum, String description) {

	    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    	Calendar calendar = GregorianCalendar.getInstance();
    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString = dateFormatter.format(calendar.getTime());
		
		
		DataOutPacket packet = new DataOutPacket(DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN);
		packet.add(DataOutHandlerTags.version, description);
		packet.add(DataOutHandlerTags.CHECKIN_CHECKIN_TIME, timeString);
		packet.add(DataOutHandlerTags.CHECKIN_HABIT_ID, 1);
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		

	public UnitTestParams generateTestPacketCheckinH4H(int testCaseNum, String description) {

	    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    	Calendar calendar = GregorianCalendar.getInstance();
//    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));   // Drupal wants normal format
        String timeString = dateFormatter.format(calendar.getTime());
		
		
		DataOutPacket packet = new DataOutPacket(DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN_H4H);
		packet.mTitle = "Test Checkin";
//		packet.add(DataOutHandlerTags.version, description);
		packet.add(DataOutHandlerTags.CHECKIN_CHECKIN_TIME, timeString);
		packet.add(DataOutHandlerTags.CHECKIN_HABIT_ID, 1);
		
		// Now create pass/fail criteria
		UnitTestParams unitTestParams = new UnitTestParams(packet, String.valueOf(testCaseNum), null, null, false, description);

		return unitTestParams;
	}		

}
