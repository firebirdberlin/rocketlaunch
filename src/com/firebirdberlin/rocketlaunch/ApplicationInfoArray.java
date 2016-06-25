package com.firebirdberlin.rocketlaunch;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.lang.StringBuilder;
import java.util.ArrayList;


public class ApplicationInfoArray extends ArrayList<mApplicationInfo>{
    private final static String LOG_TAG = "ApplicationInfoArray";
    private Context ctx;

    public void setContext(Context c) {ctx = c;}

    public ApplicationInfoArray(){
        super();
        ctx = null;
    }

    public ApplicationInfoArray(Context c){
        super();
        ctx = c;
    }

    public ApplicationInfoArray(Context c, int count){
        super(count);
        ctx = c;
    }

    public static ApplicationInfoArray load(Context ctx, String filename) {
        ApplicationInfoArray res = new ApplicationInfoArray();
        try {
            InputStream inputStream = ctx.openFileInput(filename);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = "";

                while ( (line = bufferedReader.readLine()) != null ) {
                    if (line.length() > 0) {
                        mApplicationInfo appInfo = new mApplicationInfo(line);
                        if (appInfo.isValid()) {
                            res.add(appInfo);
                        }
                    }
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot read file: " + e.toString());
        }

        return res;
    }

    public void saveToFile(String filename ) {
        if (ctx  == null) {
            Log.w(LOG_TAG, "saveToFile() : You have to set the context variable !");
            return;
        }
        StringBuilder res = new StringBuilder();
        for (mApplicationInfo appInfo : this) {
            res.append(appInfo.toString());
            res.append("\n");
        }

        FileOutputStream outputStream;
        try {
            outputStream = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(res.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
