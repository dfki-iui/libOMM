package de.dfki.omm.interfaces;

import java.util.Collection;

import de.dfki.omm.types.OMMStructureInfo;
import de.dfki.omm.types.OMMStructureRelation;

/***
 * Contains all information of a OMM structure block.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMStructureBlock extends OMMBlock
{
	/**
	 * Gets a collection of all structure information.
	 * @return the collection of all structure information
	 */
	public Collection<OMMStructureInfo> getStructureInfos();
	
	/**
	 * Gets the collection of strucutre information related to the given relation.
	 * @param relationType the relation as {@link OMMStructureRelation}
	 * @return requested structure information as {@link Collection}of {@link OMMStructureInfo}s
	 */
	public Collection<OMMStructureInfo> getStructureInfoByType(OMMStructureRelation relationType);
	
	/**
	 * Adds a new structure information to this block.
	 * @param info the structure information as {@link OMMStructureInfo} to add
	 */
	public void addStructureInfo(OMMStructureInfo info);
	
	/**
	 * Removes an existing structure information from this block.
	 * @param info the structure information as {@link OMMStructureInfo} to remove
	 */
	public void removeStructureInfo(OMMStructureInfo info);
}
