package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.R;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

public class TextSelectionActions implements ActionMode.Callback {

	private TextSelectionCallback callBack;
	private BookView bookView;

	public TextSelectionActions(TextSelectionCallback callBack,
			BookView bookView) {
		this.callBack = callBack;
		this.bookView = bookView;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// TODO Auto-generated method stub

		return true;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		menu.add(R.string.wikipedia_lookup)
			.setOnMenuItemClickListener(
				new MenuItem.OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						callBack.lookupWikipedia(bookView.getSelectedText());
						return true;
					}
				});

		if (callBack.isDictionaryAvailable()) {
			menu.add(R.string.dictionary_lookup)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(android.view.MenuItem item) {
					callBack.lookupDictionary(bookView.getSelectedText());
					return true;
				}
			});
		}

		menu.add(R.string.wikipedia_lookup)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(android.view.MenuItem item) {
				callBack.lookupWikipedia(bookView.getSelectedText());
				return true;
			}
		});

		menu.add(R.string.google_lookup)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(android.view.MenuItem item) {
				callBack.lookupGoogle(bookView.getSelectedText());
				return true;
			}
		});

		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

		return true;
	}

}
