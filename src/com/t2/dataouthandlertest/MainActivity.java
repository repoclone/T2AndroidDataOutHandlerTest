/*****************************************************************
MainActivity

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
import com.t2.h2test.UnitTests;




import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity implements OnSharedPreferenceChangeListener, T2AuthDelegate, 
	DatabaseCacheUpdateListener, OnItemClickListener  {

    private static List<UnitTestParams> UnitTestQueue =
            Collections.synchronizedList(new ArrayList<UnitTestParams>());	
    
    private UnitTests unitTests;

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String APP_ID = "DataOutHandlerTest";	
	private static final String NOT_USED_STRING = "";
	private static final Long NOT_USED_LONG = (long) 0;
	
	public static final int ACTIVITY_REFERENCE = 0x302;		
	public static final int TEST_CASE_TIMEOUT = 30000;
	
	private boolean mLoggingEnabled = false;
	private boolean mLogCatEnabled = true;
	
	private String mDatabaseTypeString = "";
	
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
	
	private List<String> dataTypesToShow = new ArrayList<String>();
	
	private boolean[]  mDataTypesToggleArray;
	
	void initDatabase() {

		Log.d(TAG, "Initializing  database at " + mRemoteDatabaseUri);
		
		

		dataTypesToShow.add(DataOutHandlerTags.STRUCTURE_TYPE_HABIT);
		dataTypesToShow.add(DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN);
		dataTypesToShow.add(DataOutHandlerTags.STRUCTURE_TYPE_SENSOR_DATA);
		
		mDataTypesToggleArray = new boolean[GlobalH2.VALID_DATA_TYPES.length];
		for (int i = 0; i < GlobalH2.VALID_DATA_TYPES.length; i++) {
			mDataTypesToggleArray[i] = true;
		}
		
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
			
			Log.d(TAG, "Using DataOutHandler version " + DataOutHandler.getVersion());
			
			
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			mDatabaseTypeString = sharedPreferences.getString("external_database_type", "AWS");
			
			
		    unitTests = new UnitTests(this, mDatabaseTypeString);    
			
		    Global.sDataOutHandler.setRequiresCSRF(true);
						
//			Global.sDataOutHandler.initializeDatabase( mRemoteDatabaseUri, mDatabaseTypeString, this);
			Global.sDataOutHandler.initializeDatabase( mRemoteDatabaseUri, DataOutHandler.DATABASE_TYPE_T2_DRUPAL, this);

			
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

    
    
    
    @Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
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
  	    //textView.setText(item.mRecordId);
  	    textView.setText(item.mTitle);
  	    
  	    Button editButton = (Button) rowView.findViewById(R.id.button_edit);
  	    editButton.setOnClickListener(new View.OnClickListener() {
  	         public void onClick(View v) {
  	        	 Log.e(TAG, "Edit Button " + buttonposition);
				Intent intent = new Intent(mContext, EditRecordActivity.class);
				// Send the currently selected DataOutPacked for editing
				Bundle args = new Bundle();
				
				Log.e(TAG, "**** " + item.mChangedDate );
				Log.e(TAG, "**** " + item.toString() );
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
 				}
  	         }
  	    });  	    
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

        Button loginButton = (Button) findViewById(R.id.button_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "loggin in");
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
				
				AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle("Add data");
                alert.setItems(GlobalH2.VALID_DATA_TYPES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	switch (which) {
                    	case 0:
                    		UnitTestParams params = unitTests.generatePacketFullGood(12345678, "sendFullPayload");
            				SendPacket(params.mPacketUnderTest);
                    		break;
                	
                    	case 1:
                    		params = unitTests.generateTestPacketHabit(12345678, "sendFullPayload");
            				SendPacket(params.mPacketUnderTest);
                    		break;
                	
                    	case 2:
                    		params = unitTests.generateTestPacketCheckinH4H(12345678, "sendFullPayload");
            				SendPacket(params.mPacketUnderTest);
                	
                    		default:
                    			break;
                    	}
                    }
                });
                alert.show();				
				
			}
		});        
        
        Button chooseDataTypesButton = (Button) findViewById(R.id.button_choose_data_types);
        chooseDataTypesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
		    	alert.setTitle("Choose data types to display");
				    	alert.setMultiChoiceItems(GlobalH2.VALID_DATA_TYPES,
		    			mDataTypesToggleArray,
	                    new DialogInterface.OnMultiChoiceClickListener() {

		    			public void onClick(DialogInterface dialog, int whichButton,boolean isChecked) {
		    			}
	                    });
		    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	
	                	// Set dataTypesToShow based on selections made
	            		dataTypesToShow.clear();
	  
	            		for (int i = 0; i < GlobalH2.VALID_DATA_TYPES.length; i++) {
	            			if (mDataTypesToggleArray[i] == true) {
	            				dataTypesToShow.add(GlobalH2.VALID_DATA_TYPES[i]);
	            			}
	            		}
	                	
	            		final ArrayList packetList = Global.sDataOutHandler.getPacketList(dataTypesToShow);
	                    if (packetList != null) {
	                        MainActivity.this.runOnUiThread(new Runnable(){
	                            public void run(){
	                        		DataOutPacketArrayAdapter adapter2 = new DataOutPacketArrayAdapter(mActivity, packetList);
	                                mListview.setAdapter(adapter2);                 
	                            }
	                        }); 		
	                    }      
	                	
	            		
	                }
	            });

				alert.show();							
			}
		});        
        
        Button testDataButton = (Button) findViewById(R.id.button_TestData);
        testDataButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				unitTests.performUnitTests();
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

		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);			
			String applicationVersion = info.versionName;
			String versionString = APP_ID + " application version: " + applicationVersion;

			// Don't do this here, wait until we're authenticated
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
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketList(dataTypesToShow);
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

        Log.d(TAG, "Login Successful: " + "displayName = " + displayName + ", verifiedEmail = " + verifiedEmail);
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

				Log.e(TAG, "**** " + updatedPacket.mChangedDate );

				
				try {
					updatedPacket.updateChangedDate();

					Global.sDataOutHandler.updateRecordInCache(updatedPacket);
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
		Log.d(TAG, "Packet Created/Updated: " + packet.mTitle + ", "+ packet.mRecordId);
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketList(dataTypesToShow);
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
		Log.e(TAG, "Packet deleted: " + packet.mTitle + ", " + packet.mRecordId);
		
		final ArrayList packetList = Global.sDataOutHandler.getPacketList(dataTypesToShow);
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
		//Log.e(TAG, "remoteDatabaseSyncComplete() ");
		unitTests.processUnitTests(remoteContentsMap);
		//Log.e(TAG, "End remoteDatabaseSyncComplete() ");
	}


	
	
}
