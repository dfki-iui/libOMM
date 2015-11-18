package de.dfki.omm.impl.rest.activity;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.dfki.omm.impl.rest.activity.OMMSnippetTrigger.OMMSnippetState;

public class OMMSnippet
{	
	private String m_restURL, m_name, m_type, m_block, m_creator;
	private int m_heartBeatRate;
	private HashSet<OMMSnippetTrigger> m_trigger;
	
	public OMMSnippet(String restURL, String name, String type, String block, String creator, int heartBeatRate, HashSet<OMMSnippetTrigger> trigger)
	{
		m_restURL = restURL;
		m_name = name;
		m_type = type;
		m_heartBeatRate = heartBeatRate;
		m_trigger = trigger;
		m_block = block;
		m_creator = creator;
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getType()
	{
		return m_type;
	}
	
	public String getCorrespondingBlock()
	{
		return m_block;
	}
	
	public String getBlockCreator()
	{
		return m_creator;
	}
	
	public int getHeartBeatRate()
	{
		return m_heartBeatRate;
	}
	
	public List<OMMSnippetTrigger> getTriggerList()
	{
		return Arrays.asList(m_trigger.toArray(new OMMSnippetTrigger[m_trigger.size()]));
	}
	
	public Collection<OMMSnippetTrigger> getTriggerCollection()
	{
		return m_trigger;
	}
	
	public boolean addHeartBeatTrigger(int limit, int interval)
	{
		return addTrigger(new OMMSnippetHeartBeatTrigger(OMMSnippetState.DISABLED, limit, interval));
	}
	
	public boolean addEventTrigger(String event)
	{		
		return addTrigger(new OMMSnippetEventTrigger(OMMSnippetState.DISABLED, event));		
	}
	
	public boolean removeTrigger(OMMSnippetTrigger trigger)
	{
		if (isTriggerKnown(trigger))
		{
			try
			{
				trigger.delete(m_restURL+"/ctrl/"+URLEncoder.encode(m_name, "utf-8"));
			}
			catch(Exception e) { e.printStackTrace(); return false; } 
			return true;
		}
		
		return false;
	}
	
	public String getRestURL()
	{
		try
		{
			return m_restURL + "/" + URLEncoder.encode(m_name, "utf-8") + "/";
		}
		catch(Exception e) {e.printStackTrace();}
		
		return null;
	}
	
	@Override
	public String toString()
	{
		String text = "Snippet '"+m_name+"' of type '"+m_type+"' with heartbeat rate '"+m_heartBeatRate+"' @ '"+getRestURL()+"'";
		
		if (m_trigger.size() > 0)
		{		
			int counter = 1;
			for(OMMSnippetTrigger trigger : m_trigger)
			{
				text += "\r\n\tTrigger " + (counter++) + ": " + trigger.toString();
			}
		}
		else
		{
			text += "\r\n\tNo triggers defined. -> Script is DISABLED.";
		}
		
		return text;
	}
	
	private boolean addTrigger(OMMSnippetTrigger trigger)
	{
		if (isTriggerKnown(trigger)) return false;
		
		try
		{
			trigger.add(m_restURL+"/ctrl/"+URLEncoder.encode(m_name, "utf-8"));
			return true;
		}
		catch(Exception e) { e.printStackTrace(); return false; } 
	}
	
	private boolean isTriggerKnown(OMMSnippetTrigger trigger)
	{
		for(OMMSnippetTrigger pivot : m_trigger)
		{
			if (pivot.equals(trigger))
			{				
				return true;
			}
		}	
		
		return false;
	}
}
