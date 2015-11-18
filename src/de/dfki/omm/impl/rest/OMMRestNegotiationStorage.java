package de.dfki.omm.impl.rest;

import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.omm.interfaces.JSONOutput;

/** Representation of an OMM's block storage node in the REST interface. */
public class OMMRestNegotiationStorage implements JSONOutput
{
	private boolean m_distributed = false;
	private boolean m_deleteDisabled = false;
	private String m_link = null;
	private long m_capacity = Long.MAX_VALUE, m_freeSpace = Long.MAX_VALUE;		
	
	private OMMRestNegotiationStorage() {}
	
	/** Creates a new block storage node representation, given all necessary attributes.
	 * 
	 * @param link Address of the block storage node.
	 * @param capacity Total capacity of the storage node.
	 * @param freeSpace Unused and available capacity of the storage node.  
	 * @param isDistributed True, if the storage is distributed.
	 * @param isDeleteDisabled True, if memory deletion is disabled. 
	 * @return The created {@link OMMRestNegotiationStorage} object. 
	 */
	public static OMMRestNegotiationStorage create(String link, long capacity, long freeSpace, boolean isDistributed, boolean isDeleteDisabled)
	{
		OMMRestNegotiationStorage storage = new OMMRestNegotiationStorage();
		
		storage.m_distributed = isDistributed;
		storage.m_deleteDisabled = isDeleteDisabled;
		storage.m_link = link;
		storage.m_capacity = capacity;
		storage.m_freeSpace = freeSpace;
		
		return storage;
	}
	
	/** Creates a new block storage node representation from a JSON description.
	 * 
	 * @param obj The block storage node's description as a {@link JSONObject}.
	 * @return The created {@link OMMRestNegotiationStorage} object. 
	 */
	public static OMMRestNegotiationStorage createFromJSON(JSONObject obj)
	{
		if (obj == null) return null;
		
		try
		{
			OMMRestNegotiationStorage storage = new OMMRestNegotiationStorage();
			
			storage.m_distributed = obj.getBoolean("DISTRIBUTED");
			storage.m_link = obj.getString("LINK");
			storage.m_capacity = convertValue(obj.getString("CAPACITY"));
			storage.m_freeSpace = convertValue(obj.getString("FREE_SPACE"));
			storage.m_deleteDisabled = obj.getBoolean("DELETE_DISABLED");
			
			return storage;
		}
		catch(Exception e){e.printStackTrace();}
		
		return null;
	}
	
	/** Converts a numerical value to a String.
	 * @param value The numerical value as long.
	 * @return A String containing the abbreviated numerical value, or "UNLIMITED" for maximal and "UNDEFINED" for minimal value. 
	 */
	public static String convertValue(long value)
	{
		if (value == Long.MAX_VALUE) return "UNLIMITED";
		else if (value == Long.MIN_VALUE) return "UNDEFINED";
		
		if (value >= Math.pow(1024, 5))
		{
			return Math.ceil(value / Math.pow(1024, 5)) + "P";
		}
		else if (value >= Math.pow(1024, 4))
		{
			return Math.ceil(value / Math.pow(1024, 4)) + "T";
		} 
		else if (value >= Math.pow(1024, 3))
		{
			return Math.ceil(value / Math.pow(1024, 3)) + "G";
		}
		else if (value >= Math.pow(1024, 2))
		{
			return Math.ceil(value / Math.pow(1024, 2)) + "M";
		}
		else if (value >= Math.pow(1024, 1))
		{
			return Math.ceil(value / Math.pow(1024, 1)) + "K";	
		}
		
		return value+"";		
	}
	
	/** Converts value in a String to a numerical value.
	 * @param value The numerical value as abbreviated String.
	 * @return A numerical value as long, or Long.MAX_VALUE for the String "UNLIMITED" and Long.MIN_VALUE for null. 
	 */
	public static long convertValue(String string)
	{
		if (string == null) return Long.MIN_VALUE;
		if (string.toLowerCase().equals("unlimited")) return Long.MAX_VALUE;
		if (string.endsWith("K")) return convertValue(string.replace("K", ""), Math.pow(1024, 1));
		if (string.endsWith("M")) return convertValue(string.replace("M", ""), Math.pow(1024, 2));
		if (string.endsWith("G")) return convertValue(string.replace("G", ""), Math.pow(1024, 3));
		if (string.endsWith("T")) return convertValue(string.replace("T", ""), Math.pow(1024, 4));
		if (string.endsWith("P")) return convertValue(string.replace("P", ""), Math.pow(1024, 5));
		
		return Long.MIN_VALUE;
	}
	
	/** Converts value in a String to a numerical value.
	 * @param value The numerical value as abbreviated String.
	 * @param factor A factor with which to multiply the abbreviated value. 
	 * @return A numerical value as long. 
	 */
	public static long convertValue(String value, double factor)
	{
		long longValue = (long)Double.parseDouble(value);
		return longValue * (long)factor;
	}
	
	/** Checks whether the storage is distributed.
	 * @return True, if so. 
	 */
	public boolean isDistributed()
	{
		return m_distributed;
	}
	
	/** Retrieves the link to the block storage node in the REST interface. 
	 * @return Storage address as String. 
	 */
	public String getLink()
	{
		return m_link;
	}
	
	/** Retrieves the capacity of the block storage. 
	 * @return Storage capacity. 
	 */
	public long getCapacity()
	{
		return m_capacity;
	}
	
	/** Retrieves the free space in the block storage. 
	 * @return Storage's free capacity. 
	 */
	public long getFreeSpace()
	{
		return m_freeSpace;
	}

	@Override
	public String toString()
	{
		String capacity = convertValue(m_capacity), freeSpace = convertValue(m_freeSpace);
		
		return "Distributed: " + isDistributed() + ", " +
			   "Link: '" + getLink() + "', " + 
			   "Capacity: " + capacity+ ", " +
			   "Free Space: " + freeSpace;
	}

	@Override
	public String toJSONString() {
		JSONObject obj = toJSONObject();
		if (obj == null) return null;
		return obj.toString();
	}

	@Override
	public JSONObject toJSONObject() {
		
		JSONObject storage = new JSONObject();
		
		try {
			storage.put("LINK", getLink());
			storage.put("CAPACITY", convertValue(getCapacity()));
			storage.put("FREE_SPACE", convertValue(getFreeSpace()));
			storage.put("DISTRIBUTED", false);
			storage.put("DELETE_DISABLED", m_deleteDisabled);

			return storage;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
