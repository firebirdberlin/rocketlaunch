package com.firebirdberlin.rocketlaunch;

import android.app.ActionBar; // >= api level 11
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.inputmethod.EditorInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ProgressBar;
import java.lang.ClassCastException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RocketLaunch extends Activity {
    public static final String LOG_TAG = "RocketLaunch";
    public static final String SHARED_PREFS_FILE = "Apps.dat";
    public static final int SORT_BY_ALPHA = 0;
    public static final int SORT_BY_USAGE = 1;

    private ApplicationInfoArray mApplications;
    private GridView mGrid;
    private ApplicationsAdapter AppAdapter;
    private BroadcastReceiver AppsReceiver;
    private EditText SearchBar;
    private ProgressBar mProgress;
    private int NrLoadedApps;
    private boolean isDestroying;
    private int SortOrder;
    private boolean isUpdating;
    private TextWatcher SearchTextWatcher;
    private OnScrollListener mScrollListener;
    private static boolean debug;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug = isDebuggable();

        Logger("onCreate()");
        setContentView(R.layout.home);

        NrLoadedApps = 0;
        isDestroying = false;
        isUpdating  = false;

        ActionBar actionBar = getActionBar();
        //
        //Tell the ActionBar we want to use Tabs.
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        //actionBar.setTitle("");

        mProgress = (ProgressBar) findViewById(R.id.all_apps_progress);
        SearchBar = (EditText) findViewById(R.id.SearchBar);
        SearchBar.setEnabled(false); // will be activated after init
        SearchBar.setSingleLine();
        SearchBar.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        SearchBar.setImeActionLabel("Go", EditorInfo.IME_ACTION_DONE);

        SearchBar.setText("");
        SearchTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ( mGrid == null) return;
                if (s == null || s.length() == 0) {
                    ((Filterable) (mGrid.getAdapter())).getFilter().filter(null);
                    return;
                }

                ((Filterable) (mGrid.getAdapter())).getFilter().filter(s.toString().trim());
            }
        };

        AppsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger("somethings changed in the list of apps");
                new LoadAppsTask().execute(mApplications);
            }
        };

        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(AppsReceiver, filter);

        if (mApplications == null){
            mApplications = ApplicationInfoArray.load(this, "apps.dat");
            mApplications.setContext(this);

            if (mApplications.size() > 0 ){
                bindApplications();
            }
            //new LoadIconsTask().execute();
            new LoadAppsTask().execute(mApplications);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger("onStart()");

        if (SearchBar != null){
            SearchBar.setText("");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger("onResume()");
        if (SearchBar != null){
            SearchBar.setText("");

            SearchBar.addTextChangedListener(SearchTextWatcher);
            SearchBar.setEnabled(true);
        }

        if (AppAdapter != null) {
            //new LoadIconsTask().execute();
            AppAdapter.updateIcons();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger("onPause()");
        if (SearchBar != null){
            SearchBar.setText("");
            SearchBar.removeTextChangedListener(SearchTextWatcher);
        }

        if (AppAdapter != null) AppAdapter.clearIcons();

        // Another activity is taking focus (this activity is about to be "paused").
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
        Logger("onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger("onDestroy()");
        unregisterReceiver(AppsReceiver);
        isDestroying =  true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);

        final Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        switch (display.getRotation()) {
            case Surface.ROTATION_0: {
                break;
            }
            case Surface.ROTATION_90:{
                break;
            }
            case Surface.ROTATION_180:{
                break;
            }
            case Surface.ROTATION_270:{
                break;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                AppAdapter.toggleSortOrder();
                break;
            case R.id.menu_info:
                startActivityAppInfo(null);
                break;
            default:
                break;
        }

        return true;
    }


    private void startActivityAppInfo(String packageName){
        if ( packageName == null) {
            //Open the generic Apps page:
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
            return;

        }

        try {
            //Open the specific App Info page:
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);

        } catch ( ActivityNotFoundException e ) {
            //Open the generic Apps page:
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);

        }
    }

    /**
     * Creates a new appplications adapter for the grid view and registers it.
     */
    private void bindApplications() {
        if (mGrid == null) {
            mGrid = (GridView) findViewById(R.id.all_apps);
            mGrid.setFastScrollEnabled(true);
            mGrid.setOnItemClickListener(new ApplicationLauncher());
            mGrid.setOnItemLongClickListener(new ApplicationInfoLauncher());
        }
        if (AppAdapter == null) {
            AppAdapter = new ApplicationsAdapter(this, mApplications);
            mGrid.setAdapter(AppAdapter);
            mGrid.setSelection(0);
        }
    }

    /**
     * Starts the selected activity/application in the grid view.
     */
    private class ApplicationLauncher implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView parent, View v, int position, long id) {

            mApplicationInfo app = (mApplicationInfo) parent.getItemAtPosition(position);
            app.usage++;
            AppAdapter.save();

            if (SearchBar != null){
                SearchBar.setText("");
            }
            startActivity(app.intent);
        }
    }

    /**
     * Starts the selected activity/application info.
     */
    private class ApplicationInfoLauncher implements AdapterView.OnItemLongClickListener {
        public boolean onItemLongClick(AdapterView parent, View v, int position, long id) {
            if (Config.PRO == false) return false; // do nothing in lite version
            mApplicationInfo app = (mApplicationInfo) parent.getItemAtPosition(position);

            startActivityAppInfo(app.packageName.toString());
            return true;
        }
    }

    public void triggerLoadAppsTask(){
        new LoadAppsTask().execute(mApplications);
    }

     private class LoadAppsTask extends AsyncTask<ArrayList<mApplicationInfo>, Integer, ArrayList<mApplicationInfo> > {
         protected ArrayList<mApplicationInfo> doInBackground(ArrayList<mApplicationInfo>... params) {
            Logger("starting update task NOW.");
            isUpdating = true;
            if (params.length == 0) return  new ArrayList<mApplicationInfo>();
            ArrayList<mApplicationInfo> AppsList = new ArrayList<mApplicationInfo>();
            AppsList.addAll(params[0]);

            for (int i = AppsList.size()-1; i >= 0 ; i--){
                mApplicationInfo appInfo = AppsList.get(i);
                appInfo.confirmedByApplicationManager = false;
                Logger(String.format("%s", appInfo.toString()));
            }

            PackageManager manager = getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

            // sort operation
            Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

            if (apps != null) {
                final int count = apps.size();
                NrLoadedApps = 0;
                //mProgress.setMax(count);
                // long running operation
                for (int i = 0; i < count; i++) {
                    mApplicationInfo application = new mApplicationInfo();
                    ResolveInfo info = apps.get(i);
                    application.title = info.loadLabel(manager);
                    application.packageName = info.activityInfo.applicationInfo.packageName;
                    application.activityName = info.activityInfo.name;
                    application.confirmedByApplicationManager = true;
                    application.setActivity();

                    //if (isDestroying == true ) break;
                    // ignore self
                    if (info.activityInfo.applicationInfo.packageName.equals(getPackageName())) continue;

                    int mAppIdx = AppsList.indexOf(application);

                    if (mAppIdx > -1) {
                        mApplicationInfo pai = AppsList.get(mAppIdx);
                        pai.confirmedByApplicationManager = true;
                    } else {
                        application.usage = 0;
                        AppsList.add(application);
                    }

                    publishProgress(NrLoadedApps++);
                }
            }

            for (int i = AppsList.size() - 1; i >= 0 ; i--){
                if (AppsList.get(i).confirmedByApplicationManager == false){
                    AppsList.remove(i);
                }
            }

            return AppsList;
         }

         protected void onProgressUpdate(Integer... progress) {
         }

         protected void onPostExecute(ArrayList<mApplicationInfo> result) {
            if (isDestroying == true ) return;
            bindApplications();
            String SearchText = SearchBar.getText().toString();
            AppAdapter.updateData(result);
            isUpdating = false;
            AppAdapter.notifyDataSetChanged();
            if (SearchText.length() > 0) {
                SearchBar.setText(SearchText);
                SearchBar.setSelection(SearchText.length());
            }
            Logger("update task finalized.");
            new CacheIconsTask().execute();

         }
    }

    private class CacheIconsTask extends AsyncTask<Void, Integer,Void> {
         protected Void doInBackground(Void...params) {
            isUpdating = true;
            //if (SearchBar != null) SearchBar.setEnabled(false);
            PackageManager manager = getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

            // sort operation
            Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

            if (apps != null) {
                final int count = apps.size();

                mProgress.setMax(count);

                // long running operation
                for (int i = 0; i < count; i++) {

                    ResolveInfo info = apps.get(i);
                    // ignore self
                    if (info.activityInfo.applicationInfo.packageName.equals("com.firebirdberlin.rocketlaunch")) continue;

                    mApplicationInfo application = new mApplicationInfo();
                    application.title = info.loadLabel(manager);
                    application.packageName = info.activityInfo.applicationInfo.packageName;
                    application.activityName = info.activityInfo.name;
                    application.setActivity();
                    application.confirmedByApplicationManager = true;

                    int mAppIdx = mApplications.indexOf(application);
                    if ( (mAppIdx < 0)
                            || (mApplications.get(mAppIdx).icon == null)) { // cache only if not already loaded
                        try {
                            application.icon = (BitmapDrawable) info.activityInfo.loadIcon(manager);
                        } catch (ClassCastException e) {
                            application.icon = null;
                        }
                        application.saveIconCache(getApplicationContext());
                    }
                    publishProgress(i);
                }
            }

            return null;
         }

         protected void onProgressUpdate(Integer... progress) {
             mProgress.setProgress(progress[0]);
             //if (progress[0]+1 %24 == 0)
            if (AppAdapter!=null) AppAdapter.notifyDataSetChanged();
         }

         protected void onPostExecute(Void result) {
            if (AppAdapter!=null) {
                AppAdapter.updateIcons();
                AppAdapter.notifyDataSetChanged();
            }
            mProgress.setVisibility(View.GONE);
         }
    }

    private class LoadIconsTask extends AsyncTask<Void, Integer,Void> {
         protected Void doInBackground(Void...params) {

            if (AppAdapter != null) {
                AppAdapter.updateIcons();
            }

            return null;
         }

         protected void onProgressUpdate(Integer... progress) {
            if (AppAdapter!=null) AppAdapter.notifyDataSetChanged();
         }

         protected void onPostExecute(Void result) {
                if (SearchBar != null) SearchBar.setEnabled(true);
                if (AppAdapter!=null) AppAdapter.notifyDataSetChanged();
                new LoadAppsTask().execute(mApplications);
                //new CacheIconsTask().execute();
         }
    }

    public boolean isDebuggable(){
        return ( 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }

    public static void Logger(String message){
        if (debug == true)
            Log.d(LOG_TAG, message);
    }
}
