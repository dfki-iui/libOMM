package de.dfki.omm.tools;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Stack;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.dfki.omm.impl.OMMBlockImpl;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMPreviousBlockLink;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.OMMSubjectTag;
import de.dfki.omm.types.OMMSubjectTagType;
import de.dfki.omm.types.TypedValue;

/** A SAX handler to parse OMM blocks from XML documents. */
public class OMMBlockSaxHandler extends DefaultHandler 
{
	/** Enumerator to describe current parsing mode (which tag is being parsed). */
	protected enum OMMBlockSaxHandlerMode
	{
		Main, Creator, Contributor, Subject
	}
	
	protected OMMBlockSaxHandlerMode mode = OMMBlockSaxHandlerMode.Main;
	protected Boolean currentElement = false;
	protected String currentValue = "";
	protected String blockID = null;
	protected  OMMBlockImpl block = null;
    
	protected String xmlLang = null, ommType = null, previousBlockType = null, ommEncoding = null, ommSchema = null, ommDate = null, ommValue = null;
    
	protected URI namespace; 
	protected URL type;
	protected OMMMultiLangText title = new OMMMultiLangText();
	protected OMMMultiLangText description = new OMMMultiLangText();
	protected OMMEntityCollection contributors;
	protected OMMEntity lastContributor;
	protected OMMEntity creator;
	protected OMMFormat format;
	protected OMMSubjectCollection subject;
	protected TypedValue payload;
	protected Element payloadElement;
	protected TypedValue link;
	protected TypedValue primaryID;
	protected String linkHash;
	protected OMMPreviousBlockLink previousID;
	protected boolean isSecurityBlock = false;
	
	protected Stack<OMMSubjectTag> workingStack = null;
	    
    /** Constructor.
     * @param blockID ID of the block to parse. 
     */
    public OMMBlockSaxHandler(String blockID)
    {
    	if (blockID == null || blockID.equals("")) throw new IllegalArgumentException("BlockID must not be null or empty!");
    	this.blockID = blockID;
    	workingStack = new Stack<OMMSubjectTag>();
    }
    
    /** Retrieves the parsed OMM block. 
     * @return Parsed block as {@link OMMBlockImpl}. 
     */
    public OMMBlockImpl getOMMBlock()
    {
    	return block;
    }
    
	// Called when tag starts
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
 
    	currentElement = true;
        currentValue = "";

        xmlLang = attributes.getValue("xml:lang");
        ommType = attributes.getValue("omm:type");
        previousBlockType = attributes.getValue("omm:previousBlockType");
        ommEncoding = attributes.getValue("omm:encoding");
        ommSchema = attributes.getValue("omm:schema");
        ommValue = attributes.getValue("omm:value");
        
        if (mode == OMMBlockSaxHandlerMode.Main)
        {
	        if (qName.equals("omm:creation")) 
	        {
	            mode = OMMBlockSaxHandlerMode.Creator;
	        }
	        else if (qName.equals("omm:contribution")) 
	        {
	            mode = OMMBlockSaxHandlerMode.Contributor;
	        }
	        else if (qName.equals("omm:subject")) 
	        {
	            mode = OMMBlockSaxHandlerMode.Subject;
	        }
        }
        else if (mode == OMMBlockSaxHandlerMode.Subject && qName.equals("omm:tag")) 
        {
        	OMMSubjectTagType type = OMMSubjectTagType.Text;	        		
    		if ("Ontology".equals(ommType)) type = OMMSubjectTagType.Ontology;
    		OMMSubjectTag localTag = new OMMSubjectTag(type, ommValue, null);
    		
    		if (!workingStack.isEmpty())
    		{
    			workingStack.peek().setChild(localTag);
    		}
    		
        	workingStack.push(localTag);
        }
    }
 
    // Called when tag closing
    @Override
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
 
        currentElement = false;
        
        switch(mode)
        {
	        case Creator:
	        	if (qName.equals("omm:creator"))
	        	{
        			creator = new OMMEntity(ommType, currentValue, ommDate);
	        	}
	        	else if (qName.equals("omm:date"))
	        	{
	        		if (ommEncoding != null && !ommEncoding.toLowerCase().equals("iso8601"))
	        		{
	        			throw new IllegalArgumentException("Date is not ISO8601 format!");
	        		}
	        		ommDate = currentValue;
	        		if (creator != null) creator = new OMMEntity(creator.getType(), creator.getValue(), ommDate);
	        	}
	        	if (qName.equals("omm:creation")) mode = OMMBlockSaxHandlerMode.Main;
	        	break;
	        case Contributor:
	        	if (qName.equals("omm:contributor"))
	        	{
        			lastContributor = new OMMEntity(ommType, currentValue, ommDate);
	        	}
	        	else if (qName.equals("omm:date"))
	        	{
	        		if (ommEncoding != null && !ommEncoding.toLowerCase().equals("iso8601"))
	        		{
	        			throw new IllegalArgumentException("Date is not ISO8601 format!");
	        		}
	        		ommDate = currentValue;
//	        		if (lastContributor != null) lastContributor = new OMMEntity(creator.getType(), creator.getValue(), ommDate); 
	        		if (lastContributor != null) lastContributor = new OMMEntity(lastContributor.getType(), lastContributor.getValue(), ommDate);
	        		if (contributors == null) contributors = new OMMEntityCollection();
	        		contributors.add(lastContributor);
	        		lastContributor = null;
	        	}
//	        	if (qName.equals("omm:contributor")) {
	        	if (qName.equals("omm:contribution")) mode = OMMBlockSaxHandlerMode.Main;
	        		
	        	break;
	        case Subject:
	        	if (qName.equals("omm:tag"))
	        	{
	        		if (subject == null) subject = new OMMSubjectCollection();
	        		subject.add(workingStack.pop());
	        	}
	        	if (qName.equals("omm:subject")) mode = OMMBlockSaxHandlerMode.Main;
	        	break;
	        case Main:
	        	if (qName.equals("omm:primaryID"))
	        	{
	        		primaryID = OMMXMLConverter.getTypedValue(ommType, currentValue);
	        	}
	        	else if (qName.equals("omm:title"))
	        	{
	        		title.put(new Locale(xmlLang), currentValue);
	        	}
	        	else if (qName.equals("omm:description"))
	        	{
	        		description.put(new Locale(xmlLang), currentValue);
	        	}
	        	else if (qName.equals("omm:format"))
	        	{	     
	        		try {
	        			URL schema = null;
	        			if (ommSchema != null) schema = URI.create(ommSchema).toURL();
						format = new OMMFormat(currentValue, schema, ommEncoding);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
	        	}
	        	else if (qName.equals("omm:namespace"))
	        	{
	        		namespace = URI.create(currentValue);
	        	}
	        	else if (qName.equals("omm:type"))
	        	{
	        		try {
						type = URI.create(currentValue).toURL();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
	        	}
	        	else if (qName.equals("omm:previousBlock"))
	        	{
    				previousID = OMMPreviousBlockLink.createFromString(currentValue, previousBlockType);
	        	}	        		
	        	else if (qName.equals("omm:link"))
	        	{
	        		link = OMMXMLConverter.getTypedValue(ommType, currentValue);
	        	}
	        	else if (qName.equals("omm:payload"))
	        	{
	        		payload = OMMXMLConverter.getTypedValue(ommType, currentValue);
	        	}
	        	else if (qName.equals("ds:Signature"))
	        	{
					isSecurityBlock = true;
	        	}
	        	break;
        }
        
        ommSchema = ommEncoding = ommType = xmlLang = ommDate = ommValue = null; 
    }

    // Called at the end of the document
    @Override
    public void endDocument()
    {
    	block = (OMMBlockImpl)OMMBlockImpl.create(blockID, primaryID, namespace, type, title, description, contributors, creator, format, subject, previousID, payload, payloadElement, link, linkHash);
    }

    // Called to get tag characters inside tags
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
       if (currentElement) {
            currentValue = currentValue +  new String(ch, start, length);
        }
    }
	
}
