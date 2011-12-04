/*
 * Copyright (C) 2011 Alex Kuiper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightwhistler.pageturner;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ChooseFileActivity extends Activity {
	
	 protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;
     protected static final int REQUEST_CODE_GET_CONTENT = 2;
     
     private EditText mEditText;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choosefile);
        
        mEditText = (EditText) findViewById(R.id.editText1);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        mEditText.setText( settings.getString("last_file", "" ));    
           
    }
    
    public void browseClicked( View view ) {
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
                startActivityForResult(intent, REQUEST_CODE_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
                // No compatible file manager was found.
                Toast.makeText(this, "Please install OI File Manager from the Android Market.", 
                                Toast.LENGTH_SHORT).show();
        }

    }
    
    public void openFileClicked( View view ) {    	
    	Intent i = new Intent(this, ReadingActivity.class);
    	i.putExtra("file_name", this.mEditText.getText().toString() );
    	startActivity(i);
    }   
    
    public void prefsClicked( View view ) {    	
    	Intent i = new Intent(this, PageTurnerPrefsActivity.class);    	
    	startActivity(i);
    } 
    
    private SharedPreferences.Editor getPrefs() {
    	SharedPreferences settings = getSharedPreferences(this.getString(R.string.pref_key), 0);
		SharedPreferences.Editor editor = settings.edit();
		
		return editor;
    }
    
    private void storeSetting( String key, String value ) {
    	
    	SharedPreferences.Editor editor = getPrefs();
    	editor.putString( key, value );    		    	

    	// Commit the edits!
    	editor.commit();
    }    
    
    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (resultCode == RESULT_OK && data != null) {
    		// obtain the filename
    		Uri fileUri = data.getData();
    		if (fileUri != null) {
    			String filePath = fileUri.getPath();
    			if (filePath != null) {
    				mEditText.setText(filePath);
    				storeSetting("last_file", filePath);
    			}
    		}
    	}    	
    }
	
}
