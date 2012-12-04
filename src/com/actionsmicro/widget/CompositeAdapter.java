package com.actionsmicro.widget;

import java.util.ArrayList;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class CompositeAdapter extends BaseAdapter {

	private DataSetObserver dataSetObserver = new DataSetObserver() {
		public void onChanged () {
			notifyDataSetChanged();
		}
		public void onInvalidated () {
			// TODO Auto-generated method stub			
		}
	};
	private ArrayList<ListAdapter> subadapters = new ArrayList<ListAdapter>();
	@Override
	public int getCount() {
		int count = 0;
		for (ListAdapter adapter : subadapters) {
			count += adapter.getCount();
		}
		return count;
	}

	@Override
	public Object getItem(int position) {
		
		for (ListAdapter adapter : subadapters) {
			int size = adapter.getCount();

			if (position < size) 
				return adapter.getItem(position);

			// otherwise jump into next adapter
			position -= size;
		}
		
		return null;
	}
	@Override
	public int getItemViewType(int position) {
		for (ListAdapter adapter : subadapters) {
			int size = adapter.getCount();

			if (position < size) 
				return adapter.getItemViewType(position);

			// otherwise jump into next adapter
			position -= size;
		}
		
		return IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public long getItemId(int position) {
		for (ListAdapter adapter : subadapters) {
			int size = adapter.getCount();

			if (position < size) 
				return adapter.getItemId(position);

			// otherwise jump into next adapter
			position -= size;
		}
		
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		for (ListAdapter adapter : subadapters) {
			int size = adapter.getCount();

			if (position < size) 
				return adapter.getView(position, convertView, parent);

			// otherwise jump into next adapter
			position -= size;
		}
		
		return null;
	}
	@Override
	public boolean isEnabled(int position) {
		for (ListAdapter adapter : subadapters) {
			int size = adapter.getCount();

			if (position < size) 
				return adapter.isEnabled(position);

			// otherwise jump into next adapter
			position -= size;
		}
		return false;
	}
	@Override
	public boolean areAllItemsEnabled () {
		return false;
	}
	
	public void addAdapter(ListAdapter subadapter) {
		subadapter.registerDataSetObserver(dataSetObserver);
		subadapters.add(subadapter);
		notifyDataSetChanged();
	}
	public void removeAdapter(ListAdapter subadapter) {
		if (subadapters.contains(subadapter)) {
			subadapter.unregisterDataSetObserver(dataSetObserver);
			subadapters.remove(subadapter);
			notifyDataSetChanged();
		}		
	}
}
