package org.t2.dataouthandler.classes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Handles all database operations
 * 
 * @author Scott Coleman (scott.coleman@tee2.org)
 */

public class DatabaseHelper
{

	private static final String DATABASE_NAME = "dataouthandlertest.db";
	private static final int DATABASE_VERSION = 1;

	private Context context;
	private SQLiteDatabase db;

	public DatabaseHelper(Context context) 
	{
		this.context = context;      
	}

	public static String scrubInput(String input)
	{
		//add more reserved SQL characters to prevent a sql injection attack or just a crash
		String Output = input.replace("'", "''");
		return Output;
	}

	public List<SqlPacket> getPacketList()
	{

		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, Packet, DrupalId, RecordId from PERSISTENTCACHE";
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) 
		{
			List<SqlPacket> packets = new ArrayList<SqlPacket>();

			do 
			{
				SqlPacket packet = new SqlPacket();
				packet.setPacketId(cursor.getString(0));
				packet.setPacket(cursor.getString(1));
				packet.setDrupalId(cursor.getString(2));
				packet.setRecordId(cursor.getString(3));
				packets.add(packet);
			}
			while (cursor.moveToNext());

			if (cursor != null && !cursor.isClosed()) 
			{
				cursor.close();
			}

			db.close();
			return packets;
		}
		else
		{
			cursor.close();
			db.close();
			return null;
		}
	}	
	
	public SqlPacket createNewSqlPacket(String packet, String recordId, String drupalId)
	{

		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();

		try
		{
			ContentValues insertValues = new ContentValues();
			insertValues.put("Packet", packet);
			insertValues.put("RecordId", recordId);
			insertValues.put("DrupalId", drupalId);
			db.insert("PERSISTENTCACHE", null, insertValues);

			return getPacketByRecordId(recordId);
		}
		catch(Exception ex)
		{
			return null;
		}
		finally
		{
			db.close();
		}

	}	
	
	public SqlPacket getPacketByRecordId(String recordId)
	{
		SqlPacket outPacket = null;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, RecordId, DrupalID, Packet from PERSISTENTCACHE where RecordId = '" + recordId + "'" ;
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) 
		{
			do 
			{
				outPacket = new SqlPacket();
				outPacket.setPacketId(cursor.getString(0));
				outPacket.setRecordId(cursor.getString(1));
				outPacket.setDrupalId(cursor.getString(2));
				outPacket.setPacket(cursor.getString(3));
				
			}
			while (cursor.moveToNext());

			if (cursor != null && !cursor.isClosed()) 
			{
				cursor.close();
			}

			db.close();
			return outPacket;
		}
		else
		{
			cursor.close();
			db.close();
			return null;
		}
	}	
	
	private static class OpenHelper extends SQLiteOpenHelper 
	{
		Context dbContext;

		OpenHelper(Context context) 
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			dbContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{	//TODO: DATABSE STRUCTURE MARKING

			//PERSISTENTCACHE
			String createPERSISTENTCACHE = "CREATE TABLE IF NOT EXISTS PERSISTENTCACHE (PacketID INTEGER PRIMARY KEY, RecordId TEXT, Packet TEXT, DrupalId TEXT);";
			db.execSQL(createPERSISTENTCACHE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			//needs to NOT drop users,usermeds,reminders, and pillstaken
			//else the user will lose data on upgrade.
			try
			{
				db.execSQL("drop table PERSISTENTCACHE");
			}
			catch(Exception ex)
			{}
			onCreate(db);
		}	
	
	}
}