package de.dfki.omm.types;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/** A single subject tag which might be or be part of an OMM block's subject field. */
public class OMMSubjectTag implements Serializable
{
	private OMMSubjectTagType m_type;
	private String m_value;
	private OMMSubjectTag m_child;
	
	/** Constructor.
	 * @param type Type of the subject tag as an {@link OMMSubjectTagType}. 
	 * @param value Value of the subject tag as String. 
	 * @param child Another subject tag to be nested into this one. Can be null. 
	 */
	public OMMSubjectTag(OMMSubjectTagType type, String value, OMMSubjectTag child)
	{
		this.m_type = type;
		this.m_value = value;
		this.m_child = child;		
	}
	
	/** Retrieves the subject tag's type. 
	 * @return Type as an {@link OMMSubjectTagType}. 
	 */
	public OMMSubjectTagType getType() { return m_type; }

	/** Retrieves the subject tag's value. 
	 * @return Value as {@link String}. 
	 */
	public String getValue() { return m_value; }
	
	/** Retrieves the subject tag's child. 
	 * @return Child as {@link OMMSubjectTag}, if there is one, else null. 
	 */
	public OMMSubjectTag getChild() { return m_child; }
	
	/** Sets the subject tag's child to the given tag. 
	 * @param child Another subject tag to be nested into this one. 
	 */
	public void setChild(OMMSubjectTag child) { m_child = child; }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String retVal = m_value+" <of type> "+m_type;
		if (m_child != null) retVal += " ("+m_child.toString()+")";
		return retVal;
	}
	
	/**
	 * Helper to get subject tags in JSON format recursively.
	 * 
	 * @param ommSubjectTag The tag to convert
	 * @return The tag as a JSON object
	 */
	public JSONObject getJSONRepresentation () {

		JSONObject jsonTag = new JSONObject();
		
		try {
			jsonTag.put("@type", this.getType());
			jsonTag.put("@value", this.getValue());
			OMMSubjectTag child = this.getChild();
			if (child != null) jsonTag.put("tag", child.getJSONRepresentation());
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		return jsonTag;
	}
}
