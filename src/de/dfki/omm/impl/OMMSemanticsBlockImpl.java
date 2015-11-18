package de.dfki.omm.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.dfki.omm.interfaces.OMMSemanticsBlock;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.ISO8601;
import de.dfki.omm.types.OMMSemanticsGroup;
import de.dfki.omm.types.OMMSemanticsInfo;

/** Implementation of {@link OMMSemanticsBlock}. */
public class OMMSemanticsBlockImpl extends OMMBlockImpl implements OMMSemanticsBlock
{
	LinkedList<OMMSemanticsGroup> m_semantics = null;
	
	@SuppressWarnings("unused")
	private OMMSemanticsBlockImpl() 
	{
		//super();
	}
	
	/** Constructor.
	 * @param block An {@link OMMBlockImpl} to use for the construction of a OMMSemanticsBlockImpl. 
	 */
	public OMMSemanticsBlockImpl(OMMBlockImpl block)
	{
		this.m_ID = block.m_ID;
		this.m_namespace = block.m_namespace;
		this.m_type = block.m_type;
		this.m_title = block.m_title;
		this.m_description = block.m_description;
		this.m_contributors = block.m_contributors;
		this.m_creator = block.m_creator;
		this.m_format = block.m_format;
		this.m_subject = block.m_subject;
		this.m_payload = block.m_payload;
		this.m_payloadElement = block.m_payloadElement;
		this.m_link = block.m_link;
		this.m_linkHash = block.m_linkHash;
		this.m_semantics = new LinkedList<OMMSemanticsGroup>();
		updateLocalData();
	}

	
	public Collection<OMMSemanticsGroup> getSemanticGroups()
	{
		return m_semantics;
	}

	public void addSemanticsGroup(OMMSemanticsGroup group)
	{
		m_semantics.add(group);
		updatePayload();
	}

	public void removeSemanticsGroup(OMMSemanticsGroup group)
	{
		m_semantics.remove(group);
		updatePayload();
	}

	/** Private helper method to initialize the {@link OMMSemanticsGroup} representation in the block's payload. */
	private void updateLocalData()
	{
		if (m_payloadElement == null && m_payload.getValue() instanceof String)
		{
			String v = (String)m_payload.getValue();
			v = "<omm:payload xmlns:omm=\"http://www.w3.org/2005/Incubator/omm/elements/1.0/\">"+v+"</omm:payload>";
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
				Element root = m_payloadElement;// doc.getRootElement();
				
				if (root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":payload"))
				{					
					
					NodeList nl = root.getChildNodes();
					for(int i = 0; i < nl.getLength(); i++)					
					{
						Node objRoot = nl.item(i);
						
						if (!objRoot.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":semantic")) throw new IllegalArgumentException("invalid XML file!");
						
						OMMSemanticsGroup infoList = new OMMSemanticsGroup();
						
						NodeList nlChild = objRoot.getChildNodes();
						for(int k = 0; k < nlChild.getLength(); k++)					
						{
							Node objChild = nlChild.item(i);
							
							if (objChild instanceof Element && objChild.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":semanticInformation"))
							{
								Element child = (Element)objChild;
								OMMSemanticsInfo info = getSemanticsInformation(child);
								infoList.add(info);
							}
						}
						
						m_semantics.add(infoList);
					}
				}
				else if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":semantic")) throw new IllegalArgumentException("invalid XML file!");
				
				OMMSemanticsGroup infoList = new OMMSemanticsGroup();
				
				
				NodeList nlChild = root.getChildNodes();
				for(int k = 0; k < nlChild.getLength(); k++)					
				{
					Node objChild = nlChild.item(k);
					
					if (objChild instanceof Element && objChild.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":semanticInformation"))
					{
						Element child = (Element)objChild;
						OMMSemanticsInfo info = getSemanticsInformation(child);
						infoList.add(info);
					}
				}											
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
	}
	
	
	/** Private helper method to read semantic information from an XML representation. 
	 * @param root The XML element containing the semantic information. 
	 * @return The parsed information as {@link OMMSemanticsInfo}. 
	 */
	private OMMSemanticsInfo getSemanticsInformation(Element root)
	{
		if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":semanticInformation")) throw new IllegalArgumentException("invalid XML file!");
		
		OMMSemanticsInfo info = null;
		String object = null, relation = null, subject = "[this]";
		
		Element subjElement = OMMXMLConverter.findChild(root, "subject");
		if (subjElement != null)
		{
			subject = subjElement.getNodeValue();
		}
		
		Element relElement = OMMXMLConverter.findChild(root, "relation");
		if (relElement != null)
		{
			relation = relElement.getNodeValue();
		}

		Element objElement = OMMXMLConverter.findChild(root, "object");
		if (objElement != null)
		{
			object = objElement.getNodeValue();
		}

		
		Element dateElement = OMMXMLConverter.findChild(root, "date");
		
		if (dateElement != null)
		{
			String date = OMMXMLConverter.getISO8601String(dateElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), dateElement.getNodeValue());
			
			info = new OMMSemanticsInfo(subject, relation, object, ISO8601.parseDate(date).getTime());	
		}
		else
		{
			Element spanElement = OMMXMLConverter.findChild(dateElement, "timeSpan");
			
			Element beginElement = OMMXMLConverter.findChild(spanElement, "begin");
			Element endElement = OMMXMLConverter.findChild(spanElement, "end");
			
			String startDate = OMMXMLConverter.getISO8601String(beginElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), beginElement.getNodeValue());
			String endDate = OMMXMLConverter.getISO8601String(endElement.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding"), endElement.getNodeValue());
			
			info = new OMMSemanticsInfo(subject, relation, object, ISO8601.parseDate(startDate).getTime(), ISO8601.parseDate(endDate).getTime());
		}
		
		return info;
	}

	/** Private helper method to represent changes to the {@link OMMSemanticsGroup} representation in the block's payload. */
	private void updatePayload()
	{
		Document doc = OMMXMLConverter.createNewXmlDocument();
		//doc.setBaseURI(OMMXMLConverter.OMM_NAMESPACE_URI);
		
		//Element e = new Element("structure", OMM_IO.OMM_NAMESPACE_NS);
		m_payloadElement = OMMXMLConverter.createXmlElementAndAppend(doc, "semantic", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
		for(OMMSemanticsGroup group : m_semantics)
		{
			Element eS = OMMXMLConverter.createXmlElement(doc, "semantic", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
			for(OMMSemanticsInfo info : group)
			{
				System.out.println("size = " + m_semantics.size());
				Element eSI = OMMXMLConverter.createXmlElement(doc, "semanticInformation", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				
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
					
				if (info.getSubject() != null && !info.getSubject().equals("[this]"))
				{
					Element eSubj = OMMXMLConverter.createXmlElement(doc, "subject", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
					eSubj.setTextContent(info.getSubject());
					eSI.appendChild(eSubj);
				}
				
				Element eR = OMMXMLConverter.createXmlElement(doc, "relation", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				eR.setTextContent(info.getRelation());
				eSI.appendChild(eR);
				
				Element eO = OMMXMLConverter.createXmlElement(doc, "object", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				eO.setTextContent(info.getObject());
				eSI.appendChild(eO);
				
				eS.appendChild(eSI);
			}		
			
			m_payloadElement.appendChild(eS);
		}
		
		System.out.println("size after = " + m_semantics.size());
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
