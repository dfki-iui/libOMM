package de.dfki.omm.impl;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.dfki.omm.events.OMMEvent;
import de.dfki.omm.events.OMMEventType;
import de.dfki.omm.interfaces.OMMAttributeListBlock;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.OMMEntity;

/** 
 * Implementation of {@link OMMAttributeListBlock}. 
 */
public class OMMAttributeListBlockImpl extends OMMBlockImpl implements OMMAttributeListBlock
{
	private HashMap<String, String> m_attributeList = null;
	private boolean dirty = false;
	
	@SuppressWarnings("unused")
	private OMMAttributeListBlockImpl() { }
	
	/** Constructor.
	 * @param block An {@link OMMBlockImpl} to use for the construction of a OMMAttributeListBlockImpl. 
	 */
	public OMMAttributeListBlockImpl(OMMBlockImpl block)
	{
		this(block, false);
	}
	
	
	/** Constructor.
	 * @param block An {@link OMMBlockImpl} to use for the construction of a OMMAttributeListBlockImpl. 
	 * @param isEmptyPayload True if payload is empty, otherwise false.
	 */
	public OMMAttributeListBlockImpl(OMMBlockImpl block, boolean isEmptyPayload)
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
		this.m_attributeList = new HashMap<String, String>();
		if (!isEmptyPayload) updateLocalData();
	}
	
	public void addAttribute(String attributeName, String value, OMMEntity entity)
	{
		m_link = null;		
		if (m_attributeList.containsKey(attributeName)) throw new IllegalArgumentException("key is already present!");
		
		m_attributeList.put(attributeName, value);
		dirty = true;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	}

	public void removeAttribute(String attributeName, OMMEntity entity)
	{
		if (!m_attributeList.containsKey(attributeName)) throw new IllegalArgumentException("key is not present!");
		
		m_attributeList.remove(attributeName);
		dirty = true;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	}

	public String getAttribute(String attributeName)
	{
		if (!m_attributeList.containsKey(attributeName)) throw new IllegalArgumentException("key is not present!");
		
		return m_attributeList.get(attributeName);
	}

	public HashMap<String, String> getAllAttributes()
	{		
		dirty = true;
		return m_attributeList;
	}

	@Override
	public Element getPayloadElement() 
	{ 
	  /*<omm:attributeList>
	      <omm:attribute omm:key="numberPillA">1</omm:attribute>
	      <omm:attribute omm:key="numberPillB">2</omm:attribute>
	    </omm:attributeList>*/
		
		if (dirty)
		{
			dirty = false;
			Document doc = OMMXMLConverter.createNewXmlDocument();			
						
			m_payloadElement = OMMXMLConverter.createXmlElement(doc, "payload", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
			m_payloadElement.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":encoding", "none");
			
			Element list = OMMXMLConverter.createXmlElement(doc, "attributeList", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
			m_payloadElement.appendChild(list);
			
			for(String key : m_attributeList.keySet())
			{
				String value = m_attributeList.get(key);			
				Element attributeElement = OMMXMLConverter.createXmlElement(doc, "attribute", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
				attributeElement.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":key", key);
				attributeElement.setTextContent(value);
				list.appendChild(attributeElement);
			}
		}
		
		return m_payloadElement; 
	}
	
	/** Private helper to update the attribute list from the block's payload. */
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
		
		if (m_payloadElement != null)
		{	
			try
			{
				Element root = m_payloadElement;
				
				if (root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":payload") && root.getChildNodes().getLength() > 0)
				{
					root = OMMXMLConverter.firstElement(root);
				}
				
				if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":attributeList")) throw new IllegalArgumentException("invalid XML file!");
				
				NodeList nl = root.getChildNodes();
				for(int i = 0; i < nl.getLength(); i++)					
				{
					Node node = nl.item(i);
					if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":attribute"))
					{					
						Element child = (Element)node;
						String key = child.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":key");
						String value = child.getNodeValue();
						
						m_attributeList.put(key, value);
					}
				}				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
	}
	
	@Override
	public String toString()
	{
		return super.toString() +"\n\t\tAttributeBlock="+m_attributeList.toString();
	}
}
