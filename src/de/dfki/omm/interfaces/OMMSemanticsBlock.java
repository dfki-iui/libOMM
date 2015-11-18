package de.dfki.omm.interfaces;

import java.util.Collection;

import de.dfki.omm.types.OMMSemanticsInfo;
import de.dfki.omm.types.OMMSemanticsGroup;

/***
 * Contains all information of a OMM semantics block.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMSemanticsBlock extends OMMBlock
{
	/**
	 * Gets a collection of all semantics information.
	 * @return the collection of all semantics information
	 */
	public Collection<OMMSemanticsGroup> getSemanticGroups();

	/**
	 * Adds a new semantics information to this block.
	 * @param info the semantics information as {@link OMMSemanticsInfo} to add
	 */
	public void addSemanticsGroup(OMMSemanticsGroup info);
	
	/**
	 * Removes an existing semantics information from this block.
	 * @param info the semantics information as {@link OMMSemanticsInfo} to remove
	 */
	public void removeSemanticsGroup(OMMSemanticsGroup info);
}
