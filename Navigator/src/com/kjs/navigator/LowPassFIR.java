package com.hw5;

//A class for lowpass filter
public class LowPassFIR {
	 double ALPHA = 0;
	 //double[] Prior = {Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN};
	 int N = 16;
	 double[] Prior = new double[N];
	
	LowPassFIR(double d) {
		this.ALPHA = d;
		for(int i = 0; i < N; i++){
			Prior[i] = Double.NaN;
		}
	}

	double transform_1(double input) {
		double output;
		if(Double.isNaN(Prior[4])){
			output = input;
			Prior[4] = input;
			return output;
		}
		if(Double.isNaN(Prior[3])){
			output = input;
			Prior[3] = input;
			return output;
		}
		if(Double.isNaN(Prior[2])){
			output = input;
			Prior[2] = input;
			return output;
		}
		if(Double.isNaN(Prior[1])){
			output = input;
			Prior[1] = input;
			return output;
		}
		output = (1.0/8) * (2.0 * input + Prior[1] - Prior[3] - 2.0 * Prior[4]);
		Prior[4] = Prior[3];
		Prior[3] = Prior[2];
		Prior[2] = Prior[1];
		Prior[1] = input;
		
		return output;
	}
	
	double transform_2(double input){
		double output;
		if(Double.isNaN(Prior[1])){
			output = input;
		}
		else
			output = Prior[1] + this.ALPHA*(input - Prior[1]);
		Prior[1] = output;
		return output;
	}
	
	double integrate(double input) {
		double output;
		if(Double.isNaN(Prior[4])){
			output = input;
			Prior[4] = input;
			return output;
		}
		if(Double.isNaN(Prior[3])){
			output = input;
			Prior[3] = input;
			return output;
		}
		if(Double.isNaN(Prior[2])){
			output = input;
			Prior[2] = input;
			return output;
		}
		if(Double.isNaN(Prior[1])){
			output = input;
			Prior[1] = input;
			return output;
		}
		if(Double.isNaN(Prior[0])){
			output = input;
			Prior[0] = input;
			return output;
		}
		output = (1.0/6) * (input + Prior[0] + Prior[1] + Prior[2] + Prior[3] + Prior[4]);
		Prior[4] = Prior[3];
		Prior[3] = Prior[2];
		Prior[2] = Prior[1];
		Prior[1] = Prior[0];
		Prior[0] = input;
		return output;
	}
	
	double integrate1(double input) {
		double output;
		
		for( int i = N - 1; i >= 0; i--)
		{
			if(Double.isNaN(Prior[i])){
				output = input;
				Prior[i] = input;
				return output;
			}
		}
		output = (1.0/(N+1)) * (input + new Tool().sum(Prior));
		for( int i = N - 2; i >= 0; i--){
			Prior[i+1] = Prior[i];
		}
		Prior[0] = input;
		return output;
	}
	
}

