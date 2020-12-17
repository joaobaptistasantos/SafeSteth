package com.uc.healthlab.safesteth.model;

import android.app.Application;

import com.mmm.healthcare.scope.Stethoscope;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
public class DataHolder extends Application {

    private static final DataHolder holder = new DataHolder();
    private Stethoscope mStethoscope;

    /* No need to synchronized */
    public static DataHolder getInstance() {
        return holder;
    }

    public Stethoscope getmStethoscope() {
        return mStethoscope;
    }

    public void setmStethoscope(Stethoscope mStethoscope) {
        this.mStethoscope = mStethoscope;
    }
}
