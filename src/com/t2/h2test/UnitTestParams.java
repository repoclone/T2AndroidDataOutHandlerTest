/*****************************************************************
UnitTestParams

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
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import android.os.AsyncTask;
import android.util.Log;

import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandlertest.Global;
import com.t2.dataouthandlertest.MainActivity;

/**
 * This class is meant to contain all parameters for defining a test case
 *   Both the test case data itself and the pass/fail results criteria
 *   
 * @author scott.coleman
 *
 */
public class UnitTestParams {
	private static final String TAG = UnitTestParams.class.getSimpleName();


	public DataOutPacket mPacketUnderTest;
	public String mTestCase;
	public DataOutPacket mAlternateResultPacket;
	public List<String> mIgnoreList;
	public Boolean mReverseResults;	
	public int mStatus;
	public String mDescription;
	
	/**
	 * Creates a unit test
	 * 
	 * @param packetUnderTest - DataOutPacket to test
	 * @param testCase - Test case number
	 * @param alternateResultPacket - Packet to test against (if null, it compares to packetUnderTest) 
	 * @param ignoreList - Parameters to ignore in the test
	 * @param reverseResults - Pass becomes fail, visa versa
	 * @param description - Description of the test case
	 */
	public UnitTestParams(DataOutPacket packetUnderTest, String testCase, DataOutPacket alternateResultPacket, 
			List<String> ignoreList, Boolean reverseResults, String description) {
		mPacketUnderTest = packetUnderTest; 
		mTestCase = testCase;
		mAlternateResultPacket = alternateResultPacket; 
		mIgnoreList = ignoreList;
		mReverseResults = reverseResults;
		mStatus = Global.UNIT_TEST_WAITING;
		mDescription = description;
	}
	
}
