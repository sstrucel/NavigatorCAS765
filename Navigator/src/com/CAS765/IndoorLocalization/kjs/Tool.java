package com.CAS765.IndoorLocalization.kjs;

import java.util.ArrayList;
import java.util.Collections;

import android.util.Log;

//A class for some common tools
public class Tool {
	public double norm(double x, double y, double z){
		double res = (x*x)+(y*y)+(z*z);
		return Math.sqrt(res);
//		return res;
	}
	
	public double clamp(double v, double min, double max){
		if(v > max)
			return max;
		else if (v < min)
			return min;
		else 
			return v;
	}
	
	//According to the ajust data, estimate the threshold
	public double getThreshold(int step_count, ArrayList<Double> peak_list){
		Collections.sort(peak_list);
		//Log.i("peak", Integer.toString(peak_list.size()));
		if (step_count >= peak_list.size())
			return Double.NaN;
		else
			return peak_list.get(peak_list.size() - step_count - 1) - 0.0000001;
	}
	
	public double sum(double[] source){
		double total = 0.0;
		for(double i:source){
			total += i;
		}
		//Log.i("hello", Double.toString(total));
		return total;
	}
}
