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
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DbManager extends SQLiteOpenHelper {

	public static final int DB = 0;
	public static final int SCHEMA = 1;
	public static final int AUTO = 2;

	private String dbPath;
	private String dbName;
	private static final Integer DB_VERSION = 1;
	private final Context context;
	private SQLiteDatabase db;
	private int mode;

	
	public static DbManager createAuto(Context context) {
		return new DbManager(context);
	}
	
	public static DbManager createFromDb(Context context, String resource) {
		return new DbManager(context, DB, resource);
	}
	
	public static DbManager createFromSchema(Context context, String resource) {
		return new DbManager(context, SCHEMA, resource);
	}
	
	public DbManager(Context context) {
		super(context, null, null, DB_VERSION);
		this.context = context;
		this.mode = AUTO;
		db = context.openOrCreateDatabase(
				context.getPackageName() + "_auto_db" + ".sqlite", 
				SQLiteDatabase.CREATE_IF_NECESSARY, 
				null
			);
		//db.execSQL("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT DEFAULT 'en_US');");
	}

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
		SQLiteDatabase checkDB = null;

		try {
			String myPath = dbPath + dbName;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
		}

		if (checkDB != null) {
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	private void copyDatabase() {
		File f = new File(dbPath);
		if (!f.exists()) {
			f.mkdir();
		}

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

	public int getMode() {
		return this.mode;
	}

	public SQLiteDatabase getDb() {
		return this.db;
	}

	@Override
	public synchronized void close() {
		if (db != null) {
			db.close();
		}

		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}

}
