package de.dfki.omm.types;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/** Describes the format of an OMM block's payload. */
public class OMMFormat implements Serializable
{
	private String m_mimeType;
	private URL m_schema;
	private String m_encoding;
	
	private static final String MIME_TYPE_FILE = "mime-types.txt";
	
	private static List<String> m_mimeTypes;
	
	static
	{
		m_mimeTypes = new Vector<String>();		
		try		
		{
			if (new File(MIME_TYPE_FILE).exists())
			{
				InputStream input = new FileInputStream(MIME_TYPE_FILE);// OMMFormat.class.getResourceAsStream(MIME_TYPE_FILE);
				BufferedReader br = new BufferedReader(new InputStreamReader(input));
				String thisLine;
				while ((thisLine = br.readLine()) != null) 
				{
					m_mimeTypes.add(thisLine);
			    } 			
				br.close();
				Collections.sort(m_mimeTypes);
			}
			else
			{
				m_mimeTypes.add("text/plain");
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/** Constructor.
	 * @param mimeType The type of content, using standard MIME types, for example "image/png", "text/plain". 
	 * @param schema XML schema definition if the mimeType is "application/xml". 
	 * @param encoding The payload's encryption algorithm. 
	 */
	public OMMFormat(String mimeType, URL schema, String encoding)
	{
		this.m_mimeType = mimeType;
		this.m_schema = schema;
		this.m_encoding = encoding;
	}
	
	/** Retrieves a list of all known MIME types. 
	 * @return List of MIME types as Strings. 
	 */
	public static List<String> getMIMETypes()
	{
		return m_mimeTypes;
	}	
	
	/** Retrieves the MIME type specified in this format object. 
	 * @return MIME type as String. 
	 */
	public String getMIMEType() { return m_mimeType; }
	
	/** Retrieves the schema URL specified in this format object. 
	 * @return Schema as {@link URL}. 
	 */
	public URL getSchema() { return m_schema; }
	
	/** Retrieves the encryption algorithm specified in this format object. 
	 * @return Encryption algorithm type as String. 
	 */
	public String getEncryption() { return m_encoding; }
	
	public String toString()
	{
		String retVal = m_mimeType;
		
		if (m_schema != null) retVal += " (Schema="+m_schema.toString()+")";
		if (m_encoding != null) retVal += " (Encoding="+m_encoding.toString()+")";
		
		return retVal;
	}
}
