package br.upe.dsc.bpel2net;

public class Arc {

	private String id;
	
	private String fromId;
	
	private String toId;

	public Arc(String id, String fromId, String toId) {
	
		this.id = id;
		this.fromId = fromId;
		this.toId = toId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFromId() {
		return fromId;
	}

	public void setFromId(String fromId) {
		this.fromId = fromId;
	}

	public String getToId() {
		return toId;
	}

	public void setToId(String toId) {
		this.toId = toId;
	}
	
	
	
}
