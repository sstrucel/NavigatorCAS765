package com.kjs.navigator;

import java.io.IOException;
import java.io.InputStream;


import com.kjs.navigator.R;
import com.kjs.navigator.StepCounter.OnStepEventListener;
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

public class Navigator extends Activity implements SensorEventListener, OnStepEventListener{

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
	private double stepLength = 15; //step length in cm
	private double CMPERPIXEL = 5;
	private int M = 10; 			// Particle count
	private Point startLocation;
	private Point[] particles = new Point[10];
	private Polygon ITBhalls; 
	
	private double previousStepTimestamp = 0;
	
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
		
		stepCounter= new StepCounter(this);
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
		float theta;
		for (int i = 0; i < M ; i++){
			theta = (float) (i * (2 * Math.PI / M));
			float x = (float) (startLocation.x + stepLength * Math.cos(theta));
			float y = (float) (startLocation.y + stepLength * Math.sin(theta));
			particles[i] = new Point(x,y);
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





	@Override
	public void stepEvent() {
		// TODO Auto-generated method stub
		Log.d("Function call","Step Event triggered");
		
	}


	public void stepTaken(double currentStepTimestamp){
		
		//TODO: calculate Stride Length from step period
		double period = previousStepTimestamp - currentStepTimestamp;
		previousStepTimestamp = currentStepTimestamp;
		// these number are from the graph in the first paper
		// 	Lg = a*f + b
		stepLength = ((1/period) * 22.2 + 29) * CMPERPIXEL;

		//TODO: Generate new particles based on new heading and stride length
		// Check for dead particles

		// compare particles with walls, eliminate bad particles		
		for (int i = 0; i < M ; i++){
			if (ITBhalls.contains(particles[i]));{
				//TODO set point to new point
				
			}
		}
		//TODO: possibly calculate probability of remaining particles and eliminate outliers
		//TODO: plot remaining particles on the map.
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
