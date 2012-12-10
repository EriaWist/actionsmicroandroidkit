package com.actionsmicro.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class CompositeAdapter<T extends ListAdapter> extends BaseAdapter {

	private DataSetObserver dataSetObserver = new DataSetObserver() {
		public void onChanged () {
			notifyDataSetChanged();
		}
		public void onInvalidated () {
			// TODO Auto-generated method stub			
		}
	};
	private ArrayList<T> subadapters = new ArrayList<T>();
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
	
	public void addAdapter(T subadapter) {
		subadapter.registerDataSetObserver(dataSetObserver);
		subadapters.add(subadapter);
		notifyDataSetChanged();
	}
	public void removeAdapter(T subadapter) {
		if (subadapters.contains(subadapter)) {
			subadapter.unregisterDataSetObserver(dataSetObserver);
			subadapters.remove(subadapter);
			notifyDataSetChanged();
		}		
	}
	/**
     * Sorts the content of this adapter using the specified comparator.
     *
     * @param comparator The comparator used to sort the objects contained
     *        in this adapter.
     */
    public void sortAdapters(Comparator<? super T> comparator) {
    	Collections.sort(subadapters, comparator);
        notifyDataSetChanged();
    }
}
