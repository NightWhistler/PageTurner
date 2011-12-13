package net.nightwhistler.pageturner.library;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class QueryResultAdapter<T> extends BaseAdapter {

	QueryResult<T> result;
	
	public void setResult(QueryResult<T> result) {
		this.result = result;
		notifyDataSetChanged();
	}
	
	public void clear() {
		result = null;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if ( this.result == null ) {
			return 0;
		}
		
		return result.getSize();
	}
	
	@Override
	public Object getItem(int position) {
		
		if ( result == null ) {
			return null;
		}
		
		return result.getItemAt(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, result.getItemAt(position), convertView, parent);
	}
	
	public T getResultAt(int position) {
		
		if ( result == null ) {
			return null;
		}
		
		return result.getItemAt(position);
	}
	
	public abstract View getView( int index, T object, View convertView, ViewGroup parent );
	
}
