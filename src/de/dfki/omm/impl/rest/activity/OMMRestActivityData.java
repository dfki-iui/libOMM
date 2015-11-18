package de.dfki.omm.impl.rest.activity;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.dfki.omm.tools.DownloadHelper;

/** Parses and provides a list of rest activity data from an OMS node. */
public class OMMRestActivityData 
{	
	List<OMMSnippet> m_snippets;
	
	private OMMRestActivityData() {}
	
	/** Parses REST activity data from an OMS node.
	 * 
	 * @param givenRestURL The REST address where to get the activity data. 
	 * @return Parsed {@link OMMRestActivityData} from the given address. 
	 */
	public static OMMRestActivityData downloadAndCreate(String givenRestURL)
	{
		String restURL = givenRestURL;
		if (!restURL.endsWith("/")) restURL += "/"; 
		restURL += "list";	
		
		OMMRestActivityData retVal = new OMMRestActivityData();
		
		try
		{		
			String data = DownloadHelper.downloadData(restURL,null);
			JSONObject json = new JSONObject(data);
						
			JSONArray array = json.getJSONArray("SNIPPET_LIST");
			retVal.m_snippets = new Vector<OMMSnippet>(array.length());
					
			for(int i = 0; i < array.length(); i++)
			{
				JSONObject jsonSnippet = array.getJSONObject(i);				
				JSONArray triggers = jsonSnippet.getJSONArray("TRIGGER_LIST");
				String correspondingBlock = jsonSnippet.getString("BLOCK_NAME");
				String correspondingBlockCreator = jsonSnippet.getString("BLOCK_CREATOR");
				HashSet<OMMSnippetTrigger> triggerList = new HashSet<OMMSnippetTrigger>(triggers.length());
				for(int k = 0; k < triggers.length(); k++)
				{
					JSONObject trigger = triggers.getJSONObject(k);
					String type = trigger.getString("TYPE");
					String stateStr = trigger.getString("STATE");
					OMMSnippetTrigger.OMMSnippetState state = OMMSnippetTrigger.OMMSnippetState.valueOf(stateStr);
					
					if ("HEARTBEAT".equals(type))
					{
						String limit = trigger.getString("HEARTBEAT_LIMIT");
						String interval = trigger.getString("HEARTBEAT_INTERVAL");
						OMMSnippetHeartBeatTrigger t = new OMMSnippetHeartBeatTrigger(state, getValue(limit), getValue(interval));						
						triggerList.add(t);
					}
					else
					{
						String subs = trigger.getString("EVENT_SUBSCRIPTION");
						OMMSnippetEventTrigger t = new OMMSnippetEventTrigger(state, subs);			
						triggerList.add(t);
					}
				}
				OMMSnippet snippet = new OMMSnippet(givenRestURL, jsonSnippet.getString("NAME"), jsonSnippet.getString("TYPE"), correspondingBlock, correspondingBlockCreator, jsonSnippet.getInt("HEARTBEAT_RATE"), triggerList);
				 
				retVal.m_snippets.add(snippet);
			}
			
			return retVal;
		}
		catch(Exception e){e.printStackTrace();}
		
		return null;
	}	

	/** Retrieves the list of OMM Snippets. 
	 * @return {@link List} of {@link OMMSnippet}s. 
	 */
	public List<OMMSnippet> getSnippets()
	{
		return m_snippets;
	}
	
	/** Converts a String value to int. 
	 * @param value
	 * @return Value as int, with Integer.MAX_VALUE for "UNLIMITED", and Integer.MIN_VALUE for a null String. 
	 */
	public static int getValue(String value)
	{
		if (value == null) return Integer.MIN_VALUE;
		if ("UNLIMITED".equals(value.toUpperCase())) return Integer.MAX_VALUE;
		return Integer.parseInt(value);			
	}
	
	/** Converts an int value to a String. 
	 * @param value
	 * @return Value as String, with "UNLIMITED" for Integer.MAX_VALUE, and null for Integer.MIN_VALUE. 
	 */
	public static String getString(int value)
	{
		if (value == Integer.MIN_VALUE) return null;
		if (value == Integer.MAX_VALUE) return "UNLIMITED";
		return value+"";
	}
}
