package de.dfki.omm.types;

import java.util.Date;

import de.dfki.omm.interfaces.OMMSemanticsBlock;


/** Provides semantic information for an {@link OMMSemanticsBlock},
 *  comprising a subject, predicate, object and a time span or date for which the predicate is valid for the two units. */
public class OMMSemanticsInfo
{
	String m_subject = "[this]", m_relation = null, m_object = null;
	Date m_dateStart = null, m_dateEnd = null;
	
	/** Constructor.
	 * @param subject The subject of the semantic relationship, default: "[this]". 
	 * @param relation The predicate of the semantic relationship. 
	 * @param object The object of the semantic relationship. 
	 * @param date A validity date for the semantic relationship. 	
	 */
	public OMMSemanticsInfo(String subject, String relation, String object, Date date)
	{
		m_subject = subject;
		m_relation = relation;
		m_object = object;
		m_dateStart = date;
	}
	
	/** Constructor.
	 * @param subject The subject of the semantic relationship, default: "[this]". 
	 * @param relation The predicate of the semantic relationship. 
	 * @param object The object of the semantic relationship. 
	 * @param startDate A start date for the semantic relationship (can be open ended). 	
	 * @param endDate An end date for the semantic relationship (can be open ended). 
	 */
	public OMMSemanticsInfo(String subject, String relation, String object, Date startDate, Date endDate)
	{
		m_subject = subject;
		m_relation = relation;
		m_object = object;
		m_dateEnd = endDate;
	}
	
	/** Retrieves the subject of the semantic relationship. 
	 * @return The subject as String. 
	 */
	public String getSubject()
	{
		return m_subject;
	}
	
	/** Retrieves the predicate of the semantic relationship. 
	 * @return The predicate as String. 
	 */
	public String getRelation()
	{
		return m_relation;
	}
	
	/** Retrieves the object of the semantic relationship. 
	 * @return The object as String. 
	 */
	public String getObject()
	{
		return m_object;
	}
	
	/** Checks whether the relationship is valid for a time span (instead of a single date). 
	 * @return True, if so. 
	 */
	public boolean isTimeSpan()
	{
		return (m_dateEnd != null && m_dateStart != null);
	}
	
	/** Retrieves the validity date of the semantic relationship. 
	 * @return The validity date as {@link Date}. 
	 */
	public Date getDate()
	{
		return m_dateStart;
	}
	
	/** Retrieves the start date of the semantic relationship. 
	 * @return The start date as {@link Date}. 
	 */
	public Date getStartDate()
	{
		return m_dateStart;
	}
	
	/** Retrieves the end date of the semantic relationship. 
	 * @return The end date as {@link Date}. 
	 */
	public Date getEndDate()
	{
		return m_dateEnd;
	}
	
}
