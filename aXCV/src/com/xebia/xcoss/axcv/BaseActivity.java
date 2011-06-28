package com.xebia.xcoss.axcv;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.xebia.xcoss.axcv.logic.ConferenceServer;
import com.xebia.xcoss.axcv.model.Conference;
import com.xebia.xcoss.axcv.model.Session;
import com.xebia.xcoss.axcv.util.SecurityUtils;
import com.xebia.xcoss.axcv.util.XCS;
import com.xebia.xcoss.axcv.util.XCS.LOG;

import de.quist.app.errorreporter.ExceptionReportService;
import de.quist.app.errorreporter.ReportingActivity;

public abstract class BaseActivity extends Activity {

	public static final String IA_CONFERENCE = "ID-conference";
	public static final String IA_SESSION = "ID-session";
	public static final String IA_AUTHORS = "ID-authors";
	public static final String IA_AUTHOR = "ID-author";
	public static final String IA_LABELS = "ID-labels";

	private MenuItem miSettings;
	private MenuItem miSearch;
	private MenuItem miList;
	private MenuItem miAdd;
	private MenuItem miEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ImageView conferenceButton = (ImageView) findViewById(R.id.conferenceButton);
		if (conferenceButton != null) {
			conferenceButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showConferencesList();
				}
			});
			conferenceButton.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Intent intent = new Intent(BaseActivity.this, CVSplashLoader.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("exit", true);
					startActivity(intent);
					return true;
				}
			});
		}
		super.onCreate(savedInstanceState);
	}

	protected void close() {
		ConferenceServer.getInstance().close();
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		miAdd = menu.add(0, XCS.MENU.ADD, Menu.NONE, R.string.menu_add);
		miEdit = menu.add(0, XCS.MENU.EDIT, Menu.NONE, R.string.menu_edit);
		miList = menu.add(0, XCS.MENU.OVERVIEW, Menu.NONE, R.string.menu_overview);
		miSettings = menu.add(0, XCS.MENU.SETTINGS, Menu.NONE, R.string.menu_settings);
		miSearch = menu.add(0, XCS.MENU.SEARCH, Menu.NONE, R.string.menu_search);

		miAdd.setIcon(android.R.drawable.ic_menu_add);
		miEdit.setIcon(android.R.drawable.ic_menu_edit);
		miSettings.setIcon(android.R.drawable.ic_menu_preferences);
		miSearch.setIcon(android.R.drawable.ic_menu_search);
		miList.setIcon(R.drawable.ic_menu_list);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case XCS.MENU.SETTINGS:
				startActivity(new Intent(this, CVSettings.class));
			break;
			case XCS.MENU.OVERVIEW:
				showConferencesList();
			break;
			case XCS.MENU.SEARCH:
				startActivity(new Intent(this, CVSearch.class));
			break;
		}
		return true;
	}

	private void showConferencesList() {
		Intent intent = new Intent(this, CVConferences.class);
		intent.putExtra("redirect", false);
		// Clears out the activity call stack
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public Conference getConference() {
		return getConference(true);
	}

	protected Conference getConference(boolean useDefault) {
		Conference conference = null;
		ConferenceServer server = getConferenceServer();

		int identifier = -1;
		try {
			identifier = getIntent().getExtras().getInt(IA_CONFERENCE);
			conference = server.getConference(identifier);
		}
		catch (Exception e) {
			Log.w(LOG.ALL, "No conference with ID " + identifier);
		}
		if (conference == null && useDefault) {
			conference = server.getUpcomingConference();
			Log.w(LOG.ALL, "Conference default " + conference.getTitle());
		}
		Log.e("XCS", "[GET] Conference (on '" + identifier + "') = " + conference);
		return conference;
	}

	protected Session getSession(Conference conference) {
		return getSession(conference, true);
	}

	protected Session getSession(Conference conference, boolean useDefault) {
		Session session = null;
		int identifier = -1;
		try {
			identifier = getIntent().getExtras().getInt(IA_SESSION);
			session = conference.getSessionById(identifier);
		}
		catch (Exception e) {
			Log.w(LOG.ALL, "No session with ID " + identifier + " or conference not found.");
		}
		if (session == null && useDefault) {
			Log.w(LOG.ALL, "Conference default : " + (conference == null ? "NULL" : conference.getTitle()));
			session = conference.getUpcomingSession();
			Log.w(LOG.ALL, "Session default " + (session == null ? "NULL" : session.getTitle()));
		}
		return session;
	}

	protected ConferenceServer getConferenceServer() {
		ConferenceServer server = ConferenceServer.getInstance();
		if (server == null || server.isLoggedIn() == false) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			String user = sp.getString(XCS.PREF.USERNAME, null);
			String password = SecurityUtils.decrypt(sp.getString(XCS.PREF.PASSWORD, ""));
			server = ConferenceServer.createInstance(user, password, getServerUrl());
		}
		return server;
	}

	protected Dialog createDialog(String title, String message) {
		return createDialog(this, title, message);
	}

	public static Dialog createDialog(Activity ctx, String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		return builder.create();
	}

	protected void onSuccess() {
	}

	protected void onFailure() {
	}

	class DataRetriever extends AsyncTask<String, Void, Boolean> {

		// http://appfulcrum.com/?p=126

		private ProgressDialog dialog;
		private BaseActivity ctx;
		private Exception resultingException;

		public DataRetriever(BaseActivity ctx) {
			this.ctx = ctx;
		}

		@Override
		protected void onPreExecute() {
			this.dialog = ProgressDialog.show(ctx, null, "Loading. Please wait...", true);
		}

		@Override
		protected Boolean doInBackground(String... arg0) {
			try {
				List<Conference> conferences = getConferenceServer().getUpcomingConferences(4);
				for (Conference conference : conferences) {
					conference.getSessions();
				}
			}
			catch (Exception e) {
				resultingException = e;
				Log.e(XCS.LOG.COMMUNICATE, "[Initial load] Failure: " + e.getMessage());
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			dialog.cancel();

			if (result) {
				// Note, authentication may still be invalid.
				ctx.onSuccess();
			} else {
				Dialog errorDialog = createDialog("Error", "Connection to server failed ("+resultingException.getMessage()+").");
				errorDialog.setOnDismissListener(new Dialog.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface di) {
						ctx.onFailure();
						di.dismiss();
					}
				});
				errorDialog.show();
			}
		}
	}

	private String getServerUrl() {
		try {
			// Invoke trim to make sure the value is specified
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			return ai.metaData.getString("com.xebia.xcoss.serverUrl").trim();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("serverUrl is undefined");
		}
	}
}