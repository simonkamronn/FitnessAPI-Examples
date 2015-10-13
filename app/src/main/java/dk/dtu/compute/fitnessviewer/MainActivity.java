package dk.dtu.compute.fitnessviewer;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;

public class MainActivity extends Activity
        implements FitnessFragment.FitnessListener,
        NavigationDrawerFragment.NavigationDrawerCallbacks {
    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";

    private boolean authInProgress = false;
    private GoogleApiSubscription mGoogleApiSubscription;
    private FitnessFragment fitnessFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Set up fragments
        if (savedInstanceState == null) {
            fitnessFragment = new FitnessFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fitnessFragment)
                    .commit();
        }

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        // Build API to Google Play Service and subscriber to Fitness API
        mGoogleApiSubscription = new GoogleApiSubscription(authInProgress, this);
        mGoogleApiSubscription.buildFitnessClient();
        authInProgress = mGoogleApiSubscription.isAuthInProgress();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonClick(String text, int position) {
        mGoogleApiSubscription.listSubscriptions();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action){
                case GoogleApiSubscription.SUBSCRIPTION_LIST_RECEIVED:
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            mGoogleApiSubscription.setAuthInProgress(authInProgress);
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiSubscription.isConnecting() && !mGoogleApiSubscription.isConnected()) {
                    mGoogleApiSubscription.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.d(TAG, "Connecting...");
        mGoogleApiSubscription.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GoogleApiSubscription.SUBSCRIPTION_LIST_RECEIVED);
        registerReceiver(mReceiver, intentFilter);

        //Get data
        new GoogleApiSubscription.requestDataTask(stepListener).execute(
                GoogleApiSubscription.queryStepData());
        new GoogleApiSubscription.requestDataTask(activityListener).execute(
                GoogleApiSubscription.queryActivityData(
                        TimeUnit.DAYS));
        new GoogleApiSubscription.requestDataTask(calorieListener).execute(
                GoogleApiSubscription.queryCalorieData(
                        TimeUnit.DAYS));

        // Try new method
        //new GoogleApiSubscription.requestDataTask(genericListener).execute(
        //        GoogleApiSubscription.dataRequestObject());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiSubscription.isConnected()) {
            Log.i(TAG, "Disconnecting from Google API");
            mGoogleApiSubscription.disconnect();
        }
    }

    private Bundle formatCalories(DataReadResult dataReadResult){
        Bundle bundle = new Bundle();

        // Calorie expenditure
        List<DataPoint> dataPointList = dataReadResult.
                getDataSet(DataType.TYPE_CALORIES_EXPENDED).
                getDataPoints();
        float[] calories = new float[dataPointList.size()];
        long[] startTime = new long[dataPointList.size()];
        long[] endTime = new long[dataPointList.size()];
        int idx = 0;
        for (DataPoint dp : dataPointList) {
            calories[idx] = dp.getValue(Field.FIELD_CALORIES).asFloat();
            startTime[idx] = dp.getStartTime(TimeUnit.MILLISECONDS);
            endTime[idx] = dp.getEndTime(TimeUnit.MILLISECONDS);
            ++idx;
        }
        bundle.putFloatArray("CALORIES", calories);
        bundle.putLongArray("START_TIME", startTime);
        bundle.putLongArray("END_TIME", endTime);
        Log.d(TAG, String.format("Calorie data points: %d", idx));

        return bundle;
    }

    private Bundle formatActivity(DataReadResult dataReadResult){
        Bundle bundle = new Bundle();

        // Activity Recognition
        List<DataPoint> dataPointList = dataReadResult.
                getDataSet(DataType.TYPE_ACTIVITY_SAMPLE).
                getDataPoints();
        int[] activity = new int[dataPointList.size()];
        long[] endTime = new long[dataPointList.size()];
        float[] confidence = new float[dataPointList.size()];
        int idx = 0;
        for (DataPoint dp : dataPointList) {
            activity[idx] = dp.getValue(Field.FIELD_ACTIVITY).asInt();
            confidence[idx] = dp.getValue(Field.FIELD_CONFIDENCE).asFloat();
            endTime[idx] = dp.getEndTime(TimeUnit.MILLISECONDS);
            ++idx;
        }
        bundle.putIntArray("ACTIVITY", activity);
        bundle.putFloatArray("CONFIDENCE", confidence);
        bundle.putLongArray("TIMESTAMP", endTime);
        Log.d(TAG, String.format("Activity data points: %d", idx));

        return bundle;
    }

    private Bundle formatStepCount(DataReadResult dataReadResult){
        Bundle bundle = new Bundle();

        // Step Count
        List<DataPoint> dataPointList = dataReadResult.
                getDataSet(DataType.TYPE_STEP_COUNT_DELTA).
                getDataPoints();
        int[] steps = new int[dataPointList.size()];
        long[] startTime = new long[dataPointList.size()];
        long[]endTime = new long[dataPointList.size()];
        int idx = 0;
        for (DataPoint dp : dataPointList) {
            steps[idx] = dp.getValue(Field.FIELD_STEPS).asInt();
            startTime[idx] = dp.getStartTime(TimeUnit.MILLISECONDS);
            endTime[idx] = dp.getEndTime(TimeUnit.MILLISECONDS);
            ++idx;
        }
        bundle.putIntArray("STEPS", steps);
        bundle.putLongArray("START_TIME", startTime);
        bundle.putLongArray("END_TIME", endTime);
        Log.d(TAG, String.format("Step data points: %d", idx));

        return bundle;
    }

    private Bundle formatDistance(DataReadResult dataReadResult){
        Bundle bundle = new Bundle();

        // Distance
        List<DataPoint> dataPointList = dataReadResult.
                getDataSet(DataType.TYPE_DISTANCE_DELTA).
                getDataPoints();
        float[] distance = new float[dataPointList.size()];
        long[] startTime = new long[dataPointList.size()];
        long[]endTime = new long[dataPointList.size()];
        int idx = 0;
        for (DataPoint dp : dataPointList) {
            distance[idx] = dp.getValue(Field.FIELD_DISTANCE).asFloat();
            startTime[idx] = dp.getStartTime(TimeUnit.MILLISECONDS);
            endTime[idx] = dp.getEndTime(TimeUnit.MILLISECONDS);
            ++idx;
        }
        bundle.putFloatArray("DISTANCE", distance);
        bundle.putLongArray("START_TIME", startTime);
        bundle.putLongArray("END_TIME", endTime);
        Log.d(TAG, String.format("Distance data points: %d", idx));

        return bundle;
    }

    GoogleApiDataListener genericListener = new GoogleApiDataListener() {
        @Override
        public void onDataReceived(DataReadResult dataReadResult) {
            // GoogleApiSubscription.printData(dataReadResult);

            Log.d(TAG, "Received generic result");
            if (dataReadResult.getDataSets().size() > 0) {
                // Go through the requested data types
                Bundle bundle = formatActivity(dataReadResult);
            }
        }
    };

    GoogleApiDataListener stepListener = new GoogleApiDataListener() {
        @Override
        public void onDataReceived(DataReadResult dataReadResult) {
            Log.d(TAG, "Received step result");

            final String DATE_FORMAT = "dd/MM";
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            List<Column> columns = new ArrayList<>();
            List<SubcolumnValue> values;
            List<AxisValue> xAxisValues = new ArrayList<>();

            if (dataReadResult.getBuckets().size() > 0) {
                fitnessFragment.mColumnChart.clearAnimation();
                for (Bucket bucket : dataReadResult.getBuckets()) { // days/hours, depends on request
                    DataSet dataset = bucket.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);
                    List<DataPoint> dataPointList = dataset.getDataPoints();
                    for (DataPoint dp : dataPointList) {
                        // add to bar chart
                        String timestamp = dateFormat.format(dp.getTimestamp(TimeUnit.MILLISECONDS));
                        float steps = (float) dp.getValue(Field.FIELD_STEPS).asInt();

                        // Set axis labels
                        xAxisValues.add(new AxisValue(columns.size()).setLabel(timestamp));

                        values = new ArrayList<>();
                        values.add(new SubcolumnValue(steps, ChartUtils.COLOR_RED));
                        columns.add( new Column(values));
                    }
                }
                fitnessFragment.setStepsData(xAxisValues, columns);
            }
        }
    };

    GoogleApiDataListener activityListener = new GoogleApiDataListener() {
        @Override
        public void onDataReceived(DataReadResult dataReadResult) {
            Log.d(TAG, "Received activity result");
            final String DATE_FORMAT = "dd/MM";
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            //GoogleApiSubscription.printData(dataReadResult);

            List<Line> lines = new ArrayList<>(); // all lines
            List<PointValue> walking = new ArrayList<>(); // a single line
            List<PointValue> biking = new ArrayList<>();
            List<PointValue> in_vehicle = new ArrayList<>();
            List<AxisValue> xAxisValues = new ArrayList<>();

            int x_val = 0;

            if (dataReadResult.getBuckets().size() > 0) {
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    String date = dateFormat.format(bucket.getStartTime(TimeUnit.MILLISECONDS));
                    DataSet dataSet = bucket.getDataSet(DataType.AGGREGATE_ACTIVITY_SUMMARY);
                    Log.d(TAG, "New databucket with date: " + date);

                    // Increase counter
                    x_val = x_val + 1;

                    // Set axis labels
                    xAxisValues.add(new AxisValue(x_val).setLabel(date));

                    // Add a zero measurement point
                    walking.add(new PointValue(x_val, 0));
                    biking.add(new PointValue(x_val, 0));
                    in_vehicle.add(new PointValue(x_val, 0));

                    List<DataPoint> dataPointList = dataSet.getDataPoints();
                    for (DataPoint dp : dataPointList) {
                        String timestamp = dateFormat.format(dp.getTimestamp(TimeUnit.MILLISECONDS));
                        String activity = dp.getValue(Field.FIELD_ACTIVITY).asActivity();
                        int duration = dp.getValue(Field.FIELD_DURATION).asInt();

                        if(dp.getValue(Field.FIELD_ACTIVITY).asActivity() == "walking"){
                            walking.set(x_val - 1, new PointValue(x_val, duration / 60000));
                        }
                        else if(dp.getValue(Field.FIELD_ACTIVITY).asActivity() == "biking") {
                            biking.set(x_val - 1, new PointValue(x_val, duration / 60000));
                        }
                        else if(dp.getValue(Field.FIELD_ACTIVITY).asActivity() == "in_vehicle") {
                            in_vehicle.set(x_val - 1, new PointValue(x_val, duration / 60000));
                        }
                        Log.d(TAG, "timestamp: " + timestamp + ", activity: " + activity + ", duration: " + duration / 60000);
                    }
                }
                Line line = new Line(walking);
                line.setColor(ChartUtils.COLOR_GREEN);
                line.setHasLines(true);
                line.setHasPoints(false);
                line.setCubic(false);
                lines.add(line);

                line = new Line(biking);
                line.setColor(ChartUtils.COLOR_BLUE);
                line.setHasLines(true);
                line.setHasPoints(false);
                line.setCubic(false);
                lines.add(line);

                line = new Line(in_vehicle);
                line.setColor(ChartUtils.COLOR_VIOLET);
                line.setHasLines(true);
                line.setHasPoints(false);
                line.setCubic(false);
                lines.add(line);

                fitnessFragment.setActivityData(xAxisValues, lines);
            }
        }
    };

    GoogleApiDataListener calorieListener = new GoogleApiDataListener() {
        @Override
        public void onDataReceived(DataReadResult dataReadResult) {
            Log.d(TAG, "Received activity result");
            final String DATE_FORMAT = "dd/MM";
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

            List<Line> lines = new ArrayList<>(); // all lines
            List<PointValue> calories; // a single line
            calories = new ArrayList<>();
            int x_val = 0;
            List<AxisValue> xAxisValues = new ArrayList<>();

            if (dataReadResult.getBuckets().size() > 0) {
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    String date = dateFormat.format(bucket.getStartTime(TimeUnit.MILLISECONDS));
                    DataSet dataSet = bucket.getDataSet(DataType.AGGREGATE_CALORIES_EXPENDED);
                    Log.d(TAG, "New databucket with date: " + date);

                    // Increase counter
                    x_val = x_val + 1;

                    // Set axis labels
                    xAxisValues.add(new AxisValue(x_val).setLabel(date));

                    // Add a zero measurement point
                    calories.add(new PointValue(x_val, 0));

                    List<DataPoint> dataPointList = dataSet.getDataPoints();
                    for (DataPoint dp : dataPointList) {
                        String timestamp = dateFormat.format(dp.getTimestamp(TimeUnit.MILLISECONDS));
                        float calorie = dp.getValue(Field.FIELD_CALORIES).asFloat();

                        // Set graph values
                        calories.set(x_val - 1, new PointValue(x_val, calorie));
                        Log.d(TAG, "timestamp: " + timestamp + ", calories: " + calorie);
                    }
                }
                Line line = new Line(calories);
                line.setColor(ChartUtils.COLOR_BLUE);
                line.setHasLines(true);
                line.setHasPoints(false);
                line.setCubic(false);
                line.setHasLabels(false);
                line.setHasLabelsOnlyForSelected(true);
                lines.add(line);

                fitnessFragment.setCalorieData(xAxisValues, lines);
            }
        }
    };


}

