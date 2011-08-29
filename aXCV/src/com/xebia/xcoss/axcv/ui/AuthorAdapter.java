package com.xebia.xcoss.axcv.ui;

import java.util.List;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xebia.xcoss.axcv.BaseActivity;
import com.xebia.xcoss.axcv.R;
import com.xebia.xcoss.axcv.model.Author;

public class AuthorAdapter extends BaseAdapter {
	private List<Author> data;
	private BaseActivity ctx;
	private int viewResource;
	
	public AuthorAdapter(BaseActivity context, int viewResourceId, List<Author> data) {
		this.ctx = context;
		this.viewResource = viewResourceId;
		this.data = data;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Author author = (Author) getItem(position);
		LayoutInflater inflater = ctx.getLayoutInflater();
		View row = inflater.inflate(viewResource, parent, false);
		TextView titleView = (TextView) row.findViewById(R.id.author_name);
		titleView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				createContextMenu(menu, v, menuInfo, position);
			}
		});
		titleView.setText(author.getName());
		return row;
	}
	
	
	protected void createContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, int position) {
		menu.add(position, R.id.view, Menu.NONE, R.string.context_menu_author_view);
		menu.add(position, R.id.remove, Menu.NONE, R.string.context_menu_author_remove);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
