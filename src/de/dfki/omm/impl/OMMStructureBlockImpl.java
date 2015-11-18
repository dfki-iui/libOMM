package de.dfki.omm.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.dfki.omm.interfaces.OMMStructureBlock;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.ISO8601;
import de.dfki.omm.types.OMMStructureInfo;
import de.dfki.omm.types.OMMStructureRelation;
import de.dfki.omm.types.TypedValue;

/** Implementation of {@link OMMStructureBlock}. 
 * A structure block allows the definition of relationships between itself and others, as specified in an {@link OMMStructureInfo}. */
public class OMMStructureBlockImpl extends OMMBlockImpl implements OMMStructureBlock
{
	LinkedList<OMMStructureInfo> m_structureInfos = null;
	
	@SuppressWarnings("unused")
	private OMMStructureBlockImpl() 
	{
		//super();
	}
	
	/** Constructor.
	 * @param block An {@link OMMBlockImpl} to use for the construction of a OMMStructureBlockImpl. 
	 */
	public OMMStructureBlockImpl(OMMBlockImpl block)
	{
		this.m_ID = block.m_ID;
		this.m_primaryID = block.m_primaryID;
		this.m_namespace = block.m_namespace;
		this.m_type = block.m_type;
		this.m_title = block.m_title;
		this.m_description = block.m_description;
		this.m_contributors = block.m_contributors;
		this.m_creator = block.m_creator;
		this.m_format = block.m_format;
		this.m_subject = block.m_subject;
		this.m_previousBlock = block.m_previousBlock;
		this.m_payload = block.m_payload;
		this.m_payloadElement = block.m_payloadElement;
		this.m_link = block.m_link;
		this.m_linkHash = block.m_linkHash;
		this.m_structureInfos = new LinkedList<OMMStructureInfo>();
		updateLocalData();
	}

	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMStructureBlock#getStructureInfos()
	 */
	public Collection<OMMStructureInfo> getStructureInfos()
	{
		return m_structureInfos;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMStructureBlock#getStructureInfoByType(de.dfki.omm.types.OMMStructureRelation)
	 */
	public Collection<OMMStructureInfo> getStructureInfoByType(OMMStructureRelation relationType)
	{
		LinkedList<OMMStructureInfo> retVal = new LinkedList<OMMStructureInfo>();
		
		for(OMMStructureInfo info : m_structureInfos)
		{
			if (info.getRelationType() == relationType) retVal.add(info);
		}
		
		return retVal;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMStructureBlock#addStructureInfo(de.dfki.omm.types.OMMStructureInfo)
	 */
	public void addStructureInfo(OMMStructureInfo info)
	{
		m_structureInfos.add(info);
		updatePayload();
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMStructureBlock#removeStructureInfo(de.dfki.omm.types.OMMStructureInfo)
	 */
	public void removeStructureInfo(OMMStructureInfo info)
	{
		m_structureInfos.remove(info);
		updatePayload();
	}

	/** Private helper method to initialize the {@link OMMStructureInfo} representation in the block's payload. */
	private void updateLocalData()
	{
		if (m_payloadElement == null && m_payload.getValue() instanceof String)
		{
			String v = (String)m_payload.getValue();
			try
			{
				Document doc = OMMXMLConverter.getXmlDocumentFromString(OMMXMLConverter.getInputStreamFromText(v));
				m_payloadElement = doc.getDocumentElement();				
			}
			catch(Exception e){e.printStackTrace(); return;}
		}
		
		if (m_payloadElement != null)// && (m_payload.getType() == null || m_payload.getType().equals("none")))
		{	
			try
			{
				/*String xml = (String)m_payload.getValue();
				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + 
					  "<omm xmlns=\"http://www.w3.org/2005/Incubator/omm/elements/1.0/\"" +
					  "xmlns:omm=\"http://www.w3.org/2005/Incubator/omm/elements/1.0/\">" +
					  xml;
				
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build((String)m_payload.getValue());*/
				
				Element root = m_payloadElement;// doc.getRootElement();
				
				if (root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":payload") && root.getChildNodes().getLength() > 0)
				{
					root = OMMXMLConverter.firstElement(root);
				}
				
				if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":structure")) throw new IllegalArgumentException("invalid XML file!");
				
				NodeList nl = root.getChildNodes();
				for(int i = 0; i < nl.getLength(); i++)
				{
					Node node = nl.item(i);
					if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":structureInformation"))
					{					
						Element child = (Element)node;
						OMMStructureInfo info = getStructureInformation(child);
						m_structureInfos.add(info);
					}
				}				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
	}
	
	/** Private helper method to read structure information from an XML representation. 
	 * @param root The XML element containing the semantic information. 
	 * @return The parsed information as {@link OMMStructureInfo}. 
	 */
	private OMMStructureInfo getStructureInformation(Element root)
	{
		if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":structureInformation")) throw new IllegalArgumentException("invalid XML file!");
		
		OMMStructureInfo info = null;
		OMMStructureRelation relationType = null;
		TypedValue relationTarget = null;
		
		Element relElement = OMMXMLConverter.findChild(root, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":relation");
		
		if (relElement != null)
		{
			String relation = relElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":relationType");
			relationTarget = OMMXMLConverter.getTypedValue(relElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type"), relElement.getNodeValue());
			relationType = OMMStructureRelation.valueOf(relation);
		}
		
		Element dateElement = OMMXMLConverter.findChild(root, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":date");
		
		if (dateElement != null)
		{
			String date = OMMXMLConverter.getISO8601String(dateElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), dateElement.getNodeValue());
			
			info = new OMMStructureInfo(relationType, relationTarget, ISO8601.parseDate(date).getTime());	
		}
		else
		{
			Element spanElement = OMMXMLConverter.findChild(dateElement, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":timeSpan");
			
			Element beginElement = OMMXMLConverter.findChild(spanElement, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":begin");
			Element endElement = OMMXMLConverter.findChild(spanElement, OMMXMLConverter.OMM_NAMESPACE_PREFIX+":end");
			
			String startDate = OMMXMLConverter.getISO8601String(beginElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), beginElement.getNodeValue());
			String endDate = OMMXMLConverter.getISO8601String(endElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), endElement.getNodeValue());
			
			info = new OMMStructureInfo(relationType, relationTarget, ISO8601.parseDate(startDate).getTime(), ISO8601.parseDate(endDate).getTime());
		}
		
		return info;
	}

	/** Private helper method to represent changes to the {@link OMMStructureInfo} representation in the block's payload. */
	private void updatePayload()
	{
		Document doc = OMMXMLConverter.createNewXmlDocument();			
		//doc.setBaseURI(OMMXMLConverter.OMM_NAMESPACE_URI);
		
		//Element e = new Element("structure", OMM_IO.OMM_NAMESPACE_NS);
		m_payloadElement = OMMXMLConverter.createXmlElement(doc, "structure", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
		for(OMMStructureInfo info : m_structureInfos)
		{
			System.out.println("size = " + m_structureInfos.size());
			Element eSI = OMMXMLConverter.createXmlElement(doc, "structureInformation", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
			
			if (info.getEndDate() != null)
			{
				// begin -> end
				Element timeSpan = OMMXMLConverter.createXmlElement(doc, "timeSpan", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				
				Element dateBegin = OMMXMLConverter.createXmlElement(doc, "begin", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				dateBegin.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding", "ISO8601");
				dateBegin.setTextContent(ISO8601.getISO8601String(info.getStartDate()));				
				//eSI.addContent(dateBegin);
				timeSpan.appendChild(dateBegin);
				
				Element dateEnd = OMMXMLConverter.createXmlElement(doc, "end", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				dateEnd.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding", "ISO8601");
				dateEnd.setTextContent(ISO8601.getISO8601String(info.getEndDate()));				
				//eSI.addContent(dateEnd);
				timeSpan.appendChild(dateEnd);
				
				eSI.appendChild(timeSpan);
			}
			else
			{
				Element date = OMMXMLConverter.createXmlElement(doc, "date", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				date.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding", "ISO8601");
				date.setTextContent(ISO8601.getISO8601String(info.getStartDate()));				
				eSI.appendChild(date);
			}
				
			
			Element eR = OMMXMLConverter.createXmlElement(doc, "relation", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
			eR.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":relationType", info.getRelationType().toString());
			eR.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type", info.getRelationTarget().getType());
			eR.setTextContent(info.getRelationTarget().getValue().toString());
			eSI.appendChild(eR);
			
			m_payloadElement.appendChild(eSI);
		}
		//System.out.println("size after = " + m_structureInfos.size());
		/*XMLOutputter out = new XMLOutputter();
		out.setFormat(Format.getPrettyFormat());
		StringWriter sw = new StringWriter();
		try 
		{
			out.output(doc, sw);
			m_payload = new GenericTypedValue("none", OMM_IO.removeXMLHeader(sw.toString()));
		} 
		catch (IOException ex) { ex.printStackTrace(); }*/
		
		//super.m_payloadElement = null;
	}
	
//	@Override
//	public Element getPayloadElement() {
//		//regenerate
//		return null;
//	}
	
}
