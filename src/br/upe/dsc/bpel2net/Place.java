package br.upe.dsc.bpel2net;

public class Place {

	private String id;
	
	private String initialMarking;

	public Place(String id, String initialMarking) {
		this.id = id;
		this.initialMarking = initialMarking;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInitialMarking() {
		return initialMarking;
	}

	public void setInitialMarking(String initialMarking) {
		this.initialMarking = initialMarking;
	}
	
	
	
}
