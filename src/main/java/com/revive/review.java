package com.revive;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;
import com.markupartist.android.widget.ActionBar;
import net.nightwhistler.pageturner.activity.LibraryActivity;

public class review extends Activity{
    private static final int SELECT_AUDIO = 2;
    String selectedPath = "";
    private ProgressDialog dialog;
    HttpURLConnection connection = null;
    DataOutputStream outputStream = null;
    DataInputStream inputStream = null;
    private Button upload,btnselect;
    Bitmap bitmap;

    // String pathToOurFile = "/sdcard/character.mp3";
    String urlServer = " http://192.168.43.53/audio/Upload.php";
    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary =  "*****";

    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1*1024*1024;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.review);
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Your Voice");
        actionBar.addAction(new ActionBar.IntentAction(this, bookshop(),
                R.drawable.book_refresh));
        actionBar.setHomeAction(new ActionBar.IntentAction(this, home(),
                com.revive.R.drawable.book_refresh));

        btnselect = (Button) findViewById(R.id.button2);
        btnselect.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                openGalleryAudio();
            }
        });

        upload = (Button) findViewById(R.id.button1);
        upload.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (selectedPath == "") {
                    Toast.makeText(getApplicationContext(), " Please Select a Recording or Record", 32).show();
                } else {
                    //Toast.makeText(getApplicationContext(), "Yes", 32).show();
                    upload();
                }
                /*dialog = ProgressDialog.show(Postaudio.this, "Uploading",
                            "Please wait...", true);
                        new ImageUploadTask().execute();*/
            }

        });
    }
    public void openGalleryAudio(){

        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        //intent.setType("audio/*");
        //intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult( intent, SELECT_AUDIO);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_AUDIO)
            {
                System.out.println("SELECT_AUDIO");
                Uri selectedImageUri = data.getData();
                selectedPath = getPath(selectedImageUri);
                System.out.println("SELECT_AUDIO Path : " + selectedPath);
                upload.setEnabled(true);
                //	Toast.makeText(getApplicationContext(), ""+selectedImageUri, 32).show();
                //doFileUpload();
            }

        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    class ImageUploadTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... unsued) {
            try {


                FileInputStream fileInputStream = new FileInputStream(new File(selectedPath) );
                Log.v("Path", selectedPath);
                URL url = new URL(urlServer);
                connection = (HttpURLConnection) url.openConnection();

                // Allow Inputs & Outputs
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                // Enable POST method
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

                outputStream = new DataOutputStream( connection.getOutputStream() );
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + selectedPath +"\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                //  Toast.makeText(getApplicationContext(), "HHHHH=="+bytesAvailable, 32).show();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int  serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.isLoggable("Code", serverResponseCode);
                Log.v("Response", serverResponseMessage);
                // Toast.makeText(getApplicationContext(), "HH=="+serverResponseMessage, 32).show();
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
                return null;
            } catch (Exception e) {
                if (dialog.isShowing())
                    dialog.dismiss();
                //Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                Log.e(e.getClass().getName(), e.getMessage(), e);
                return null;
            }

            // (null);
        }

        @Override
        protected void onProgressUpdate(Void... unsued) {

        }

        @Override
        protected void onPostExecute(String sResponse) {
            try {
                if (dialog.isShowing())
                    dialog.dismiss();
                upload.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Audio Uploaded successfully", 32).show();
                if (sResponse != null) {

                    int  serverResponseCode = connection.getResponseCode();
                    String serverResponseMessage = connection.getResponseMessage();


                    /*JSONObject JResponse = new JSONObject(sResponse);
                                 int success = JResponse.getInt("SUCCESS");
                                 String message = JResponse.getString("MESSAGE");*/
                    if (serverResponseCode == 0) {
                        Toast.makeText(getApplicationContext(), serverResponseMessage,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Review uploaded successfully",
                                Toast.LENGTH_LONG).show();
                        //caption.setText("");
                    }
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),
                        e.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(e.getClass().getName(), e.getMessage(), e);
            }
        }
    }
    void chkConnectionStatus()
    {
        ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);


        final android.net.NetworkInfo wifi =
                connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


        final android.net.NetworkInfo mobile =
                connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);


        if( wifi.isAvailable() ){
            //Toast.makeText(this, "Wifi" , Toast.LENGTH_LONG).show();
        }
        else if( mobile.isAvailable() ){
            //Toast.makeText(this, "Mobile 3G " , Toast.LENGTH_LONG).show();
        }
        else
        {Toast.makeText(this, "No Network " , Toast.LENGTH_LONG).show();}
    }
    public void upload(){
        File file = new File(selectedPath);
        long length = file.length();
        length = length/1024;
        System.out.println("Filelength is:"+length);
        if (length<=3000){
            chkConnectionStatus();
            dialog = ProgressDialog.show(review.this, "Uploading",
                    "Please wait...", true);
            new ImageUploadTask().execute();
        }
        else{
            System.out.println("File too big");

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Reset...");
            alertDialog.setMessage("I toid you?");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // here you can add functions
                }
            });
            alertDialog.setIcon(R.drawable.icon);
            alertDialog.show();
        }
    }

    private Intent bookshop() {
        final Intent intent = new Intent();
        intent.setClass(this, bookshop.class);
        // intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
        return (intent);
    }
    private Intent home() {
        final Intent intent = new Intent();
        intent.setClass(this, LibraryActivity.class);
        // intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
        return (intent);
    }
}
