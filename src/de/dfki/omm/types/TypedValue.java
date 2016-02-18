package de.dfki.omm.types;

import java.io.Serializable;

/** Interface for typed OMM values, such as binary coded payloads or URLs. */
public interface TypedValue extends Serializable
{
	/** Retrieves the type of this object. */
	public String getType();	
	/** Retrieves the value of this object. */
	public Object getValue();
	
	/** Sets a new type or replaces an old. 
	 * @param type The type of this object. */
	public void setType(String type);
	/** Sets a new value or replaces an old. 
	 * @param value The value of this object. */
	public void setValue(String value);
}
