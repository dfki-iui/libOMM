package de.dfki.omm.types;

import de.dfki.omm.interfaces.BinaryCodec;
import de.dfki.omm.tools.SunCodec;

/** Implementation of {@link TypedValue} modeling binary values (such as binary coded payloads). */
public class BinaryValue implements TypedValue 
{
	public static BinaryCodec BINARYCODEC = null;
	
	String m_type = null;
	String m_value = null;
	
	/** Constructor. 
	 * @param type The used encoding (for example "base64" or "none"). 
	 * @param value The value as String. 
	 */
	public BinaryValue(String type, String value)
	{
		this.m_type = type;
		this.m_value = value;
	}
	
	/** Constructor. 
	 * @param type The used encoding (for example "base64" or "none"). 
	 * @param value The value as byte[]. 
	 */
	public BinaryValue(String type, byte[] value)
	{
		this.m_type = type;
		m_value = encodePayload(type, value);
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getType()
	 */
	public String getType() { return m_type; }

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getValue()
	 */
	public String getValue() { return m_value;	}
	
	/** Retrieves the value of this object as bytes. 
	 * @return The value as byte[]. 
	 */
	public byte[] getValueAsByteArray() 
	{
		return decodePayload(m_type, m_value);	
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getValueAsByteArray() +" <of type> "+m_type;
	}
	
	
	/** Decodes a payload String to its byte representation.  
	 * @param type The type of encoding. 
	 * @param payload The String to convert.
	 * @return The decoded payload as byte array. 
	 */
	public static byte[] decodePayload(String type, String payload)
	{			
		if (BINARYCODEC == null) BINARYCODEC = new SunCodec();
		
		return BINARYCODEC.decodePayload(type, payload);
	}
	
	/** Encodes a payload byte aray to its String representation.  
	 * @param type The type of encoding. 
	 * @param payload The byte[] to convert.
	 * @return The encoded payload as a String. 
	 */
	public static String encodePayload(String type, byte[] payload)
	{
		if (BINARYCODEC == null) BINARYCODEC = new SunCodec();
		
		return BINARYCODEC.encodePayload(type, payload);
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

	/** Initializes the codec used for de- and encoding. */
	public static void initCodec()
	{
		if (BINARYCODEC == null) BINARYCODEC = new SunCodec();
	}
	
}
