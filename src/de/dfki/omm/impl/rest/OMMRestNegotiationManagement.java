package de.dfki.omm.impl.rest;

import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.omm.interfaces.JSONOutput;

/** Representation of an OMM's management node in the REST interface. */
public class OMMRestNegotiationManagement implements JSONOutput
{
	private String m_link = null;
	private boolean m_flush = false;
	
	private OMMRestNegotiationManagement() {}
	
	/** Creates a new management node representation, given all necessary attributes.
	 * 
	 * @param link Address of the management node.
	 * @param isFlushNecessary True, if flush is necessary.
	 * @return The created {@link OMMRestNegotiationManagement} object. 
	 */
	public static OMMRestNegotiationManagement create(String link, boolean isFlushNecessary)
	{
		OMMRestNegotiationManagement man = new OMMRestNegotiationManagement();
		man.m_flush = isFlushNecessary;
		man.m_link = link;
		return man;
	}
	
	/** Creates a new management node representation from a JSON description.
	 * 
	 * @param obj The management node's description as a {@link JSONObject}.
	 * @return The created {@link OMMRestNegotiationManagement} object. 
	 */
	public static OMMRestNegotiationManagement createFromJSON(JSONObject obj)
	{
		if (obj == null) return null;
		
		try
		{
			OMMRestNegotiationManagement man = new OMMRestNegotiationManagement();
			man.m_flush = obj.getBoolean("FLUSH");
			man.m_link = obj.getString("LINK");
			return man;
		}
		catch(Exception e){e.printStackTrace();}
		
		return null;
	}
	
	/** Checks whether the management has to be flushed. 
	 * @return True, if so. 
	 */
	public boolean isFlushNecessary()
	{
		return m_flush;
	}
	
	/** Retrieves the address of the management node in the REST interface. 
	 * @return The URL to the containing OMM's "/mgmt" node as String.  
	 */
	public String getLink()
	{
		return m_link;
	}
	
	@Override
	public String toString()
	{
		return "Link: '" + getLink() + "', " + 
			   "Is Flush Necessary: " + isFlushNecessary();
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
			management.put("FLUSH", m_flush);
			return management;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
