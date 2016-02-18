package de.dfki.omm.types;

import de.dfki.omm.interfaces.OMMBlock;

import java.io.Serializable;

/** Provides the ID and type of a previous block. */
public class OMMPreviousBlockLink implements Serializable{

	private String m_id;
	private OMMPreviousBlockType m_type;
	
	/** Constructor.
	 * @param previousBlockID ID of the previous block as String. 
	 * @param type An {@link OMMPreviousBlockType} describing the previous block's type. 
	 */
	public OMMPreviousBlockLink(String previousBlockID, OMMPreviousBlockType type)	
	{
		this.m_id = previousBlockID;
		this.m_type = type;
	}
	
	/** Constructor.
	 * @param previousBlock An {@link OMMBlock} to be used as previous block. 
	 * @param type An {@link OMMPreviousBlockType} describing the previous block's type. 
	 */
	public OMMPreviousBlockLink(final OMMBlock previousBlock, OMMPreviousBlockType type)	
	{		
		this.m_id = previousBlock.getID();
		this.m_type = type;
	}
	
	/** Constructs and returns previous block link from a block ID and a type. 
	 * @param previousBlockID ID of the previous block as String. 
	 * @param type An {@link OMMPreviousBlockType} describing the previous block's type. 
	 * @return The created {@link OMMPreviousBlockLink}. 
	 */
	public static OMMPreviousBlockLink createFromString(String previousBlockID, String type)	
	{
		OMMPreviousBlockType enumType = OMMPreviousBlockType.Previous;
		
		if ("previous".equals(type))
		{
			enumType = OMMPreviousBlockType.Previous;
		}		
		else if ("removes".equals(type))
		{
			enumType = OMMPreviousBlockType.Removes;
		}
		else if ("supersedes".equals(type))
		{
			enumType = OMMPreviousBlockType.Supersedes;
		}
		
		return new OMMPreviousBlockLink(previousBlockID, enumType);
	}
		
	/** Retrieves the ID of the previous block.
	 * @return Previous block's ID as String. 
	 */
	public String getBlockID()
	{
		return m_id;
	}
	
	/** Retrieves the type of the previous block.
	 * @return Previous block's type as {@link OMMPreviousBlockType}. 
	 */
	public OMMPreviousBlockType getType()
	{
		return m_type;
	}
	
	@Override
	public String toString()
	{
		return "'"+m_id + "' <of type> "+m_type ;
	}
}
