package de.dfki.omm.impl.rest.activity;

import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;


public class OMMSnippetEventTrigger extends OMMSnippetTrigger
{
	String m_event;
	
	public OMMSnippetEventTrigger(OMMSnippetState state, String event)
	{
		m_state = state;
		m_event = event;
	}
	
	public String getEvent()
	{
		return m_event;
	}
	
	@Override
	public String toString()
	{
		return "EventTrigger with State: '"+m_state+"' and Event: '"+m_event+"'";
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof OMMSnippetEventTrigger)) return false;
		
		OMMSnippetEventTrigger otherTrigger = (OMMSnippetEventTrigger)other;
		return otherTrigger.m_event.equals(m_event);
	}
	
	@Override
	public void add(String restURL) {
		String url = restURL;
		if (!url.endsWith("/")) url += "/";
		url += "tr";
		
		StringRepresentation stringRep = new StringRepresentation(m_event);
		stringRep.setMediaType(MediaType.TEXT_PLAIN);
		
		ClientResource c = new ClientResource(url);
		c.post(stringRep);
	}

	@Override
	public void delete(String restURL) {
		String url = restURL;
		if (!url.endsWith("/")) url += "/";
		url += "tr";
		
		ClientResource c = new ClientResource(url);
		c.delete();
	}
}
