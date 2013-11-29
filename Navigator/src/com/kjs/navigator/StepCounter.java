package com.kjs.navigator;


import android.app.Activity;
import android.util.Log;


public class StepCounter {
	 OnStepEventListener mCallback;
	
    public interface OnStepEventListener {
        public void stepEvent();
        
    }
    public StepCounter(Activity activity){
        try {
            mCallback = (OnStepEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    	   
    }
    public void pushdata(int x,int y,int z)
    {
    	Log.d("Function Call","Inside Step CounterX:"+x+" Y:"+y+" Z:"+z);
    	mCallback.stepEvent();
    	
    }
}
