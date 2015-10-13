package dk.dtu.compute.fitnessviewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleApiSubscription {
    public static final int REQUEST_OAUTH = 1;
    private static final String TAG = GoogleApiSubscription.class.getSimpleName();
    public static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";

    private static GoogleApiClient mClient = null;
    private static Activity mActivity;
    private static boolean authInProgress;
    private Context context;

    public static final String SUBSCRIPTION_LIST_RECEIVED = "subscriptionListReceived";
    public List<String> subscriptions = new ArrayList<>();
    public GoogleApiSubscription(boolean authInProgress, Activity activity) {
        this.authInProgress = authInProgress;
        this.mActivity = activity;
        this.context = activity.getApplicationContext();
    }


    /**
     * Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     * to connect to Fitness APIs. The scopes included should match the scopes your app needs
     * (see documentation for details). Authentication will occasionally fail intentionally,
     * and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     * can address. Examples of this include the user never having signed in before, or having
     * multiple accounts on the device and needing to specify which account to use, etc.
     */
    public void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(mActivity.getApplicationContext())
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .useDefaultAccount()
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");

                                // Subscribe, just in case
                                subscribeAll();

                                // Check subscriptions
                                listSubscriptions();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            mActivity, 0).show();
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!authInProgress) {
                                    try {
                                        Log.i(TAG, "Attempting to resolve failed connection");
                                        authInProgress = true;
                                        result.startResolutionForResult(mActivity,
                                                REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG, "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();
    }

    /**
     * Manage subscriptions
     */
    public void subscribe(DataType dataType) {
        Fitness.RecordingApi.subscribe(mClient, dataType).setResultCallback(subscribeCallback);
    }

    public void subscribeAll(){
        subscribe(DataType.TYPE_STEP_COUNT_DELTA);
        subscribe(DataType.TYPE_ACTIVITY_SAMPLE);
        subscribe(DataType.TYPE_CALORIES_EXPENDED);
        subscribe(DataType.TYPE_DISTANCE_DELTA);
    }

    ResultCallback<Status> subscribeCallback =  new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (status.isSuccess()) {
                if (status.getStatusCode()
                        == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                    Log.i(TAG, "Existing subscription.");
                } else {
                    Log.i(TAG, "Successfully subscribed to Fitness API!");
                }
            } else {
                Log.i(TAG, "There was a problem subscribing.");
            }
        }
    };

    public void unSubscribe(DataType dataType) {
        Fitness.RecordingApi.unsubscribe(mClient, dataType).setResultCallback(unSubscribeCallback);
    }

    ResultCallback<Status> unSubscribeCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            if (status.isSuccess()) {
                Log.i(TAG, "Successfully unsubscribed");
            } else {
                // Subscription not removed
                Log.i(TAG, "Failed to unsubscribe");
            }
        }
    };

    public void listSubscriptions() {
        //TODO Set custom result callback so the result is return to the activity
        Log.d(TAG, "List of subscriptions requested");
        subscriptions.clear();
        Fitness.RecordingApi.listSubscriptions(mClient)
                // Create the callback to retrieve the list of subscriptions asynchronously.
                .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                    @Override
                    public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                        for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                            String dt = sc.getDataType().getName();
                            subscriptions.add(dt);
                            context.sendBroadcast(new Intent(SUBSCRIPTION_LIST_RECEIVED));
                            Log.i(TAG, "Active subscription for data type: " + dt);
                        }
                    }
                });
    }

    /**
     * Query the History API
     */
    public static class requestDataTask extends AsyncTask<DataReadRequest, Void, DataReadResult> {
        GoogleApiDataListener listener = null;

        public requestDataTask(GoogleApiDataListener listener){
            this.listener = listener;
        }

        protected DataReadResult doInBackground(DataReadRequest... dataReadRequest) {
            if (dataReadRequest.equals(null)) {
                cancel(true);
                Log.d(TAG, "No type defined. Cancelling requestDataTask");
            }

            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            return Fitness.HistoryApi.readData(mClient, dataReadRequest[0]).await(1, TimeUnit.MINUTES);
        }

        @Override
        protected void onPostExecute(DataReadResult result) {
            super.onPostExecute(result);
            Log.d(TAG, "Sending data to listener");
            listener.onDataReceived(result);
        }
    }

    public static DataReadRequest dataRequestObject() {
        // Setting a start and end date using a range of x weeks before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        // Set end time to yesterday evening
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);

        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Set start time to the morning
        cal.add(Calendar.MINUTE, 1);
        long startTime = cal.getTimeInMillis();

        return new DataReadRequest.Builder()
                .read(DataType.TYPE_CALORIES_EXPENDED)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .read(DataType.TYPE_ACTIVITY_SAMPLE)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Return a {@link DataReadRequest} for all step count changes in the past week.
     */
    public static DataReadRequest queryStepData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of x weeks before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -2);
        long startTime = cal.getTimeInMillis();
        // [END build_read_data_request]

        return new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    public static DataReadRequest queryActivityData(TimeUnit timeUnit) {
        // [START build_read_data_request]
        // Setting a start and end date using a range of x weeks before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -2);
        long startTime = cal.getTimeInMillis();
        // [END build_read_data_request]

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, timeUnit)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    public static DataReadRequest queryCalorieData(TimeUnit timeUnit) {
        // [START build_read_data_request]
        // Setting a start and end date using a range of x weeks before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -2);
        long startTime = cal.getTimeInMillis();
        // [END build_read_data_request]

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, timeUnit)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        //.read(DataType.TYPE_CALORIES_EXPENDED)
    }


    /**
     * Log a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would
     * dump all the data. In this sample, logging also prints to the device screen, so we can see
     * what the query returns, but your app should not log fitness information as a privacy
     * consideration. A better option would be to dump the data you receive to a local data
     * directory to avoid exposing it to other applications.
     */
    public static void printData(DataReadResult dataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    /**
     * Delete a {@link DataSet} from the History API. In this example, we delete all
     * step count data for the past 24 hours.
     */
    private void deleteData() {
        Log.i(TAG, "Deleting today's step count data");

        // [START delete_dataset]
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build();

        // Invoke the History API with the Google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully deleted today's step count data");
                        } else {
                            // The deletion will fail if the requesting app tries to delete data
                            // that it did not insert.
                            Log.i(TAG, "Failed to delete today's step count data");
                        }
                    }
                });
        // [END delete_dataset]
    }
    // [END parse_dataset]

    public void getActivitiesData(long from, long till) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(from, till, TimeUnit.MILLISECONDS)
                .build();

        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(DataReadResult dataReadResult) {
                Status status = dataReadResult.getStatus();
                if (status.isSuccess()) {
                    for (Bucket bucket : dataReadResult.getBuckets()) {
                        if (!bucket.getDataSets().isEmpty()) {
                            DataSet dataSet = bucket.getDataSets().get(0);
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                for (Field field : dp.getDataType().getFields()) {
                                    String fieldName = field.getName();
                                    if (fieldName != null && fieldName.equals("activity")) {
                                        String type = dp.getDataType().getName();
                                        Date from = new Date(dp.getStartTime(TimeUnit.MILLISECONDS));
                                        Date till = new Date(dp.getEndTime(TimeUnit.MILLISECONDS));
                                        Log.d(TAG, "" + type + " " + from + " " + till);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public boolean isAuthInProgress() {
        return authInProgress;
    }

    public void setAuthInProgress(boolean authInProgress) {
        this.authInProgress = authInProgress;
    }

    public void connect() {
        mClient.connect();
    }

    public void disconnect() {
        mClient.disconnect();
    }

    public boolean isConnected() {
        return mClient.isConnected();
    }

    public boolean isConnecting() {
        return mClient.isConnecting();
    }
}
