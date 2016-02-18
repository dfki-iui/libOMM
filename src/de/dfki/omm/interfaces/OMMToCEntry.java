package de.dfki.omm.interfaces;

import java.io.Serializable;
import java.net.URI;
import java.util.Locale;

import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.OMMSubjectTagType;

/***
 * Contains all meta data information of a OMM table of contents entry.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMToCEntry extends Serializable
{
	/**
	 * Gets the ID of this block.
	 * @return the ID as {@link String}
	 */
	public String getID();
	
	/**
	 * Sets the ID of this block.
	 * @param id the ID as {@link String} to set
	 */
	public void setID(String id);
	
	
	/**
	 * Gets the namespace of this block.
	 * @return the namespace as {@link URI}
	 */
	public URI getNamespace();
	
	
	/**
	 * Gets the title of this block.
	 * @return the title as {@link OMMMultiLangText}
	 */
	public OMMMultiLangText getTitle();
	
	/**
	 * Gets the title of this block.
	 * @param language the language of the requested title
	 * @return the title as {@link String}
	 */
	public String getTitle(Locale language);
	
	
	/**
	 * Gets the creator of the block.
	 * @return the creator as {@link OMMEntity}
	 */
	public OMMEntity getCreator();
	
	/**
	 * Gets the contributors of the block.
	 * @return the contributors as {@link OMMEntityCollection}
	 */
	public OMMEntityCollection getContributors();
	
	
	/**
	 * Gets the subjects of the block.
	 * @return the subjects as {@link OMMSubjectCollection}
	 */
	public OMMSubjectCollection getSubject();
	
	/**
	 * Indicates whether a subject is present in this block.
	 * @param type the type of the subject to check
	 * @param value the value of the subject to check
	 * @return true if subject is present in this block
	 */
	public boolean isSubjectPresent(OMMSubjectTagType type, String value);
}
