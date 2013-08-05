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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Vector;


import org.t2health.lib1.SharedPref;

import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRDictionary;
import com.janrain.android.utils.StringUtils;
import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandler.T2AuthDelegate;
import com.t2.dataouthandler.DatabaseCacheUpdateListener;
import com.t2.dataouthandler.dbcache.SqlPacket;
//import com.t2.dataouthandler.DataOutHandler;
//import com.t2.dataouthandler.DataOutHandler.DataOutPacket;
import com.t2.dataouthandlertest.Archiver.LoadException;




import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener, T2AuthDelegate, 
	DatabaseCacheUpdateListener, OnItemClickListener  {

//	private DatabaseHelper db;
	
	
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String APP_ID = "DataOutHandlerTest";	
	private static final String NOT_USED_STRING = "";
	private static final Long NOT_USED_LONG = (long) 0;
	
	public static final int ACTIVITY_REFERENCE = 0x302;		
	
	public static final int TEST_CASE_TIMEOUT = 30000;
	
	
	private boolean mLoggingEnabled = false;
	private boolean mLogCatEnabled = true;
	
	private String mDatabaseTypeString = "";
	int mLargePacketLength;
	int mTooLargePacketLength;
	
	private Context mContext;
	private MainActivity mActivity;
	
	private ListView mListview;
	private DataOutPacketArrayAdapter mPacketDataAdapter;	
	
	/**
	 * Default Server database to sync to
	 * Not: to change this change it in strings.xml
	 */
	private String mDefaultRemoteDatabaseUri;	
	
	/**
	 * Database uri that the service will sync to 
	 */
	private String mRemoteDatabaseUri;	
	
	private DataOutPacket mPacketUnderTest = null;

	private DataOutPacket mPacketTestResult = null;
	
	private String mPacketTestResultNodeId = null;
	

	void initDatabase() {

		Log.d(TAG, "Initializing  database at " + mRemoteDatabaseUri);

		Calendar cal = Calendar.getInstance();						
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
		String sessionDate = sdf.format(new Date());
		String userId = SharedPref.getString(this, "SelectedUser", 	"Scott");
		long sessionId = 0;		
		
		// ----------------------------------------------------
		// Create a data handler to handle outputting data
		//	to files and database
		// ----------------------------------------------------		
		try {
			Global.sDataOutHandler = DataOutHandler.getInstance(this, userId,sessionDate, APP_ID,DataOutHandler.DATA_TYPE_INTERNAL_SENSOR, sessionId);
			
			Global.sDataOutHandler.enableLogging(this);
			Global.sDataOutHandler.enableLogCat();
			
			Global.sDataOutHandler.setDatabaseUpdateListener(this);
			
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			mDatabaseTypeString = sharedPreferences.getString("external_database_type", "AWS");
			
			if (mDatabaseTypeString.equalsIgnoreCase(getString(R.string.database_type_drupal))) {
				mLargePacketLength = 20000;
				mTooLargePacketLength = 24000;
			}
			else if (mDatabaseTypeString.equalsIgnoreCase(getString(R.string.database_type_aws))) {
				mLargePacketLength = 64000;
				mTooLargePacketLength = 24001;
			}
			else {
				mLargePacketLength = 64000;
				mTooLargePacketLength = 24001;
			}
						
			
			
			
			Global.sDataOutHandler.initializeDatabase( mRemoteDatabaseUri, mDatabaseTypeString, this);
			Global.sDataOutHandler.setRequiresAuthentication(true);
			
			
			
		} catch (Exception e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		}        
		
		if (mLoggingEnabled) {
			Global.sDataOutHandler.enableLogging(this);
		}   
		
		if (mLogCatEnabled) {
			Global.sDataOutHandler.enableLogCat();
		}  	
		
	     
	}
	
    @Override
	protected void onDestroy() {
    	Global.sDataOutHandler.close();
    	Global.sDataOutHandler = null;
		super.onDestroy();
	}

    public class DataOutPacketArrayAdapter extends ArrayAdapter<DataOutPacket> {
  	  private final Context context;

  	  public DataOutPacketArrayAdapter(Context context, List<DataOutPacket> values) {
  	    super(context, R.layout.row_layout, values);
  	    this.context = context;
  	  }

  	  @Override
  	  public View getView(final int position, View convertView, ViewGroup parent) {
  	    LayoutInflater inflater = (LayoutInflater) context
  	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final int buttonposition = position;	  	    
  	    
  	    View rowView = inflater.inflate(R.layout.manager_item, parent, false);
  	    TextView textView = (TextView) rowView.findViewById(R.id.label);

  	    final DataOutPacket item = (DataOutPacket) this.getItem(position);  	    
  	    textView.setText(item.mRecordId);

  	    
  	    Button editButton = (Button) rowView.findViewById(R.id.button_edit);
  	    editButton.setOnClickListener(new View.OnClickListener() {
  	         public void onClick(View v) {
  	        	 Log.e(TAG, "Edit Button " + buttonposition);
				Intent intent = new Intent(mContext, EditRecordActivity.class);
				// Send the currently selected DataOutPacked for editing
				Bundle args = new Bundle();
				args.putSerializable("EXISTINGITEM", item);
				intent.putExtras(args);
				startActivityForResult(intent, ACTIVITY_REFERENCE);				
				
				
  	         }
  	    });
  	    
  	    Button deleteButton = (Button) rowView.findViewById(R.id.button_delete);
  	    deleteButton.setOnClickListener(new View.OnClickListener() {
  	         public void onClick(View v) {
  	        	 Log.e(TAG, "Delete Button " + buttonposition);
 				try {
 				Global.sDataOutHandler.deleteRecord(item);
 				} catch (DataOutHandlerException e) {
 					Log.e(TAG, e.toString());
 					//e.printStackTrace();
 				}

  	         }
  	    });  	    
  	    
//  	    Button deleteButton = (Button) rowView.findViewById(R.id.delete);
//  	    editButton.setOnClickListener(new View.OnClickListener() {
//  	         public void onClick(View v) {
//  	        	 Log.e(TAG, "Button " + buttonposition);
//  	        	 Log.e(TAG, "2");
//  				Intent intent = new Intent(mContext, EditRecordActivity.class);
//  				Bundle bundle = new Bundle();
//  				// TODO maybe put the entire record in as serializable (see pill planner)
//  				bundle.putInt("article_id",position);  				
//  				intent.putExtras(bundle);				
//  				startActivity(intent);	
//  	         }
//  	    });
  	    
  	    return rowView;
  	  }
  	}       
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mActivity = this;
        
        mListview = (ListView) findViewById(R.id.listView1);
        
        mListview.setOnItemClickListener(this);        

//        List<SqlPacket> fakePacketListSql = new ArrayList<SqlPacket>();		
//    	SqlPacket pktSql = new SqlPacket();
//    	pktSql.setRecordId("123");
//    	fakePacketListSql.add(pktSql);
//    	pktSql = new SqlPacket();
//    	pktSql.setRecordId("456");
//    	fakePacketListSql.add(pktSql);
//		
//    	List<DataOutPacket> fakePacketList = new ArrayList<DataOutPacket>();		
//        DataOutPacket pkt = new DataOutPacket();
//        fakePacketList.add(pkt);
//        pkt = new DataOutPacket();
//        fakePacketList.add(pkt);
//        pkt = new DataOutPacket();
//        fakePacketList.add(pkt);
        
//        DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(this, fakePacketListSql);
//        mListview.setAdapter(adapter2);        
//        
        
        Button loginButton = (Button) findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				try {
//					Global.sDataOutHandler.initializeDatabase( mRemoteDatabaseUri, mDatabaseTypeString, mActivity);
//				} catch (DataOutHandlerException e) {
//					Log.e(TAG, e.toString());
//					e.printStackTrace();
//				}
				Global.sDataOutHandler.logIn(mActivity);
			}
		});        
        
        Button logoutButton = (Button) findViewById(R.id.button_Logout);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Global.sDataOutHandler.logOut();
			} 
		});        

        Button altActivityButton = (Button) findViewById(R.id.button_alt_entry_activity);
        altActivityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(mContext, AltEntryActivity.class);
				startActivity(intent);				
				
			}
		});        
        
        Button addDataButton = (Button) findViewById(R.id.button_AddData);
        addDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				DataOutPacket packet = generatePacketFullGood();
				SendPacket(packet);			
			}
		});        
        
        Button updateDataButton = (Button) findViewById(R.id.button_fetch_all);
        updateDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
			}
		});        
        
        Button testDataButton = (Button) findViewById(R.id.button_TestData);
        testDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				performUnitTests();
			}
		});          
        
// 		We'll leave this blank, that way DataOutHandler will pick the default uri based on database type
//      mDefaultRemoteDatabaseUri = getResources().getString(R.string.default_aws_database_uri);	
        mDefaultRemoteDatabaseUri = "";	
        mRemoteDatabaseUri = SharedPref.getString(this, "database_sync_name", mDefaultRemoteDatabaseUri);
        Log.d(TAG, "Remote database Uri = " + mRemoteDatabaseUri);	

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(getString(R.string.external_database_type), getString(R.string.database_type_drupal)); // Set database type to DRUPAL
//      prefsEditor.putString(getString(R.string.external_database_type), getString(R.string.database_type_aws)); // Set database type to AWS
//      prefsEditor.putString(getString(R.string.external_database_type), getString(R.string.database_type_t2_rest)); // Set database type to T2
        prefsEditor.commit();
        
	    prefs.registerOnSharedPreferenceChangeListener(this);    
        
	    initDatabase();

		// Log the version
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);			
			String applicationVersion = info.versionName;
			String versionString = APP_ID + " application version: " + applicationVersion;

//			DataOutPacket packet = new DataOutPacket();
//			packet.add("version", versionString);
//			Global.sDataOutHandler.handleDataOut(packet);				

		}
		catch (Exception e) {
		   	Log.e(TAG, e.toString());
		}
		
		
    }

	
	
    @Override
	protected void onResume() {
		super.onResume();
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketListDOP();
        if (packetList != null) {
            MainActivity.this.runOnUiThread(new Runnable(){
                public void run(){
            		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                    mListview.setAdapter(adapter2);                 
                }
            }); 		
        }
		
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    public boolean onOptionsItemSelected(MenuItem item) {
        //respond to menu item selection

		switch (item.getItemId()) {
		    case R.id.menu_settings:
			    startActivity(new Intent(this, DataOutTestPreferences.class));
		    return true;

		    default:
		    return super.onOptionsItemSelected(item);
		}    	
    }    

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	    if (key.endsWith("external_database_type")) {
	    	String stringValue = sharedPreferences.getString(key, "-1");
	      	Global.sDataOutHandler.close();
	      	Global.sDataOutHandler = null;
	    	initDatabase();
	      }	
	}

	@Override
	public void T2AuthSuccess(JRDictionary auth_info, String provider,
			HttpResponseHeaders responseHeaders,String responsePayload) {
		
		
		
        JRDictionary profile = (auth_info == null) ? null : auth_info.getAsDictionary("profile");
        String identifier = (profile == null) ? "" : profile.getAsString("identifier");
        String displayName = (profile == null) ? "" : profile.getAsString("displayName");
        String verifiedEmail = (profile == null) ? "" : profile.getAsString("verifiedEmail");

        Log.e(TAG, "Login Successful: " + "displayName = " + displayName + ", verifiedEmail = " + verifiedEmail);

		
		
        new AlertDialog.Builder(mContext).setMessage("Login was successful.").setPositiveButton("OK", null).setCancelable(true).create().show();
	}

	@Override
	public void T2AuthFail(JREngageError error, String provider) {
        new AlertDialog.Builder(mContext).setMessage("Login failed!").setPositiveButton("OK", null).setCancelable(true).create().show();
	}

	@Override
	public void T2AuthNotCompleted() {
        new AlertDialog.Builder(mContext).setMessage("Login not completed").setPositiveButton("OK", null).setCancelable(true).create().show();
	}
	
	// ------------------------------------------------------------------
	// Test Cases
	// ------------------------------------------------------------------
	
	/**
	 * Performs unit tests on system by sending various forms of data packets to server
	 */
	void performUnitTests() {
		try {
			
			int testCase = 1;
			DataOutPacket packet;
			List<String> ignoreList;			
			
			Log.d(TAG, "Test case " + testCase + ": sendFullPayload");
			packet = generatePacketFullGood();
			TestPacket(packet, String.valueOf(testCase), null, null, false);			
			testCase++;
			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketNumericAsStrings");
//			packet = generateTestPacketNumericAsStrings();
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketEmpty");
//			packet = generateTestPacketEmpty();
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;
//			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketMinimalVersionOnly");
//			packet = generateTestPacketMinimalVersionOnly();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketLarge");
//			packet = generateTestPacketLarge();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketNull");
//			packet = generateTestPacketNull();				// Should throw null pointer exception (but not crash)
//			try {
//				TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			} catch (Exception e) {
//				
//				if (e.toString().equalsIgnoreCase("java.lang.NullPointerException") ) {
//					Log.d(TAG, "Test Case " + testCase + "           PASSED");
//				}
//				Log.d(TAG, e.toString());
//			}			
//			testCase++;
//
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketRepeatedParameters");
//			packet = generateTestPacketRepeatedParameters();
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;
//			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketEmptyJSONArray");
//			// We need to ignore the vector field because of the way drupal reports an empty vector arry
//			packet = generateTestPacketEmptyJSONArray();	
//			ignoreList = new ArrayList<String>();
//			ignoreList.add(DataOutHandlerTags.TASKS);
//			TestPacket(packet, String.valueOf(testCase), null, ignoreList, false);			
//			testCase++;
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketJSONArrayTooManyLevels");
//			// Note that in this case we need to supply an alternative reference packet
//			// Since the database converts the extra levels to one level
//			// and in doing so we need to ignore time_stamp, and record_id parameters
//			packet = generateTestPacketJSONArrayTooManyLevels();
//			DataOutPacket expectedpacket = generateTestPacketJSONArrayTooManyLevelsAlternateResult();			
//			ignoreList = new ArrayList<String>();
//			ignoreList.add("time_stamp");
//			ignoreList.add("record_id");
//			TestPacket(packet, String.valueOf(testCase), expectedpacket, ignoreList, false);			
//			testCase++;
//
//		
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketTooLarge");
//			packet = generateTestPacketTooLarge();	
//			try {
//				TestPacket(packet, String.valueOf(testCase), null, null, true);			
//			} catch (Exception e) {
//				e.printStackTrace();
//			}			
//			testCase++;
//			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketMinimalVersionOnly");
//			packet = generateTestPacketMinimalVersionOnly();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;			
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketUnknownInconsistentTags");
//			packet = generateTestPacketUnknownInconsistentTags();			
//			DataOutPacket packetExpected = generateTestPacketUnknownInconsistentTagsExpected();		
//			ignoreList = new ArrayList<String>();
//			ignoreList.add("time_stamp");
//			ignoreList.add("record_id");			
//			TestPacket(packet, String.valueOf(testCase), packetExpected, ignoreList, false);			
//			testCase++;			
//			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketParameterTypes");
//			packet = generateTestPacketParameterTypes();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;			
//
//			Log.d(TAG, "Test case " + testCase + ": sendgenerateTestPacketInvalidCharacter");
//			packet = generateTestPacketInvalidCharacter();			
//			TestPacket(packet, String.valueOf(testCase), null, null, true);			
//			testCase++;			
//
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketParameterOutOfRange");
//			packet = generateTestPacketParameterOutOfRange();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;			
//
//			
//			Log.d(TAG, "Test case " + testCase + ": sendTestPacketParameterTypes");
//			packet = generateTestPacketParameterTypes();			
//			TestPacket(packet, String.valueOf(testCase), null, null, false);			
//			testCase++;	
//
//			
			
			
			
			
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}			
	}
	

	
	// ------------------------------------------------------------------
	// Generation of packets for test cases
	// ------------------------------------------------------------------
	
	DataOutPacket generateTestPacketParameterOutOfRange() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketParameterOutOfRange");
		// Note that we have to hyjack some tags for this test
		packet.add(DataOutHandlerTags.ACCEL_X, "S\u00ED Se\u00F1or");
		packet.add(DataOutHandlerTags.ACCEL_Y, "S\uFFFF Se\uFFFFFFFFor");
		return packet;
	}	

	DataOutPacket generateTestPacketInvalidCharacter() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestTestPacketInvalidCharacter");
		// Note that we have to hyjack some tags for this test
		packet.add(DataOutHandlerTags.ACCEL_Y, "S\uFFFF Se\uFFFFFFFFor");
		return packet;
	}	

	DataOutPacket generateTestPacketParameterTypes() {
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
		
		Vector<String> taskVector = new Vector<String>();
		taskVector.add("one");
		taskVector.add("two");
		packet.add(DataOutHandlerTags.TASKS, taskVector);		
		
		return packet;
	}	


	
	DataOutPacket generateTestPacketUnknownInconsistentTags() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketUnknownInconsistentTags");
		packet.add("UnknownTag1", "Unknown1");
		packet.add("AIRFLOW", "test@gmail.com");
		packet.add("UnknownTag2", "Unknown2");
		return packet;
	}	

	DataOutPacket generateTestPacketUnknownInconsistentTagsExpected() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketUnknownInconsistentTags");
		packet.add("AIRFLOW", "test@gmail.com");
		return packet;
	}	

	DataOutPacket generateTestPacketNumericAsStrings() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketNumericAsStrings");
		packet.add(DataOutHandlerTags.ACCEL_X, String.valueOf((double) 11.11111));
		packet.add(DataOutHandlerTags.ACCEL_Y, String.valueOf((double) 22.22222));
		packet.add(DataOutHandlerTags.ACCEL_Z, String.valueOf((double) 33.33333));
		return packet;
}	

	// Check that only the last of the repeated parameter is saved to the database
	DataOutPacket generateTestPacketRepeatedParameters() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketRepeatedParameters");
		packet.add(DataOutHandlerTags.ACCEL_Z, (double) 11.11111);
		packet.add(DataOutHandlerTags.ACCEL_Z, (double) 22.22222);			
		return packet;
	}	
	
	// Check that either record is saved or no record is saved (and no corruption of database)
	DataOutPacket generateTestPacketJSONArrayTooManyLevels() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketJSONArrayTooManyLevels");
		Vector<Vector> taskVector = new Vector<Vector>();
		Vector<String> innerVector = new Vector<String>();
		innerVector.add("one");
		taskVector.add(innerVector);
		packet.add(DataOutHandlerTags.TASKS, taskVector);			
		return packet;	
	}

	DataOutPacket generateTestPacketJSONArrayTooManyLevelsAlternateResult() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketJSONArrayTooManyLevels");
		Vector<String> taskVector = new Vector<String>();
		taskVector.add("one");
		packet.add(DataOutHandlerTags.TASKS, taskVector);			
		return packet;	
	}

	// Check that record is saved
	DataOutPacket generateTestPacketEmptyJSONArray() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketEmptyJSONArray");
		Vector<String> taskVector = new Vector<String>();
		packet.add(DataOutHandlerTags.TASKS, taskVector);			
		return packet;	
	}
	
	// Check that record is saved
	DataOutPacket generateTestPacketLarge() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketLarge");
		
		char[] array = new char[mLargePacketLength];
		
		for (int i = 0; i < mLargePacketLength; i++) {
			int ones = i % 9;
			array[i] = (char) (0x30 + ones);
		}
		
		packet.add("test_field", new String(array));
		return packet;	
	}
	
	
	// Check that record is NOT saved (and no corruption of database)
	DataOutPacket generateTestPacketTooLarge() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketTooLarge");
		
		char[] array = new char[mTooLargePacketLength];
		
		for (int i = 0; i < mTooLargePacketLength; i++) {
			int ones = i % 9;
			array[i] = (char) (0x30 + ones);
		}
		
		packet.add("test_field", new String(array));
		return packet;	
	}
	
	// Check that record is saved
	DataOutPacket generateTestPacketMinimalVersionOnly() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacket1");
		return packet;	
	}
	
	// Check that record is saved (only header data in record)
	DataOutPacket generateTestPacketEmpty() {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.version, "TestPacketEmpty");
		return packet;	
	}
	

	// Check exception is thrown (and no corruption of database)	
	DataOutPacket generateTestPacketNull() {
		return null;
	}

	DataOutPacket generatePacketFullGood() {
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
		return packet;
	}
	
	
	
	// ------------------------------------------------------------------
	// Perform one unit test
	// ------------------------------------------------------------------
	
	
	/**
	 * Sends a packet to the DataOutHandler, waits for it to process it
	 * then retrieves it and compares it to the original,
	 * logging the result	  
	 * @param packet - Packet to send
	 * @param testCase testCase - String containing test case number
	 * @param alternateResultPacket - Alternate packet to test against
	 * @param ignoreList - List of any parameter tags to ignore
	 * @param reverseResults - Reverses results (Pass/Fail)
	 * @return True if PASS, false if FAIL
	 */
	boolean TestPacket(DataOutPacket packet, String testCase, DataOutPacket alternateResultPacket, 
						List<String> ignoreList, Boolean reverseResults) {
		
		if (ignoreList != null) {
			// All tests in low case
		    ListIterator<String> iterator = ignoreList.listIterator();
		    while (iterator.hasNext())
		    {
		        iterator.set(iterator.next().toLowerCase());
		    }		
		}
		
		mPacketUnderTest = packet;
		try {
			Global.sDataOutHandler.handleDataOut(packet);
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
		}
		
		// Wait for the test case to complete
		// or timeout
		long startTime = System.currentTimeMillis();
		while (mPacketTestResultNodeId == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
			long now = System.currentTimeMillis(); 
			long span = now - startTime; 
			if ( now - startTime > TEST_CASE_TIMEOUT) {
				Log.e(TAG, "Test Case " + testCase + "           FAILED - timeout");
				mPacketTestResultNodeId = null;
				return false;			
			}
		}
		
		
		Boolean p1 = mPacketTestResultNodeId.equalsIgnoreCase("99999"); 
		if (p1) {
			Boolean p2 = reverseResults ? !p1 : p1;
			if (p2) {
				Log.e(TAG, "Test Case " + testCase + "           FAILED");
				mPacketTestResultNodeId = null;
				return false;			
			}
			else {
				Log.d(TAG, "Test Case " + testCase + "           PASSED");
				mPacketTestResultNodeId = null;
				return true;			
			}
		}
		
		
		DataOutPacket resultPacket = Global.sDataOutHandler.getPacketByDrupalId(mPacketTestResultNodeId);
		mPacketTestResultNodeId = null;
		
		Boolean packetsAreEqual;
		if (alternateResultPacket != null) {
			if (ignoreList != null)
				packetsAreEqual = resultPacket.equalsIgnoreTag(alternateResultPacket, ignoreList);
			else
				packetsAreEqual = resultPacket.equals(alternateResultPacket);
		}
		else {
			if (ignoreList != null)
				packetsAreEqual = resultPacket.equalsIgnoreTag(mPacketUnderTest, ignoreList);
			else
				packetsAreEqual = resultPacket.equals(mPacketUnderTest);
		}
		
		Boolean passed = reverseResults ? !packetsAreEqual : packetsAreEqual;
		if (packetsAreEqual) {
			Log.d(TAG, "Test Case " + testCase + "           PASSED");
			return true;
		}
		else {
			Log.e(TAG, "Test Case " + testCase + "           FAILED");
			return false;
		}
	}		
	
	/**
	 * Sends the requested packet to the DataOutHandler for Create/Update
	 * @param packet
	 */
	void SendPacket(DataOutPacket packet) {
		mPacketUnderTest = packet;
		try {
			Global.sDataOutHandler.handleDataOut(packet);
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
		}		
	}
	
	
	

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
		
		/// This gets called after EditRecord saves a record
		case ACTIVITY_REFERENCE:
			if (data != null) {
				Bundle extras = data.getExtras();
				DataOutPacket updatedPacket = (DataOutPacket) extras.getSerializable("EXISTINGITEM");
				//Log.e(TAG, updatedPacket.toString());			
				
				// Now save the updated record to database
				// Also need to update Global.sDataOutHandler.mRemotePacketCache

				try {
					Global.sDataOutHandler.updateRecord(updatedPacket);
				} catch (DataOutHandlerException e) {
					Log.e(TAG, e.toString());
					
				}

			}
			break;
		}
	}

	/* (non-Javadoc)
	 * @see com.t2.dataouthandler.DatabaseCacheUpdateListener#remoteDatabaseCreateUpdateComplete(com.t2.dataouthandler.DataOutPacket)
	 * database cache has been successfully updated from this client.
	 * 
	 *  mRemotePacketCache has been updated
	 *  
	 * the parameter msg contains the update message.
	 * For node updates it contains the node id.
	 * For array updates it contains "[true].
	 */
	@Override
	public void remoteDatabaseCreateUpdateComplete(DataOutPacket packet) {
		Log.d(TAG, "Packet Created/Updated: " + packet.mRecordId);
		
		mPacketTestResultNodeId = packet.mDrupalNid;
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketListDOP();
        if (packetList != null) {
            MainActivity.this.runOnUiThread(new Runnable(){
                public void run(){
            		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                    mListview.setAdapter(adapter2);                 
                }
            }); 		
        }      
	}

	/* (non-Javadoc)
	 * @see com.t2.dataouthandler.DatabaseCacheUpdateListener#remoteDatabaseDeleteComplete(com.t2.dataouthandler.DataOutPacket)
	 * remote database has been successfully updated from this client.
	 * 
	 * mRemotePacketCache has been updated 
	 * 
	 * the parameter msg contains the update message.
	 * For node updates it contains the node id.
	 * For array updates it contains "[true].	 * 
	 */
	@Override
	public void remoteDatabaseDeleteComplete(DataOutPacket packet) {
		Log.e(TAG, "Packet deleted: " + packet.mRecordId);
		
		mPacketTestResultNodeId = packet.mDrupalNid;		// This is for unit tests only
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketListDOP();
        if (packetList != null) {
            MainActivity.this.runOnUiThread(new Runnable(){
                public void run(){
            		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                    mListview.setAdapter(adapter2);                 
                }
            }); 		
        }   
	}

	/* (non-Javadoc)
	 * @see com.t2.dataouthandler.DatabaseCacheUpdateListener#remoteDatabaseFailure(java.lang.String)
	 * 
	 * There has been a generic error communicating with the remote database
	 */
	@Override
	public void remoteDatabaseFailure(String msg) {
		mPacketTestResultNodeId = "99999";		
	}


}
