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
//import com.t2.dataouthandler.DataOutHandler;
//import com.t2.dataouthandler.DataOutHandler.DataOutPacket;
import com.t2.dataouthandlertest.Archiver.LoadException;
import com.t2.drupalsdk.DrupalUpdateListener;



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
	DrupalUpdateListener, OnItemClickListener  {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String APP_ID = "DataOutHandlerTest";	
	private static final String NOT_USED_STRING = "";
	private static final Long NOT_USED_LONG = (long) 0;
	
	public static final int ACTIVITY_REFERENCE = 0x302;		
	
	
	private boolean mLoggingEnabled = false;
	private boolean mLogCatEnabled = true;
	
	private String mDatabaseTypeString = "";
	int mLargePacketLength;
	int mTooLargePacketLength;
	
	private Context mContext;
	private Activity mActivity;
	
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
	
	
	
	/**
	 * Class to help in saving received data to H2
	 */
	private DataOutHandler mDataOutHandler;	

	void initDatabase() {

		Log.d(TAG, "Initializing database at " + mRemoteDatabaseUri);

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
			mDataOutHandler = new DataOutHandler(this, userId,sessionDate, APP_ID, 
					DataOutHandler.DATA_TYPE_INTERNAL_SENSOR, sessionId );
			mDataOutHandler.enableLogging(this);
			mDataOutHandler.enableLogCat();
			
			mDataOutHandler.setDrupalUpdateListener(this);
			
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
						
			
			
			
			mDataOutHandler.initializeDatabase( mRemoteDatabaseUri, mDatabaseTypeString, this);
			mDataOutHandler.setRequiresAuthentication(false);
			
			
			
		} catch (Exception e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		}        
		
		if (mLoggingEnabled) {
			mDataOutHandler.enableLogging(this);
		}   
		
		if (mLogCatEnabled) {
			mDataOutHandler.enableLogCat();
		}  	
		
	     
	}
	
	void terminateDatabase() {
    	mDataOutHandler.close();
    	mDataOutHandler = null;
	}
	
	
    @Override
	protected void onDestroy() {
    	terminateDatabase();
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
  	    
  	    final DataOutPacket item = this.getItem(position);  	    
  	    textView.setText(item.mId);

  	    
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
 				// Now save the updated record to Drupal
 				// Also need to update mDataOutHandler.mRemoteDrupalPacketList

 				try {
 					mDataOutHandler.deleteRecord(item);
 				} catch (DataOutHandlerException e) {
 					Log.e(TAG, e.toString());
 					e.printStackTrace();
 				}

 				drupalReadComplete(); // TODO - change this to callback
  	        	 
  			  	        	 
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
        
        // We don't put anything in the view until we get the callback drupalUpdateComplete()

    	List<DataOutPacket> fakePacketList = new ArrayList<DataOutPacket>();		
//        DataOutPacket pkt = new DataOutPacket();
//        fakePacketList.add(pkt);
//        pkt = new DataOutPacket();
//        fakePacketList.add(pkt);
        
        DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(this, fakePacketList);
        mListview.setAdapter(adapter2);        
        
        
        Button loginButton = (Button) findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDataOutHandler.showAuthenticationDialog(mActivity);
			}
		});        
        
        Button logoutButton = (Button) findViewById(R.id.button_Logout);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				mDataOutHandler.close();
				mDataOutHandler.logOut();
			}
		});        
        
        Button addDataButton = (Button) findViewById(R.id.button_AddData);
        addDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sendTestPacketFullGood();
 				drupalReadComplete(); // TODO - change this to callback
				
			}
		});        
        
        Button updateDataButton = (Button) findViewById(R.id.button_fetch_all);
        updateDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				fetchAllData();
			}
		});        
        
        Button testDataButton = (Button) findViewById(R.id.button_TestData);
        testDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				unitTest();
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
//			mDataOutHandler.handleDataOut(packet);				

		}
		catch (Exception e) {
		   	Log.e(TAG, e.toString());
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
	    	  terminateDatabase();
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
	
	
	/**
	 * Cause the DataOutHandler to update arrays from Remote Drupal database
	 * 	mRemoteDrupalNodeIdList
	 *  mRemoteDrupalPacketList
	 * 
	 */
	void fetchAllData() {
		mDataOutHandler.getRemoteDrupalNodes();
	}
	
	
	// ------------------------------------------------------------------
	// Test Cases
	// ------------------------------------------------------------------
	
	/**
	 * Performs unit tests on system by sending various forms of data packets to server
	 */
	void unitTest() {
		try {
			sendTestPacketFullGood();
//			sendTestPacketEmpty();
//			sendTestPacketNull();				// Should throw null pointer exception (but not crash)
//			sendTestPacket1();			
//			sendTestPacketLarge();			
//			sendTestPacketTooLarge();	
//			sendTestPacketEmptyJSONArray();	
//			sendTestPacketJSONArrayTooManyLevels();
//			sendTestPacketRepeatedParameters();
//			sendTestPacketNumericAsStrings();
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}			
	}

	void sendTestPacketNumericAsStrings() {
		try {
			Log.d(TAG, "sendTestPacketNumericAsStrings");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketNumericAsStrings");
			packet.add(DataOutHandlerTags.ACCEL_X, String.valueOf((double) 11.11111));
			packet.add(DataOutHandlerTags.ACCEL_Y, String.valueOf((double) 22.22222));
			packet.add(DataOutHandlerTags.ACCEL_Z, String.valueOf((double) 33.33333));
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}	
	
	// Check that only the last of the repeated parameter is saved to the database
	void sendTestPacketRepeatedParameters() {
		try {
			Log.d(TAG, "sendTestPacketRepeatedParameters");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketRepeatedParameters");
			packet.add(DataOutHandlerTags.ACCEL_Z, (double) 11.11111);
			packet.add(DataOutHandlerTags.ACCEL_Z, (double) 22.22222);			
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}	
	
	// Check that either record is saved or no record is saved (and no corruption of database)
	
	///TODO: put check in code to throw an exception for this
	void sendTestPacketJSONArrayTooManyLevels() {
		try {
			Log.d(TAG, "sendTestPacketJSONArrayTooManyLevels");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketJSONArrayTooManyLevels");

			
			Vector<Vector> taskVector = new Vector<Vector>();
			Vector<String> innerVector = new Vector<String>();
			innerVector.add("one");

			taskVector.add(innerVector);
			
			packet.add(DataOutHandlerTags.TASKS, taskVector);			
			
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}

	// Check that record is saved
	void sendTestPacketEmptyJSONArray() {
		try {
			Log.d(TAG, "sendTestPacketEmptyJSONArray");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketEmptyJSONArray");
			Vector<String> taskVector = new Vector<String>();
			packet.add(DataOutHandlerTags.TASKS, taskVector);			
			
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}
	
	// Check that record is saved
	void sendTestPacketLarge() {
		try {
			Log.d(TAG, "sendTestPacketLarge");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketLarge");
			
			char[] array = new char[mLargePacketLength];
			
			for (int i = 0; i < mLargePacketLength; i++) {
				int ones = i % 9;
				array[i] = (char) (0x30 + ones);
			}
			
			packet.add("test_field", new String(array));
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}
	
	
	// Check that record is NOT saved (and no corruption of database)
	void sendTestPacketTooLarge() {
		try {
			Log.d(TAG, "sendTestPacketTooLarge");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketTooLarge");
			
			char[] array = new char[mTooLargePacketLength];
			
			for (int i = 0; i < mTooLargePacketLength; i++) {
				int ones = i % 9;
				array[i] = (char) (0x30 + ones);
			}
			
			packet.add("test_field", new String(array));
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}
	
	// Check that record is saved
	void sendTestPacket1() {
		try {
			Log.d(TAG, "sendTestPacket1");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacket1");
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}		
	}
	
	// Check that record is saved (only header data in record)
	void sendTestPacketEmpty() {
		try {
			Log.d(TAG, "sendTestPacketEmpty");
			DataOutPacket packet = new DataOutPacket();
			packet.add(DataOutHandlerTags.version, "sendTestPacketEmpty");
			
			mDataOutHandler.handleDataOut(packet);		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}			
	}
	

	// Check exception is thrown (and no corruption of database)	
	void sendTestPacketNull() {
		try {
			Log.d(TAG, "sendTestPacketNull");
			mDataOutHandler.handleDataOut(null);	
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}			
	}
	
	
	/**
	 * Sends a dummy packet to the database
	 * @throws DataOutHandlerException 
	 */
	// Check that record is saved
	void sendTestPacketFullGood() {
		try {
			Log.d(TAG, "sendTestPacketFullGood");		
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
			mDataOutHandler.handleDataOut(packet);
			
			
			
		} catch (Exception e) {
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
				Log.e(TAG, updatedPacket.toString());			
				
				// Now save the updated record to Drupal
				// Also need to update mDataOutHandler.mRemoteDrupalPacketList

				try {
					mDataOutHandler.updateRecord(updatedPacket);
				} catch (DataOutHandlerException e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
				}

 				drupalReadComplete(); // TODO - change this to callback
				
				
			}
			break;
		}
		
		
	}

	/* (non-Javadoc)
	 * @see com.t2.drupalsdk.DrupalUpdateListener#drupalCreateUpdateComplete(java.lang.String)
	 * drupal has been successfully updated from this client.
	 * the parameter msg contains the update message.
	 * For node updates it contains the node id.
	 * For array updates it contains "[true].
	 */
	@Override
	public void drupalCreateUpdateComplete(String msg) {
		Log.e(TAG, "create/update - " + msg);
		final ArrayList packetList = new ArrayList(mDataOutHandler.mRemoteDrupalPacketCache.values());
		
//		Log.e(TAG, packetList.toString());

        
        MainActivity.this.runOnUiThread(new Runnable(){
            public void run(){
        		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                mListview.setAdapter(adapter2);                 
            }
        });        
        
		
	}
	
	/* (non-Javadoc)
	 * @see com.t2.drupalsdk.DrupalUpdateListener#drupalCreateUpdateComplete(java.lang.String)
	 * drupal has been successfully updated from this client.
	 * the parameter msg contains the update message.
	 * For node updates it contains the node id.
	 * For array updates it contains "[true].
	 */
	@Override
	public void drupalDeleteComplete(String msg) {
		Log.e(TAG, "delete - " + msg);
		final ArrayList packetList = new ArrayList(mDataOutHandler.mRemoteDrupalPacketCache.values());
		
        MainActivity.this.runOnUiThread(new Runnable(){
            public void run(){
        		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                mListview.setAdapter(adapter2);                 
            }
        });   
		
	}

	@Override
	public void drupalReadComplete() {
		final ArrayList packetList = new ArrayList(mDataOutHandler.mRemoteDrupalPacketCache.values());
        
        MainActivity.this.runOnUiThread(new Runnable(){
            public void run(){
        		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
                mListview.setAdapter(adapter2);                 
            }
        });           
	}
	
	
	
}
