package de.dfki.omm.impl;

import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.types.TypedValue;

/** Implementation of {@link OMMHeader}. */
public class OMMHeaderImpl implements OMMHeader
{
	protected int m_version = 1;
	protected TypedValue m_primaryID = null;
	protected TypedValue m_additionalBlocks = null;
	
	protected OMMHeaderImpl() { }
	
	/** Creates a new header for an OMM. 
	 * 
	 * @param primaryID The OMM's primary ID as a {@link TypedValue}. 
	 * @param additionalBlock Addtional block(s) stored outside the memory as a {@link TypedValue}. 
	 * @return The created {@link OMMHeader}. 
	 */
	public static OMMHeader create(TypedValue primaryID, TypedValue additionalBlock)
	{
		OMMHeaderImpl retVal = new OMMHeaderImpl();
		retVal.m_primaryID = primaryID;
		retVal.m_additionalBlocks = additionalBlock;
		return retVal;				
	}

	public int getVersion() { return m_version; }

	public TypedValue getPrimaryID() { return m_primaryID; }

	public TypedValue getAdditionalBlocks() { return m_additionalBlocks; }
	
	public String toString()
	{
		if (m_additionalBlocks != null)
			return "OMM-Header: Version="+m_version+"  PrimaryID="+m_primaryID.toString()+"  AdditionalBlocks="+m_additionalBlocks.toString();
		else
			return "OMM-Header: Version="+m_version+"  PrimaryID="+m_primaryID.toString();
	}

	public void setPrimaryID(TypedValue primaryId) {
		m_primaryID = primaryId;
	}

	public void setAdditionalBlocks(TypedValue additionalBlocks) {
		m_additionalBlocks = additionalBlocks;
	}
}
