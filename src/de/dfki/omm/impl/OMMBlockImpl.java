package de.dfki.omm.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Vector;

import de.dfki.omm.types.*;
import org.json.JSONObject;
import org.w3c.dom.Element;

import de.dfki.omm.events.OMMEvent;
import de.dfki.omm.events.OMMEventType;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.tools.OMMXMLConverter;

import static de.dfki.omm.impl.OMMFactory.getNextDataFromBinary;

/** Implementation of {@link OMMBlock}. */
public class OMMBlockImpl implements OMMBlock 
{

	public static final String NEW_BLOCK_CODE =			"b";
	public static final String ID_CODE = 				"i";
	public static final String TITLE_CODE = 			"t";
	public static final String TITLELOCALE_CODE =	 	"tl";
	public static final String CREATOR_CODE = 			"c";
	public static final String PRIMARYID_CODE = 		"r";
	public static final String NAMESPACE_CODE = 		"n";
	public static final String TYPE_CODE = 				"y";
	public static final String DESCRIPTION_CODE =	 	"d";
	public static final String DESCRIPTIONLOCALE_CODE =	"dl";
	public static final String CONTRIBUTORS_CODE = 		"o";
	public static final String CONTRIBUTOR_CODE = 		"co";
	public static final String FORMAT_CODE = 			"f";
	public static final String SUBJECT_CODE = 			"s";
	public static final String SUBJECTTAG_CODE = 		"st";
	public static final String PAYLOAD_CODE = 			"p";
	public static final String LINK_CODE = 				"l";

	protected String m_ID;
	protected URI m_namespace;
	protected URL m_type;
	protected OMMMultiLangText m_title, m_description;	
	protected OMMEntityCollection m_contributors;
	protected OMMEntity m_creator;
	protected OMMFormat m_format;
	protected OMMSubjectCollection m_subject;
	protected TypedValue m_payload;
	protected Element m_payloadElement;
	protected TypedValue m_link;
	protected String m_linkHash;
	protected OMMImpl m_parentOMM;
	protected TypedValue m_primaryID;
	protected OMMPreviousBlockLink m_previousBlock;
	
	protected Element m_xmlElement;

	private static List<URL> m_types = new Vector<URL>() 
	{		
		private static final long serialVersionUID = -3791982682721243736L;
		{
			try
			{
				String baseURL = "http://purl.org/dc/dcmitype/"; 
				add(new URL(baseURL+"Collection"));
				add(new URL(baseURL+"Dataset"));
				add(new URL(baseURL+"Event"));
				add(new URL(baseURL+"Image"));
				add(new URL(baseURL+"InteractiveResource"));
				add(new URL(baseURL+"MovingImage"));
				add(new URL(baseURL+"PhysicalObject"));
				add(new URL(baseURL+"Service"));
				add(new URL(baseURL+"Software"));
				add(new URL(baseURL+"Sound"));
				add(new URL(baseURL+"StillImage"));
				add(new URL(baseURL+"Text"));
			}
			catch(Exception e){};
		}
	};
	

	protected OMMBlockImpl() 
	{
		m_ID = null;
		m_namespace = null;
		m_type = null;
		m_title = new OMMMultiLangText();
		m_description = new OMMMultiLangText();
		m_contributors = new OMMEntityCollection();		
		m_subject = new OMMSubjectCollection();
		m_payload = null;
		m_creator = null;
		m_format = null;
		m_link = null;
		m_linkHash = null;
		m_payloadElement = null;
		m_parentOMM = null;
		m_primaryID = null;
	}

	/**
	 * DO NOT USE THIS CONTSTRUCTOR FROM EXTERNAL CODE!
	 */
	public static OMMBlock create(
			String id, 
			TypedValue primaryID,
			URI namespace, 
			URL type, 
			OMMMultiLangText title, 
			OMMMultiLangText description, 
			OMMEntityCollection contributors, 
			OMMEntity creator,
			OMMFormat format,
			OMMSubjectCollection subject,
			TypedValue payload,
			Element payloadElement,
			TypedValue link,
			String linkHash)
	{
		return create(id, primaryID, namespace, type, title, description, contributors, creator, format, subject, null, payload, payloadElement, link, linkHash);
	}
	
	/**
	 * Creates a new OMMBlockImpl.
	 * 
	 * @param id
	 * @param primaryID
	 * @param namespace
	 * @param type
	 * @param title
	 * @param description
	 * @param contributors
	 * @param creator
	 * @param format
	 * @param subject
	 * @param previousBlock
	 * @param payload
	 * @param payloadElement
	 * @param link
	 * @param linkHash
	 * @return The new block as {@link OMMBlockImpl}. 
	 */
	public static OMMBlock create(
			String id, 
			TypedValue primaryID,
			URI namespace, 
			URL type, 
			OMMMultiLangText title, 
			OMMMultiLangText description, 
			OMMEntityCollection contributors, 
			OMMEntity creator,
			OMMFormat format,
			OMMSubjectCollection subject,
			OMMPreviousBlockLink previousBlock,
			TypedValue payload,
			Element payloadElement,
			TypedValue link,
			String linkHash)
	{
		if (id == null) throw new IllegalArgumentException("id must not be null!");
		if (namespace == null && format == null) throw new IllegalArgumentException("namespace and format must not be null simultaneously!");
		if (title == null) throw new IllegalArgumentException("title must not be null!");
		if (creator == null) throw new IllegalArgumentException("creator must not be null!");
		
		OMMBlockImpl block = new OMMBlockImpl();
		
		block.m_ID = id;
		block.m_primaryID = primaryID;
		block.m_namespace = namespace;
		block.m_type = type;
		block.m_title = title;
		block.m_description = description;
		block.m_contributors = contributors;
		block.m_creator = creator;
		block.m_format = format;
		block.m_subject = subject;
		block.m_previousBlock = previousBlock; 
		block.m_payload = payload;
		block.m_payloadElement = payloadElement;
		block.m_link = link;
		block.m_linkHash = linkHash;

		return block;
	}

	/** @return A list of possible payload types as {@link URL}s. */
	public static List<URL> getTypes() { return m_types; }
	
	public String getID() { return m_ID; }
	
	public TypedValue getPrimaryID() { return m_primaryID; }
	
	public URI getNamespace() { return m_namespace; }
	
	public OMMMultiLangText getTitle() { return m_title;	}
	
	public String getTitle(Locale language) 
	{ 
		if (m_title.containsKey(language)) return m_title.get(language);
		return null;	
	}
	
	public OMMMultiLangText getDescription() { return m_description;	}
	
	public String getDescription(Locale language) 
	{ 
		if (this.m_description != null && m_description.containsKey(language)) return m_description.get(language);
		return null;	
	}
	
	public OMMEntity getCreator() { return m_creator; }
	
	public OMMEntityCollection getContributors() { return m_contributors;	}
	
	public OMMFormat getFormat() { return m_format;	}
	
	public URL getType() { return m_type; }
	
	public OMMSubjectCollection getSubject() { return m_subject; }
	
	public boolean isSubjectPresent(OMMSubjectTagType type, String value)
	{
		if (m_subject == null) return false;
		
		for(OMMSubjectTag tag : m_subject)
		{
			if (isSubjectPresentRecursive(tag, type, value)) return true;
		}
		
		return false;
	}
		
	public TypedValue getPayload() { return m_payload;	}
	
	public String getPayloadAsString()
	{
		final String type = getPayload().getType();
		final Object value = getPayload().getValue();
		
		if (value instanceof String)
		{
			if ("".equals(type))
			{
				return (String)value;
			}
			else if ("cdata".equals(type))
			{
				return ((String)value).trim();
			}
			else if ("base64".equals(type.toLowerCase()))
			{
				BinaryValue.initCodec();
				return new String(BinaryValue.BINARYCODEC.decodePayload("base64", (String)value)); 
			}			
		}
		
		if (value != null) return value.toString(); 
			
		return null;
	}
	
	public String getPayloadEncoding() { return m_payload.getType(); }

	public Element getPayloadElement() { return m_payloadElement; }
	
	public void setPayloadElement(Element payload, OMMEntity entity) {
		this.m_payloadElement = payload;
		this.m_payload = new BinaryValue("none", OMMXMLConverter.toXMLFileString(payload));
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	} 
	
	public boolean isLinkBlock()
	{
		return m_link != null;
	}
	
	public TypedValue getLink()
	{
		return m_link;
	}
	
	public String getLinkHash()
	{
		return m_linkHash;
	}
	
	public OMMPreviousBlockLink getPreviousLink()
	{
		return m_previousBlock;
	}
	
	public String toString()
	{
		return  "OMM-Block [Type: "+getClass().toString()+"]: \n\t\t" +
				(m_ID != null ? "ID="+m_ID+"\n\t\t" : "") +
				(m_primaryID != null ? "Corresponding Memory="+m_primaryID+"\n\t\t" : "") +
			    (m_namespace != null ? "Namespace="+m_namespace+"\n\t\t" : "") +
		   	 	(m_title != null ? "Title="+m_title.toString()+"\n\t\t" : "") +
	   	   		(m_description != null ? "Description="+m_description.toString()+"\n\t\t" : "") +
	   	   		(m_creator != null ? "Creation="+m_creator.toString()+"\n\t\t" : "") +
	   	   		(m_contributors != null ? "Contribution="+m_contributors.toString()+"\n\t\t" : "") +
	   	   		(m_format != null ? "Format="+m_format.toString()+"\n\t\t" : "") +
	   	   		(m_type != null ? "Type="+m_type.toString()+"\n\t\t" : "") +
	   	   	    (m_subject != null ? "Subject="+m_subject.toString()+"\n\t\t" : "") +
	   	   	    (m_previousBlock != null ? "PrevBlock="+m_previousBlock.toString()+"\n\t\t" : "") +
	   	   		(m_link != null ? "Link="+m_link.toString()+"\n\t\t" : "") +
   				"Payload"+(m_linkHash != null ? " (Hash="+m_linkHash+")" : "")+"="+ (m_payload != null ? m_payload.toString(): "null");				
	}
	
	
	/**
	 * Private helper to check recursively if a given subject is present in the subject tags.
	 * 
	 * @param tag {@link OMMSubjectTag} from which to search down recursively.
	 * @param type  {@link OMMSubjectTagType} for which to search. 
	 * @param value Value of the tag for which to search.
	 * @return True if a tag fitting the given data has been found. 
	 */
	private boolean isSubjectPresentRecursive(OMMSubjectTag tag, OMMSubjectTagType type, String value)
	{
		if (tag.getType() == type && tag.getValue() == value) return true;
		return isSubjectPresentRecursive(tag.getChild(), type, value);
	}

	public void setTitle(OMMMultiLangText title, OMMEntity entity)
	{
		if (title == null) throw new IllegalArgumentException("title must not be null!");
		m_title = title;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.TITLE_CHANGED));
	}
	
	public void setTitle(Locale language, String title, OMMEntity entity)
	{
		if (m_title.containsKey(language)) m_title.remove(language);
		m_title.put(language, title);
		
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.TITLE_CHANGED));
	}
	
	// TODO is this method wanted? it's not in OMMBlock
	/**
	 * Sets the creator of this block to the given value, overwriting previous creator.
	 * @param creator The new creator of this block as {@link OMMEntity}
	 */
	public void setCreator(OMMEntity creator) 
	{ 
		if (creator == null) throw new IllegalArgumentException("creator must not be null!");
		m_creator = creator; 
		//if (m_parentOMM != null) if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, creator, OMMEventType.METADATA_CHANGED));
	}	
	
	public void setDescription(OMMMultiLangText description, OMMEntity entity)
	{
		m_description = description;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.DESCRIPTION_CHANGED));
	}
	
	public void setDescription(Locale language, String description, OMMEntity entity)
	{
		if (m_description.containsKey(language)) m_description.remove(language);
		m_description.put(language, description);
		
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.DESCRIPTION_CHANGED));
	}
	
	// TODO is this method wanted? it's not in OMMBlock
	/**
	 * Sets the contributors to this block to the given value, overwriting previous contributors.
	 * @param contributors The new contributors to this block as {@link OMMEntityCollection}
	 */
	public void setContributors(OMMEntityCollection contributors) 
	{ 
		m_contributors = contributors; 
		//if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, null, OMMEventType.METADATA_CHANGED));
	}
	
	/**
	 * Adds a contributor to this block, supplementing previous contributors.
	 * @param contributor The contributor to add to this block as {@link OMMEntity}
	 */
	public void addContributor(OMMEntity contributor) 
	{ 
		m_contributors.add(contributor); 
		//if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, contributor, OMMEventType.METADATA_CHANGED));
	}
	
	public void setType(URL type, OMMEntity entity)
	{
		m_type = type;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.TYPE_CHANGED));
	}
	
	public void setFormat(OMMFormat format, OMMEntity entity)
	{
		if (m_namespace == null && format == null) throw new IllegalArgumentException("format must not be null if namespace is null!");
		m_format = format;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.FORMAT_CHANGED));
	}
	
	public void setSubject(OMMSubjectCollection collection)
	{
		m_subject = collection;
		//if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, null, OMMEventType.METADATA_CHANGED));
	}
	
	public void addSubject(OMMSubjectTag subject, OMMEntity entity)
	{
		if (m_subject == null) m_subject = new OMMSubjectCollection();
		m_subject.add(subject);
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.SUBJECT_CHANGED));
	}

	public void changeSubject(OMMSubjectTag oldSubject, OMMSubjectTag newSubject, OMMEntity entity)	
	{
		m_subject.remove(oldSubject);
		m_subject.add(newSubject);
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.SUBJECT_CHANGED));
	}
	
	public void setLink(TypedValue link, OMMEntity entity) 
	{
		setLink(link, null, entity);
	}

	public void setLink(TypedValue link, String linkHash, OMMEntity entity) 
	{
		m_link = link;
		m_linkHash = linkHash;
		m_payload = null;		
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.LINK_CHANGED));
	}

	public void setPayload(TypedValue payload, OMMEntity entity) 
	{
		m_link = null;
		m_linkHash = null;
		m_payload = payload;	
		
		if (m_payloadElement != null)
		{
		  m_payloadElement.setTextContent(payload.getValue().toString());
		}

		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	}
	
	public void setPayload(byte[] payload, OMMEntity entity) 
	{
		m_link = null;
		m_linkHash = null;
		m_payload = new BinaryValue("base64", payload);
		m_payloadElement = null;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	}

	public void setPrimaryID (TypedValue newId) {
		m_primaryID = newId;
	}

	public boolean removeTitle(Locale language, OMMEntity entity)
	{
		if (this.m_title.size() < 2)
		{
			return false;
		}
		
		if(this.m_title.containsKey(language))
		{
			this.m_title.remove(language);
			if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.TITLE_CHANGED));
		}		
		
		return true;
	}
	
	public void removeDescription(Locale language, OMMEntity entity) {
		if(this.m_description.containsKey(language)){
			this.m_description.remove(language);
			if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.DESCRIPTION_CHANGED));
		}
	}

	public void removeType(OMMEntity entity) {
		if(this.m_type != null){
			this.m_type = null;
			if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.TYPE_CHANGED));
		}
	}


	public void removeSubject(OMMSubjectTag subject, OMMEntity entity) {
		if(this.m_subject.contains(subject)){
			this.m_subject.remove(subject);
			if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.SUBJECT_CHANGED));
		}
	}


	public void removeDescriptions(OMMEntity entity) {
		this.m_description.clear();
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.DESCRIPTION_CHANGED));
	}


	public void removeFormat(OMMEntity entity) {
		this.m_format = null;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.FORMAT_CHANGED));
	}


	public void removeSubjects(OMMEntity entity) {
		this.m_subject.clear();
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.SUBJECT_CHANGED));
	}


	public void removeLink(OMMEntity entity) {
		this.m_link = null;
		this.m_linkHash = null;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.LINK_CHANGED));
	}


	public void removePayload(OMMEntity entity) {
		this.m_payload = null;
		this.m_payloadElement = null;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.PAYLOAD_CHANGED));
	}


	public void setNamespace(URI namespace, OMMEntity entity) {
		if (namespace == null) throw new IllegalArgumentException("namespace must not be null!");
		m_namespace = namespace;
		if (m_parentOMM != null) m_parentOMM.fireOMMEvent(new OMMEvent(m_parentOMM, this, entity, OMMEventType.NAMESPACE_CHANGED));
	}

	public void setParentOMM(OMMImpl omm)
	{
		this.m_parentOMM = omm;
	}


	public void setID(String id) {
		this.m_ID = id;
	}

	@Override
	public Element getXMLElement(boolean withPayload) {
		return m_xmlElement;
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

	/**
	 * Custom method to serialize OMMBlockImpls and their content properly
	 *
	 * @param outputStream Stream to write to
	 * @throws IOException
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream outputStream) throws IOException {

		// write serializable memory information
		outputStream.defaultWriteObject();

		// write block metadata and content
		// (order of written objects will be maintained when reading)
		outputStream.writeObject(this.getPrimaryID());			// write primary ID
		outputStream.writeObject(this.getID()); 				// write ID
		outputStream.writeObject(this.getNamespace());			// write namespace
		outputStream.writeObject(this.getFormat());				// write format
		outputStream.writeObject(this.getCreator());			// write creator
		outputStream.writeObject(this.getContributors());		// write contributors
		outputStream.writeObject(this.getTitle());				// write title
		outputStream.writeObject(this.getDescription());		// write description
		outputStream.writeObject(this.getType());				// write type
		outputStream.writeObject(this.getLink());				// write link
		outputStream.writeObject(this.getLinkHash());			// write link hash
		outputStream.writeObject(this.getSubject());			// write subject
		outputStream.writeObject(this.getPreviousLink());		// write previous element
		outputStream.writeObject(this.getPayload());			// write payload
		outputStream.writeObject(this.getPayloadElement());		// write payload element

	}

	/**
	 * Custom method to deserialize OMMBlockImpls and their content properly
	 *
	 * @param inputStream Stream to read from
	 * @throws IOException
	 */
	private synchronized void readObject(java.io.ObjectInputStream inputStream) throws IOException, ClassNotFoundException {

		// read basic memory information
		inputStream.defaultReadObject();

		// read block metadata and content
		TypedValue storedPrimaryId = null;					// read primary ID
		Object loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof TypedValue) {
			storedPrimaryId = (TypedValue) loadedInfo;
		}
		String storedId = null;								// read ID
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof String) {
			storedId = (String) loadedInfo;
		}
		URI storedNamespace = null;							// read namespace
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof URI) {
			storedNamespace = (URI) loadedInfo;
		}
		OMMFormat storedFormat = null;						// read format
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMFormat) {
			storedFormat = (OMMFormat) loadedInfo;
		}
		OMMEntity storedCreator = null;						// read creator
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMEntity) {
			storedCreator = (OMMEntity) loadedInfo;
		}
		OMMEntityCollection storedContributors = null;		// read contributors
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMEntityCollection) {
			storedContributors = (OMMEntityCollection) loadedInfo;
		}
		OMMMultiLangText storedTitle = null;				// read title
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMMultiLangText) {
			storedTitle = (OMMMultiLangText) loadedInfo;
		}
		OMMMultiLangText storedDescription = null;			// read description
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMMultiLangText) {
			storedDescription = (OMMMultiLangText) loadedInfo;
		}
		URL storedType = null;								// read type
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof URL) {
			storedType = (URL) loadedInfo;
		}
		TypedValue storedLink = null;						// read link
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof TypedValue) {
			storedLink = (TypedValue) loadedInfo;
		}
		String storedLinkHash = null;						// read link hash
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof String) {
			storedLinkHash = (String) loadedInfo;
		}
		OMMSubjectCollection storedSubject = null;			// read subject
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMSubjectCollection) {
			storedSubject = (OMMSubjectCollection) loadedInfo;
		}
		OMMPreviousBlockLink storedPrevious = null;			// read previous element
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMPreviousBlockLink) {
			storedPrevious = (OMMPreviousBlockLink) loadedInfo;
		}
		TypedValue storedPayload = null;					// read payload
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof TypedValue) {
			storedPayload = (TypedValue) loadedInfo;
		}
		Element storedPayloadElement = null;				// read payload element
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof Element) {
			storedPayloadElement = (Element) loadedInfo;
		}
	}

	/**
	 * Creates a new OMMBlockImpl by reading all given info from a byte buffer
	 * @param byteBuffer the byte buffer containing binary block information
	 */
	public static OMMBlock createFromBinary(ByteBuffer byteBuffer) {

		// block variables
		String id = null;
		TypedValue primaryID = null;
		URI namespace = null;
		URL type = null;
		OMMMultiLangText title = null;
		OMMMultiLangText description = null;
		OMMEntityCollection contributors = null;
		OMMEntity creator = null;
		OMMFormat format = null;
		OMMSubjectCollection subject = null;
		TypedValue payload = null;
		TypedValue link = null;

		// read the complete byte buffer
		String entryName = null;
		while (byteBuffer.position() < byteBuffer.limit()) {

			// get entry name
			entryName = new String(getNextDataFromBinary(byteBuffer));

			// collect block data
			switch (entryName) {
				case ID_CODE:
					id = new String(getNextDataFromBinary(byteBuffer));
					break;
				case TITLE_CODE:
					title = new OMMMultiLangText();
					break;
				case TITLELOCALE_CODE:
					Locale locale = new Locale(new String(getNextDataFromBinary(byteBuffer)));
					String localizedTitle = new String(getNextDataFromBinary(byteBuffer));
					title.put(locale, localizedTitle);
					break;
				case CREATOR_CODE:
					String creatortype = new String(getNextDataFromBinary(byteBuffer));
					String creatorvalue = new String(getNextDataFromBinary(byteBuffer));
					String creatordate = new String(getNextDataFromBinary(byteBuffer));
					creator = new OMMEntity(creatortype, creatorvalue, creatordate);
					break;
				case PRIMARYID_CODE:
					String primarytype = new String(getNextDataFromBinary(byteBuffer));
					String primaryvalue = new String(getNextDataFromBinary(byteBuffer));
					primaryID = new GenericTypedValue(primarytype, primaryvalue);
					break;
				case NAMESPACE_CODE:
					String ns = new String(getNextDataFromBinary(byteBuffer));
					try { namespace = new URI(ns); }
					catch (URISyntaxException e) { e.printStackTrace(); }
					break;
				case TYPE_CODE:
					String ts = new String(getNextDataFromBinary(byteBuffer));
					try { type = new URL(ts); }
					catch (MalformedURLException e) { e.printStackTrace(); }
					break;
				case DESCRIPTION_CODE:
					description = new OMMMultiLangText();
					break;
				case DESCRIPTIONLOCALE_CODE:
					Locale dlocale = new Locale(new String(getNextDataFromBinary(byteBuffer)));
					String localizedDescription = new String(getNextDataFromBinary(byteBuffer));
					description.put(dlocale, localizedDescription);
					break;
				case CONTRIBUTORS_CODE:
					contributors = new OMMEntityCollection();
					break;
				case CONTRIBUTOR_CODE:
					String contributortype = new String(getNextDataFromBinary(byteBuffer));
					String contributorvalue = new String(getNextDataFromBinary(byteBuffer));
					String contributordate = new String(getNextDataFromBinary(byteBuffer));
					contributors.add(new OMMEntity(contributortype, contributorvalue, contributordate));
					break;
				case FORMAT_CODE:
					String mimetype = new String(getNextDataFromBinary(byteBuffer));
					String schemastring = new String(getNextDataFromBinary(byteBuffer));
					URL schema = null;
					if (schemastring.length() > 0) {
						try { schema = new URL(schemastring); }
						catch (MalformedURLException e) { e.printStackTrace(); }
					}
					String encoding = new String(getNextDataFromBinary(byteBuffer));
					if (encoding.length() == 0) encoding = null;
					format = new OMMFormat(mimetype, schema, encoding);
					break;
				case SUBJECT_CODE:
					subject = new OMMSubjectCollection();
					break;
				case SUBJECTTAG_CODE:
					subject.add(getSubjectTagFromBinary(byteBuffer));
					break;
				case PAYLOAD_CODE:
					String payloadtype = new String(getNextDataFromBinary(byteBuffer));
					String payloadvalue = new String(getNextDataFromBinary(byteBuffer));
					payload = new GenericTypedValue(payloadtype, payloadvalue);
					break;
				case LINK_CODE:
					String linktype = new String(getNextDataFromBinary(byteBuffer));
					String linkvalue = new String(getNextDataFromBinary(byteBuffer));
					link = new GenericTypedValue(linktype, linkvalue);
					break;
				case NEW_BLOCK_CODE:
					// a new block starts here, create and return this one
					return create(id, primaryID, namespace, type, title, description, contributors,
							creator, format, subject, payload, null, link, null);
				default:
					System.err.println("Could not read block entry named '" + entryName + "'.");
			}
		}

		// create and return block from collected data
		return create(id, primaryID, namespace, type, title, description, contributors,
				creator, format, subject, payload, null, link, null);
	}

	/**
	 * Writes this OMMBlockImpl and its selected contents to a byte buffer
	 * @param byteBuffer the byte buffer to write
	 */
	public static void writeToBinary(ByteBuffer byteBuffer,
		 boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription,
		 boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload,
		 boolean saveLink)
	{



	}

	/**
	 * Retrieves a subject tag and its potential children from a byte buffer.
	 * @param byteBuffer the byte buffer from which to read
	 * @return The retrieved OMMSubjectTag
	 */
	private static OMMSubjectTag getSubjectTagFromBinary(ByteBuffer byteBuffer) {

		// collect tag data
		String tagtype = new String(getNextDataFromBinary(byteBuffer));
		String tagtext = new String(getNextDataFromBinary(byteBuffer));
		String child = new String(getNextDataFromBinary(byteBuffer));

		// if the tag has a child, retrieve that too
		OMMSubjectTag tagchild = null;
		if (child.length() > 0) {
			tagchild = getSubjectTagFromBinary(byteBuffer);
		}

		// create and return new tag
		return new OMMSubjectTag(OMMSubjectTagType.valueOf(tagtype), tagtext, tagchild);
	}
}
