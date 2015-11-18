package de.dfki.omm.types;

import java.net.MalformedURLException;
import java.net.URL;

/** Implementation of {@link TypedValue} specifically modeling an URL type. */
public class URLType implements TypedValue 
{
	URL m_value = null;
	
	/** Constructor.
	 * @param value The URL. 
	 */
	public URLType(URL value) 
	{ 		
		m_value = value;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getType()
	 */
	public String getType() { return "url";	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#getValue()
	 */
	public URL getValue() { return m_value;	}
	
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
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.types.TypedValue#setValue(java.lang.String)
	 */
	public void setValue(String value) {
		try {
			this.m_value = new URL(value);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
