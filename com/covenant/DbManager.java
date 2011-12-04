package com.covenant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbManager extends SQLiteOpenHelper {

	
	public static final int DB = 0;
	public static final int SCHEMA = 1;
	
	private String dbPath;
	private String dbName;
	private static final Integer DB_VERSION = 1;
	private final Context context;
	private SQLiteDatabase db;

	public DbManager(Context context, int mode, String resource) {
		super(context, resource, null, DB_VERSION);
		this.context = context;
		this.dbName = resource;
		
		switch (mode) {
		case DB:
			this.dbPath = "/data/data/" + context.getPackageName() + "/databases/";
			if (!databaseExistsOnDevice()) {
				copyDatabase();
			}
			db = context.openOrCreateDatabase(resource, Context.MODE_PRIVATE, null);
			break;
		case SCHEMA:
			db = context.openOrCreateDatabase("data.sqlite", SQLiteDatabase.CREATE_IF_NECESSARY, null);
			
			try {
				BufferedReader r;
				r = new BufferedReader(new InputStreamReader(context.getAssets().open(resource)));
				StringBuilder schemaAll = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					schemaAll.append(line);
				}
				
				String[] statements = schemaAll.toString().split(";");
				for (String statement : statements) {
					db.execSQL(statement);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			break;
		}
	}

	private boolean databaseExistsOnDevice() {
		File dbFile = new File(dbPath + dbName);
		return dbFile.exists();	
	}

	private void copyDatabase() {
		try {
			InputStream assetsDB = context.getAssets().open(dbName);
			OutputStream dbOut = new FileOutputStream(dbPath + dbName);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = assetsDB.read(buffer)) > 0) {
				dbOut.write(buffer, 0, length);
			}

			dbOut.flush();
			dbOut.close();
			assetsDB.close();
		} catch (IOException e) {
			System.out.println(e.getStackTrace());
		}
	}

	public SQLiteDatabase getDb() {
		return this.db;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}

}
