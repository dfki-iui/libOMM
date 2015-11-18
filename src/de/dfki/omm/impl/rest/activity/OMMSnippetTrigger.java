package de.dfki.omm.impl.rest.activity;

public abstract class OMMSnippetTrigger
{

	public enum OMMSnippetState
	{
		ENABLED, DISABLED, RUNNING
	}

	OMMSnippetState m_state;
	
	public abstract void add(String restURL);
	public abstract void delete(String restURL);
}
