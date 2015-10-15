package dk.dtu.compute.vanillafitnessapp;

import com.google.android.gms.fitness.result.DataReadResult;

public interface GoogleApiDataListener {
    void onDataReceived(DataReadResult dataReadResult);
}