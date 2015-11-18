package de.dfki.omm.types;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/** An individual or agent that initiates changes in the state of an OMM. */
public class OMMEntity 
{
	private String m_value, m_type, m_date;
	private final String x509Begin = "-----BEGIN CERTIFICATE-----\n", x509End = "-----END CERTIFICATE-----";
	
	private static List<String> m_entityTypes = new Vector<String>() 
	{
		private static final long serialVersionUID = -6290063340171715119L;
		{
			add("duns");
			add("email");
			add("gln");
			add("openID");
			add("x509");
		}
	};
	
	/** Retrieves an empty entity without set properties.
	 * @return Dummy {@link OMMEntity}. 
	 */
	public static OMMEntity getDummyEntity()
	{
		return new OMMEntity(null, null, null);
	}
	
	/** Constructor.
	 * @param type The entity's type, for example "email", "openID", ...
	 * @param value The entity's identifier. 
	 * @param iso8601date The time the entity is created or active. 
	 */
	public OMMEntity(String type, String value, String iso8601date)
	{
		this.m_value = value;
		this.m_type = type;
		this.m_date = iso8601date;
	}
	
	/** Constructor. 
	 * @param cert A certificate from which to construct an entity. 
	 */
	public OMMEntity(X509Certificate cert)
	{
		try 
		{			
			this.m_value = x509Begin +  BinaryValue.encodePayload("base64", cert.getEncoded()) + x509End;
			this.m_type = "x509";
			this.m_date = ISO8601.getISO8601StringWithGMT();
		} 
		catch (CertificateEncodingException e) 
		{
			e.printStackTrace();
		}
	}
	
	/** Retrieves a list of all known entity types. 
	 * @return Entity types as a {@link List} of {@link String}s. 
	 */
	public static List<String> getEntityTypes()
	{
		return m_entityTypes;
	}	

	/** Retrieves the identifier of this entity. 
	 * @return Identifier of this entity.
	 */
	public String getValue()
	{
		return m_value;
	}
	
	/** Retrieves the type of this entity. 
	 * @return Type of this entity.
	 */
	public String getType()
	{
		return m_type;
	}
	
	/** Retrieves the certificate, if this entity is derived from one. 
	 * @return The {@link X509Certificate} used to create the entity. 
	 */
	public X509Certificate getValueAsCertificate()
	{
		if (!"x509".equals(m_type)) return null;
		
		byte[] bytes = BinaryValue.decodePayload("base64", m_value.replace(x509Begin, "").replace(x509End, ""));
		
		try {
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream in = new ByteArrayInputStream(bytes);
			X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
			return cert;
		} 
		catch (CertificateException e) 
		{
			e.printStackTrace();
		}

		return null;
	}
	
	/** Sets the entity's date or replaces an old one. 
	 * @param newDate The date as an {@link ISO8601} String. 
	 * @return The entity. 
	 */
	public OMMEntity setDateAsISO8601(String newDate)
	{
		m_date = newDate;
		return this;
	}
	
	/** Retrieves the entity's date entry. 
	 * @return The date as an {@link ISO8601} String. 
	 */
	public String getDateAsISO8601()
	{
		return m_date;
	}
	
	/** Retrieves the entity's date entry. 
	 * @return The date as an {@link Calendar} object. 
	 */
	public Calendar getDateAsCalendar()
	{
		return ISO8601.parseDate(m_date);
	}
	
	/** Retrieves the entity's date entry. 
	 * @return The date as an {@link Date} object. 
	 */
	public Date getDate()
	{
		return getDateAsCalendar().getTime();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		if ("x509".equals(m_type)) return getValueAsCertificate().getIssuerX500Principal().getName() +" <of type> "+m_type+" ("+m_date+")";
		
		return m_value+" <of type> "+m_type+" ("+m_date+")";
	}
	
	/** Checks whether this entity is the same as another, regarding type and value. 
	 * @param other The entity to compare with. 
	 * @return True, if both entities are equal in type and value. 
	 */
	public boolean equalsTypeAndValue(Object other)
	{
		if (!(other instanceof OMMEntity)) return false;
		
		OMMEntity otherEntity = (OMMEntity)other;
		return otherEntity.m_type.equals(m_type) && otherEntity.m_value.equals(m_value);
	}
	
	@Override 
	public boolean equals(Object other) 
	{
		if (!(other instanceof OMMEntity)) return false;
		
		OMMEntity otherEntity = (OMMEntity)other;
		return otherEntity.m_date.equals(m_date) && otherEntity.m_type.equals(m_type) && otherEntity.m_value.equals(m_value);
	}
}
