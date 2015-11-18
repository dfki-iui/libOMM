package de.dfki.omm.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.OMMAttributeListBlockImpl;
import de.dfki.omm.impl.OMMBlockImpl;
import de.dfki.omm.impl.OMMHeaderImpl;
import de.dfki.omm.impl.OMMIdentifierBlockImpl;
import de.dfki.omm.impl.OMMImpl;
import de.dfki.omm.impl.OMMStructureBlockImpl;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMAttributeListBlock;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.interfaces.OMMIdentifierBlock;
import de.dfki.omm.interfaces.OMMSemanticsBlock;
import de.dfki.omm.interfaces.OMMStructureBlock;
import de.dfki.omm.interfaces.OMMToCEntry;
import de.dfki.omm.types.BinaryValue;
import de.dfki.omm.types.GenericTypedValue;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMEntityCollection;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMPreviousBlockLink;
import de.dfki.omm.types.OMMSourceType;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.OMMSubjectTag;
import de.dfki.omm.types.OMMSubjectTagType;
import de.dfki.omm.types.TypedValue;
import de.dfki.omm.types.URLType;

/** Conversion tool to parse OMM elements from XML documents and create XML documents from OMM objects. */
public class OMMXMLConverter 
{
	public static final String OMM_NAMESPACE_URI = "http://www.w3.org/2005/Incubator/omm/elements/1.0/";
	public static final String OMM_NAMESPACE_PREFIX = "omm";
	
	public static final String OMM_IDS_BLOCK_NAMESPACE = "urn:omm:block:indentifications"; // TODO "n" on purpose?
	public static final String OMM_STRUCTURE_BLOCK_NAMESPACE = "urn:omm:block:structure";
	public static final String OMM_SEMANTICS_BLOCK_NAMESPACE = "urn:omm:block:semantic";
	public static final String OMM_ATTRIBUTES_BLOCK_SCHEMA = "http://www.w3.org/2005/Incubator/omm/elements/1.0/attributeList.xsd";
	
		
	/** Parses an {@link OMMHeader} from an XML Element. 
	 * @param root The {@link Element} containing the header. 
	 * @return The parsed header as {@link OMMHeaderImpl} or, if possible, {@link OMMSecurityHeaderImpl}. 
	 */
	public static OMMHeader parseHeader(Element root)
	{
		String primaryIDValue = null, primaryIDType = null, addBlockLink = null, addBlockType = null;
				
		for(int i = 0; i < root.getChildNodes().getLength(); i++)
		{
			Node child = root.getChildNodes().item(i);
			
			if (!(child instanceof Element)) continue;

			Element cElement = (Element)child;			

			if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":version"))
			{
				String vStr = cElement.getTextContent();
				int vInt = Integer.parseInt(vStr);
				if (vInt != OMMImpl.VERSION) throw new IllegalArgumentException("version mismatch!");
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":primaryID"))
			{
				primaryIDValue = cElement.getTextContent();
				primaryIDType = cElement.getAttributeNS(cElement.getNamespaceURI(), "type");
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":additionalBlocks"))
			{
				addBlockLink = cElement.getTextContent();
				addBlockType = cElement.getAttributeNS(cElement.getNamespaceURI(), "type");
			}
		}

		OMMHeaderImpl ommHeader = (OMMHeaderImpl)OMMHeaderImpl.create(getTypedValue(primaryIDType, primaryIDValue), getTypedValue(addBlockType, addBlockLink));
		return ommHeader;
	}
	
	/** Parses an {@link OMMBlock} from an XML Element. 
	 * @param e The {@link Element} containing the block. 
	 * @return The parsed block as {@link OMMBlockImpl} or, if possible, one of its more specified subtypes. 
	 */
	public static OMMBlock parseBlock(Element e)
	{
		return parseBlock(e, null);
	}
	
	/** Parses an {@link OMMBlock} from an XML Element. 
	 * @param e The {@link Element} containing the block. 
	 * @param blockID ID of the block to be parsed. 
	 * @return The parsed block as {@link OMMBlockImpl} or, if possible, one of its more specified subtypes. 
	 */
	public static OMMBlock parseBlock(Element e, String blockID)
	{
		if (blockID == null) blockID = e.getAttribute(OMM_NAMESPACE_PREFIX+":id");
		String linkHash = null;
		URI namespace = null;
		URL type = null;
		OMMFormat format = null;
		TypedValue link = null;
		TypedValue payload = null;
		Element payloadElement = null;
		OMMMultiLangText titles = new OMMMultiLangText();
		OMMMultiLangText descriptions = null;
		OMMEntity creator = null;
		OMMEntityCollection contributors = null;
		OMMSubjectCollection subjects = null;
		TypedValue primaryID = null;
		OMMPreviousBlockLink prevBlock = null;
		
		NodeList nodeList = e.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++)
		{
			Node node = nodeList.item(i);			
			if (!(node instanceof Element)) continue;

			Element cElement = (Element)node;
			if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":primaryID"))
			{
				primaryID = getTypedValue(cElement.getAttribute(OMM_NAMESPACE_PREFIX+":type"), cElement.getTextContent());
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":namespace"))
			{
				namespace = URI.create(cElement.getTextContent());
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":title"))
			{
				String langText = cElement.getAttribute("xml:lang"); 
				titles.put(new Locale(langText), cElement.getTextContent());
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":creation"))
			{
				Element creatorE = findChild(cElement, OMM_NAMESPACE_PREFIX+":creator");
				Element creatorD = findChild(cElement, OMM_NAMESPACE_PREFIX+":date");

				creator = new OMMEntity(creatorE.getAttribute(OMM_NAMESPACE_PREFIX+":type"), creatorE.getTextContent(), getISO8601String(creatorD.getAttribute(OMM_NAMESPACE_PREFIX+":encoding"), creatorD.getTextContent()));
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":contribution"))
			{
				Element creatorE = findChild(cElement, OMM_NAMESPACE_PREFIX+":contributor");
				Element creatorD = findChild(cElement, OMM_NAMESPACE_PREFIX+":date");
				
				OMMEntity contributor = new OMMEntity(creatorE.getAttribute(OMM_NAMESPACE_PREFIX+":type"), creatorE.getTextContent(), getISO8601String(creatorD.getAttribute(OMM_NAMESPACE_PREFIX+":encoding"), creatorD.getTextContent()));
				if (contributors == null) contributors = new OMMEntityCollection(); 
				contributors.add(contributor);
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":description"))
			{
				String langText = cElement.getAttribute("xml:lang");
				if (descriptions == null) descriptions = new OMMMultiLangText();
				descriptions.put(new Locale(langText), cElement.getTextContent());
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":format"))
			{
				try
				{
					String schemaValue = cElement.getAttribute(OMM_NAMESPACE_PREFIX+":schema");
					URL schema = null;
					if (schemaValue != null && !schemaValue.isEmpty()) schema = URI.create(schemaValue).toURL();
					
					String encryption = cElement.getAttribute(OMM_NAMESPACE_PREFIX+":encryption");
					
					format = new OMMFormat(cElement.getTextContent(), schema, encryption);
				}
				catch(Exception ex)				
				{
					ex.printStackTrace();
				}				
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":type"))
			{
				try
				{
					type = URI.create(cElement.getTextContent()).toURL();
				}
				catch(Exception ex) { ex.printStackTrace(); }
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":subject"))
			{
				NodeList cNodeList = cElement.getChildNodes();
				for(int k = 0; k < cNodeList.getLength(); k++)
				{
					Node subjChild = cNodeList.item(k); 
					if (!(subjChild instanceof Element)) continue;
					Element subElement = (Element)subjChild;
					
					OMMSubjectTag tag = getSubjectTag(subElement);
					if (tag != null) 
					{
						if (subjects == null) subjects = new OMMSubjectCollection();
						subjects.add(tag);						
					}
				}
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":previousBlock"))
			{
				prevBlock = OMMPreviousBlockLink.createFromString(cElement.getTextContent(), cElement.getAttribute(OMM_NAMESPACE_PREFIX+":previousBlockType"));
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":link"))
			{
				linkHash = cElement.getAttribute(OMM_NAMESPACE_PREFIX+":hash");
				link = getTypedValue(cElement.getAttribute(OMM_NAMESPACE_PREFIX+":type"), cElement.getTextContent());
			}
			else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":payload"))
			{
				String encoding = cElement.getAttribute(OMM_NAMESPACE_PREFIX+":encoding");				
				payloadElement = cElement;
				payload = getTypedValue(encoding, cElement.getTextContent());//decodePayload(payloadEncoding, cElement.getValue());
			}
		}
		
		OMMBlockImpl block = (OMMBlockImpl)OMMBlockImpl.create(blockID, primaryID, namespace, type, titles, descriptions, contributors, creator, format, subjects, prevBlock, payload, payloadElement, link, linkHash);

		// inline xml found -> might be a special OMM block
		if (namespace != null)
		{
			if (namespace.toString().equals("urn:omm:block:structure"))
			{
				OMMStructureBlock sBlock = new OMMStructureBlockImpl(block);
				return sBlock;
			}
			else if (namespace.toString().equals("urn:omm:block:indentifications"))
			{
				OMMIdentifierBlock iBlock = new OMMIdentifierBlockImpl(block);
				return iBlock;
			}
		}
		
		if (isOMMAttributesTemplate(block))
		{
			OMMAttributeListBlock aBlock = new OMMAttributeListBlockImpl(block);
			return aBlock;
		}			
		
//		System.out.println("-- parsed creator = "+creator.getType()+" / "+creator.getValue()+" / "+creator.getDateAsISO8601());
//		System.out.println("-- block is created : "+block.toString());
//		System.out.println("---");
		
		return block;
	}
	
	/** Converts an explicitly given type and value pair to a {@link TypedValue} object. 
	 * @param type The type.
	 * @param value The value. 
	 * @return A {@link TypedValue} object incorporating the given type and value, usually a {@link GenericTypedValue}, unless the type is "base64" or "uuencode", in which case the method returns a {@link BinaryValue}. 
	 */
	public static TypedValue getTypedValue(String type, String value)
	{
		if (type == null || value == null) return null;
		
		if (type.toLowerCase().equals("url"))
		{
			try 
			{
				return new URLType(URI.create(value).toURL());
			} 
			catch (Exception e) 
			{
				System.err.println("URL: "+value);
				e.printStackTrace();
			}
		}
		else if (type.toLowerCase().equals("base64") || type.toLowerCase().equals("uuencode"))
		{
			return new BinaryValue(type.toLowerCase(), value);
		}
				
		return new GenericTypedValue(type, value);
	}
	
	/** Creates an XML Document representing a given {@link OMMHeader}. 
	 * @param header The header to convert to XML. 
	 * @return A {@link Document} containing an XML representation of the header. 
	 */
	public static Document generateHeaderDocument(OMMHeader header)
	{
		try
		{
			Document doc = OMMXMLConverter.createNewXmlDocument();
			
			Element e = OMMXMLConverter.createXmlElementAndAppend(doc, "header", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			e.setAttribute("xmlns:"+OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			
			Element version = OMMXMLConverter.createXmlElement(doc, "version", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			version.setTextContent(header.getVersion()+"");
			e.appendChild(version);
			
			Element primaryID = OMMXMLConverter.createXmlElement(doc, "primaryID", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			primaryID.setTextContent(header.getPrimaryID().getValue()+"");
			primaryID.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", header.getPrimaryID().getType());
			e.appendChild(primaryID);
						
			return doc;
		}
		catch(Exception e) { e.printStackTrace(); } 
		return null;
	}
	
	/** Creates an XML Element representing a given {@link Collection} of {@link OMMToCEntry}s. 
	 * @param toc The ToC to convert to XML. 
	 * @return An {@link Element} containing an XML representation of the Table of Contents. 
	 */
	public static Element generateToCElement(Collection<OMMToCEntry> toc)
	{
		Document doc = OMMXMLConverter.createNewXmlDocument();
		
		Element eRoot = OMMXMLConverter.createXmlElementAndAppend(doc, "toc", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		
		for(OMMToCEntry tocEntry : toc)
		{
			Element e = OMMXMLConverter.createXmlElement(doc, "element", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			e.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":id", tocEntry.getID());
			List<Element> list = generateToCEntry(doc, tocEntry);
			for(Element entry : list) e.appendChild(entry);
			eRoot.appendChild(e);
		}		
		
		return eRoot;
	}

	/** Creates a {@link List} of XML {@link Element}s out of an empty XML Document and a ToC entry. 
	 * @param doc A new empty XML {@link Document} (can be obtained by calling {@link #createNewXmlDocument}). 
	 * @param toc An {@link OMMToCEntry} to convert to XML. 
	 * @return List of the created Elements. 
	 */
	public static List<Element> generateToCEntry(Document doc, OMMToCEntry toc) 
	{
		List<Element> retVal = new LinkedList<Element>();
		
		if (toc.getNamespace() != null)
		{
			Element tNamespace = OMMXMLConverter.createXmlElement(doc, "namespace", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tNamespace.setTextContent(toc.getNamespace().toString());
			retVal.add(tNamespace);
		}
		
		// creation
		retVal.add(generateCreator(doc, toc.getCreator()));
		
		// contribution
		for(Element element : generateContributors(doc, toc.getContributors()))
		{
			retVal.add(element);	
		}
		
		// title
		retVal.addAll(generateTitle(doc, toc.getTitle()));
		
		// subject
		if (toc.getSubject() != null)
			for(OMMSubjectTag tag : toc.getSubject())
			{
				retVal.add(getElementForTag(doc, tag));
			}
		
		return retVal;
	}
	
	/** Creates a new empty {@link Document} to use as a template to fill with contents. 
	 * @return The bare XMl Document. 
	 */
	public static Document createNewXmlDocument()
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.newDocument();
			doc.setXmlStandalone(true);
			return doc;
		}
		catch(Exception e) { e.printStackTrace(); }
		return null;
	}

	/** Creates an XML Element using an XML Document and the element's attributes. 
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}). 
	 * @param name The element's name.
	 * @param prefix The element's name prefix (such as "omm"). 
	 * @param namespaceURI The namespace URI to use for this element. 
	 * @return The created {@link Element}. 
	 */
	public static Element createXmlElement(Document doc, String name, String prefix, String namespaceURI)
	{
		return doc.createElementNS(namespaceURI, prefix+":"+name);
	}

	/** Creates an XML Element using an XML Document and the element's name. 
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}). 
	 * @param name The element's name.
	 * @return The created {@link Element}. 
	 */
	public static Element createXmlElement(Document doc, String name)
	{
		return doc.createElement(name);
	}

	/** Creates an XML Element using an XML Document and the element's attributes, and adds the element to the document. 
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}). 
	 * @param name The element's name.
	 * @param prefix The element's name prefix (such as "omm"). 
	 * @param namespaceURI The namespace URI to use for this element. 
	 * @return The created {@link Element} (The element is added to the document as a side effect). 
	 */
	public static Element createXmlElementAndAppend(Document doc, String name, String prefix, String namespaceURI)
	{
		Element retVal = doc.createElementNS(namespaceURI, prefix+":"+name);
		doc.appendChild(retVal);
		return retVal;
	}

	/** Converts a String from an {@link InputStream} to an XML Document. 
	 * @param is Input stream delivering the String. 
	 * @return XML {@link Document} built from the input (null if no document could be created). 
	 */
	public static Document getXmlDocumentFromString(InputStream is)
	{
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true); // <-- very important!
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(is);
			return doc;
		}
		catch(Exception e) { e.printStackTrace(); }
		
		return null;
	}
	
	/** Creates an XML Document containing a complete representation of an OMMBlock. 
	 * @param block The {@link OMMBlock} to represent in XMl. 
	 * @param withPayload True, if the payload is to be included in the representation. 
	 * @return The block as an XMl {@link Document}. 
	 */
	public static Document generateCompleteBlock(OMMBlock block, boolean withPayload)
	{
		try
		{	
			Document doc = createNewXmlDocument();
			
			Element e = createXmlElementAndAppend(doc, "block", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			// <-- THE FOLLOWING LINE IS VERY IMPORTANT!!! -->
			e.setAttribute("xmlns:"+OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			e.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":id", block.getID());
			
			List<Element> elements = generateBlock(doc, block, withPayload);
			for(Element element : elements)
			{
				e.appendChild(doc.importNode(element, true));
			}

			return doc;
		}
		catch(Exception e ) { e.printStackTrace(); }
		
		return null;
	}

	/** Creates a List of XML Elements that describe a given block.  
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param block The block to represent in XML. 
	 * @param withPayload True, if the payload is to be included in the representation. 
	 * @return A {@link List} of XML {@link Element}s that represent the block. 
	 */
	public static List<Element> generateBlock(Document doc, OMMBlock block, boolean withPayload) 
	{	
		List<Element> retVal = new LinkedList<Element>();
		
		if (block.getPrimaryID() != null)
		{
			Element tpID = OMMXMLConverter.createXmlElement(doc, "primaryID", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tpID.setTextContent(block.getPrimaryID().getValue()+"");
			tpID.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", block.getPrimaryID().getType());
			retVal.add(tpID);
		}
		
		if (block.getNamespace() != null)
		{
			Element tNamespace = OMMXMLConverter.createXmlElement(doc, "namespace", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tNamespace.setTextContent(block.getNamespace().toString());
			retVal.add(tNamespace);
		}
		
		// creation
		retVal.add(generateCreator(doc, block.getCreator()));
		
		// contribution
		if (block.getContributors() != null)
		{
			retVal.addAll(generateContributors(doc, block.getContributors()));
		}
		
		// title
		retVal.addAll(generateTitle(doc, block.getTitle()));
		
		// description
		if (block.getDescription() != null)
		{
			retVal.addAll(generateDescription(doc, block.getDescription()));
		}
		
		if (block.getFormat() != null)
		{
			retVal.add(generateFormat(doc, block.getFormat()));
		}
		
		if (block.getType() != null)
		{
			Element tType = OMMXMLConverter.createXmlElement(doc, "type", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tType.setTextContent(block.getType().toString());
			retVal.add(tType);
		}
		
		// subject
		if (block.getSubject() != null && block.getSubject().size() > 0)
		{
			Element tSubject = OMMXMLConverter.createXmlElement(doc, "subject", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			
			for(OMMSubjectTag tag : block.getSubject())
			{				
				tSubject.appendChild(getElementForTag(doc, tag));				
			}	
			retVal.add(tSubject);
		}		
		
		if (block.getPreviousLink() != null)
		{
			Element tprevBlock = OMMXMLConverter.createXmlElement(doc, "previousBlock", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tprevBlock.setTextContent(block.getPreviousLink().getBlockID()+"");
			tprevBlock.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":previousBlockType", block.getPreviousLink().getType().toString().toLowerCase());
			retVal.add(tprevBlock);
		}
		
		if (block.getLink() != null)
		{
			Element tLink = OMMXMLConverter.createXmlElement(doc, "link", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tLink.setTextContent(block.getLink().getValue()+"");
			tLink.setAttribute("type", block.getLink().getType());
			tLink.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", block.getLink().getType());
			if (block.getLinkHash() != null) tLink.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":hash", block.getLinkHash());
			retVal.add(tLink);
		}

		if (!withPayload) return retVal;

		Element payload = generatePayload(doc, block);
		if (payload != null) retVal.add(payload);
		
		return retVal;
	}

	/** Creates an XML Element representing a block's payload.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param block A {@link OMMBlock}.
	 * @return An XML {@link Element} containing the payload of the given block. 
	 */
	public static Element generatePayload(Document doc, OMMBlock block)
	{
		if (block.getPayloadElement() != null)
		{			
			Element payloadElement = block.getPayloadElement();
			
			if (!payloadElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":payload"))
			{
				Element payloadE = OMMXMLConverter.createXmlElement(doc, "payload", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
				payloadE.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":encoding", "none");
				payloadE.appendChild(doc.importNode(payloadElement, true));
				payloadElement = payloadE;
			}
			
			return payloadElement;			
		}
		else if (block.getPayload() != null)
		{
			Element tPayload = OMMXMLConverter.createXmlElement(doc, "payload", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tPayload.setTextContent(block.getPayload().getValue()+"");
			tPayload.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX + ":encoding", block.getPayload().getType());
			return tPayload;
		}		
		
		return null;
	}

	/** Creates an XML Element representing a block's format entry.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param format The block's {@link OMMFormat}. 
	 * @return An XML {@link Element} containing the format information of a block. 
	 */
	public static Element generateFormat(Document doc, OMMFormat format)
	{
		Element tFormat = OMMXMLConverter.createXmlElement(doc, "format", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		tFormat.setTextContent(format.getMIMEType());
		if (format.getSchema() != null) tFormat.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":schema", format.getSchema().toString());
		if (format.getEncryption() != null) tFormat.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":encryption", format.getEncryption());
		return tFormat;
	}

	/** Creates an XML Element representing a block's creator entry.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param creator The block's creator as {@link OMMEntity}. 
	 * @return An XML {@link Element} containing the creator information of a block. 
	 */
	public static Element generateCreator(Document doc, OMMEntity creator)
	{
		Element tCreation = OMMXMLConverter.createXmlElement(doc, "creation", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		Element tCreationV = OMMXMLConverter.createXmlElement(doc, "creator", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		tCreationV.setTextContent(creator.getValue());
		tCreationV.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", creator.getType());

		Element tCreationD = OMMXMLConverter.createXmlElement(doc, "date", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		tCreationD.setTextContent(creator.getDateAsISO8601());
		tCreationD.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":encoding", "ISO8601");
		tCreation.appendChild(tCreationV);
		tCreation.appendChild(tCreationD);		

		return tCreation;
	}

	/** Creates a List of XML Elements representing a block's contributors entry.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param creator The block's contributors as {@link OMMEntityCollection}. 
	 * @return A {@link List} of XML {@link Element}s containing the contributors information of a block. 
	 */
	public static List<Element> generateContributors(Document doc, OMMEntityCollection contributors)
	{
		List<Element> retVal = new Vector<Element>(0);

		if (contributors != null) retVal = new Vector<Element>(contributors.size());
		else return retVal;
		
		for(OMMEntity contributor : contributors)
		{
			Element tContrib = OMMXMLConverter.createXmlElement(doc, "contribution", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			Element tContribV = OMMXMLConverter.createXmlElement(doc, "contributor", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tContribV.setTextContent(contributor.getValue());
			tContribV.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", contributor.getType());

			Element tContribD = OMMXMLConverter.createXmlElement(doc, "date", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tContribD.setTextContent(contributor.getDateAsISO8601());
			tContribD.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":encoding", "ISO8601");
			tContrib.appendChild(tContribV);
			tContrib.appendChild(tContribD);
			retVal.add(tContrib);	
		}
		
		return retVal;
	}

	/** Creates an List of XML Elements representing a block's title entry.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param title The block's title as {@link OMMMultiLangText}. 
	 * @return A {@link List} of XML {@link Element}s containing the title information of a block. 
	 */
	public static List<Element> generateTitle(Document doc, OMMMultiLangText title)
	{
		List<Element> retVal = new Vector<Element>(title.size());
		
		for(Map.Entry<Locale, String> titlelang : title.entrySet())
		{
			Element tTitle = OMMXMLConverter.createXmlElement(doc, "title", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tTitle.setTextContent(titlelang.getValue());
			tTitle.setAttribute("xml:lang", titlelang.getKey().getLanguage());
			retVal.add(tTitle);
		}
		
		return retVal;
	}	
	
	/** Creates an List of XML Elements representing a block's description entry.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param descriptions The block's description as {@link OMMMultiLangText}. 
	 * @return A {@link List} of XML {@link Element}s containing the description information of a block. 
	 */
	public static List<Element> generateDescription(Document doc, OMMMultiLangText descriptions)
	{
		List<Element> retVal = new Vector<Element>(descriptions.size()); 
		
		for(Map.Entry<Locale, String> description : descriptions.entrySet())
		{
			Element tDesc = OMMXMLConverter.createXmlElement(doc, "description", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
			tDesc.setTextContent(description.getValue());
			tDesc.setAttribute("xml:lang", description.getKey().getLanguage());
			retVal.add(tDesc);
		}
		
		return retVal;
	}
	
	/** Creates an XML Element representing a subject tag.
	 * @param doc An XML {@link Document} which may be empty (can be obtained by calling {@link #createNewXmlDocument}).
	 * @param tag The {@link OMMSubjectTag} to be represented in XML.  
	 * @return An XML {@link Element} modeling the subject tag. 
	 */
	public static Element getElementForTag(Document doc, OMMSubjectTag tag)
	{
		if (tag == null) return null;
		
		Element tTag = OMMXMLConverter.createXmlElement(doc, "tag", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		tTag.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":type", tag.getType().toString());
		tTag.setAttributeNS(OMM_NAMESPACE_URI, OMM_NAMESPACE_PREFIX+":value", tag.getValue());

		Element child = getElementForTag(doc, tag.getChild());
		if (child != null) tTag.appendChild(child);
		
		return tTag;
	}

	/** Converts an XML node to a String representation to use as out- or input. 
	 * @param node The {@link Node} to convert. 
	 * @return A String representation of the XML node. 
	 */
	public static String toXMLFileString(Node node)
	{
		try
		{
			StringWriter output = new StringWriter();
						
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			//transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    transformer.transform(new DOMSource(node), new StreamResult(output));
		    String xml = output.toString();
		    return xml;
		}
		catch(Exception e) { e.printStackTrace(); }
		return null;		
	}
		
	/** Converts a block's Table of Contents to a String representation to use as out- or input. 
	 * @param toc The Table of Contents as a {@link Collection} of {@link OMMToCEntry}s. 
	 * @return A String representation of the Table of Contents. 
	 */
	public static String toXMLFileString(Collection<OMMToCEntry> toc)
	{
		Document doc = OMMXMLConverter.createNewXmlDocument();
		
		Element root = OMMXMLConverter.createXmlElement(doc, "omm", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		
		Element eToC = generateToCElement(toc);
//		doc.importNode(eToC, true);
//		root.appendChild(eToC);
		root.appendChild(doc.importNode(eToC, true));
		
		return toXMLFileString(root);
	}
	
	/** Creates an OMM from an XML representation stored in a local file. 
	 * @param xmlfile The file containing the XML text. 
	 * @return The created {@link OMM} (or null if none could be parsed). 
	 */
	public static OMM loadFromXmlFile(File xmlfile)
	{
		try 
		{
			return loadFromXmlStringReader(new FileInputStream(xmlfile), null, xmlfile, OMMSourceType.LocalFile);
		} 
		catch (Exception e) { e.printStackTrace(); }
		
		return null;
	}
		
	/** <p>Creates an OMM from an XML representation given as String with its respective source where the OMM will 
	 * be stored and loaded at restart of the server. The source can be either a {@link URL} or a {@link File} and 
	 * has to be described by a {@link OMMSourceType}. </p>
	 * <p>With one source given the other can be null, but not both.</p>
	 * @param xmlText An XML representation of an object memory.
	 * @param urlSource Source of the OMM as {@link URL}. 
	 * @param Source of the OMM as a {@link File}. 
	 * @param sourceType The {@link OMMSourceType} of the source. 
	 * @return The loaded memory as a {@link OMM}. 
	 */
	public static OMM loadFromXmlString(String xmlText, URL urlSource, File fileSource, OMMSourceType sourceType)
	{		
		return loadFromXmlStringReader(getInputStreamFromText(xmlText), urlSource, fileSource, sourceType);
	}
		
	/** <p>Creates an OMM from an XML representation given through an input stream with its respective source where 
	 * the OMM will be stored and loaded at restart of the server. The source can be either a {@link URL} or a 
	 * {@link File} and has to be described by a {@link OMMSourceType}. </p>
	 * <p>With one source given the other can be null, but not both.</p>
	 * @param xml An XML representation wrapped in an {@link InputStream}.
	 * @param urlSource Source of the OMM as {@link URL}. 
	 * @param Source of the OMM as a {@link File}. 
	 * @param sourceType The {@link OMMSourceType} of the source. 
	 * @return The loaded memory as a {@link OMM}. 
	 */
	public static OMM loadFromXmlStringReader(InputStream xml, URL urlSource, File fileSource, OMMSourceType sourceType)
	{		
		
		try
		{
			Document doc = getXmlDocumentFromString(xml);
			
			Element root = doc.getDocumentElement();
			if (!root.getNodeName().equals(OMM_NAMESPACE_PREFIX+":omm")) throw new IllegalArgumentException("xml file is not valid");
			
			OMMHeader header = null;
			Collection<OMMBlock> blocks = new LinkedList<OMMBlock>();
			
			NodeList nl = root.getChildNodes();
			for(int i = 0; i < nl.getLength(); i++)
			{
				Node child = nl.item(i);
				
				if (child instanceof Element)					
				{
					Element cElement = (Element)child;
					if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":header"))
					{
						header = OMMXMLConverter.parseHeader(cElement);
					}
					else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":block"))
					{
						OMMBlock block = OMMXMLConverter.parseBlock(cElement);
						if (block != null) blocks.add(block);						
					}
					else if (cElement.getNodeName().equals(OMM_NAMESPACE_PREFIX+":toc")) { /* IGNORE */ }
				}
			}
			
			if (header == null) throw new IllegalArgumentException("xml file is not valid");
			
			OMMImpl omm = null;
			if (sourceType == OMMSourceType.LocalFile)
				omm = (OMMImpl) OMMImpl.create(header, blocks, fileSource, sourceType);
			else 
				omm = (OMMImpl) OMMImpl.create(header, blocks, urlSource, sourceType);
			
			
			for(OMMBlock block : blocks)
			{
				((OMMBlockImpl)block).setParentOMM(omm);
			}
			
			return omm;			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/** Converts an object memory to a representative String. 
	 * @param omm The {@link OMM} to represent as String. 
	 * @param withToC True, if the Table of Contents is to be included.
	 * @return A String representation of the OMM. 
	 */
	public static String toXMLFileString(OMM omm, boolean withToC)
	{	
		Document doc = createNewXmlDocument();
		Element root = createXmlElementAndAppend(doc, "omm", OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		root.setAttribute("xmlns:"+OMM_NAMESPACE_PREFIX, OMM_NAMESPACE_URI);
		
		Element eHeader = OMMXMLConverter.generateHeaderDocument(omm.getHeader()).getDocumentElement();
		root.appendChild(doc.importNode(eHeader, true));
					
		if (withToC)
		{
			Element eToC = OMMXMLConverter.generateToCElement(omm.getTableOfContents());
			root.appendChild(doc.importNode(eToC, true));			
		}
		
		for(OMMBlock block : omm.getAllBlocks())
		{
			Element eBlock = generateCompleteBlock(block, true).getDocumentElement();
			root.appendChild(doc.importNode(eBlock, true));
		}		
				
		return toXMLFileString(root);
	}
	
	/** Retrieves the GET output of a REST node as String. 
	 * @param urlString URL to the REST node. 
	 * @param credentials {@link OMSCredentials} to use when accessing the node. 
	 * @return A String representation of the node's output (null, if none could be retrieved). 
	 */
	public static String downloadURL(String urlString, OMSCredentials credentials) {
		
		InputStream is = null;
	    String line;
	    StringBuffer buffer = new StringBuffer();

	    try {
	    	ClientResource c = new ClientResource(urlString);
			if (credentials != null) credentials.updateClientResource(c);
			Representation representation = c.get();
			if (representation == null) return null;
			BufferedReader br = new BufferedReader(representation.getReader());

	        while ((line = br.readLine()) != null) {
	        	buffer.append(line); 
	        }
	    } catch (MalformedURLException mue) {
	         mue.printStackTrace();
	    } catch (IOException ioe) {
	         ioe.printStackTrace();
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
	    }
	    
	    return (buffer == null ? null : buffer.toString());
	}
	
//	public static String downloadURLOld (String urlString) {
//	    URL url;
//	    InputStream is = null;
//	    BufferedReader br;
//	    String line;
//	    StringBuffer sb = null;
//
//	    try {
//	        url = new URL(urlString);
//	        is = url.openStream();  // throws an IOException
//	        br = new BufferedReader(new InputStreamReader(is));
//	        sb = new StringBuffer();
//
//	        while ((line = br.readLine()) != null) {
//	           sb.append(line); 
//	        }
//	    } catch (MalformedURLException mue) {
//	         mue.printStackTrace();
//	    } catch (IOException ioe) {
//	         ioe.printStackTrace();
//	    } finally {
//	        try {
//	            if (is != null) is.close();
//	        } catch (IOException ioe) {
//	            // nothing to see here
//	        }
//	    }
//	    
//	    return (sb == null ? null : sb.toString());
//	}
		
	/** Checks whether a block is an Identifier Block. 
	 * @param block The {@link OMMBlock} to check. 
	 * @return True, if the block is an {@link OMMIdentifierBlock}. 
	 */
	public static boolean isOMMIDsBlock(OMMBlock block)
	{
		if (block == null) return false;
		URI namespace = block.getNamespace();
		if (namespace == null) return false;
		
		return isOMMIDsBlock(namespace);
	}
	
	/** Checks whether a namespace URI is identical to the Identifier Block URI. 
	 * @param namespace The namespace to check as a {@link URI}. 
	 * @return True, if the given URI equals {@value #OMM_IDS_BLOCK_NAMESPACE}. 
	 */
	public static boolean isOMMIDsBlock(URI namespace)
	{
		return (namespace.toString().equals(OMM_IDS_BLOCK_NAMESPACE));
	}
	
	/** Checks whether a block is a Structure Block. 
	 * @param block The {@link OMMBlock} to check. 
	 * @return True, if the block is an {@link OMMStructureBlock}. 
	 */
	public static boolean isOMMStructureBlock(OMMBlock block)
	{
		if (block == null) return false;
		URI namespace = block.getNamespace();
		if (namespace == null) return false;
		
		return isOMMStructureBlock(block.getNamespace());
	}
	
	/** Checks whether a namespace URI is identical to the Structure Block URI. 
	 * @param namespace The namespace to check as a {@link URI}. 
	 * @return True, if the given URI equals {@value #OMM_STRUCTURE_BLOCK_NAMESPACE}. 
	 */
	public static boolean isOMMStructureBlock(URI namespace)
	{
		return (namespace.toString().equals(OMM_STRUCTURE_BLOCK_NAMESPACE));
	}
	
	/** Checks whether a block is a Semantics Block. 
	 * @param block The {@link OMMBlock} to check. 
	 * @return True, if the block is an {@link OMMSemanticsBlock}. 
	 */
	public static boolean isOMMSemanticsBlock(OMMBlock block)
	{
		if (block == null) return false;
		URI namespace = block.getNamespace();
		if (namespace == null) return false;
		
		return isOMMSemanticsBlock(block.getNamespace());
	}
		
	/** Checks whether a namespace URI is identical to the Semantics Block URI. 
	 * @param namespace The namespace to check as a {@link URI}. 
	 * @return True, if the given URI equals {@value #OMM_SEMANTICS_BLOCK_NAMESPACE}. 
	 */
	public static boolean isOMMSemanticsBlock(URI namespace)
	{
		return (namespace.toString().equals(OMM_SEMANTICS_BLOCK_NAMESPACE));
	}
	
	/** Checks whether a block is a Attribute List Block. 
	 * @param block The {@link OMMBlock} to check. 
	 * @return True, if the block is an {@link OMMAttributeListBlock}. 
	 */
	public static boolean isOMMAttributesTemplate(OMMBlock block)
	{
		if (block instanceof OMMAttributeListBlock) return true;
		if (block.getFormat() != null && block.getFormat().getMIMEType() != null && block.getFormat().getMIMEType().equals("application/xml") && block.getFormat().getSchema() != null && block.getFormat().getSchema().toString().equals(OMM_ATTRIBUTES_BLOCK_SCHEMA)) return true;
		return false;
	}

	/** Finds and returns a specific child in an XML element. 
	 * @param root The XML element to inspect. 
	 * @param childName The element name to search for. 
	 * @return The searched child {@link Element} (or null, if no match was found). 
	 */
	public static Element findChild(Element root, String childName)
	{
		NodeList nl = root.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element)
			{
				Element element = (Element)node;
				if (element.getNodeName().equals(childName)) return element;
			}
		}
		
		return null;
	}
	
	/** Retrieves the first child of an XML Element. 
	 * @param root The element of which to get the first child Element. 
	 * @return First child {@link Element} of the given root (or null, if there isn't one). 
	 */
	public static Element firstElement(Element root)
	{
		NodeList nl = root.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if (node instanceof Element) return (Element)node;
		}
		
		return null;
	}
	
	/** Removes the XML header from a given String. 
	 * @param text String from which to remove the header.
	 * @return The cleansed String (or the unaltered String, if it does not include a header). 
	 */
	public static String removeXMLHeader(String text)
	{
		int pos = text.indexOf("?>");
		if (pos < 0) return text;
		return text.substring(pos + 2);
	}

	/** Returns an ISO8601 date from a given type/value-pair if the pair describes one.
	 * @param type The type of the given value. 
	 * @param value The value to parse as an ISO8601 date, if possible. 
	 * @return The value, if the pair describes an ISO8601 date, otherwise null. 
	 */
	public static String getISO8601String(String type, String value)
	{
		if (type == null) return null;
		if (type.toLowerCase().equals("iso8601")) return value;
		
		return null;
	}

	/** Retrieves an OMM's name from its URL. 
	 * @param omsURL The address of the object memory. 
	 * @return The name of the object memory (as the last node in the given URL). 
	 */
	public static String getMemoryName(String omsURL)
	{
		try
		{
			URL url = new URL(omsURL);
			String idStr = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			return idStr;
		}
		catch(Exception e) { e.printStackTrace(); }
		
		return omsURL;
	}
	
	/** Wraps a String in an input stream. 
	 * @param text The String to wrap. 
	 * @return An {@link InputStream} wrapping the input String (or null, if it causes an error). 
	 */
	public static InputStream getInputStreamFromText(String text)
	{
		try
		{
			return new ByteArrayInputStream(text.getBytes("UTF-8"));
		}
		catch(Exception e) { e.printStackTrace(); }
		
		return null;
	}
	
	/** Retrieves a subject tag from an XML node. 
	 * @param node The XML {@link Element} to parse. 
	 * @return A {@link OMMSubjectTag} derived from the node. 
	 */
	public static OMMSubjectTag getSubjectTag(Element node)
	{
		if (node == null) return null;
		
		OMMSubjectTagType type = OMMSubjectTagType.Text;
		if (node.getAttribute(OMM_NAMESPACE_PREFIX+":type").equals("Ontology")) type = OMMSubjectTagType.Ontology;
		
		OMMSubjectTag tag = new OMMSubjectTag(type, node.getAttribute(OMM_NAMESPACE_PREFIX+":value"), getSubjectTag(findChild(node, OMM_NAMESPACE_PREFIX+":tag")));
		return tag;
	}
}
