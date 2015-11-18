package de.dfki.omm.impl;

import java.util.Collection;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.dfki.omm.interfaces.OMMIdentifierBlock;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.TypedValue;

/** Implementation of {@link OMMIdentifierBlock}. */
public class OMMIdentifierBlockImpl extends OMMBlockImpl implements OMMIdentifierBlock
{	
	LinkedList<TypedValue> m_ids = null;
	
	@SuppressWarnings("unused")
	private OMMIdentifierBlockImpl() { }
	
	/** Constructor.
	 * @param block An {@link OMMBlockImpl} to use for the construction of a OMMIdentifierBlockImpl. 
	 */
	public OMMIdentifierBlockImpl(OMMBlockImpl block)
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
		this.m_ids = new LinkedList<TypedValue>();
		updateLocalData();
	}
	
	
	public Collection<TypedValue> getIdentifier()
	{
		return m_ids;
	}

	public void addIdentifier(TypedValue id)
	{
		m_ids.add(id);
		updatePayload();
	}

	public void removeIdentifier(TypedValue id)
	{
		m_ids.remove(id);
		updatePayload();
	}

	
	/** Private helper method to represent changes to the identifier list in the block's payload. */
	private void updatePayload(){

		Document doc = OMMXMLConverter.createNewXmlDocument();
		
		m_payloadElement = OMMXMLConverter.createXmlElementAndAppend(doc, "identification", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
		for(TypedValue id : m_ids)
		{			
			Element eID = OMMXMLConverter.createXmlElement(doc, "identifier", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
			eID.setAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type", id.getType());
			eID.setTextContent((String) id.getValue().toString());
			/*
			if(id.getValue() instanceof URL){
				eID.setText(id.getValue().toString());
			}else if(id.getValue() instanceof String){
				eID.setText((String)id.getValue());
			}
			*/
			m_payloadElement.appendChild(eID);
		}
		
	
	}
	
	/** Private helper method to initialize the identifier list in the block's payload. */
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
				
				if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":identification")) throw new IllegalArgumentException("invalid XML file!");
				
				NodeList nl = root.getChildNodes();
				for(int i = 0; i < nl.getLength(); i++)					
				{
					Node node = nl.item(i);
					if (node instanceof Element && node.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":identifier"))
					{
						Element child = (Element)node;
						String type = child.getAttribute(OMMXMLConverter.OMM_NAMESPACE_PREFIX+":type");
						String value = child.getNodeValue();
						
						TypedValue tv = OMMXMLConverter.getTypedValue(type, value);
						if (tv != null) m_ids.add(tv);
					}						
				}								
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}
		
	}
}
