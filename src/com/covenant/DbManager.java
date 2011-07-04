package com.covenant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbManager extends SQLiteOpenHelper {

	private String dbPath;
	private String dbName;
	private static final Integer DB_VERSION = 1;
	private final Context context;
	private SQLiteDatabase db;

	public DbManager(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
		this.context = context;
		this.dbName = dbName;
		this.dbPath = "/data/data/" + context.getPackageName() + "/databases/";
		if (!databaseExistsOnDevice()) {
			copyDatabase();
		}
		db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
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
