package com.kjs.navigator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


import com.kjs.navigator.R;
import com.qozix.tileview.TileView;
import com.qozix.tileview.TileView.TileViewEventListener;
import com.qozix.tileview.markers.MarkerEventListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Navigator extends Activity implements SensorEventListener{

	private TileView tileView;
	private ImageView naviSymbol;
	private int currentX;
	private int currentY;
	private int roundedHeading;
	private double scale=1;
	private boolean gettingInitialPoint=false;
	private boolean gettingSecondaryPoint=false;
	private ImageView startSymbol;
	private ImageView headingSymbol;
	private StepCounter stepCounter;
	//private TileViewEventListener tileEventListener;
	
	//##### Public Variables #####
	private double[] stepLength; //step length in cm
	private double CMPERPIXEL = 5;
	private int numParticles = 50; 			// Particle count
	private Point startLocation;
	private Point currentLocation;
	private Polygon ITBhalls;
	
	private double previousStepTimestamp = 0;
	
	
	//need to create initial particles X and Y cloud around initial position
	private Point[] particles = new Point[numParticles];
	double[] particleA = new double[numParticles];
	double[] particleB = new double[numParticles];

	double locationMean = 0.0;	//Mean will almost always be zero for location
	double locationStd = 5;		//standard deviation in pixels
	
	double aMean = 20.0;		//from figure 6 in paper, approx mean of slope
	double aStd = 10;			//guess at slope std
	
	double bMean = 30;			//from figure 6 in paper, approx value of offset
	double bStd = 10;			//guess at offset std

	Random rng = new Random();
	
	double[] heading = new double[100];   //need to have a past history of heading values, maybe shift new values in
	double[] filteredHeading = new double[100];
	int stepsSinceLastTurn = 0;
	double deltaHeadingThreshold = Math.PI/2;
	double averagePastHeading = 0; //TODO needs to be initialized it heading  
	double currentHeading = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_navigator);
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
		tileView.addMarkerEventListener( markerEventListener );

		tileView.addTileViewEventListener(tileEventListener);
		
//		stepCounter= new StepCounter(this); //CAUSES CRASH ON STARTUP FOR NOW
		// add some pins...
		roundedHeading=0;
		currentX=200;
		currentY=200;
		updateLocal(currentX,currentY,true);
		// scale it down to manageable size
		tileView.setScale( 0.5 );
		// center the frame
		frameTo( 0.5, 0.5 );
		
		
	}// End onCreate
	
	


	//######### Custom Functions #############
	
	public void inputStartLocation()
	{
		if (!gettingInitialPoint && !gettingSecondaryPoint)
		{
			Log.d("Function Call","Input Start");

			gettingInitialPoint=true;
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

			ITBhalls = Polygon.Builder()
				.addVertex(new Point(560, 180))
				.addVertex(new Point(580, 180))
				.addVertex(new Point(588, 226))
				.addVertex(new Point(1126, 221))
				.addVertex(new Point(1126, 721))
				.addVertex(new Point(961, 721))
				.addVertex(new Point(961, 750))
				.addVertex(new Point(913, 750))
				.addVertex(new Point(913, 721))
				.addVertex(new Point(518, 721))
				.addVertex(new Point(517, 688))
				.addVertex(new Point(773, 688))
				.addVertex(new Point(773, 488))
				.addVertex(new Point(886, 488))
				.addVertex(new Point(886, 462))
				.addVertex(new Point(851, 462))
				.addVertex(new Point(851, 450))
				.addVertex(new Point(900, 450))
				.addVertex(new Point(900, 488))
				.addVertex(new Point(950, 480))
				.addVertex(new Point(950, 688))
				.addVertex(new Point(1102, 688))
				.addVertex(new Point(1102, 247))
				.addVertex(new Point(600, 247))
				.addVertex(new Point(600, 318))
				.addVertex(new Point(560, 318))
				.close()
				.addVertex(new Point(789, 504))
				.addVertex(new Point(789, 688))
				.addVertex(new Point(926, 688))
				.addVertex(new Point(924, 504))
				.build();
	}

	public void createStartSymbol(int x, int y)
	{
		startSymbol = new ImageView( this);
		startSymbol.setImageResource( R.drawable.push_pin );
		getTileView().addMarker(startSymbol, x, y );
		startLocation = new Point(x,y);   
	}
	public void createHeadingSymbol(int x, int y)
	{
		headingSymbol = new ImageView( this);
		headingSymbol.setImageResource( R.drawable.maps_marker_blue );
		getTileView().addMarker(headingSymbol, x, y );
	}
	public void reset()
	{
		Log.d("Function Call","Reset");
		currentX+=10;
		currentY+=10; 
		roundedHeading=(roundedHeading+60)%360;
		updateLocal(currentX,currentY,false);
	}
	public void start()
	{
		Log.d("Function Call","Start");
		stepCounter.pushdata(1, 2, 3);
	}
	/*
	private void addPin( double x, double y ) {
		ImageView imageView = new ImageView( this );
		imageView.setImageResource( R.drawable.push_pin );
		getTileView().addMarker( imageView, x, y );
	}*/
	
	private void updateLocal( double x, double y ,boolean first) {
		if (!first)
		{
			getTileView().removeMarker(naviSymbol);
		}
		naviSymbol = new ImageView( this );
		naviSymbol.setImageBitmap(getBitmapFromAssets("pointer/naviPointer-"+roundedHeading+".png"));
		//naviSymbol.setImageResource( R.drawable.push_pin );
		getTileView().addMarker( naviSymbol, x, y );
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
		// TODO Auto-generated method stub

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
				initializeParticleFilter();
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
		case R.id.action_inputStart:
			inputStartLocation();
			return true;
		case R.id.action_reset:
			reset();
			return true;
		case R.id.action_start:
			start();
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
		Log.d("Function call","Step Event triggered");
		
	}

	public void stepTaken(double currentStepTimestamp){
		//First, we shall try to determine if the user has turned

		//Perform filter on heading directions
		//Not sure how that is done
		

		stepsSinceLastTurn++;
		double headingSum = 0;
		
		for (int i = 0; i<stepsSinceLastTurn; i++) {
			headingSum += filteredHeading[i];
		}
		double averagePastHeading = headingSum / stepsSinceLastTurn;
		
		double deltaHeading = filteredHeading[filteredHeading.length - 1] - averagePastHeading;
		
		if (deltaHeading > deltaHeadingThreshold) {
			stepsSinceLastTurn = 0;
		}
		
		currentHeading = averagePastHeading;
		
		int totalX = 0;
		int totalY = 0;
		
		// calculate step frequency from step period
		double period = previousStepTimestamp - currentStepTimestamp;
		previousStepTimestamp = currentStepTimestamp;

		
		//Second, we update all of the particles, we also compute the average X and Y at this point
		for (int i = 0; i<numParticles; i++) {
			stepLength[i] = ( particleA[i] * 1/period) + particleB[i];
		
			
			float x = (float) (particles[i].x + ( ( stepLength[i] ) * Math.cos(currentHeading ) ));
			float y = (float) (particles[i].y + ( ( stepLength[i] ) * Math.sin(currentHeading ) ));
			particles[i] = new Point(x,y);	
			
			totalX += particles[i].x;
			totalY += particles[i].y;
		}
		
		int averageX = totalX / numParticles;
		int averageY = totalY / numParticles;
		
		totalX = 0;
		totalY = 0;
		
		for (int i = 0; i < numParticles; i++) {
			if (ITBhalls.contains(particles[i])) {
				float x = (float) (averageX + locationStd * rng.nextGaussian());
				float y = (float) (averageY + locationStd * rng.nextGaussian());
				particles[i] = new Point(x,y);
			}
			
			totalX += particles[i].x;
			totalY += particles[i].y;
		}

		averageX = totalX / numParticles;
		averageY = totalY / numParticles;
		
		currentLocation = new Point (averageX, averageY);	
	
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
