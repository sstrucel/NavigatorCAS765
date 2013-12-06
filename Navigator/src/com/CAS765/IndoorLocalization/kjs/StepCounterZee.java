package com.CAS765.IndoorLocalization.kjs;


import java.util.LinkedList;
import java.util.List;

import android.app.Activity;


public class StepCounterZee {
	public interface OnStepEventListener {
		public void stepEvent();
	}

	private OnStepEventListener mCallback;
	public StepCounterZee(Activity activity){
		try {
			mCallback = (OnStepEventListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnHeadlineSelectedListener");
		}


	}

	// --------------------------------------------------------------------
	// These are the variables needed for our calculations
	public static final int 
	INACTIVE = 0, 
	WALKING = 1,
	MAXDATA = 200;						//the maximum amount of data points saved

	private int state = INACTIVE;    			// maybe either 0, for inactive, or 1, for walking

	//List<Double> v = new LinkedList<Double>();	// Array of maximum auto-correlation values across samples
	//double v = 0;
	// Current data input sample 'm'
	List<Double> accelData  = new  LinkedList<Double>();	// Input acceleration data, assumed in g

	private int T = 0,
			Topt = 70,					// Optimal tau value, changed in program
			Tmin = 40,					// Minimum tau value
			Tmax = 100, 				// Maximum tau value
			samplesWalking = 0;			// m for simplicity is set for currentsamples;
	// --------------------------------------------------------------------
	
	public void pushdata(double xR,double yR,double zR)
	{
		// calculate magnitude of acceleration.
		double accelX = xR;
		double accelY = yR;
		double accelZ = zR;

		double accelMag = Math.sqrt(Math.pow(accelX,2) + Math.pow(accelY,2) + Math.pow(accelZ,2));

		accelData.add(accelMag);

		if (accelData.size() > MAXDATA)
			accelData.remove(0);
		else
			return; 		// 

		List<Double> x = new  LinkedList<Double>();		// Array of normalized auto-correlation values for this sample
		double numerator, denominator;
		numerator=0;

		for (T = Tmin; T <= Tmax; T++)		// Loop through search window
		{
			double totalNumerator = 0;
			double mean1 = mean(0,T),
					mean2 = mean(T,T);
			for (int k = 0; k<= T-1; k++)	// Calculate numerator sum, mean still needs to be done
			{
				numerator = (accelData.get(k) - mean1) * (accelData.get(k+T) - mean2); 	
				totalNumerator = totalNumerator + numerator;
			}

			denominator = T * (stdDev(0,T,mean1)) * (stdDev(T,T,mean2));	//  Calculate denominator, stdDev needs to be figured out
			x.add( totalNumerator / denominator);	// Put values of x in arrayList
		}
		double v=0;

		for (int i = 0; i < x.size(); i++ )	// Find maximum value in x, and set v(m) and Topt accordingly
		{
			if (Math.abs(x.get(i)) >v)
			{
				v =  Math.abs(x.get(i));
				Topt = i + Tmin;
			}
		}

		Tmin = Topt - 10;				// Set Tmin / Tmax, in paper they used +/-10
		if (Tmin<=40) {Tmin=40;}
		Tmax = Topt + 10;
		if (Tmax>=100) {Tmax=100;}


		if (stdDev(0,Tmax,mean(0,Tmax)) < 0.01*9.8)		// If standard deviation of current data is <0.01, set state back to IDLE
		{
			state = INACTIVE;
			samplesWalking = 0;			// Also reset samples spent walking
		}
		else if (v > 0.7)
		{
			state = WALKING;			// If v for current sample is above 0.7, state is now WALKING
		}


		if (state == WALKING)			// When in walking state, do this
		{
			samplesWalking++;			// Increment samples spent walking
			if (samplesWalking > (Topt/2))		// 
			{
				samplesWalking = 0;
				//Fire stepEvent
				mCallback.stepEvent();
			}
		}	

	}
	public double mean (int start, int range)
	{
		double sum=0;
		for(int i=0;i<range;i++)
		{
			sum+=accelData.get(start+i);
		}
		return sum*1.0/range;
	}
	public double stdDev(int start, int range, double mean)
	{
		double sum=0;
		//double mean= mean(start,range);
		int n=0;
		for(int i=0;i<range;i++)
		{
			sum+=(accelData.get(start+i)-mean)*(accelData.get(start+i)-mean);
			n++;
		}
		return Math.sqrt(sum/n);
	}
}
