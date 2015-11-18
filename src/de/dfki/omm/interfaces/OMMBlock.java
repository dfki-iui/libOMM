package de.dfki.omm.interfaces;

import java.net.URI;
import java.net.URL;
import java.util.Locale;

import org.w3c.dom.Element;

import de.dfki.omm.events.OMMEvent;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMPreviousBlockLink;
import de.dfki.omm.types.OMMSubjectTag;
import de.dfki.omm.types.TypedValue;

/***
 * Contains all meta data information of a OMM block.
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMMBlock extends OMMToCEntry
{
	/**
	 * Gets the primary ID of the corresponding OMM	 * 
	 * @return the primary ID as {@link TypedValue}
	 */
	public TypedValue getPrimaryID();
	
	/**
	 * Gets the title of this block.
	 * @return the title as {@link OMMMultiLangText}
	 */
	public OMMMultiLangText getTitle();
	
	/**
	 * Gets the title of this block.
	 * @param language the desired language of the title string
	 * @return the title as {@link String}
	 */
	public String getTitle(Locale language);
	
	/**
	 * Sets the title of this block to a new value. An {@link OMMEvent} is triggered.
	 * @param text the new title of this block
	 * @param entity the entity that is performing this task
	 */
	public void setTitle(OMMMultiLangText text, OMMEntity entity);

	/**
	 * Sets a new title to this block. An {@link OMMEvent} is triggered.
	 * @param language the language of the title
	 * @param title the title text
	 * @param entity the entity that is performing this task
	 */
	public void setTitle(Locale language, String title, OMMEntity entity);
	
	/**
	 * Removes the title of the given language. The last remaining title cannot be removed. An {@link OMMEvent} is triggered.
	 * @param language the title tagged with this language is removed
	 * @param entity the entity that is performing this task 
	 * @return indicates whether this operation was successful (true) or not (false).
	 */
	public boolean removeTitle(Locale language, OMMEntity entity);
	
	
	/**
	 * Gets the description of this block
	 * @return the description as {@link OMMMultiLangText}
	 */
	public OMMMultiLangText getDescription();
	
	/**
	 * Gets the description of this block.
	 * @param language the desired language of the description string
	 * @return the description as {@link String}
	 */
	public String getDescription(Locale language);
	
	
	/**
	 * Sets the description of this block to a new value. An {@link OMMEvent} is triggered.
	 * @param text the new description of this block
	 * @param entity the entity that is performing this task
	 */
	public void setDescription(OMMMultiLangText text, OMMEntity entity);

	/**
	 * Sets a new description to this block. An {@link OMMEvent} is triggered.
	 * @param language the language of the description
	 * @param description the description text
	 * @param entity the entity that is performing this task
	 */
	public void setDescription(Locale language, String description, OMMEntity entity);

	
	/**
	 * Removes the description of the given language. An {@link OMMEvent} is triggered.
	 * @param language the description tagged with this language is removed
	 * @param entity the entity that is performing this task 
	 */
	public void removeDescription(Locale language, OMMEntity entity);
	
	/**
	 * Removes all descriptions of this block. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removeDescriptions(OMMEntity entity);
	
		
	/**
	 * Sets the namespace of this block. An {@link OMMEvent} is triggered.
	 * @param namespace the namespace as {@link URI}
	 * @param entity the entity that is performing this task
	 */
	public void setNamespace(URI namespace, OMMEntity entity);
	
	
	/**
	 * Gets the format of this block.
	 * @return the format as {@link OMMFormat}
	 */
	public OMMFormat getFormat();
	
	/**
	 * Sets the format of this block to the given value. An {@link OMMEvent} is triggered.
	 * @param format the new format of this block as {@link OMMFormat}
	 * @param entity the entity that is performing this task
	 */
	public void setFormat(OMMFormat format, OMMEntity entity);
	
	/**
	 * Removes the format attribute. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removeFormat(OMMEntity entity);
	

	/**
	 * Returns the link to the previous block
	 * @return The links as {@link OMMPreviousBlockLink}
	 */
	public OMMPreviousBlockLink getPreviousLink();

	/**
	 * Gets the type of this block.
	 * @return the type as {@link URL} 
	 */
	public URL getType();
	
	/**
	 * Removes the type from this block. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removeType(OMMEntity entity);
	
	/**
	 * Set the type of this block
	 * @param type the type as {@link URL}
	 * @param entity the entity that is performing this task
	 */
	public void setType(URL type, OMMEntity entity);
	
	
	/**
	 * Adds a new subject to the block. An {@link OMMEvent} is triggered.
	 * @param subject the subject as {@link OMMSubjectTag} to add
	 * @param entity the entity that is performing this task
	 */
	public void addSubject(OMMSubjectTag subject, OMMEntity entity);
	
	/**
	 * Replaces a given subject with a new one. An {@link OMMEvent} is triggered.
	 * @param oldSubject the old subject to replace
	 * @param newSubject the new subject
	 * @param entity the entity that is performing this task
	 */
	public void changeSubject(OMMSubjectTag oldSubject, OMMSubjectTag newSubject, OMMEntity entity);
	
	/**
	 * Removes the given subject from this block. An {@link OMMEvent} is triggered.
	 * @param subject the subject to remove
	 * @param entity the entity that is performing this task
	 */
	public void removeSubject(OMMSubjectTag subject, OMMEntity entity);	//hyx
	
	/**
	 * Removes all subjects from this block. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removeSubjects(OMMEntity entity);	//hyx
	
	
	/**
	 * Indicates whether this block contains a link to the payload
	 * @return true if block is a LinkBlock
	 */
	public boolean isLinkBlock();
	
	/**
	 * Gets the link of this block. 
	 * @return the link as {@link TypedValue}
	 */
	public TypedValue getLink();
	
	/**
	 * Gets the hash value of the data indicated by the link
	 * @return the hash value as {@link String}
	 */
	public String getLinkHash();
	
	/**
	 * Sets the link of this block without giving a hash value. If the block contains a payload it is removed. An {@link OMMEvent} is triggered.
	 * @param link the link as {@link TypedValue} to set
	 * @param entity the entity that is performing this task
	 */
	public void setLink(TypedValue link, OMMEntity entity);
	
	/**
	 * Sets the link of this block with an additional hash value. If the block contains a payload it is removed. An {@link OMMEvent} is triggered.
	 * @param link the link as {@link TypedValue} to set
	 * @param linkHash the hash value of the data indicated by the link as {@link String}
	 * @param entity the entity that is performing this task
	 */
	public void setLink(TypedValue link, String linkHash, OMMEntity entity);
	
	/**
	 * Removes the link of this block. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removeLink(OMMEntity entity);
	
	/**
	 * Gets the payload of this block.
	 * @return the payload as {@link TypedValue}
	 */
	public TypedValue getPayload();
	
	/**
	 * Gets the payload as string. If the payload is encoded, the result will be decoded.
	 * @return The payload as {@link String}.
	 */
	public String getPayloadAsString();
	
	/**
	 * Returns the encoding type of the payload as a String.
	 * @return encoding type of the payload as a String
	 */
	public String getPayloadEncoding();
	
//	/**
//	 * Sets the encoding of the payload. An {@link OMMEvent} is triggered.
//	 * @param encoding the encoding to set
//	 * @param entity the entity that is performing this task
//	 */
//	public void setPayloadEncoding(String encoding, OMMEntity entity);
	
	/**
	 * Gets the payload JDOM {@link Element} of this block.
	 * @return payload JDOM {@link Element} of this block
	 */
	public Element getPayloadElement();
	
	/**
	 * Sets the payload of this block. If a link was set, it will be removed. An {@link OMMEvent} is triggered.
	 * @param payload the payload as {@link TypedValue} to set
	 * @param entity the entity that is performing this task
	 */
	public void setPayload(TypedValue payload, OMMEntity entity);
	
	/**
	 * Sets the payload of this block. If a link was set, it will be removed. An {@link OMMEvent} is triggered.
	 * @param payload the payload as byte-array to set
	 * @param entity the entity that is performing this task
	 */
	public void setPayload(byte[] payload, OMMEntity entity);
	
	/**
	 * Removes the payload of this block. An {@link OMMEvent} is triggered.
	 * @param entity the entity that is performing this task
	 */
	public void removePayload(OMMEntity entity);	
	
	/**
	 * Gets the XML-Code of the block. The given boolean flag withPayload controls if payload is included. 
	 * @param withPayload true if payload is included
	 * @return XML-Code of the block as {@link Element}
	 */
	public Element getXMLElement(boolean withPayload);

	/** 
	 * Creates a JSON object containing a complete representation of an OMMBlock. 
	 * @return The JSON String. 
	 */
	public String getJsonRepresentation();

}
