package com.kjs.navigator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.kjs.navigator.R;
import com.qozix.tileview.TileView;
import com.qozix.tileview.TileView.TileViewEventListener;
import com.qozix.tileview.hotspots.HotSpot;
import com.qozix.tileview.hotspots.HotSpotEventListener;
import com.qozix.tileview.markers.MarkerEventListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Navigator extends Activity {

	private TileView tileView;
	private ImageView naviSymbol;
	private int currentX;
	private int currentY;
	private int roundedHeading;
	private double scale=1;
	private boolean gettingInitialPoint=false;
	//private TileViewEventListener tileEventListener;
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
		// add some pins...
		//addPin( 0.25, 0.25 );
		//addPin( 0.25, 0.75 );
		//addPin( 0.75, 0.25 );
		//addPin( 0.75, 0.75 );
		//addPin( 0.50, 0.50 );
		roundedHeading=0;
		currentX=200;
		currentY=200;
		updateLocal(currentX,currentY,true);
		// scale it down to manageable size
		tileView.setScale( 0.5 );

		// center the frame
		frameTo( 0.5, 0.5 );


	}
	private void addPin( double x, double y ) {
		ImageView imageView = new ImageView( this );
		imageView.setImageResource( R.drawable.push_pin );
		getTileView().addMarker( imageView, x, y );
	}
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
	private MarkerEventListener markerEventListener = new MarkerEventListener() {
		@Override
		public void onMarkerTap( View v, int x, int y ) {
			Toast.makeText( getApplicationContext(), "You are here", Toast.LENGTH_LONG ).show();
		}		
	};
	
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
	//######### Custom Functions #############
	public void inputStart()
	{
		Log.d("Function Call","InputStart");
		//HotSpot allMap= new HotSpot(0,0,2048,1887);
		//allMap.setHotSpotEventListener(startHotSpotEventListener);
		ArrayList<double[]> points = new ArrayList<double[]>();
		{
			points.add( new double[] { 0, 0 } );
			points.add( new double[] { 0, 100 } );
			points.add( new double[] { 100, 0 } );
			points.add( new double[] { 100, 100 } );
		}
		hotMap=tileView.addHotSpot(points, startHotSpotEventListener);
		
		gettingInitialPoint=true;
		Log.d("Function Call","InputStart Finished");
		
	}
	public void createStartSymbol(int x, int y)
	{
		ImageView startSymbol = new ImageView( this);
		startSymbol.setImageResource( R.drawable.push_pin );
		getTileView().addMarker(startSymbol, x, y );
	}
	private HotSpot hotMap;
	private HotSpotEventListener startHotSpotEventListener = new HotSpotEventListener() {
		@Override
		public void onHotSpotTap( HotSpot h, int x, int y ) {
			Log.d("Variables", "Initial Position X: "+x+" Y: "+y);
			//tileView.removeHotSpot(hotMap);
			
			
		}		
	};
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
	
	
	public void reset()
	{
		Log.d("Function Call","Reset");
		currentX+=10;
		currentY+=10;
		roundedHeading=(roundedHeading+60)%60;
		updateLocal(currentX,currentY,false);
	}
	
	
	//########## PREMADE STUFF ###############
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
	            inputStart();
	            return true;
	        case R.id.action_reset:
	            reset();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

}
