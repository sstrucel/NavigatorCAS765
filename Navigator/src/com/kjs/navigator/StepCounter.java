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
		
		// 	textTH.setText("TH="+Double.toString(Peak_TH));
    	// event object contains values of acceleration
    	double x = event.values[0];
    	double y = event.values[1];
    	double z = event.values[2];
    	
    	final double alpha = 0.8;		// Constant for our filter below
    	
    	double[] gravity = {0,0,0};
    	
    	// Isolate the force of gravity with low-pass filter.
    	gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
    	gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
    	gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
    	
    	// Remove gravity contribution with high-pass filter
    	x = event.values[0] - gravity[0];
    	y = event.values[1] - gravity[1];
    	z = event.values[2] - gravity[2];
    	
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
    		
    		Log.i("aaa", lastReading + " ? " + currentReading);
    		
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
    			textPN.setText("P_N=" + peakCount);
    			
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
    			textVN.setText("V_N=" + valleyCount);
    			lastValleyValue = lastReading;
    		}
    		
    		lastReading = currentReading;
    		
    		
    	}
		
		
    	mCallback.stepEvent();
    	
    }
}
