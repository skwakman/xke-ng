package com.xebia.xcoss.axcv.logic;

import hirondelle.date4j.DateTime;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.xebia.xcoss.axcv.model.Session;
import com.xebia.xcoss.axcv.util.StringUtil;
import com.xebia.xcoss.axcv.util.XCS;

public class ProfileManager extends SQLiteOpenHelper {

	private static final int DAYS_TO_KEEP_IN_CACHE = 1;

	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_NAME = "xkeng.db";
	private static final String TRACK_TABLE = "Track";
	private static final String OWNED_TABLE = "Owned";
	private static final String CACHE_TABLE = "Cached";
	private static final String SES_COL_ID = "id";
	private static final String SES_COL_USER = "user";
	private static final String SES_COL_SESSION = "sid";
	private static final String SES_COL_HASH = "hash";
	private static final String SES_COL_DATE = "timestamp";
	private static final String CACHE_COL_KEY = "key";
	private static final String CACHE_COL_OBJ = "value";
	private static final String CACHE_COL_DATE = "timestamp";

	private static final String SES_QUERY_TRACKABLE = SES_COL_USER + " = ? AND " + SES_COL_SESSION + " = ?";
	private static final String SES_QUERY_NAME = SES_COL_USER + " = ?";
	private static final String SES_QUERY_PRUNE = SES_COL_DATE + " < ?";
	private static final String CACHE_QUERY = CACHE_COL_KEY + " = ?";
	private static final String CACHE_QUERY_TYPE = CACHE_COL_KEY + " like ? || '%'";
	private static final String CACHE_QUERY_PRUNE = CACHE_COL_DATE + " < ?";

	public class Trackable {
		public long hash;
		public Integer sessionId;
		public String userId;
		public DateTime date;
	};

	private SQLiteDatabase database = null;

	public ProfileManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void openConnection() {
		try {
			if (database == null || !database.isOpen()) {
				database = getWritableDatabase();
			}
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Opening database failed: " + StringUtil.getExceptionMessage(e));
			database = null;
		}
	}

	public void closeConnection() {
		try {
			if (database != null) database.close();
			database = null;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Closing database failed: " + StringUtil.getExceptionMessage(e));
		}
		close();
	}

	private void checkConnection() {
		if (database == null) {
			throw new SQLException("Database not started!");
		}
		if (!database.isOpen() || database.isReadOnly()) {
			throw new SQLException("Database not open or readonly!");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuilder create = new StringBuilder();
		create.append("create table ");
		create.append(TRACK_TABLE);
		create.append(" (");
		create.append(SES_COL_ID);
		create.append(" integer primary key autoincrement, ");
		create.append(SES_COL_USER);
		create.append(" text not null, ");
		create.append(SES_COL_SESSION);
		create.append(" int, ");
		create.append(SES_COL_HASH);
		create.append(" text not null, ");
		create.append(SES_COL_DATE);
		create.append(" datetime);");
		db.execSQL(create.toString());

		create = new StringBuilder();
		create.append("create table ");
		create.append(OWNED_TABLE);
		create.append(" (");
		create.append(SES_COL_ID);
		create.append(" integer primary key autoincrement, ");
		create.append(SES_COL_USER);
		create.append(" text not null, ");
		create.append(SES_COL_SESSION);
		create.append(" text not null, ");
		create.append(SES_COL_HASH);
		create.append(" text not null, ");
		create.append(SES_COL_DATE);
		create.append(" datetime);");
		db.execSQL(create.toString());

		create = new StringBuilder();
		create.append("create table ");
		create.append(CACHE_TABLE);
		create.append(" (");
		create.append(CACHE_COL_KEY);
		create.append(" text primary key not null, ");
		create.append(CACHE_COL_OBJ);
		create.append(" text not null,");
		create.append(CACHE_COL_DATE);
		create.append(" datetime);");
		db.execSQL(create.toString());
	}

	public boolean markSession(String user, Session session) {
		if ( StringUtil.isEmpty(user)) {
			return false;
		}
		Log.v(XCS.LOG.COMMUNICATE, "Marking session " + session.getId() + " for user " + user);
		try {
			checkConnection();
			ContentValues row = new ContentValues();
			row.put(SES_COL_USER, user);
			row.put(SES_COL_SESSION, session.getId());
			row.put(SES_COL_HASH, session.getModificationHash());
			row.put(SES_COL_DATE, session.getStartTime().getMilliseconds(XCS.TZ));
			long rv = database.insert(TRACK_TABLE, null, row);
			return rv >= 0;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Marking session failed: " + StringUtil.getExceptionMessage(e));
			return false;
		}
	}

	public boolean unmarkSession(String user, Session session) {
		if ( StringUtil.isEmpty(user)) {
			return false;
		}
		Log.v(XCS.LOG.COMMUNICATE, "Unmarking session " + session.getId() + " for user " + user);
		try {
			checkConnection();
			String[] whereArgs = new String[2];
			whereArgs[0] = user;
			whereArgs[1] = String.valueOf(session.getId());
			int rv = database.delete(TRACK_TABLE, SES_QUERY_TRACKABLE, whereArgs);
			return rv > 0;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Unmarking session failed: " + StringUtil.getExceptionMessage(e));
			return false;
		}
	}

	public void pruneMarked(DateTime today) {
		Log.v(XCS.LOG.COMMUNICATE, "Pruning database");
		try {
			checkConnection();
			String[] whereArgs = new String[1];
			whereArgs[0] = String.valueOf(today.getMilliseconds(XCS.TZ));
			database.delete(TRACK_TABLE, SES_QUERY_PRUNE, whereArgs);
			database.delete(OWNED_TABLE, SES_QUERY_PRUNE, whereArgs);
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Pruning database failed: " + StringUtil.getExceptionMessage(e));
		}
	}

	public boolean isMarked(String user, Integer sessionId) {
		if ( StringUtil.isEmpty(user)) {
			return false;
		}
		Log.v(XCS.LOG.COMMUNICATE, "Check if marked " + sessionId + " for user " + user);
		try {
			checkConnection();
			String[] whereArgs = new String[2];
			whereArgs[0] = user;
			whereArgs[1] = String.valueOf(sessionId);
			Cursor query = database.query(TRACK_TABLE, new String[] { SES_COL_ID }, SES_QUERY_TRACKABLE, whereArgs,
					null, null, null);
			boolean hasMark = query.getCount() > 0;
			query.close();
			return hasMark;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Unmarking session failed: " + StringUtil.getExceptionMessage(e));
			e.printStackTrace();
			return false;
		}
	}

	public Integer[] getMarkedSessionIds(String user) {
		Log.v(XCS.LOG.COMMUNICATE, "Get all marked sessions for user " + user);
		try {
			checkConnection();
			String[] whereArgs = new String[1];
			whereArgs[0] = user;
			Cursor query = database.query(TRACK_TABLE, new String[] { SES_COL_SESSION }, SES_QUERY_NAME,
					new String[] { user }, null, null, SES_COL_DATE + " ASC");
			Integer[] result = new Integer[query.getCount()];
			int i = 0;
			for (query.moveToFirst(); !query.isAfterLast(); query.moveToNext()) {
				result[i++] = query.getInt(query.getColumnIndex(SES_COL_SESSION));
			}
			query.close();
			return result;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Retrieval failed: " + StringUtil.getExceptionMessage(e));
			return new Integer[0];
		}
	}

	public Trackable[] getMarkedSessions(String user) {
		return getSessions(user, TRACK_TABLE);
	}

	public Trackable[] getOwnedSessions(String user) {
		return getSessions(user, OWNED_TABLE);
	}

	public void updateMarkedSession(Trackable trackable) {
		updateSession(trackable, TRACK_TABLE);
	}

	public void updateOwnedSession(Trackable trackable) {
		updateSession(trackable, OWNED_TABLE);
	}

	private Trackable[] getSessions(String user, String table) {
		Log.v(XCS.LOG.COMMUNICATE, "Get all sessions for user " + user + " from " + table);
		try {
			checkConnection();
			String[] whereArgs = new String[] { user };
			Cursor query = database.query(table, new String[] { SES_COL_SESSION, SES_COL_HASH, SES_COL_DATE },
					SES_QUERY_NAME, whereArgs, null, null, SES_COL_HASH + " ASC");
			Trackable[] result = new Trackable[query.getCount()];
			int i = 0;
			for (query.moveToFirst(); !query.isAfterLast(); query.moveToNext()) {
				Trackable trackable = new Trackable();
				trackable.sessionId = query.getInt(query.getColumnIndex(SES_COL_SESSION));
				trackable.hash = query.getLong(query.getColumnIndex(SES_COL_HASH));
				trackable.userId = user;
				trackable.date = DateTime.forInstant(query.getLong(query.getColumnIndex(SES_COL_DATE)), XCS.TZ);
				result[i++] = trackable;
			}
			query.close();
			return result;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Retrieval failed: " + StringUtil.getExceptionMessage(e));
			return new Trackable[0];
		}
	}

	private void updateSession(Trackable trackable, String table) {
		int update = -1;
		ContentValues values = new ContentValues();

		try {
			checkConnection();
			String[] whereArgs = new String[2];
			values.put(SES_COL_HASH, trackable.hash);
			values.put(SES_COL_DATE, trackable.date.getMilliseconds(XCS.TZ));
			whereArgs[0] = trackable.userId;
			whereArgs[1] = String.valueOf(trackable.sessionId);
			update = database.update(table, values, SES_QUERY_TRACKABLE, whereArgs);
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Update failed: " + StringUtil.getExceptionMessage(e));
		}
		if (update == 0) {
			try {
				values.put(SES_COL_USER, trackable.userId);
				values.put(SES_COL_SESSION, trackable.sessionId);
				values.put(SES_COL_DATE, trackable.hash);
				values.put(SES_COL_HASH, trackable.date.getMilliseconds(XCS.TZ));
				database.insert(table, null, values);
			}
			catch (Exception e) {
				Log.w(XCS.LOG.COMMUNICATE, "Insert failed: " + StringUtil.getExceptionMessage(e));
			}
		}
	}

	public String[] getCachedObjects(Class<?> type) {
		if (type != null) {
			return doGetCachedObjects(CACHE_QUERY_TYPE, new String[] { type.getSimpleName() });
		}
		return doGetCachedObjects(null, null);
	}
	
	public String[] getCachedObjects(String key) {
		if (!StringUtil.isEmpty(key)) {
			return doGetCachedObjects(CACHE_QUERY, new String[] { key });
		}
		return doGetCachedObjects(null, null);
	}
	
	private String[] doGetCachedObjects(String whereCause, String[] whereArgs) {
		try {
			checkConnection();
			Cursor query = database.query(CACHE_TABLE, new String[] { CACHE_COL_OBJ }, whereCause, whereArgs, null,
					null, null);
			String[] result = new String[query.getCount()];
			int i = 0;
			for (query.moveToFirst(); !query.isAfterLast(); query.moveToNext()) {
				result[i++] = query.getString(query.getColumnIndex(CACHE_COL_OBJ));
			}
			query.close();
			return result;
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Retrieval failed: " + StringUtil.getExceptionMessage(e));
			return new String[0];
		}
	}

	public void updateCachedObject(String key, String value) {
		int update = -1;
		ContentValues values = new ContentValues();
		long now = System.currentTimeMillis();
		try {
			checkConnection();
			String[] whereArgs = new String[] { key };
			values.put(CACHE_COL_KEY, key);
			values.put(CACHE_COL_OBJ, value);
			values.put(CACHE_COL_DATE, now);
			update = database.update(CACHE_TABLE, values, CACHE_QUERY, whereArgs);
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Update failed: " + StringUtil.getExceptionMessage(e));
		}
		if (update == 0) {
			try {
				values.put(CACHE_COL_KEY, key);
				values.put(CACHE_COL_OBJ, value);
				values.put(CACHE_COL_DATE, now);
				database.insert(CACHE_TABLE, null, values);
			}
			catch (Exception e) {
				Log.w(XCS.LOG.COMMUNICATE, "Insert failed: " + StringUtil.getExceptionMessage(e));
			}
		}
	}

	public void deleteCachedObject(String key) {
		try {
			checkConnection();
			String[] whereArgs = new String[] { key };
			database.delete(CACHE_TABLE, CACHE_QUERY, whereArgs);
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Delete failed: " + StringUtil.getExceptionMessage(e));
		}
	}
	
	public void purgeCache() {
		try {
			checkConnection();
			String[] whereArgs = new String[1];
			DateTime moment = DateTime.now(XCS.TZ).minusDays(DAYS_TO_KEEP_IN_CACHE);
			whereArgs[0] = String.valueOf(moment.getMilliseconds(XCS.TZ));
			database.delete(CACHE_TABLE, CACHE_QUERY_PRUNE, whereArgs);
		}
		catch (Exception e) {
			Log.w(XCS.LOG.COMMUNICATE, "Delete failed: " + StringUtil.getExceptionMessage(e));
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2) {
		// Not supported for the first release.
	}
}
