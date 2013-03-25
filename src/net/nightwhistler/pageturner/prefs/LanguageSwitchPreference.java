package net.nightwhistler.pageturner.prefs;

import net.nightwhistler.pageturner.R;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class LanguageSwitchPreference extends ListPreference {
	
	private Context context;	

	private String oldValue;

	public LanguageSwitchPreference(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.context = context;				
	}
	
	@Override
	public void setValue(String value) {
		super.setValue(value);
		
		if (oldValue != null && !value.equalsIgnoreCase(oldValue) ) {
			Toast.makeText(context, R.string.language_switch_message, Toast.LENGTH_LONG).show();			
		}
		
		this.oldValue = value;
	}
	
}
