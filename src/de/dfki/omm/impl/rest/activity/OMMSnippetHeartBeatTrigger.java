package de.dfki.omm.impl.rest.activity;

import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;


public class OMMSnippetHeartBeatTrigger extends OMMSnippetTrigger
{
	int m_limit, m_interval;
	
	public OMMSnippetHeartBeatTrigger(OMMSnippetState state, int limit, int interval)
	{
		m_state = state;
		m_limit = limit;
		m_interval = interval;
	}
	
	public int getLimit()
	{
		return m_limit;
	}
	
	public int getInterval()
	{
		return m_interval;
	}
	
	@Override
	public String toString()
	{
		return "HeartBeatTrigger with State: '"+m_state+"', Interval: '"+m_interval+"' and Limit: '"+OMMRestActivityData.getString(m_limit)+"'";
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof OMMSnippetHeartBeatTrigger)) return false;
		
		OMMSnippetHeartBeatTrigger otherTrigger = (OMMSnippetHeartBeatTrigger)other;
		return otherTrigger.m_limit == m_limit && otherTrigger.m_interval == m_interval;
	}
	
	public void add(String restURL) {
		String url = restURL;
		if (!url.endsWith("/")) url += "/";
		url += "hb";
		
		JSONObject json = new JSONObject();
		json.put("step", m_interval);
		json.put("limit", m_limit);
		
		StringRepresentation stringRep = new StringRepresentation(json.toString());
		stringRep.setMediaType(MediaType.APPLICATION_JSON);
		
		ClientResource c = new ClientResource(url);
		c.post(stringRep);
	}

	@Override
	public void delete(String restURL) {
		String url = restURL;
		if (!url.endsWith("/")) url += "/";
		url += "hb";
		
		ClientResource c = new ClientResource(url);
		c.delete();
	}
}
 