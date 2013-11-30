package com.kjs.navigator;


import java.util.ArrayList;

import com.kjs.navigator.Tool;
import com.kjs.navigator.HighPassFIR;
import com.kjs.navigator.LowPassFIR;

import android.app.Activity;
import android.util.Log;


public class StepCounter {
	 OnStepEventListener mCallback;
	private boolean mInitialized;
	private double mLastX;
	private double mLastY;
	private double mLastZ;
	private final float NOISE = (float) 2.0;
	LowPassFIR lowPassFIR_filter, lowPassFIR_integrate;
	HighPassFIR highPassFIR;
	double peak, valley, lastReading = Double.MIN_VALUE, largestReading = 0.0;
	private boolean  climbing = true, descending = false;		// to initialize sensor only once
	private boolean isCalibrating = false;
	int stepCount = 0, peakCount = 0, valleyCount = 0;
	private ArrayList<Double> peak_list; //record the peak value
	private ArrayList<Double> interval_list; //record the intervals between peaks and valleys
	final double defaultPeakTH = 0.0;
	final double defaultIntervalTH = 0.16;
	
	double Peak_TH = defaultPeakTH;
	double Interval_TH = defaultIntervalTH;
	Long lastPeakTime = 0l;
	double lastPeakValue = 0.0;
	double lastValleyValue = 0.0;
	Tool tool;
	
	
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
        mInitialized=false;
        lowPassFIR_filter = new LowPassFIR(0.09);
        lowPassFIR_integrate = new LowPassFIR(0.09);
        
        highPassFIR = new HighPassFIR(30, 0.9f);
        tool = new Tool();
    	   
    }
    public void pushdata(double xR,double yR,double zR)
    {
    	Log.d("Function Call","Inside Step Counter X:"+xR+" Y:"+yR+" Z:"+zR);
//     	textTH.setText("TH="+Double.toString(Peak_TH));
    	// event object contains values of acceleration

    	
    	final double alpha = 0.8;		// Constant for our filter below
    	
    	double[] gravity = {0,0,0};
    	
    	// Isolate the force of gravity with low-pass filter.
    	gravity[0] = alpha * gravity[0] + (1 - alpha) * xR;
    	gravity[1] = alpha * gravity[1] + (1 - alpha) * yR;
    	gravity[2] = alpha * gravity[2] + (1 - alpha) * zR;
    	
    	// Remove gravity contribution with high-pass filter
    	double x = xR - gravity[0];
    	double y = yR - gravity[1];
    	double z = zR - gravity[2];
    	
    	if (!mInitialized){
    		// Sensor is used for the first time, initialize the last read values
    		mLastX = x;
    		mLastY = y;
    		mLastZ = z;
    		mInitialized = true;
    	} else {
    		// Sensor already initialized, and we have previously read values.
    		// Take difference of past and current values and decide which axis
    		// acceleration was detected on by comparing values
    		double deltaX = Math.abs(mLastX - x);
    		double deltaY = Math.abs(mLastY - y);
    		double deltaZ = Math.abs(mLastZ - z);
    		
    		if (deltaX < NOISE)
    			deltaX = (float) 0.0;
    		if (deltaY < NOISE)
    			deltaY = (float) 0.0;
    		if (deltaZ < NOISE)
    			deltaZ = (float) 0.0;
    		
    		//AccelData data = new AccelData(deltaX, deltaY, deltaZ);
    		//AccelData filtered_data = highPassFIR.transform(deltaX, deltaY, deltaZ);
    		
    		//double currentReading = tool.norm(data.getX(), data.getY(), data.getZ());
    		double currentReading = lowPassFIR_integrate.integrate1(Math.pow((lowPassFIR_filter.transform_2(tool.norm(deltaX, deltaY, deltaZ))), 2));
    		long currentTime = System.currentTimeMillis();
    		
    		//Log.i("aaa", lastReading + " ? " + currentReading);
    		
    		if (largestReading < currentReading){
    			largestReading = currentReading;
    		}
    		
    		if(climbing && (lastReading > currentReading)){
    			//Log.i("test", "" + (currentTime - lastPeakTime));
    			if(isCalibrating){
    				peak_list.add(lastReading);
    				interval_list.add(lastReading - lastValleyValue);
    			}
    			peakCount++;
    			//textPN.setText("P_N=" + peakCount);
    			
    			if(lastReading > Peak_TH 
    				&& ((currentTime - lastPeakTime) > 0)        //set the threshold of interval of peaks' timestamps
    				&& (lastReading - lastValleyValue) > Interval_TH) //set the threshold of interval between peak and valley
    				{
						stepCount++;
						mCallback.stepEvent();
					}
    			
    			climbing = false;
    			descending = true;
    			lastPeakTime = currentTime;
    			lastPeakValue = lastReading;
    		}
    		
    		if(descending && lastReading < currentReading){
    			climbing = true;
    			descending = false;
    			valleyCount++;
    			//textVN.setText("V_N=" + valleyCount);
    			lastValleyValue = lastReading;
    		}
    		
    		lastReading = currentReading;
    		
    		
    	}
    	//mCallback.stepEvent();
    	
    }
}
