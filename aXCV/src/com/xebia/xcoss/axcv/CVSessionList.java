package com.xebia.xcoss.axcv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.xebia.xcoss.axcv.model.BreakSession;
import com.xebia.xcoss.axcv.model.Conference;
import com.xebia.xcoss.axcv.model.Session;
import com.xebia.xcoss.axcv.ui.ScreenTimeUtil;
import com.xebia.xcoss.axcv.ui.SessionAdapter;
import com.xebia.xcoss.axcv.util.XCS;
import com.xebia.xcoss.axcv.util.XCS.LOG;

public class CVSessionList extends BaseActivity {

	private Conference currentConference;
	private Session[] sessions;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.schedule);

		currentConference = getConference();
		sessions = currentConference.getSessions().toArray(new Session[0]);

		TextView title = (TextView) findViewById(R.id.conferenceTitle);
		title.setText(currentConference.getTitle());

		TextView date = (TextView) findViewById(R.id.conferenceDate);
		String val = new ScreenTimeUtil(this).getAbsoluteDate(currentConference.getDate());
		date.setText(val);

		Log.w(LOG.ALL, "Conference has " + currentConference.getSessions().size() + " sessions.");
		SessionAdapter adapter = new SessionAdapter(this, R.layout.session_item, R.layout.mandatory_item, sessions);
		ListView sessionList = (ListView) findViewById(R.id.sessionList);
		sessionList.setAdapter(adapter);
		sessionList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int paramInt, long paramLong) {
				// Adapter = listview, view = tablelayout.
				switchTo(currentConference, paramInt);
			}
		});
		super.onCreate(savedInstanceState);
	}

	
	private void switchTo(Conference conference, int sessionIndex) {
		Session session = sessions[sessionIndex];
		if ( session instanceof BreakSession ) {
			// No navigation to this session
			return;
		}
		Intent intent = new Intent(this, CVSessionView.class);
		intent.putExtra(BaseActivity.IA_CONFERENCE, conference.getId());
		intent.putExtra(BaseActivity.IA_SESSION, session.getId());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Add a session
		if (item.getItemId() == XCS.MENU.ADD) {
			Intent intent = new Intent(this, CVSessionAdd.class);
			intent.putExtra(BaseActivity.IA_CONFERENCE, currentConference.getId());
			startActivity(intent);
			return true;
		}
		// Edit the conference
		if (item.getItemId() == XCS.MENU.EDIT) {
			Intent intent = new Intent(this, CVConferenceAdd.class);
			intent.putExtra(BaseActivity.IA_CONFERENCE, currentConference.getId());
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}