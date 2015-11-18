package de.dfki.omm.interfaces;

import java.util.HashMap;

import de.dfki.omm.types.OMMEntity;

/***
 * Contains all information of a OMM attribute list block.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMAttributeListBlock extends OMMBlock
{	
	/**
	 * Adds a new attribute to the block.
	 * @param attributeName the name of the attribute (key)
	 * @param value the value of the attribute (value)
	 * @param entity the OMMEntity who is adding the attribute
	 */
	public void addAttribute(String attributeName, String value, OMMEntity entity);
	
	/**
	 * Removes a attribute from this block.
	 * @param attributeName the attribute name (key) to remove
	 * @param entity the OMMEntity who is removing the attribute
	 */
	public void removeAttribute(String attributeName, OMMEntity entity);
	
	/**
	 * Gets the attribute value of the given name
	 * @param attributeName the attribute name (key)
	 * @return the requested attribute as String
	 */
	public String getAttribute(String attributeName);
	
	/**
	 * Gets all attributes of this block.
	 * @return the attributes as {@link HashMap} 
	 */
	public HashMap<String, String> getAllAttributes();
}
