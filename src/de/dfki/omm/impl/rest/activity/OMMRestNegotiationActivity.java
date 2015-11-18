package de.dfki.omm.impl.rest.activity;

import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.omm.interfaces.JSONOutput;

public class OMMRestNegotiationActivity implements JSONOutput
{
	private String m_link = null;
	
	private OMMRestNegotiationActivity() {}
	
	public static OMMRestNegotiationActivity create(String link)
	{
		OMMRestNegotiationActivity man = new OMMRestNegotiationActivity();
		man.m_link = link;
		return man;
	}
	
	public static OMMRestNegotiationActivity createFromJSON(JSONObject obj)
	{
		if (obj == null) return null;
		
		try
		{
			OMMRestNegotiationActivity man = new OMMRestNegotiationActivity();
			man.m_link = obj.getString("LINK");
			return man;
		}
		catch(Exception e){e.printStackTrace();}
		
		return null;
	}
	
	public OMMRestActivityData getActivityData()
	{				
		return OMMRestActivityData.downloadAndCreate(m_link);
	}
	
	public String getLink()
	{
		return m_link;
	}
	
	@Override
	public String toString()
	{
		return "Link: '" + getLink() + "'";
	}

	@Override
	public String toJSONString() {
		JSONObject obj = toJSONObject();
		if (obj == null) return null;
		return obj.toString();
	}

	@Override
	public JSONObject toJSONObject() {

		JSONObject management = new JSONObject();
		
		try {
			management.put("LINK", getLink());
			return management;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
