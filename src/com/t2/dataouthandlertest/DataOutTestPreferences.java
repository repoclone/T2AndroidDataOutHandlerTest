package com.t2.dataouthandlertest;



import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;


public class DataOutTestPreferences extends PreferenceActivity
implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener, OnPreferenceClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        // Load the preferences from an XML resource
		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.data_out_prefs);		
	}

	@Override
	protected void onStop() {

		
		
//		 finish();		
			super.onStop();
	}

	
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	      if (key.endsWith("external_database_type")) {
	    	  String stringValue = sharedPreferences.getString(key, "-1");
	        	      Toast.makeText(this, "Database type changed to " + stringValue , Toast.LENGTH_LONG).show();
	      }		
		
	}

	@Override
    protected void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }    
	

}
