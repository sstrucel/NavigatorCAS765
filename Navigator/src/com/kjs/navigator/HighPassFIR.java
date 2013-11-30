package com.kjs.navigator;

//A class for the Highpass FIR
public class HighPassFIR {
	float updateFreq = 0; // match this to your update speed
    float cutOffFreq = 0; // the cut-off frequency
    
    HighPassFIR(float u, float c){
    	this.cutOffFreq = c;
    	this.updateFreq = u;
    }
    
    public AccelData transform(double accelX, double accelY, double accelZ){
	    float RC = 1.0f / cutOffFreq;
	    float dt = 1.0f / updateFreq;
	    float filterConstant = RC / (dt + RC);
	    float alpha = filterConstant; 
	    float kAccelerometerMinStep = 0.033f;
	    float kAccelerometerNoiseAttenuation = 3.0f;
	    double accelFilter[] = new double[3];
	    double lastAccel[] = new double[3];
	    Tool tool = new Tool();
	    
	    float d = (float) tool.clamp(Math.abs(tool.norm(accelFilter[0], accelFilter[1], accelFilter[2]) - tool.norm(accelX, accelY, accelZ)) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
	    alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;

	    accelFilter[0] = (float) (alpha * (accelFilter[0] + accelX - lastAccel[0]));
	    accelFilter[1] = (float) (alpha * (accelFilter[1] + accelY - lastAccel[1]));
	    accelFilter[2] = (float) (alpha * (accelFilter[2] + accelZ - lastAccel[2]));

	    lastAccel[0] = accelX;
	    lastAccel[1] = accelY;
	    lastAccel[2] = accelZ;
	    
	    AccelData accel = new AccelData(accelFilter[0], accelFilter[1], accelFilter[2]);
	    return accel;
    }
}
