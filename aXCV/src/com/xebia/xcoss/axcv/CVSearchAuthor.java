package com.xebia.xcoss.axcv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.xebia.xcoss.axcv.model.Author;
import com.xebia.xcoss.axcv.tasks.RetrieveAuthorsTask;
import com.xebia.xcoss.axcv.tasks.TaskCallBack;
import com.xebia.xcoss.axcv.ui.AuthorAdapter;
import com.xebia.xcoss.axcv.util.StringUtil;
import com.xebia.xcoss.axcv.util.XCS;

public class CVSearchAuthor extends BaseActivity {

	private List<Author> selectedAuthors;
	private AuthorAdapter authorAdapter;
	private AutoCompleteTextView textView;
	private HashMap<String, Author> allAuthors;
	private boolean singleMode;
	private String startText;
	private Intent result = new Intent();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.search_items);
		textView = (AutoCompleteTextView) findViewById(R.id.ssa_text);
		startText = getResources().getString(R.string.default_input_text);
		textView.setSelection(0, startText.length());
		initSelectedAuthors();

		Button closeButton = (Button) findViewById(R.id.ssa_close);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateTypedText();
				finish();
			}
		});
		super.onCreate(savedInstanceState);
	}
	
	private void updateAuthors(List<Author> allPersons) {
		// Fill the list of options
		allAuthors = new HashMap<String, Author>();
		String[] data = new String[allPersons.size()];
		int i=0;
		for (Author author : allPersons) {
			data[i] = author.getName();
			allAuthors.put(data[i], author);
			i++;
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, data);
		textView.setAdapter(adapter);
		textView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
				updateTypedText();
				return true;
			}
		});
	}

	private boolean addAuthor(String authorName) {
		if (StringUtil.isEmpty(authorName)) {
			return true;
		}
		if (allAuthors.containsKey(authorName)) {
			final Author author = allAuthors.get(authorName);
			boolean contains = false;
			for (Author a : selectedAuthors) {
				if (author.compareTo(a) == 0) {
					contains = true;
				}
			}
			if (!contains) {
				if (singleMode && selectedAuthors.size() > 0) {
					challengeSelection(author);
				} else {
					selectedAuthors.add(author);
					Collections.sort(selectedAuthors);
					authorAdapter.notifyDataSetChanged();
				}
			}
			return true;
		}
		return false;
	}

	private void challengeSelection(final Author author) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.single_select);
		builder.setMessage(R.string.single_select_message);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				selectedAuthors.clear();
				selectedAuthors.add(author);
				authorAdapter.notifyDataSetChanged();
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.keep, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	private void initSelectedAuthors() {
		if (selectedAuthors == null) {
			selectedAuthors = new ArrayList<Author>();
			try {
				Bundle extras = getIntent().getExtras();
				selectedAuthors.clear();
				selectedAuthors.addAll((List<Author>) extras.getSerializable(IA_AUTHORS));
				singleMode = extras.getBoolean("singleSelectionMode", false);
			}
			catch (Exception e) {
				Log.w(XCS.LOG.COMMUNICATE, "No authors loaded: " + e.getMessage());
			}
		}
	}

	@Override
	protected void onResume() {
		new RetrieveAuthorsTask(R.string.action_retrieve_authors, this, new TaskCallBack<List<Author>>() {
			@Override
			public void onCalled(List<Author> result) {
				updateAuthors(result);
			}
		}).execute();
		
		initSelectedAuthors();
		authorAdapter = new AuthorAdapter(this, R.layout.author_item_small, selectedAuthors);
		ListView authorList = (ListView) findViewById(R.id.ssa_list);
		authorList.setAdapter(authorAdapter);

		super.onResume();
	}

	@Override
	public void onBackPressed() {
		updateTypedText();
		super.onBackPressed();
	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		int position = menuItem.getGroupId();

		switch (menuItem.getItemId()) {
			case R.id.view:
				Author author = selectedAuthors.get(position);
				Intent intent = new Intent(this, CVAuthor.class);
				intent.putExtra(BaseActivity.IA_AUTHOR, author.getUserId());
				startActivity(intent);
				return true;
			case R.id.remove:
				selectedAuthors.remove(position);
				updateResult();
				authorAdapter.notifyDataSetChanged();
				Toast.makeText(getApplicationContext(), R.string.author_removed, Toast.LENGTH_SHORT).show();
				return true;
			default:
				return super.onContextItemSelected(menuItem);
		}
	}

	private void updateTypedText() {
		String authorName = textView.getText().toString();
		if (startText.equals(authorName)) {
			textView.getText().clear();
			authorName = null;
		}

		if (!StringUtil.isEmpty(authorName)) {

			if (textView.isPopupShowing() && textView.getListSelection() != ListView.INVALID_POSITION) {
				authorName = (String) textView.getAdapter().getItem(textView.getListSelection());
			}

			if (!addAuthor(authorName)) {
				Toast.makeText(getApplicationContext(), R.string.select_valid_author, Toast.LENGTH_SHORT).show();
			}
			textView.getText().clear();
			updateResult();
		}
	}

	private void updateResult() {
		result.putExtra(IA_AUTHORS, (Serializable) selectedAuthors);
		setResult(RESULT_OK, result);
	}
}
