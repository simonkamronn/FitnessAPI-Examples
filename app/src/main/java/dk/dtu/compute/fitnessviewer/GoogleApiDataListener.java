package dk.dtu.compute.fitnessviewer;

import com.google.android.gms.fitness.result.DataReadResult;

public interface GoogleApiDataListener {
    void onDataReceived(DataReadResult dataReadResult);
}