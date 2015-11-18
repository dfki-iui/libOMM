package de.dfki.omm.interfaces;

import java.util.Collection;

import de.dfki.omm.types.TypedValue;

/***
 * Contains all information of an OMM identification block.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMIdentifierBlock extends OMMBlock
{
	/**
	 * Gets the collection of all identifiers.
	 * @return the collection of all identifiers
	 */
	public Collection<TypedValue> getIdentifier();

	/**
	 * Adds a new identifier to this block.
	 * @param id the identifier to add as {@link TypedValue}
	 */
	public void addIdentifier(TypedValue id);
	
	/**
	 * Removes given identifier from this block.
	 * @param id the identifier to remove as {@link TypedValue}
	 */
	public void removeIdentifier(TypedValue id);
}
