package com.kjs.navigator;

public class Particle {

	private Point p;
	private double a;
	private double b;
	
	public Particle(Point p,double a ,double b)
	{
		this.p=p;
		this.a=a;
		this.b=b;
	}
	public Particle(double x,double y,double a ,double b)
	{
		this.p=new Point((float)x,(float)y);
		this.a=a;
		this.b=b;
	}
	
	public Particle cloneParticle()
	{
		return new Particle(p,a,b);
	}
	
	
	
}
