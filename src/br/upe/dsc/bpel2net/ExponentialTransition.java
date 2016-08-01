package br.upe.dsc.bpel2net;

public class ExponentialTransition {

	private String id;
	
	private boolean isInfinity;
	
	private double delay;
	
	
	public ExponentialTransition(String id, double delay, boolean isInfinity) {
		
		this.id = id;
		this.delay = delay;
		this.isInfinity = isInfinity;
	}
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isInfinity() {
		return isInfinity;
	}

	public void setInfinity(boolean isInfinity) {
		this.isInfinity = isInfinity;
	}

	public double getDelay() {
		return delay;
	}

	public void setDelay(double delay) {
		this.delay = delay;
	}
	
	
	
}
