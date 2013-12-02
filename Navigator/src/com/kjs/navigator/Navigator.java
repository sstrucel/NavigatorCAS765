package com.kjs.navigator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


import com.kjs.navigator.R;
import com.kjs.navigator.StepCounter.OnStepEventListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.TileView.TileViewEventListener;
import com.qozix.tileview.markers.MarkerEventListener;
import com.qozix.tileview.paths.DrawablePath;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Navigator extends Activity implements SensorEventListener, OnStepEventListener{

	private TileView tileView;
	private ImageView naviSymbol;
	private int currentX;
	private int currentY;
	private int roundedHeading;
	private double scale=1;
	private boolean gettingInitialPoint=false;
	private boolean gettingSecondaryPoint=false;
	private boolean firstAddSymbol=true;
	private ImageView startSymbol;
	private ImageView headingSymbol;
	private DrawablePath startPath;
	private StepCounter stepCounter;
	//private TileViewEventListener tileEventListener;

	//##### Public Variables #####

	private double CMPERPIXEL = 4.03;
	private int numParticles = 50; 			// Particle count
	private double[] stepLength= new double[numParticles]; //step length in cm 
	private Point startLocation;
	private Point headingLocation;
	//private Point[] particles = new Point[10];
	private Point currentLocation=new Point(0,0);
	private Polygon ITBhalls;

	private double previousStepTimestamp = 0;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer, mCompass;
	
	private int upNumber=0;
	
	//########### CONSTANTS###################
	private final double PIXELS_PER_METER= 90/10; //pixels in measurement/ meters in measurement
	private final double MAP_NORTH= 90;//Value of compass when pointing north on our map
	private final int UPDATE_PACE=50;
	
	//need to create initial particles X and Y cloud around initial position
	private Point[] particles = new Point[numParticles];
	double[] particleA = new double[numParticles];
	double[] particleB = new double[numParticles];
	double locationMean = 0.0;	//Mean will almost always be zero for location
	double locationStd = 25;		//standard deviation in pixels

	double aMean = 20.0;		//from figure 6 in paper, approx mean of slope
	double aStd = 10;			//guess at slope std

	double bMean = 30;			//from figure 6 in paper, approx value of offset
	double bStd = 10;			//guess at offset std

	Random rng = new Random();	//Random Number Generator spreads our particles in a gaussian distribution.

	//need to have a past history of heading values, maybe shift new values in
	ArrayList<Float> rawHeading = new ArrayList<Float>();
	int rawHeadingMAX = 9; 
	ArrayList<Float> headingAtStep = new ArrayList<Float>();
	int headingAtStepMAX = 50;

	int stepsSinceLastTurn = 0;
	double deltaHeadingThreshold = Math.PI/2;
	double averagePastHeading = 0; //TODO needs to be initialized it heading  
	float currentHeading = 0;
	private boolean startLocationEntered=false;

	private DrawablePath particlePath;
	private boolean firstPath=true;
	private DrawablePath badPath;
	
	PendingIntent intent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigator);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		tileView = new TileView( this );
		setContentView( tileView );
		// size of original image at 100% scale
		tileView.setSize( 2048, 1887 );

		// detail levels
		tileView.addDetailLevel( 1.000f, "tiles/3/%col%/%row%.png", "downsamples/map.png");
		tileView.addDetailLevel( 0.500f, "tiles/2/%col%/%row%.png", "downsamples/map.png");
		tileView.addDetailLevel( 0.250f, "tiles/1/%col%/%row%.png", "downsamples/map.png");
		tileView.addDetailLevel( 0.125f, "tiles/0/%col%/%row%.png", "downsamples/map.png");

		// let's use 0-1 positioning...
		tileView.defineRelativeBounds( 0, 0, 2048,  1887);

		// center markers along both axes
		tileView.setMarkerAnchorPoints( -0.5f, -0.5f );

		// add a marker listener
		//tileView.addMarkerEventListener( markerEventListener );

		tileView.addTileViewEventListener(tileEventListener);

		stepCounter= new StepCounter(this);
		// add some pins...
		//roundedHeading=0;
		//currentX=200;
		//currentY=200;
		Log.d("Created", "Current Position "+currentLocation.toString());
		naviSymbol = new ImageView( this );
		//naviSymbol.setImageBitmap(getBitmapFromAssets("pointer/naviPointer-"+0+".png"));
		firstAddSymbol=true;
		updateLocal(currentLocation.x,currentLocation.y,currentHeading);
		firstAddSymbol=false;
		Log.d("Created", "Current Position "+currentLocation.toString());
		//updateLocal(currentX,currentY,roundedHeading);
		// scale it down to manageable size
		tileView.setScale( 0.5 );
		// center the frame
		frameTo( 0.5, 0.5 );

		ITBhalls = Polygon.Builder()
				.addVertex(new Point(318, 154))
				.addVertex(new Point(397, 154))
				.addVertex(new Point(397, 275))
				.addVertex(new Point(1808, 275))
				.addVertex(new Point(1808, 1574))
				.addVertex(new Point(1463, 1574))
				.addVertex(new Point(1463, 1654))
				.addVertex(new Point(1164, 1654))
				.addVertex(new Point(1164, 1573))
				.addVertex(new Point(207, 1573))
				.addVertex(new Point(207, 1486))
				.addVertex(new Point(880, 1486))
				.addVertex(new Point(880, 965))
				.addVertex(new Point(1180, 965))
				.addVertex(new Point(1180, 890))
				.addVertex(new Point(1086, 890))
				.addVertex(new Point(1086, 860))
				.addVertex(new Point(1207, 860))
				.addVertex(new Point(1207, 950))
				.addVertex(new Point(1346, 950))
				.addVertex(new Point(1346, 1487))
				.addVertex(new Point(1736, 1487))
				.addVertex(new Point(1736, 340))
				.addVertex(new Point(422, 340))
				.addVertex(new Point(422, 516))
				.addVertex(new Point(318, 516))
				.close()
				.addVertex(new Point(920, 1005))
				.addVertex(new Point(920, 1490))
				.addVertex(new Point(1277, 1490))
				.addVertex(new Point(1277, 1005))
				.build();
		ArrayList<double[]> itbpointsOuter = new ArrayList<double[]>();
		{
			itbpointsOuter.add( new double[] { 318, 154 } );//1
			itbpointsOuter.add( new double[] { 397, 154 } );
			itbpointsOuter.add( new double[] { 397, 275 } );
			itbpointsOuter.add( new double[] { 1808, 275 } );
			itbpointsOuter.add( new double[] { 1808, 1574 } );
			itbpointsOuter.add( new double[] { 1463, 1574 } );
			itbpointsOuter.add( new double[] { 1463, 1654 } );
			itbpointsOuter.add( new double[] { 1164, 1654 } );
			itbpointsOuter.add( new double[] { 1164, 1573 } );
			itbpointsOuter.add( new double[] { 207, 1573 } );
			itbpointsOuter.add( new double[] { 207, 1486 } );
			itbpointsOuter.add( new double[] { 880, 1486 } );
			itbpointsOuter.add( new double[] { 880, 965 } );
			itbpointsOuter.add( new double[] { 1180, 965 } );
			itbpointsOuter.add( new double[] { 1180, 890 } );
			itbpointsOuter.add( new double[] { 1086, 890 } );
			itbpointsOuter.add( new double[] { 1086, 860 } );
			itbpointsOuter.add( new double[] { 1207, 860 } );
			itbpointsOuter.add( new double[] { 1207, 950 } );
			itbpointsOuter.add( new double[] { 1346, 950 } );
			itbpointsOuter.add( new double[] { 1346, 1487 } );
			itbpointsOuter.add( new double[] { 1736, 1487 } );
			itbpointsOuter.add( new double[] { 1736, 340 } );
			itbpointsOuter.add( new double[] { 422, 340 } );
			itbpointsOuter.add( new double[] { 422, 516 } );
			itbpointsOuter.add( new double[] { 318, 516 } );	
			itbpointsOuter.add( new double[] { 318, 154 } );
		}
		ArrayList<double[]> itbpointsInner = new ArrayList<double[]>();
		{
			itbpointsInner.add( new double[] { 920, 1005 } );//1
			itbpointsInner.add( new double[] { 920, 1490} );
			itbpointsInner.add( new double[] { 1277, 1490 } );
			itbpointsInner.add( new double[] { 1277, 1005 } );
			itbpointsInner.add( new double[] { 920, 1005 } );
		}

		tileView.drawPath(itbpointsOuter);
		tileView.drawPath(itbpointsInner);

		intent = PendingIntent.getActivity(this.getBaseContext(), 0,
	            new Intent(getIntent()), getIntent().getFlags());
	}// End onCreate




	//######### Custom Functions #############

	public void inputStartLocation()
	{
		if (!gettingInitialPoint && !gettingSecondaryPoint)
		{
			Log.d("Function Call","Input Start");

			gettingInitialPoint=true;
			//startLocationEntered=false;
			//Log.d("Function Call","InputStart Finished");
		}
	}

	private void initializeParticleFilter(){

		//Generate particle cloud around starting location
		for (int i = 0; i<numParticles; i++) {
			float x = (float) (startLocation.x + locationMean + locationStd * rng.nextGaussian());
			float y = (float) (startLocation.y + locationMean + locationStd * rng.nextGaussian());
			particles[i] = new Point(x,y);

			particleA[i] = aMean + aStd * rng.nextGaussian();
			particleB[i] = bMean + bStd * rng.nextGaussian();
		}

	}

	public void createStartSymbol(int x, int y)
	{
		startSymbol = new ImageView( this);
		startSymbol.setImageResource( R.drawable.start );
		getTileView().addMarker(startSymbol, x, y );
		startLocation = new Point(x,y);   
		currentLocation=new Point(x,y);   
	}
	public void createHeadingSymbol(int x, int y)
	{
		headingSymbol = new ImageView( this);
		headingSymbol.setImageResource( R.drawable.heading );
		getTileView().addMarker(headingSymbol, x, y );
		headingLocation = new Point(x,y);  
		ArrayList<double[]> points = new ArrayList<double[]>();
		{
			points.add( new double[] { headingLocation.x, headingLocation.y } );
			points.add( new double[] { startLocation.x,  startLocation.y } );
		}
		startPath=tileView.drawPath(points);

	}
	public boolean initializeStartHeading()
	{
		int headingDeg=findHeading(startLocation.x,startLocation.y, headingLocation.x,headingLocation.y);
		Log.d("xHeading Created","Start Location X:"+startLocation.x+" Y:"+startLocation.y+" Heading Angle:"+headingDeg);
		for (int i=0;i<rawHeadingMAX;i++)
		{
			rawHeading.add((float) headingDeg);
			if (rawHeading.size() > rawHeadingMAX ){
				rawHeading.remove(0);
			}
		}
		currentHeading=headingDeg;
		if (ITBhalls.contains(startLocation))
		{
			updateLocal(startLocation.x,startLocation.y,headingDeg);
			return true;
		}
		else
		{
			return false;
		}
	}
	public void updatePosition()
	{
		int stepLength=10;
		currentLocation.x=(int)(currentLocation.x+stepLength*Math.sin(currentHeading*Math.PI/180));
		currentLocation.y=(int)(currentLocation.y-stepLength*Math.cos(currentHeading*Math.PI/180));
		Log.d("xPosition Updated","Current Location X:"+currentLocation.x+" Y:"+currentLocation.y+" Heading Angle:"+currentHeading);
		updateLocal(currentLocation.x,currentLocation.y,currentHeading);
	}
	public int findHeading(float x,float y, float x2,float y2)
	{
		double dX= x-x2;
		double dY= y-y2;
		//return (int)((Math.atan2(dY, dX)*180)/Math.PI);
		double arcTanVal=(Math.atan2(dY, dX)*180)/Math.PI;

		//return (int)(270-arcTanVal);
		double newVal= (360-(-arcTanVal+180)+90);
		//Log.d("Heading Raw","Heading Raw A:"+newVal);
		if (newVal>360)
		{
			newVal=newVal-360;
		}
		//Log.d("Heading Raw","Heading Raw B:"+newVal);
		return (int)newVal;
	}
	public void cleanupSymbols()
	{
		tileView.removeMarker(startSymbol);
		tileView.removeMarker(headingSymbol);
		tileView.removePath(startPath);
	}
	public void reset()
	{
		Log.d("Function Call","Reset");
		stop();
		AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
		System.exit(2);
		//Reset Variables

	}
	public void start()
	{
		Log.d("Function Call","Start");
		inputStartLocation();
		//mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		//mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_GAME);
	}
	public void startSensors()
	{
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_GAME);
	}
	private void stop() {
		// TODO Auto-generated method stub
		Log.d("Function Call","Stop");
		mSensorManager.unregisterListener(this, mAccelerometer);
		mSensorManager.unregisterListener(this, mCompass);
	}
	/*
	private void addPin( double x, double y ) {
		ImageView imageView = new ImageView( this );
		imageView.setImageResource( R.drawable.push_pin );
		getTileView().addMarker( imageView, x, y );
	}*/
	private double pixelsToMeters(double pixels)
	{
		return pixels*PIXELS_PER_METER;
	}
	private void updateLocal( double x, double y ,double angle) {
		if (!firstAddSymbol)
		{
			getTileView().removeMarker(naviSymbol);

		}
		//naviSymbol = new ImageView( this );
		//naviSymbol.setImageBitmap(getBitmapFromAssets("pointer/naviPointer-"+angle+".png"));
		//naviSymbol.setImageResource( R.drawable.smallpoint);
		naviSymbol.setImageBitmap(getBitmapFromAssets("smallpointer/naviPointer-"+(int)angle+".png"));
		getTileView().addMarker( naviSymbol, x, y );
		firstAddSymbol=false;
	}

	private MarkerEventListener markerEventListener = new MarkerEventListener() {
		@Override
		public void onMarkerTap( View v, int x, int y ) {
			Toast.makeText( getApplicationContext(), "You are here", Toast.LENGTH_LONG ).show();
		}		
	};


	public Bitmap getBitmapFromAssets(String fileName) {
		AssetManager assetManager = getAssets();

		InputStream istr = null;
		try {
			istr = assetManager.open(fileName); 
		} catch (IOException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Bitmap bitmap = BitmapFactory.decodeStream(istr);

		return bitmap;
	}
	//################## END OF CUSTOM FUNCTIONS #####################



	///##################SENSOR BEHAVIOUR############################
	@Override
	public void onSensorChanged(SensorEvent arg0) {
		if (arg0.sensor == mAccelerometer)
			stepCounter.pushdata(arg0.values[0], arg0.values[1], arg0.values[2]);
		else if (arg0.sensor == mCompass){
			if (rawHeading.size() > rawHeadingMAX ){
				rawHeading.remove(0);
			} 
			//Adjust heading reading by offset
			rawHeading.add((float)(arg0.values[0]+MAP_NORTH)%360);
			fixCompassReadings();
			//Log.i("Raw headings","Size:"+rawHeading.size());
			if (rawHeading.size() == rawHeadingMAX+1 )
				{
				//Log.i("HEADING UPDATE","Size:"+rawHeading.size());
				upNumber=(upNumber+1)%UPDATE_PACE;
				if (upNumber==0)
				{
				currentHeading = ( ( rawHeading.get(4) + rawHeading.get(5) + rawHeading.get(6) ) / 3 );
				updateLocal(currentLocation.x,currentLocation.y,currentHeading);
				}
				
				}
		}

	}

	//#################### TILE TOUCH BEHAVIOUR #######################
	private TileViewEventListener tileEventListener = new TileViewEventListener() {

		@Override
		public void onDetailLevelChanged() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDoubleTap(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDrag(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFingerDown(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFingerUp(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFling(int arg0, int arg1, int arg2, int arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onFlingComplete(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPinch(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPinchComplete(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPinchStart(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRenderComplete() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRenderStart() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScaleChanged(double arg0) {
			// TODO Auto-generated method stub
			scale=arg0;
		}

		@Override
		public void onScrollChanged(int arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTap(int arg0, int arg1) {
			// TODO Auto-generated method stub
			if (gettingInitialPoint)
			{
				Log.d("Tap Event","X:"+arg0+" Y:"+arg1);
				int scaledX=(int)(arg0/scale);
				int scaledY=(int)(arg1/scale);
				Log.d("Tap Event","Scaled X:"+scaledX+" Scaled Y:"+scaledY);
				createStartSymbol(scaledX,scaledY);
				gettingInitialPoint=false;
				gettingSecondaryPoint=true;
			}
			else if (gettingSecondaryPoint)
			{
				Log.d("Tap Event","Secondary X:"+arg0+" Y:"+arg1);
				int scaledX=(int)(arg0/scale);
				int scaledY=(int)(arg1/scale);
				Log.d("Tap Event","Secondary Scaled X:"+scaledX+" Scaled Y:"+scaledY);
				createHeadingSymbol(scaledX,scaledY);
				gettingSecondaryPoint=false;
				//Wipe the Symbols
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// Do something after 5s = 5000ms
						cleanupSymbols();
						//initializeStartHeading();
						if(initializeStartHeading())
						{
							initializeParticleFilter();
							startSensors();
						}
						else
						{
							new AlertDialog.Builder(Navigator.this)
							.setTitle("Position out of Bounds!")
							.setMessage("You have selected an invalid starting position. Click OK to re-enter your starting location")
							.setNegativeButton("Ok",new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,int id) {
									// if this button is clicked, just close
									// the dialog box and do nothing
									inputStartLocation();
									dialog.cancel();

								}
							})
							.create()
							.show();
						}


					}
				}, 1000);
				//cleanupSymbols();
				//initializeParticleFilter();
			}

		}

		@Override
		public void onZoomComplete(double arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onZoomStart(double arg0) {
			// TODO Auto-generated method stub

		}

	};

	//########## PREMADE STUFF ###############
	//Options Premade
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigator, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
//		case R.id.action_inputStart:
//			inputStartLocation();
//			return true;
		case R.id.action_reset:
			reset();
			return true;
		case R.id.action_start:
			start();
			return true;
		case R.id.action_stop:
			stop();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}





	//Sensor Premade
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}
	// TILE Premade
	public TileView getTileView(){
		return tileView;
	}
	/**
	 * This is a convenience method to moveToAndCenter after layout (which won't happen if called directly in onCreate
	 * see https://github.com/moagrius/TileView/wiki/FAQ
	 */
	public void frameTo( final double x, final double y ) {
		getTileView().post( new Runnable() {
			@Override
			public void run() {
				getTileView().moveToAndCenter( x, y );
			}			
		});		
	}




	public void stepEvent() {
		// TODO Auto-generated method stub
		Log.d("Step event","Step Event triggered");
		stepTaken(System.currentTimeMillis()/1000); 
		//updatePosition();

	}
	public void fixCompassReadings()
	{
		float shift=findshift();
		for (int i=0;i<rawHeading.size();i++)
		{
			rawHeading.set(i, (rawHeading.get(i)+360-shift)%360);
		}
	}
	public float findshift()
	{
		float first =shiftQuadrants(0);
		float second =shiftQuadrants(first);	
		if (first==second)
		{
			return first;
		}
		while (first!=second)
		{
			first =shiftQuadrants(second);
			second =shiftQuadrants(first);	
		}
		return first;
	}
	public float shiftQuadrants(float p)
	{
		//Find min and Max of array
		float max=0;
		float min=360;
		for (int i=0;i<rawHeading.size();i++)
		{
			if (rawHeading.get(i)>max)
			{
				max=rawHeading.get(i);
			}
			if (rawHeading.get(i)<min)
			{
				min=rawHeading.get(i);
			}

		}
		//If differ by 180 +90 to the mix 
		if (Math.abs(max-min)>180)
		{
			for (int i=0;i<rawHeading.size();i++)
			{
				rawHeading.set(i, (rawHeading.get(i)+90)%360);
			}
			return (p+90)%360;
		}
		return p%360;
	}
	public void logCompass()
	{
		for (int i=0;i<rawHeading.size();i++)
		{
			Log.d("Compass Readings","Reading "+i+": "+rawHeading.get(i));
		}
	}

	public void stepTaken(double currentStepTimestamp){
		//First, we shall try to determine if the user has turned

		//Perform filter on heading directions
		//Not sure how that is done
		//sort recent heading values
		//logCompass();
		fixCompassReadings();
		//Log.d("Compass Shift","Shift: "+shift); 
		//logCompass();
		//Float[] sortedHeading = (Float[]) rawHeading.toArray();
		Collections.sort(rawHeading);
		//Arrays.sort(sortedHeading);
		//logCompass();

		//TODO headingAtCurrentStep[] = ( ( sortedHeading[4] + sortedHeading[5] + sortedHeading[6] ) / 3 );
		//minHeading=Math.min(d1, d2)
		currentHeading = ( ( rawHeading.get(4) + rawHeading.get(5) + rawHeading.get(6) ) / 3 );
		Log.d("Compass Heading","Heading: "+currentHeading);
		headingAtStep.add(currentHeading);

		stepsSinceLastTurn++;
		double headingSum = 0;

		for (int i = 0; i<headingAtStep.size(); i++) {
			headingSum += headingAtStep.get(i);
		}
		double averagePastHeading = headingSum / stepsSinceLastTurn;

		double deltaHeading = headingAtStep.get(headingAtStep.size() - 1) - averagePastHeading;

		if (deltaHeading > deltaHeadingThreshold) {
			stepsSinceLastTurn = 0;
			headingAtStep.removeAll(headingAtStep);
		}

		//currentHeading = averagePastHeading;

		int totalX = 0;
		int totalY = 0;

		// calculate step frequency from step period
		double period = currentStepTimestamp-previousStepTimestamp ;
		previousStepTimestamp = currentStepTimestamp;


		//Second, we update all of the particles, we also compute the average X and Y at this point
		for (int i = 0; i<numParticles; i++) {
			stepLength[i] = ( particleA[i] * 1/period) + particleB[i];
			//Log.i("Step Length", "Length:"+stepLength[i]);

			float x = (float) (particles[i].x + ( ( stepLength[i] ) * Math.sin(currentHeading*Math.PI/180) ));
			float y = (float) (particles[i].y - ( ( stepLength[i] ) * Math.cos(currentHeading*Math.PI/180) ));
			particles[i] = new Point(x,y);	

			totalX += particles[i].x;
			totalY += particles[i].y;
		} 

		int averageX = totalX / numParticles;
		int averageY = totalY / numParticles;

		totalX = 0;
		totalY = 0;

		int survivingParticles = 0;
		int[] particleNeedsReplacing = new int[numParticles];
		float survivingAverageX;
		float survivingAverageY;
		ArrayList<double[]> points = new ArrayList<double[]>();
		ArrayList<double[]> badPoints = new ArrayList<double[]>();
		
		for (int i = 0; i < numParticles; i++) {

			if (!ITBhalls.contains(particles[i])) {
				badPoints.add( new double[] { particles[i].x, particles[i].y } );
				particleNeedsReplacing[i] = 1;
			}
			else {
				survivingParticles ++;
				particleNeedsReplacing[i] = 0;
				points.add( new double[] { particles[i].x, particles[i].y } );
				totalX += particles[i].x;
				totalY += particles[i].y;
			}
		}

		if (survivingParticles>0)
		{
			survivingAverageX = totalX / survivingParticles;
			survivingAverageY = totalY / survivingParticles;
		}
		else
		{
			survivingAverageX=currentLocation.x;
			survivingAverageY=currentLocation.y;
		}
		survivingParticles = 0;

		for (int i = 0; i < numParticles; i++) {
			while (particleNeedsReplacing[i] == 1) {
				float x = (float) (survivingAverageX + 0.1*locationStd * rng.nextGaussian());
				float y = (float) (survivingAverageY + 0.1*locationStd * rng.nextGaussian());
				particles[i] = new Point(x,y);

				if (stepsSinceLastTurn<=4) {
					float currClosest = 50;
					int closestParticle=0;
					for (int j = 0; j < numParticles; j++) {
						if ((particleNeedsReplacing[j] == 0) && (i != j)) {
							float deltaX = particles[i].x - particles[j].x;
							float deltaY = particles[i].y - particles[j].y;
							float distance = (float) Math.sqrt ((deltaX*deltaX + deltaY*deltaY));
							if (distance < currClosest) {
								currClosest = distance;
								closestParticle = j;
							}
						}
					}
					particleA[i]=particleA[closestParticle];
					particleB[i]=particleB[closestParticle];
					//particles[i].a = particles[closestParticle].a;
					//particles[i].b = particles[closestParticle].b; 
				}
				
				if(ITBhalls.contains(particles[i]))
				{
					particleNeedsReplacing[i] = 0;     
					totalX += particles[i].x;
					totalY += particles[i].y;
				}

			}
		}
		averageX = totalX / numParticles;
		averageY = totalY / numParticles;

		currentLocation = new Point (averageX, averageY);
		//DEBUG PARTICLES
		if (firstPath)
		{
			firstPath=false;
		}
		else
		{
			tileView.removePath(particlePath);
			tileView.removePath(badPath);
		}
		Paint r= new Paint();
		Paint g= new Paint();
		r.setColor(Color.RED);
		g.setColor(Color.GREEN);
		if(points.size()>0)
		{
			particlePath = tileView.drawPath(points,g);
			Log.i("Good Points",points.toArray().toString());
		}

		if(badPoints.size()>0) 
		{
			badPath = tileView.drawPath(badPoints,r);
			Log.i("Bad Points",badPoints.toArray().toString());
		}
		//tileView.drawPath(positions, paint)

		Log.d("Current Location","X: "+currentLocation.x+" Y:"+currentLocation.y);
		updateLocal(currentLocation.x,currentLocation.y,currentHeading);



		//TODO: plot current position
	}


	//############## Unused Stuff #####################
	/*
	private HotSpot hotMap;
	private HotSpotEventListener startHotSpotEventListener = new HotSpotEventListener() {
		@Override
		public void onHotSpotTap( HotSpot h, int x, int y ) {
			Log.d("Variables", "Initial Position X: "+x+" Y: "+y);
			//tileView.removeHotSpot(hotMap);


		}		
	};*/
	//HotSpot allMap= new HotSpot(0,0,2048,1887);
	//allMap.setHotSpotEventListener(startHotSpotEventListener);
	/*ArrayList<double[]> points = new ArrayList<double[]>();
	{
		points.add( new double[] { 0, 0 } );
		points.add( new double[] { 0, 100 } );
		points.add( new double[] { 100, 0 } );
		points.add( new double[] { 100, 100 } );
	}
	hotMap=tileView.addHotSpot(points, startHotSpotEventListener);
	 */

}
