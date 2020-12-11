package com.uc.health.stethforcovid.model;

import android.app.Application;

import com.mmm.healthcare.scope.Stethoscope;

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
