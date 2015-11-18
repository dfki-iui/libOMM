package de.dfki.omm.types;

import java.util.Date;

import de.dfki.omm.interfaces.OMMStructureBlock;

/** Provides structure information for an {@link OMMStructureBlock}, 
 * comprising a relation type, target and a time span or date for which the relationship is valid. */
public class OMMStructureInfo
{
	OMMStructureRelation m_relationType = null;
	TypedValue m_relationTarget = null;
	Date m_dateStart = null, m_dateEnd = null;
	
	/** Constructor.
	 * @param relationType The relation type, specified as an {@link OMMStructureRelation}. 
	 * @param relationTarget The target of the relation as a sort of {@link TypedValue}. 
	 * @param date The validity date for the relationship.
	 */
	public OMMStructureInfo(OMMStructureRelation relationType, TypedValue relationTarget, Date date)
	{
		m_relationTarget = relationTarget;
		m_relationType = relationType;
		m_dateStart = date;
	}
	
	/** Constructor.
	 * @param relationType The relation type, specified as an {@link OMMStructureRelation}. 
	 * @param relationTarget The target of the relation as a sort of {@link TypedValue}. 
	 * @param @param startDate A start date for the relationship (can be open ended). 	
	 * @param endDate An end date for the relationship (can be open ended). 
	 */
	public OMMStructureInfo(OMMStructureRelation relationType, TypedValue relationTarget, Date startDate, Date endDate)
	{
		m_relationTarget = relationTarget;
		m_relationType = relationType;
		m_dateStart = startDate;
		m_dateEnd = endDate;
	}
	
	/** Retrieves the type of the relationship.
	 * @return Type as an {@link OMMStructureRelation}. 
	 */
	public OMMStructureRelation getRelationType()
	{
		return m_relationType;
	}
	
	/** Retrieves the target of the relationship.
	 * @return Target as a {@link TypedValue}. 
	 */
	public TypedValue getRelationTarget()
	{
		return m_relationTarget;
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
