package br.upe.dsc.bpel2net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import br.upe.dsc.calo.simplex.SimplexReader2;

public class DataReader {

	private HashMap<String, Double> data = new HashMap<String, Double>();
	
	SimplexReader2 reader;
	
	public DataReader(File f) throws FileNotFoundException
	{
		this.reader = new SimplexReader2(new FileInputStream(f));
	}
	
	
	public void loadData() throws ParserConfigurationException, IOException, SAXException
	{
		reader.load();
		
		if (reader.enter("arrival"))
		{
			double rate = Double.parseDouble(reader.getNodeAttribute("rate"));
			
			data.put("arrival", rate);
		}
		else
			data.put("arrival", 1.0);
		
		if (reader.enter("resources"))
		{//<resources>
			
			//varre a lista de papéis
			int numberOfRoles = reader.countNodes("role");
			
			for (int i = 1; i <= numberOfRoles; i++)
			{
				reader.enter("role", i);
								
				//<role>
				String name = reader.getNodeAttribute("name");
				int number = Integer.parseInt(reader.getNodeAttribute("resources"));
				
				data.put(name, (double) number);
				
				//</role>
				reader.exit();
			}
			
			reader.exit();
			//</resources>
		}
		if (reader.enter("delays"))
		{//<delays>
			
			//varre a lista de atividades
			int numberOfActivities = reader.countNodes("activity");
			
			for (int i = 1; i <= numberOfActivities; i++)
			{
				reader.enter("activity", i);
				//<activity>
				
				String name = reader.getNodeAttribute("name");
				double delay = Double.parseDouble(reader.getNodeAttribute("delay"));
				
				data.put(name, delay);
				
				//</activity>
				reader.exit();
			}
			
			reader.exit();
			//</delays>
		}
		if (reader.enter("probabilities"))
		{//<probabilities>
			
			//varre a lista de condiçoes
			int numberOfConditions = reader.countNodes("condition");
			
			for (int i = 1; i <= numberOfConditions; i++)
			{
				reader.enter("condition", i);
				//<condition>
				
				String expr = reader.getNodeAttribute("expression");
				double prob = Double.parseDouble(reader.getNodeAttribute("probability"));
				
				data.put(expr, prob);
				
				//</condition>
				reader.exit();
			}
			
			reader.exit();
			//</probabilities>
		}
	}
	
	public double getDelay(String activityName)
	{
		try
		{
			return data.get(activityName);
		}
		catch (NullPointerException e)
		{
			return 1.0;
		}
	}
	
	public double getProbability(String condition)
	{
		try
		{
		 return data.get(condition);
		}
		catch (NullPointerException e)
		{
			return 1.0;
		}
	}
	
	public int getNumberOfResources(String roleName)
	{
		return data.get(roleName).intValue();
	}


	public double getArrivalDelay() {

		double arrivalRate = data.get("arrival").doubleValue();
		
		if (arrivalRate == 0.0) return 1.0;
		
		return 1.0/arrivalRate;
		
	}
	
}
