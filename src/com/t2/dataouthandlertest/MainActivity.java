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
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import com.t2.dataouthandler.GlobalH2;


import org.t2health.lib1.SharedPref;

import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRDictionary;
import com.janrain.android.utils.StringUtils;
import com.t2.aws.DynamoDBManager;
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
import com.t2.h2test.UnitTestParams;




import android.os.AsyncTask;
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
import android.graphics.drawable.Drawable;
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
	
    private static List<UnitTestParams> UnitTestQueue =
            Collections.synchronizedList(new ArrayList<UnitTestParams>());	

    //It is imperative that the user manually synchronize on the returned list when iterating over it:
//    List list = Collections.synchronizedList(new ArrayList());
//    ...
//synchronized(list) {
//    Iterator i = list.iterator(); // Must be in synchronized block
//    while (i.hasNext())
//        foo(i.next());
//}    
    
	
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
	
	
	private HashMap<String, String> mRemoteContentsMap = null;
	
	
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
				
				UnitTestParams params = UnitTestParams.generatePacketFullGood(12345678, "sendFullPayload");
				SendPacket(params.mPacketUnderTest);			
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
			
	
			UnitTestParams params1 = UnitTestParams.generatePacketFullGood(1, "sendFullPayload");
			UnitTestParams params2 = UnitTestParams.generatePacketFullGood(2, "sendTestPacketNumericAsStrings");
			UnitTestParams params3 = UnitTestParams.generateTestPacketEmpty(3, "sendTestPacketEmpty");
			UnitTestParams params4 = UnitTestParams.generatePacketFullGood(4, "sendTestPacketMinimalVersionOnly");
			UnitTestParams params5 = UnitTestParams.generateTestPacketLarge(5, "sendTestPacketLarge", mLargePacketLength);
//			UnitTestParams params6 = UnitTestParams.generateTestPacketNull(testCase++, "sendTestPAcketNull");
			UnitTestParams params7 = UnitTestParams.generateTestPacketRepeatedParameters(6, "sendTestPacketRepeatedParameters");
			UnitTestParams params8 = UnitTestParams.generateTestPacketEmptyJSONArray(7, "sendTestPacketEmptyJSONArray");
			// 9 fails
						UnitTestParams params9 = UnitTestParams.generateTestPacketJSONArrayTooManyLevels(8, "sendTestPacketJSONArrayTooManyLevels");
			UnitTestParams params10 = UnitTestParams.generateTestPacketTooLarge(9, "sendTestPacketTooLarge", mTooLargePacketLength );
			// 10 fails
				UnitTestParams params11 = UnitTestParams.generateTestPacketUnknownInconsistentTags(10, "sendTestPacketUnknownInconsistentTags");
			UnitTestParams params12 = UnitTestParams.generateTestPacketParameterTypes(11, "sendTestPacketParameterTypes");
			UnitTestParams params13 = UnitTestParams.generateTestPacketInvalidCharacter(12, "sendgenerateTestPacketInvalidCharacter");
			UnitTestParams params14 = UnitTestParams.generateTestPacketParameterOutOfRange(13, "sendTestPacketParameterOutOfRange");


			
			// Now add the test cases to the queue and execute them
			new PacketTestTask().execute(params1, params2, params3, params4, params5, 
					params7, params8, params9, params10, params11, params12, params13, params14 
					);		
			
			
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
	 *   	When the client receives the remoteDatabaseCurrentContents() callback
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
			

			
			for (UnitTestParams param : unitTestParams) {
				// --------------------------------------------------
				// Step 1 - Send the packet
				// --------------------------------------------------
				try {
					Log.e(TAG, "Adding test case " + param.mTestCase + " : " + param.mDescription);
					Global.sDataOutHandler.handleDataOut(param.mPacketUnderTest);
					param.mStatus = Global.UNIT_TEST_EXECUTING;
					
					synchronized(UnitTestQueue) {
						UnitTestQueue.add(param);
					}
					
					
					
					// Arbitrary delay between test cases
					Thread.sleep(1000);
				} catch (DataOutHandlerException e) {
					Log.e(TAG, e.toString());
				}	
				catch (InterruptedException e) {
					Log.e(TAG, e.toString());
				}

			}
			
			return packetsAreEqual;
	    }

	    protected void onPostExecute(Boolean packetsAreEqual) {
	    }
	 }	
	
	
	
	/**
	 * Sends the requested packet to the DataOutHandler for Create/Update
	 * @param packet
	 */
	void SendPacket(DataOutPacket packet) {
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
//		mPacketTestResultNodeId = "99999";		
	}


	/* (non-Javadoc)
	 * 
	 * Getting here means the API just got a fresh update of database contents from the database
	 * and that the cache is now in sync with the remote database
	 * 
	 * @see com.t2.dataouthandler.DatabaseCacheUpdateListener#remoteDatabaseSyncComplete(java.util.HashMap)
	 */
	@Override
	public void remoteDatabaseSyncComplete(
			HashMap<String, String> remoteContentsMap) {
		mRemoteContentsMap = remoteContentsMap;
		Log.e(TAG, "remoteDatabaseCurrentContents() ");
		processUnitTests(remoteContentsMap);
		Log.e(TAG, "End remoteDatabaseCurrentContents() ");

		
	}

	
	private void processUnitTests(HashMap<String, String> remoteContentsMap) {
		
		if (remoteContentsMap == null)
			return;

		synchronized(UnitTestQueue) {
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
					
					
				}
				
				
			}
		}

		
		
	}
	
	

}
