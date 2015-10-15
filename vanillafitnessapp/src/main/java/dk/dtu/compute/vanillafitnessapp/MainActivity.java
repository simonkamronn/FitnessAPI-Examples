package dk.dtu.compute.vanillafitnessapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";

    private boolean authInProgress = false;
    private GoogleApiSubscription mGoogleApiSubscription;
    private TextView subscription_list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        subscription_list = (TextView) findViewById(R.id.subscription_list);

        // Build API to Google Play Service and subscriber to Fitness API
        mGoogleApiSubscription = new GoogleApiSubscription(authInProgress, this);
        mGoogleApiSubscription.buildFitnessClient();
        authInProgress = mGoogleApiSubscription.isAuthInProgress();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action){
                case GoogleApiSubscription.SUBSCRIPTION_LIST_RECEIVED:
                    String subscription_string = "";
                    for (String s: mGoogleApiSubscription.subscriptions){
                        subscription_string += s + "\n";
                    }
                    subscription_list.setText(subscription_string);
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
            mGoogleApiSubscription.setAuthInProgress(false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
}
