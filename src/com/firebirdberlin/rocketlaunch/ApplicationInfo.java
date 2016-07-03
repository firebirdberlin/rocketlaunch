package com.firebirdberlin.rocketlaunch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Represents a launchable application. An application is made of a name (or title), an intent
 * and an icon.
 */
class mApplicationInfo {
    final String SEPARATOR = " | ";
    /**
     * The application name.
     */
    CharSequence title;

    CharSequence packageName;

    CharSequence activityName;

    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * The application icon.
     */
    BitmapDrawable icon;

    /**
     * When set to true, indicates that the icon has been resized.
     */
    boolean filtered;

    /**
     * usage counter
     */
    int usage;

    boolean valid;
    boolean confirmedByApplicationManager;
    public mApplicationInfo() {
        confirmedByApplicationManager = false;
    }

    public mApplicationInfo(String cache_str) {
        String[] app_info_str_split = cache_str.split("\\s\\|\\s");
        if (app_info_str_split.length != 4) {
            valid = false;
            return;
        }

        title        = app_info_str_split[0];
        packageName  = app_info_str_split[1];
        activityName = app_info_str_split[2];
        usage        = Integer.parseInt(app_info_str_split[3]);
        confirmedByApplicationManager = false;
        if (packageName != null && activityName != null) {
            setActivity();
            valid = true;
        } else {
            valid = false;
        }
        //calculateAlternateLabelAndPackageName();
    }

    public boolean isValid(){return valid;}
    /**
     * Creates the application intent based on a component name and various launch flags.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
    }


    final void setActivity() {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        intent.setComponent(new ComponentName(packageName.toString(),
                                                activityName.toString())
                            );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof mApplicationInfo)) {
            return false;
        }

        mApplicationInfo that = (mApplicationInfo) o;
        return title.equals(that.title) &&
                intent.getComponent().getClassName().equals(
                        that.intent.getComponent().getClassName());
    }

    @Override
    public int hashCode() {
        int result;
        result = (title != null ? title.hashCode() : 0);
        final String name = intent.getComponent().getClassName();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public String toString(){
        return title + SEPARATOR + packageName
                     + SEPARATOR + activityName
                     + SEPARATOR + String.valueOf(usage);
    }

    public void saveIconCache(Context ctx){
        try{
            FileOutputStream outStream;
            String filename = ctx.getCacheDir() + "/" + hashCode() + ".png";

            outStream = new FileOutputStream(filename);

            if (Config.PRO) {
                int width  = (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size)/2;
                int height = (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size)/2;
                Bitmap resized = Bitmap.createScaledBitmap(icon.getBitmap(), width, height, false);
                resized.compress(Bitmap.CompressFormat.PNG, 0, outStream);
            } else {
                int width  = (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size)/4;
                int height = (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size)/4;
                Bitmap resized = Bitmap.createScaledBitmap(icon.getBitmap(), width, height, false);
                resized.compress(Bitmap.CompressFormat.PNG, 0, outStream);
            }

            outStream.flush();
            outStream.close();
        }catch (FileNotFoundException e) {
            Log.e(RocketLaunch.LOG_TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(RocketLaunch.LOG_TAG, "Cannot read file: " + e.toString());
        }
    }

    public void loadIconCache(Context ctx){
        if (icon != null) return;
        try{
            String filename = ctx.getCacheDir()    + "/" + hashCode() + ".png";

            FileInputStream fs = new FileInputStream(filename);
            icon = new BitmapDrawable(ctx.getResources(), fs);
            //icon.setTargetDensity(100);
            fs.close();
            fs = null;
            filtered = false;

        }catch (FileNotFoundException e) {
            Log.e(RocketLaunch.LOG_TAG, "File not found: " + e.toString());
            icon = null;
        } catch (IOException e) {
            Log.e(RocketLaunch.LOG_TAG, "Cannot read file: " + e.toString());
            icon = null;
        }
    }


    public void filterIcon(Context ctx) {
        if (filtered == true) return;

        int width =  (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size);
        int height = (int) ctx.getResources().getDimension(android.R.dimen.app_icon_size);
        icon.setBounds(0, 0, width,height);
    }
}
