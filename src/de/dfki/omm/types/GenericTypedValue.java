package de.dfki.omm.types;

/** Implementation of {@link TypedValue} modeling any values for which no specific class exists. */
public class GenericTypedValue implements TypedValue 
{
	private String m_value;
	private String m_type;
	
	/** Constructor. 
	 * @param type The type as String. 
	 * @param value The value as String. 
	 */
	public GenericTypedValue(String type, String value)
	{
		m_value = value;
		m_type = type;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getType()
	 */
	public String getType()	
	{
		return m_type;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getValue()
	 */
	public String getValue()
	{
		return m_value;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return m_value.toString();
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#setType(java.lang.String)
	 */
	public void setType(String type) {
		this.m_type = type;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#setValue(java.lang.String)
	 */
	public void setValue(String value) {
		this.m_value = value;
	}
}
