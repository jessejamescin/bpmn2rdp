package br.upe.dsc.bpel2net;

import java.util.ArrayList;

public class Subprocess {

	private String startingPlace;
	
	private ArrayList<String> deptTransitions = new ArrayList<String>();
	

	public String getStartingPlace() {
		return startingPlace;
	}

	public void setStartingPlace(String startingPlace) {
		this.startingPlace = startingPlace;
	}

	public ArrayList<String> getDeptTransitions() {
		return deptTransitions;
	}

	public void setDeptTransitions(ArrayList<String> deptTransitions) {
		this.deptTransitions = deptTransitions;
	}
	
	public String toString()
	{
		String s = "Start: "+startingPlace+"; End: ";
		for (String t : deptTransitions)
			s += t+" ";
		
		return s;
	}

	
}


	
	

