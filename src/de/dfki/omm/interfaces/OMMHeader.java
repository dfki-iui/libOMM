package de.dfki.omm.interfaces;

import de.dfki.omm.types.TypedValue;

import java.io.Serializable;

/***
 * Contains all information of a OMM header.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMHeader extends Serializable
{
	/***
	 * Retrieves the version of this object memory model.
	 * @return returns the version.
	 */
	public int getVersion();
	
	/***
	 * Retrieves the primary ID of this object memory.
	 * @return returns the primary ID as {@link TypedValue} instance.
	 */
	public TypedValue getPrimaryID();
	
	/***
	 * Set the primary ID of this object memory.
	 * @param primaryId as {@link TypedValue}
	 */
	public void setPrimaryID(TypedValue primaryId);
	
	/***
	 * Retrieves the additional blocks information of this object memory.
	 * @return returns the additional blocks information as {@link TypedValue} instance.
	 */
	public TypedValue getAdditionalBlocks();
	
	/***
	 * Set the additional blocks information of this object memory.
	 * @param additionalBlocks as {@link TypedValue}. 
	 */
	public void setAdditionalBlocks(TypedValue additionalBlocks);
}
