package de.dfki.omm.interfaces;

import org.json.JSONObject;

/** Interface for objects that need to present themselves as JSON objects or JSON Strings in order to output OMS contents. */
public interface JSONOutput {
	
	/** Converts this object to a JSON String representing it.
	 * @return JSON String representation of this object.
	 */
	public String toJSONString();
	
	/** Converts this object to a JSON object representing it.
	 * @return {@link JSONObject} representation of this object.
	 */
	public JSONObject toJSONObject();
}
