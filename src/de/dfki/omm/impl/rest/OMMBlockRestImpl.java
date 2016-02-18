/**
 * 
 */
package de.dfki.omm.impl.rest;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONObject;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.OMMAttributeListBlockImpl;
import de.dfki.omm.impl.OMMBlockImpl;
import de.dfki.omm.impl.OMMIdentifierBlockImpl;
import de.dfki.omm.impl.OMMSemanticsBlockImpl;
import de.dfki.omm.impl.OMMStructureBlockImpl;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMIdentifierBlock;
import de.dfki.omm.interfaces.OMMSemanticsBlock;
import de.dfki.omm.interfaces.OMMStructureBlock;
import de.dfki.omm.tools.OMMBlockSaxHandler;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.GenericTypedValue;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMPreviousBlockLink;
import de.dfki.omm.types.OMMRestAccessMode;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.OMMSubjectTag;
import de.dfki.omm.types.OMMSubjectTagType;
import de.dfki.omm.types.TypedValue;

/** Implementation of {@link OMMBlock} for the REST interface. 
 * @author samuel
 */
public class OMMBlockRestImpl implements OMMBlock {
	
	protected String	id;
	protected String	url;
	protected OMMRestImpl parentOMM = null;
	protected OMMRestAccessMode mode;
	protected OMMBlockImpl shadowBlock = null;
	protected Long lastAccess = 0L;
	
	protected Map<String, Map.Entry<Long, Document>> cache = lruCache(8);

	/** Constructor. 
	 * @param id The block's ID. 
	 * @param url The address to the block's containing OMM's storage node. 
	 * @param mode The {@link OMMRestAccessMode} to use. 
	 * @param parent The block's parent {@link OMMRestImpl}. 
	 */
	public OMMBlockRestImpl(String id, String url, OMMRestAccessMode mode, OMMRestImpl parent) {
		this.id = id;
		this.url = url;
		this.parentOMM = parent; 
		this.mode = mode;
	}
	
	/** Deletes the internal {@link OMMBlockImpl} used to store the block's contents, forcing a reload on the next access. */
	public void invalidateCache()
	{
		shadowBlock = null; 
	}
	
	/** Retrieves the block and converts it if necessary.
	 * @return The block as {@link OMMIdentifierBlockImpl}.
	 * @throws Exception If conversion is attempted in SingleAccess mode.
	 */
	public OMMIdentifierBlock getAsIdentifierBlock() throws Exception
	{
		if (mode == OMMRestAccessMode.SingleAccess) throw new Exception("Conversion not available with SingleAccess mode!");
		
		UpdateShadowBlock();
		((OMMBlockImpl)shadowBlock).setPayload(getPayload(), OMMEntity.getDummyEntity());
		
		return new OMMIdentifierBlockImpl((OMMBlockImpl)shadowBlock);
	}
	
	/** Retrieves the block and converts it if necessary.
	 * @return The block as {@link OMMStructureBlock}.
	 * @throws Exception If conversion is attempted in SingleAccess mode.
	 */
	public OMMStructureBlock getAsStructureBlock() throws Exception
	{
		if (mode == OMMRestAccessMode.SingleAccess) throw new Exception("Conversion not available with SingleAccess mode!");
		
		UpdateShadowBlock();
		((OMMBlockImpl)shadowBlock).setPayload(getPayload(), OMMEntity.getDummyEntity());
		
		return new OMMStructureBlockImpl((OMMBlockImpl)shadowBlock);
	}
	
	/** Retrieves the block and converts it if necessary.
	 * @return The block as {@link OMMSemanticsBlock}.
	 * @throws Exception If conversion is attempted in SingleAccess mode.
	 */
	public OMMSemanticsBlock getAsSemanticsBlock() throws Exception
	{
		if (mode == OMMRestAccessMode.SingleAccess) throw new Exception("Conversion not available with SingleAccess mode!");
		
		UpdateShadowBlock();
		((OMMBlockImpl)shadowBlock).setPayload(getPayload(), OMMEntity.getDummyEntity());
		
		return new OMMSemanticsBlockImpl((OMMBlockImpl)shadowBlock);
	}
	
	/** Retrieves the block and converts it if necessary.
	 * @return The block as {@link OMMAttributeListBlockImpl}.
	 * @throws Exception If conversion is attempted in SingleAccess mode.
	 */
	public OMMAttributeListBlockImpl getAsAttibuteBlock() throws Exception
	{
		if (mode == OMMRestAccessMode.SingleAccess) throw new Exception("Conversion not available with SingleAccess mode!");
		
		UpdateShadowBlock();
		((OMMBlockImpl)shadowBlock).setPayload(getPayload(), OMMEntity.getDummyEntity());
		
		return new OMMAttributeListBlockImpl((OMMBlockImpl)shadowBlock);
	}

	/** Retrieves the shadowBblock as a regular OMMBlockImpl.
	 * @return The block as {@link OMMBlockImpl}.
	 * @throws Exception If conversion is attempted in SingleAccess mode.
	 */
	public OMMBlockImpl getAsRegularBlock()	{

		UpdateShadowBlock();
		if (getCreator() == null)
			shadowBlock.setPayload(getPayload(), OMMEntity.getDummyEntity());
		else shadowBlock.setPayload(getPayload(), getCreator());

		return shadowBlock;
	}
	
	/** Retrieves the current access mode.
	 * @return Current {@link OMMRestAccessMode} of this block.
	 */
	public OMMRestAccessMode getMode() { return mode; } 
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPrimaryID()
	 */
	public TypedValue getPrimaryID()
	{
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getPrimaryID();
		}
		
		Document d = this.getDoc("meta/primaryID");
		String link = d.getDocumentElement().getNodeValue();
		String attr = d.getDocumentElement().getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type");
		return new GenericTypedValue(attr, link);
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#getID()
	 */
	public String getID() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#setID(java.lang.String)
	 */
	public void setID(String id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#getNamespace()
	 */
	public URI getNamespace() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getNamespace();
		}
		
		Document d = this.getDoc("meta/namespace");
		try {
			return new URI(d.getDocumentElement().getNodeValue());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#getCreator()
	 */
	public OMMEntity getCreator() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getCreator();
		}
		
		Document d = this.getDoc("meta");
		Element root = d.getDocumentElement();
		Element creation = OMMXMLConverter.findChild(root, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":creation");
		Element creator = OMMXMLConverter.findChild(creation, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":creator");
		Element date = OMMXMLConverter.findChild(creation, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":date");
		OMMEntity e = new OMMEntity(creator.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type"), creator.getNodeValue(), date.getNodeValue());
		return e;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#getContributors()
	 */
	public OMMEntityCollection getContributors() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getContributors();
		}
		
		Document d = this.getDoc("meta");
		Element root = d.getDocumentElement();
		OMMEntityCollection conts = new OMMEntityCollection();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":contribution"))
			{
				Element contribution = (Element)node;
				Element contributor = OMMXMLConverter.findChild(contribution, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":contributor");
				Element date = OMMXMLConverter.findChild(contribution, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":date");
				OMMEntity e = new OMMEntity(contributor.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type"), contributor.getNodeValue(), date.getNodeValue());
				conts.add(e);
			}
		}
		return conts;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#getSubject()
	 */
	public OMMSubjectCollection getSubject() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getSubject();
		}
		
		Document d = this.getDoc("meta/subject");
		Element root = d.getDocumentElement();
		OMMSubjectCollection subjects = new OMMSubjectCollection();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element)
			{
				Element e = (Element)node;
				OMMSubjectTagType type = OMMSubjectTagType.valueOf(e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX + ":type"));
				String value = e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX + ":value");
				OMMSubjectTag tag = new OMMSubjectTag(type, value, null);
				subjects.add(tag);
			}
		}
		return subjects;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMToCEntry#isSubjectPresent(de.dfki.omm.types.OMMSubjectTagType, java.lang.String)
	 */
	public boolean isSubjectPresent(OMMSubjectTagType type, String value) {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.isSubjectPresent(type, value);
		}
		
		Document d = this.getDoc("meta/subject");
		Element root = d.getDocumentElement();
			
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element)
			{
				Element e = (Element)node;
				if (type.equals(OMMSubjectTagType.valueOf(e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type"))) && 
					value.equals(e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":value"))) 
				{
					return true;
				}	
			}
			
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getTitle()
	 */
	public OMMMultiLangText getTitle() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getTitle();
		}
		
		Document d = this.getDoc("meta/");
		Element root = d.getDocumentElement();
		OMMMultiLangText titles = new OMMMultiLangText();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":title"))
			{
				Element r = (Element)node;
				titles.put(new Locale(r.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":lang")), r.getNodeValue());
			}
		}

		return titles;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getTitle(java.util.Locale)
	 */
	public String getTitle(Locale language) {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getTitle(language);
		}
		
		Document d = this.getDoc("meta/");
		Element root = d.getDocumentElement();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":title"))
			{
				Element r = (Element)node;
				if (language.equals(new Locale(r.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":lang")))) 
				{
					return r.getNodeValue();
				}
			}
		}

		return "";
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setTitle(de.dfki.omm.types.OMMMultiLangText, de.dfki.omm.types.OMMEntity)
	 */
	public void setTitle(OMMMultiLangText text, OMMEntity entity) {
		/*for (Locale lang : text.keySet()) {
			ClientResource r = new ClientResource(this.url + "/block/"
					+ this.id + "/meta/title");
		}*/
		throw new UnsupportedOperationException("Not Implemented in REST Interface");

	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setTitle(java.util.Locale, java.lang.String, de.dfki.omm.types.OMMEntity)
	 */
	public void setTitle(Locale language, String title, OMMEntity entity) {
		/*String request = this.url + "/block/" + this.id + "/meta/title/";
		ClientResource c = new ClientResource(request);*/
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeTitle(java.util.Locale, de.dfki.omm.types.OMMEntity)
	 */
	public boolean removeTitle(Locale language, OMMEntity entity) {
		// FIXME keine Dokumentation zum Loeschen von Titeln
		return false;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getDescription()
	 */
	public OMMMultiLangText getDescription() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getDescription();
		}
		
		Document d = this.getMetaDoc();
		Element root = d.getDocumentElement();
		OMMMultiLangText descs = new OMMMultiLangText();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":description"))
			{
				Element e = (Element)node;
				descs.put(new Locale(e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":lang")), e.getNodeValue());
			}
		}		
		return descs;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale language) {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getDescription(language);
		}
		
		Document d = this.getMetaDoc();
		Element root = d.getDocumentElement();
		NodeList nl = root.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":description"))
			{
				Element e = (Element)node;
				String loc = e.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":lang");
				if (language.equals(new Locale(loc))) 
				{
					return e.getNodeValue();
				}
			}
		}	

		return "";
	}

	/** Retrieves the content of the block's /meta node.  
	 * @return The block's meta information as {@link Document}. 
	 */
	protected Document getMetaDoc() {
		return this.getDoc("meta/");
	}

	/** Retrieves a specific entry of the block's meta information.
	 * @param query Name of the requested information (for example <code>format</code>, <code>link</code>, <code>creator</code>). 
	 * @return The requested information as {@link Document}. 
	 */
	protected Document getDoc(String query)
	{
		String request = this.url + "/block/" + this.id + "/" + query;
		if (cache.containsKey(request))
		{
			long timeStamp = cache.get(request).getKey();
			if (new GregorianCalendar().getTime().getTime() - timeStamp < 1000*OMMRestImpl.REST_CACHE_TIME_IN_SECONDS)
				return cache.get(request).getValue();		
		}
		
		try {			
			ClientResource c = new ClientResource(request);
			OMSCredentials credentials = parentOMM.getCredentials();
			if (credentials != null) credentials.updateClientResource(c);
			//System.out.println(request);
			Representation r = c.get();
			Document d = OMMXMLConverter.getXmlDocumentFromString(r.getStream());
			cache.put(request, new AbstractMap.SimpleEntry<Long, Document>(new GregorianCalendar().getTime().getTime(), d));
			return d;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** Uses a SAXParser to parse the block on the OMS. 
	 * @return A {@link HashMap} of the block as {@link OMMBlockImpl} and its XML representation as {@link String}. 
	 */
	protected HashMap<OMMBlockImpl, String> getOMMBlockWithSAXParser()
	{
		String request = this.url + "/block/" + this.id + "/meta";
		
		try {			
			ClientResource c = new ClientResource(request);
			OMSCredentials credentials = parentOMM.getCredentials();
			if (credentials != null) credentials.updateClientResource(c);
			Representation r = c.get();
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            OMMBlockSaxHandler handler = new OMMBlockSaxHandler(this.id);
            xr.setContentHandler(handler);
            InputSource inStream = new InputSource();
            String text = r.getText();
            inStream.setCharacterStream(new StringReader(text));        
            xr.parse(inStream); 
            
			HashMap<OMMBlockImpl, String> hm = new HashMap<OMMBlockImpl, String>();
			hm.put(handler.getOMMBlock(), text);
			return hm;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Reloads the internal {@link OMMBlockImpl} used to store the block's contents, if it is either not set yet or expired. */
	protected void UpdateShadowBlock() 
	{
		long now = new GregorianCalendar().getTime().getTime();
		if (shadowBlock == null || (mode == OMMRestAccessMode.CompleteDownloadLimitedLifetime && (now - lastAccess) > OMMRestImpl.REST_CACHE_TIME_IN_SECONDS))
		{				
			lastAccess = now;
			/*Document d = this.getDoc("meta");
			shadowBlock = OMMXMLConverter.parseBlock(d.getDocumentElement(), id);*/
			HashMap<OMMBlockImpl, String> result = getOMMBlockWithSAXParser();
			Entry<OMMBlockImpl, String> entry = result.entrySet().iterator().next();
			shadowBlock = entry.getKey();
		}
		
//		System.out.println("shadowBlock:");
//		System.out.println(shadowBlock);
		
	}	
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setDescription(de.dfki.omm.types.OMMMultiLangText, de.dfki.omm.types.OMMEntity)
	 */
	public void setDescription(OMMMultiLangText text, OMMEntity entity) {
		// FIXME Momentan ist per REST nur die Standard Beschreibung erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setDescription(java.util.Locale, java.lang.String, de.dfki.omm.types.OMMEntity)
	 */
	public void setDescription(Locale language, String description,
			OMMEntity entity) {
		// FIXME Momentan ist per REST nur die Standard Beschreibung erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeDescription(java.util.Locale, de.dfki.omm.types.OMMEntity)
	 */
	public void removeDescription(Locale language, OMMEntity entity) {
		// FIXME Momentan ist per REST nur die Standard Beschreibung erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeDescriptions(de.dfki.omm.types.OMMEntity)
	 */
	public void removeDescriptions(OMMEntity entity) {
		// FIXME Momentan ist per REST nur die Standard Beschreibung erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setNamespace(java.net.URI, de.dfki.omm.types.OMMEntity)
	 */
	public void setNamespace(URI namespace, OMMEntity entity) {
		//FIXME: In der Dokumentation ist der Namespace als nicht aenderbar definiert
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getFormat()
	 */
	public OMMFormat getFormat() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getFormat();
		}
		
		Document d = this.getDoc("meta/format");
		Element root = d.getDocumentElement();
		String mime = root.getNodeValue();
		ClientResource c = new ClientResource(this.url + "/block/" + this.id + "/meta/encoding");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(c);
		Representation r = c.get();
		String enc = "";
		try {
			enc = r.getText();
		} catch (IOException e) {
			e.printStackTrace();
		}
		OMMFormat format = new OMMFormat(mime, null, enc);
		return format;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setFormat(de.dfki.omm.types.OMMFormat, de.dfki.omm.types.OMMEntity)
	 */
	public void setFormat(OMMFormat format, OMMEntity entity) {
		//FIXME: In der Dokumentation ist das Format als nicht aenderbar definiert
		throw new UnsupportedOperationException("Not Implemented in REST Interface");

	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeFormat(de.dfki.omm.types.OMMEntity)
	 */
	public void removeFormat(OMMEntity entity) {
		//FIXME: In der Dokumentation ist das Format als nicht aenderbar definiert
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getType()
	 */
	public URL getType() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
			return shadowBlock.getType();
		}
		
		Document d = this.getDoc("meta/type");
		try {
			URL type = new URL(d.getDocumentElement().getNodeValue());
			return type;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException("REST Error", e);
		}
		
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeType(de.dfki.omm.types.OMMEntity)
	 */
	public void removeType(OMMEntity entity) {
		//FIXME: In der Dokumentation ist der Typ als nicht aenderbar definiert
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setType(java.net.URL, de.dfki.omm.types.OMMEntity)
	 */
	public void setType(URL type, OMMEntity entity) {
		//FIXME: In der Dokumentation ist der Typ als nicht aenderbar definiert
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#addSubject(de.dfki.omm.types.OMMSubjectTag, de.dfki.omm.types.OMMEntity)
	 */
	public void addSubject(OMMSubjectTag subject, OMMEntity entity) {
		String sub = "<omm:subject xmlns:omm=\"http://www.w3.org/2005/Incubator/omm/elements/1.0/\"><omm:tag omm:type=\""+subject.getType()+"\" omm:value=\""+subject.getValue()+"\" /></omm:subject>";
		ClientResource r = new ClientResource(this.url + "/block/" + this.id + "/meta/subject");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(r);
		Representation rep = r.post(sub);
		try {
			System.out.println(rep.getText());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#changeSubject(de.dfki.omm.types.OMMSubjectTag, de.dfki.omm.types.OMMSubjectTag, de.dfki.omm.types.OMMEntity)
	 */
	public void changeSubject(OMMSubjectTag oldSubject,
			OMMSubjectTag newSubject, OMMEntity entity) {
		// FIXME Nicht alle Subjects sind erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeSubject(de.dfki.omm.types.OMMSubjectTag, de.dfki.omm.types.OMMEntity)
	 */
	public void removeSubject(OMMSubjectTag subject, OMMEntity entity) {
		// FIXME Nicht alle Subjects sind erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeSubjects(de.dfki.omm.types.OMMEntity)
	 */
	public void removeSubjects(OMMEntity entity) {
		// FIXME Nicht alle Subjects sind erreichbar
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPreviousLink()
	 */
	public OMMPreviousBlockLink getPreviousLink()	
	{
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
		}
		
		if (mode != OMMRestAccessMode.SingleAccess && shadowBlock != null)
		{
			return shadowBlock.getPreviousLink();
		}
		
		try
		{
			Document d = getDoc("meta/previousBlock");
			if (d != null)
			{
				String id = d.getDocumentElement().getNodeValue();
				String attr = d.getDocumentElement().getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":previousBlockType");
				return OMMPreviousBlockLink.createFromString(id, attr);
			}
		}
		catch(Exception e){}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#isLinkBlock()
	 */
	public boolean isLinkBlock() {
		return getLink() != null;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getLink()
	 */
	public TypedValue getLink() {
		
		if (mode != OMMRestAccessMode.SingleAccess)
		{
			UpdateShadowBlock();			
		}
		
		if (mode != OMMRestAccessMode.SingleAccess && shadowBlock != null)
		{
			return shadowBlock.getLink();
		}
		
		try
		{
			Document d = getDoc("meta/link");
			if (d != null)
			{
				String link = d.getDocumentElement().getNodeValue();
				String attr = d.getDocumentElement().getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type");
				return new GenericTypedValue(attr, link);
			}
		}
		catch(Exception e){}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getLinkHash()
	 */
	public String getLinkHash() {
		// FIXME Null POointers im Server
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setLink(de.dfki.omm.types.TypedValue, de.dfki.omm.types.OMMEntity)
	 */
	public void setLink(TypedValue link, OMMEntity entity) {
		// FIXME Null POointers im Server
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setLink(de.dfki.omm.types.TypedValue, java.lang.String, de.dfki.omm.types.OMMEntity)
	 */
	public void setLink(TypedValue link, String linkHash, OMMEntity entity) {
		// FIXME Null POointers im Server
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removeLink(de.dfki.omm.types.OMMEntity)
	 */
	public void removeLink(OMMEntity entity) {
		// FIXME Null POointers im Server
		throw new UnsupportedOperationException("Not Implemented in REST Interface");
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPayload()
	 */
	public TypedValue getPayload() {
		TypedValue v = new GenericTypedValue("base64", this.getPayloadAsString());
		return v;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPayloadAsString()
	 */
	public String getPayloadAsString() {		
		
		String url = this.url + "/block/" + this.id + "/payload";

//		return OMMXMLConverter.downloadURL(url);
		OMSCredentials credentials = parentOMM.getCredentials();
		return OMMXMLConverter.downloadURL(url,credentials);
		
		/*ClientResource c = new ClientResource(url);
		System.out.println("Reading payload via REST: "+url);
		Representation r = c.get();
		try {
			return r.getNodeValue();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("REST Error", e);
		}*/
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPayloadEncoding()
	 */
	public String getPayloadEncoding() {
		return this.getFormat().getEncryption();
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getPayloadElement()
	 */
	public Element getPayloadElement() {
		
		ClientResource c = new ClientResource(this.url + "/block/" + this.id + "/payload");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(c);
//		Representation r = c.get();

		return null; // ?!
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setPayload(de.dfki.omm.types.TypedValue, de.dfki.omm.types.OMMEntity)
	 */
	public void setPayload(TypedValue payload, OMMEntity entity) {
		ClientResource c = new ClientResource(this.url + "/block/" + this.id + "/payload");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(c);
		
		c.post(payload.getValue());

	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#setPayload(byte[], de.dfki.omm.types.OMMEntity)
	 */
	public void setPayload(byte[] payload, OMMEntity entity) {
		ClientResource c = new ClientResource(this.url + "/block/" + this.id + "/payload");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(c);
		
		c.post(payload);
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#removePayload(de.dfki.omm.types.OMMEntity)
	 */
	public void removePayload(OMMEntity entity) {
		ClientResource c = new ClientResource(this.url + "/block/" + this.id + "/payload");
		OMSCredentials credentials = parentOMM.getCredentials();
		if (credentials != null) credentials.updateClientResource(c);
		
		c.delete();
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMBlock#getXMLElement(boolean)
	 */
	public Element getXMLElement(boolean withPayload) {
		return null;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		if (shadowBlock == null) UpdateShadowBlock();
		
		if (shadowBlock == null)
		{
			return "BLOCK UNAVAILABLE";
		}
		return shadowBlock.toString();

	}

	/** Retrieves the internal {@link OMMBlockImpl} used to store the block's contents. */
	public OMMBlockImpl getShadowBlock()
	{
		if (shadowBlock == null) UpdateShadowBlock();
		return (OMMBlockImpl)shadowBlock;
	}
	
	protected static <K,V> Map<K,V> lruCache(final int maxSize) 
	{
	    return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) 
	    {
			private static final long serialVersionUID = -3016039276055185967L;

			@Override
	        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) 
	        {
	            return size() > maxSize;
	        }
	    };
	}

	@Override
	public String getJsonRepresentation() {

		JSONObject wrapper = new JSONObject();
		JSONObject jsonBlock = new JSONObject();
		try {
			
			// id
			jsonBlock.put("id",this.getID());
			
			// primary id
			if (this.getPrimaryID() != null)
				jsonBlock.put("primary_id", this.getPrimaryID());
			
			// namespace
			if (this.getNamespace() != null)
				jsonBlock.put("namespace", this.getNamespace());
			
			// format
			if (this.getFormat() != null)
				jsonBlock.put("format", this.getFormat().toString().replace(" (Encoding=)", ""));
			
			// creator
			OMMEntity creator = this.getCreator();
			if (creator != null) {
				JSONObject jsonCreation = new JSONObject();
				jsonCreation.put("type", creator.getType());
				jsonCreation.put("creator", creator.getValue());
				jsonCreation.put("date", creator.getDateAsISO8601());
				jsonBlock.put("creation", jsonCreation);
			}
			
			// contributor
			OMMEntityCollection contributors = this.getContributors();
			if (contributors != null) {
				JSONObject jsonContribution = new JSONObject();
				for (OMMEntity con : contributors) {
					JSONObject jsonContributor = new JSONObject();
					jsonContributor.put("type", con.getType());
					jsonContributor.put("value", con.getValue());
					jsonContributor.put("date", con.getDateAsISO8601());
					jsonContribution.put("contributor", jsonContributor);
				}
				jsonBlock.put("contribution", jsonContribution);
			}
			
			// title
			OMMMultiLangText title = this.getTitle();
			if (title != null) {
				JSONObject jsonTitle = new JSONObject();
				for (Entry<Locale, String> entry : title.entrySet()) 
					jsonTitle.put(entry.getKey().toString(), entry.getValue());
				jsonBlock.put("title", jsonTitle);
			}
			
			// description
			OMMMultiLangText description = this.getDescription();
			if (description != null) {
				JSONObject jsonDescription = new JSONObject();
				for (Entry<Locale, String> entry : description.entrySet()) 
					jsonDescription.put(entry.getKey().toString(), entry.getValue());
				jsonBlock.put("description", jsonDescription);
			}
			
			// type
			if (this.getType() != null) 
				jsonBlock.put("type", this.getType().toString());
			
			// subject
			OMMSubjectCollection subjects = this.getSubject();
			if (subjects != null) {
				JSONObject jsonSubjects = new JSONObject();
				JSONObject[] tags = new JSONObject[subjects.size()];
				for (int i = 0; i < subjects.size(); i++) {
					tags[i] = subjects.get(i).getJSONRepresentation();
				}
				jsonSubjects.put("tag", tags);
				jsonBlock.put("subject", jsonSubjects);
			}
			
			// link
			TypedValue link = this.getLink();
			if (link != null) {
				JSONObject jsonLink = new JSONObject();
				jsonLink.put("type", link.getType());
				jsonLink.put("value", link.getValue());
				jsonBlock.put("link", jsonLink);
			}
			
			// wrap it up
			wrapper.put("block", jsonBlock);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		String message = wrapper.toString();
		return message;
	}




//	/**
//	 * Custom method to deserialize OMMBlockRestImpls and their content properly
//	 *
//	 * @param inputStream Stream to read from
//	 * @throws IOException
//	 */
//	private synchronized void readObject(java.io.ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
//
//		System.out.println("OMMBlockRestImpl.readObject");
//
//		// read basic memory information
//		inputStream.defaultReadObject();
//
//		// read block metadata and content
//		TypedValue storedPrimaryId = null;					// read primary ID
//		Object loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof TypedValue) {
//			storedPrimaryId = (TypedValue) loadedInfo;
//		}
//		String storedId = null;								// read ID
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof String) {
//			storedId = (String) loadedInfo;
//		}
//		URI storedNamespace = null;							// read namespace
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof URI) {
//			storedNamespace = (URI) loadedInfo;
//		}
//		OMMFormat storedFormat = null;						// read format
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMFormat) {
//			storedFormat = (OMMFormat) loadedInfo;
//		}
//		OMMEntity storedCreator = null;						// read creator
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMEntity) {
//			storedCreator = (OMMEntity) loadedInfo;
//		}
//		OMMEntityCollection storedContributors = null;		// read contributors
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMEntityCollection) {
//			storedContributors = (OMMEntityCollection) loadedInfo;
//		}
//		OMMMultiLangText storedTitle = null;				// read title
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMMultiLangText) {
//			storedTitle = (OMMMultiLangText) loadedInfo;
//		}
//		OMMMultiLangText storedDescription = null;			// read description
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMMultiLangText) {
//			storedDescription = (OMMMultiLangText) loadedInfo;
//		}
//		URL storedType = null;								// read type
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof URL) {
//			storedType = (URL) loadedInfo;
//		}
//		TypedValue storedLink = null;						// read link
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof TypedValue) {
//			storedLink = (TypedValue) loadedInfo;
//		}
//		String storedLinkHash = null;						// read link hash
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof String) {
//			storedLinkHash = (String) loadedInfo;
//		}
//		OMMSubjectCollection storedSubject = null;			// read subject
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMSubjectCollection) {
//			storedSubject = (OMMSubjectCollection) loadedInfo;
//		}
//		OMMPreviousBlockLink storedPrevious = null;			// read previous element
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof OMMPreviousBlockLink) {
//			storedPrevious = (OMMPreviousBlockLink) loadedInfo;
//		}
//		TypedValue storedPayload = null;					// read payload
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof TypedValue) {
//			storedPayload = (TypedValue) loadedInfo;
//		}
//		Element storedPayloadElement = null;				// read payload element
//		loadedInfo = inputStream.readObject();
//		if (loadedInfo instanceof Element) {
//			storedPayloadElement = (Element) loadedInfo;
//		}
//
//		// create shadowBlock from the gathered info
//		shadowBlock = (OMMBlockImpl) OMMBlockImpl.create(storedId, storedPrimaryId, storedNamespace, storedType, storedTitle, storedDescription, storedContributors, storedCreator, storedFormat, storedSubject, storedPrevious, storedPayload, storedPayloadElement, storedLink, storedLinkHash);
//
//	}

}
