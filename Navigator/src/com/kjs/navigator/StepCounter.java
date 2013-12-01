package com.kjs.navigator;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.kjs.navigator.Tool;
import com.kjs.navigator.HighPassFIR;
import com.kjs.navigator.LowPassFIR;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
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
	private ArrayList<Long> stepPeriodList; //record the peak value
	final double defaultPeakTH = 0.0;
	final double defaultIntervalTH = 0.16;
	private int stepsTaken=0;
	double Peak_TH = defaultPeakTH;
	double Interval_TH = defaultIntervalTH;
	Long lastPeakTime = 0l;
	double lastPeakValue = 0.0;
	double lastValleyValue = 0.0;
	Tool tool;
	private long currentStepTime;
	private long previousStepTime;
	private boolean firstStep=true;
	private Timer filterTimer;
	private TimerTask timer;
	private boolean firstStart=true;
	Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String text = (String)msg.obj;
            //call setText here
            mCallback.stepEvent();
        }
	};
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
		filterTimer = new Timer(); 
		timer= new TimerTask(mCallback);
		new Thread(timer).start();
		stepPeriodList= new ArrayList<Long>();

	}
	public void pushdata(double xR,double yR,double zR)
	{
		//Log.d("Function Call","Inside Step Counter X:"+xR+" Y:"+yR+" Z:"+zR);
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
					//mCallback.stepEvent();
					stepFilter();
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

	public void stepFilter()
	{
		stepsTaken++;
		if (firstStep)
		{
			currentStepTime=System.currentTimeMillis();
			Log.d("First Step Time","C:"+currentStepTime);
			firstStep=false;
		}
		else
		{
			previousStepTime=currentStepTime;
			currentStepTime=System.currentTimeMillis();
			long timeBetweenSteps=currentStepTime-previousStepTime;
			//Log.d("Times","C:"+currentStepTime+"P:"+previousStepTime);
			//Log.d("TimeDiff",""+timeBetweenSteps);
			stepPeriodList.add(timeBetweenSteps);
			if (stepPeriodList.size()>10) stepPeriodList.remove(0);
			//Find average time between steps
			long total=0;
			for(int i=0; i<stepPeriodList.size();i++){
				total+=stepPeriodList.get(i);
			}
			long averageTime=total/stepPeriodList.size();
			//Log.d("TimeAvg","total:"+total+" size:"+stepPeriodList.size()+"avg: "+averageTime);
			if(averageTime>100)
			{
				if (timeBetweenSteps>averageTime*3)
				{
					firstStep=true;
					firstStart=true;
					stepPeriodList.clear();
					timer.cancel();
					return;
				}
				//Log.d("AverageTime",""+averageTime);
				if(firstStart)timer.start(averageTime);
				else timer.changePeriod(averageTime);

			}
		}
	}
	class TimerTask implements Runnable {

		private long x;
		private long p;
		private boolean cancel;
		private int steps;
		OnStepEventListener mCallback;

		public TimerTask (OnStepEventListener call,long p)
		{
			this.mCallback=call;
			this.x=System.currentTimeMillis();
			this.p=p;
			steps=0;
			cancel=false;
		}
		public TimerTask (OnStepEventListener call)
		{
			this.mCallback=call;
			this.x=System.currentTimeMillis();
			cancel=true;
		}
		@Override
		public void run() {
			while (true){
				if(!cancel)
				{
					if(System.currentTimeMillis()>(x+p))
					{
						//Fire event and increase x
						//mCallback.stepEvent();
						if (steps<2)
						{
							//Log.i("Step Fired","Step Callback Should be Invoked");
							//mCallback.stepEvent();
							Message msg = new Message();
							String textTochange = "text";
							msg.obj = textTochange;
							mHandler.sendMessage(msg);
							steps++;
						}
						x=x+p;
					}
				}
			}

		}
		public void changePeriod(long p)
		{
			this.p=p;
			steps=0;
		}
		public void cancel()
		{
			cancel=true;
		}
		public void start(long p)
		{
			this.x=System.currentTimeMillis();
			this.p=p;
			steps=0;
			cancel=false;
		}

	}



}
