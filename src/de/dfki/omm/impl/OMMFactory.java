package de.dfki.omm.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sun.net.www.protocol.http.HttpURLConnection;
import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.GenericTypedValue;
import de.dfki.omm.types.ISO8601;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMFormat;
import de.dfki.omm.types.OMMMultiLangText;
import de.dfki.omm.types.OMMPreviousBlockLink;
import de.dfki.omm.types.OMMSourceType;
import de.dfki.omm.types.OMMSubjectCollection;
import de.dfki.omm.types.TypedValue;

/** Factory for OMM creation, handling and destruction. */
public class OMMFactory 
{
	public static final String OWNER_BLOCK_ID = "@@@???OWNER_BLOCK???@@@";
	/** Sequence of special characters used to separate owner name and credentials in internal owner representations: "{@value}". */
	public static final String OWNER_SEPARATOR = "\\|\\|\\|";
	/** Sequence of special characters used to separate username and password in internal credential representations: "{@value}". */
	public static final String CREDENTIAL_SEPARATOR = "@@@";
	
	/** Changes the OMM's owner using the REST interface for access.  
	 * 
	 * @param omsURL URL to the OMM as {@link String}. 
	 * @param oldOwnerBlock Owner to be replaced as {@link OMMBlock}.
	 * @param newOwnerBlock New owner as {@link OMMBlock}.
	 * @return True, if owner has succesfully been changed. 
	 */
	public static boolean changeOMMOwnerViaOMSRestInterface(String omsURL, OMMBlock oldOwnerBlock, OMMBlock newOwnerBlock)
	{
		if (omsURL == null || omsURL.isEmpty() || oldOwnerBlock == null || newOwnerBlock == null) return false;
		
		ClientResource cr = new ClientResource(omsURL+"/mgmt/owner");
		
		Document doc = OMMXMLConverter.createNewXmlDocument();
		Element eRoot = OMMXMLConverter.createXmlElementAndAppend(doc, "omm", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
		Element eOldOwnerBlock = OMMXMLConverter.generateCompleteBlock(oldOwnerBlock, true).getDocumentElement();
		eRoot.appendChild(doc.importNode(eOldOwnerBlock, true));
		Element eNewOwnerBlock = OMMXMLConverter.generateCompleteBlock(newOwnerBlock, true).getDocumentElement();
		eRoot.appendChild(doc.importNode(eNewOwnerBlock, true));
		
		String string = OMMXMLConverter.toXMLFileString(doc);
		StringRepresentation stringRep = new StringRepresentation(string);
		
		//System.out.println(stringRep.getText());
		
		try
		{
			cr.put(stringRep, MediaType.APPLICATION_XML);
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return cr.getStatus().isSuccess();
	}
	
	
	/** Sends an OMM as XML string to /mgmt/cloneMemory
	 * 
	 * @param cloneMemoryURL The OMS's REST path to the cloneMemory node
	 * @param omm The OMM to clone
	 * @param ownerBlock Owner of the cloned OMM
	 * @return True, if memory could be cloned on the OMS
	 */
	public static boolean cloneOMMViaOMSRestInterface(String cloneMemoryURL, OMM omm, OMMBlock ownerBlock) {

		// document and root
		Document doc = OMMXMLConverter.createNewXmlDocument();
		Element eRoot = OMMXMLConverter.createXmlElementAndAppend(doc, "omm", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);
		
		// header
		Element eHeader = OMMXMLConverter.generateHeaderDocument(omm.getHeader()).getDocumentElement();
		eRoot.appendChild(doc.importNode(eHeader, true));
		
		// owner
		if (ownerBlock != null)
		{
			Element eOwnerBlock = OMMXMLConverter.generateCompleteBlock(ownerBlock, true).getDocumentElement();
			eRoot.appendChild(doc.importNode(eOwnerBlock, true));
		}
		
		// blocks
		for (OMMBlock block : omm.getAllBlocks()) {
			if (block.getID().equals(OWNER_BLOCK_ID)) continue; // ignore owner block
			Element eBlock = OMMXMLConverter.generateCompleteBlock(block, true).getDocumentElement();
			eRoot.appendChild(doc.importNode(eBlock, true));
		}

		// send XML representation of the OMM to /cloneMemory 
		ClientResource cr = new ClientResource(cloneMemoryURL);
		String string = OMMXMLConverter.toXMLFileString(doc);
		StringRepresentation stringRep = new StringRepresentation(string);
		//System.out.println("stringRep: "+stringRep);
		try
		{
			cr.post(stringRep, MediaType.APPLICATION_XML);
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return cr.getStatus().isSuccess();
	}
	
	/** Creates an {@link OMM} without content from its address on the OMS. 
	 * 
	 * @param primaryID {@link TypedValue} containing the primary ID of the empty OMM. 
	 * @return The new empty OMM. 
	 */
	public static OMM createEmptyOMM(TypedValue primaryID)
	{
		return OMMImpl.create(primaryID);
	}
	
	/** Creates an {@link OMM} without content from its header. 
	 * 
	 * @param header {@link OMMSecurityHeader} for the empty OMM. 
	 * @return The new empty {@link OMM}. 
	 */
	public static OMM createEmptyOMM(OMMHeader header)
	{
		return OMMImpl.create(header);
	}

	/** Creates an {@link OMMBlock} without content. 
	 * 
	 * @param baseOMM {@link OMM} to which the block belongs. 
	 * @param namespace Namespace for the block as {@link URI}. 
	 * @param title The block's title as {@link OMMMultiLangText}.
	 * @param creator The block's creator as {@link OMMEntity}. 
	 * @return The new empty {@link OMMBlock}. 
	 */
	public static OMMBlock createEmptyOMMBlock(OMM baseOMM, URI namespace, OMMMultiLangText title, OMMEntity creator)
	{
		OMMHeader header = baseOMM.getHeader();
		OMMBlock block = OMMBlockImpl.create(getFreeBlockID(baseOMM), header.getPrimaryID(), namespace, null, title, null, null, creator, null, null, null, null, null, null);	
		return block;
	}
	
	/** Creates an {@link OMMBlock} without content. 
	 * 
	 * @param baseOMM {@link OMM} to which the block belongs. 
	 * @param namespace Namespace for the block as {@link URI}. 
	 * @param title The block's title as {@link OMMMultiLangText}.
	 * @param creator The block's creator as {@link OMMEntity}. 
	 * @param link A {@link OMMPreviousBlockLink} to a previous block. 
	 * @return The new empty {@link OMMBlock}. 
	 */
	public static OMMBlock createEmptyOMMBlock(OMM baseOMM, URI namespace, OMMMultiLangText title, OMMEntity creator, OMMPreviousBlockLink link)
	{
		OMMHeader header = baseOMM.getHeader();
		OMMBlock block = OMMBlockImpl.create(getFreeBlockID(baseOMM), header.getPrimaryID(), namespace, null, title, null, null, creator, null, null, link, null, null, null, null);	
		return block;
	}

	
	/** Creates an {@link OMMBlock} without content. 
	 * 
	 * @param baseOMM {@link OMM} to which the block belongs. 
	 * @param namespace Namespace for the block as {@link URI}. 
	 * @param type The block's type as {@link URL}.
	 * @param title The block's title as {@link OMMMultiLangText}.
	 * @param description The block's description as {@link OMMMultiLangText}.
	 * @param format The block's format as {@link OMMFormat}.
	 * @param subject The block's subject as {@link OMMSubjectCollection}.
	 * @return The new empty {@link OMMBlock}. 
	 */
	public static OMMBlock createEmptyOMMBlock(OMM baseOMM, URI namespace, URL type, OMMMultiLangText title, OMMMultiLangText description, OMMFormat format, OMMSubjectCollection subject)
	{
		OMMBlock block = OMMBlockImpl.create(getFreeBlockID(baseOMM), baseOMM.getHeader().getPrimaryID(), namespace, type, title, description, null, null, format, subject, null, null, null, null);		
		return block;
	}
	
	/** Creates an {@link OMMBlock} without content. 
	 * 
	 * @param baseOMM {@link OMM} to which the block belongs. 
	 * @param namespace Namespace for the block as {@link URI}. 
	 * @param type The block's type as {@link URL}.
	 * @param title The block's title as {@link OMMMultiLangText}.
	 * @param format The block's format as {@link OMMFormat}.
	 * @return The new empty {@link OMMBlock}. 
	 */
	public static OMMBlock createEmptyOMMBlock(OMM baseOMM, URI namespace, URL type, OMMMultiLangText title, OMMFormat format)
	{
		OMMBlock block = OMMBlockImpl.create(getFreeBlockID(baseOMM), baseOMM.getHeader().getPrimaryID(), namespace, type, title, null, null, null, format, null, null, null, null, null);		
		return block;
	}

	/** Given a header and a String describing the owner, creates a new owner block to be added to an OMM. 
	 * 
	 * @param header {@link OMMHeader} for the OMM. 
	 * @param ownerString {@link String} describing the new owner (name, user name, password). 
	 * @return New owner block as {@link OMMBlock}.
	 */
	public static OMMBlock createOMMOwnerBlock(OMMHeader header, String ownerString)
	{
		try
		{
			OMMFormat format = new OMMFormat("text/plain", null, null);
			OMMEntity entity = new OMMEntity("name", ownerString, ISO8601.getISO8601StringWithGMT());
			OMMMultiLangText title = new OMMMultiLangText();
			title.put(Locale.ENGLISH, "owner block");
			OMMBlock block = OMMBlockImpl.create(OWNER_BLOCK_ID, header.getPrimaryID(), URI.create("urn:omm:ownerBlock"), null, title, null, null, entity, format, null, null, null, null, null);
			block.setPayload(new GenericTypedValue("none", ownerString), null);
			return block;
		}
		catch(Exception e) { e.printStackTrace(); }
		
		return null;
	}
	
	/** Given the owner information as separate Strings, creates a normed owner representation String.
	 * 
	 * @param clearTextName Clear name of the owner to be displayed to the outside. 
	 * @param username Owner's username to be used internally. 
	 * @param password Password to the username. 
	 * @return Normed owner representation String formatted as "[clearTextName][owner separator][username][credential separator][password]".
	 */
	public static String createOMMOwnerStringFromUsernamePassword(String clearTextName, String username, String password)
	{
		return clearTextName + OWNER_SEPARATOR.replace("\\", "") + username + CREDENTIAL_SEPARATOR + password;
	}
	
	/** Creates an XML representation of the given data and sends it to the REST interface in order to create a new OMM. 
	 * 
	 * @param omsURL Full address of the "mgmt/createMemory" node of the REST interface.
	 * @param header {@link OMMHeader} for the new OMM. 
	 * @param ownerBlock Owner block as {@link OMMBlock} for the new OMM. 
	 * @return True, if creation was successful. 
	 */
	public static boolean createOMMViaOMSRestInterface(String omsURL, OMMHeader header, OMMBlock ownerBlock)
	{
		ClientResource cr = new ClientResource(omsURL);
				
		Document doc = OMMXMLConverter.createNewXmlDocument();
		Element eRoot = OMMXMLConverter.createXmlElementAndAppend(doc, "omm", OMMXMLConverter.OMM_NAMESPACE_PREFIX, OMMXMLConverter.OMM_NAMESPACE_URI);

		Element eHeader = OMMXMLConverter.generateHeaderDocument(header).getDocumentElement();
		eRoot.appendChild(doc.importNode(eHeader, true));

		if (ownerBlock != null)
		{
			Element eOwnerBlock = OMMXMLConverter.generateCompleteBlock(ownerBlock, true).getDocumentElement();
			eRoot.appendChild(doc.importNode(eOwnerBlock, true));
		}
		
		String string = OMMXMLConverter.toXMLFileString(doc);
		StringRepresentation stringRep = new StringRepresentation(string);

//		System.out.println(stringRep.toString());
		
		try
		{
			cr.post(stringRep, MediaType.APPLICATION_XML);
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return cr.getStatus().isSuccess();
	}
	
	/** Private helper to format an {@link OMMEntity} as a String.
	 * 
	 * @param entity
	 * @return Entity String formatted as "[type]###[value]".
	 */
	protected static String getOMMEntityForUpload(OMMEntity entity)
	{
		if (entity == null) return null;
		
		try {
			return URLEncoder.encode(entity.getType(), "utf-8") + 
			   "###" +
			   URLEncoder.encode(entity.getValue(), "utf-8");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}	
	
	/** Given an existing OMM, looks for a free block ID to assign to a new block.
	 * 
	 * @param omm The memory in which to look for the next free block ID. 
	 * @return The smallest available free block ID. 
	 */
	public static String getFreeBlockID(OMM omm)
	{
		HashSet<String> blockIDs = new HashSet<String>(omm.getAllBlockIDs());
		
		for(int i = 1; i < Integer.MAX_VALUE; i++)
		{
			if (blockIDs.contains(i+"")) continue;
			return i+"";
		}
		
		return null;
	}
	
	
	/** Deletes a memory from the OMS using the REST interface. 
	 * 
	 * @param ommUrl The {@link URL} to the memory, for example <code>http://localhost:10082/rest/MemoryName/</code>. 
	 * @return True, if deletion was successful. 
	 */
	public static boolean deleteOMMViaOMSRestInterface (URL ommUrl) {
		
		if (ommUrl == null) return false;
		
		ClientResource deleteCr = new ClientResource(ommUrl.toString());
		try {
			deleteCr.delete();
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return deleteCr.getStatus().isSuccess();
	}
	
	/** Deletes a memory from the OMS using the REST interface. 
	 * 
	 * @param omm The {@link OMMRestImpl} to delete from the server. 
	 * @return Null, if deletion was successful, else the given OMM.  
	 */
	public static OMMRestImpl deleteOMMViaOMSRestInterface (OMMRestImpl omm) {

		URL ommUrl = null;
		try {
			ommUrl = new URL (omm.getHeader().getPrimaryID().getValue().toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		if (deleteOMMViaOMSRestInterface(ommUrl)) {
			return null;
		}
		
		return omm;
	}
	
	/** Deletes a memory from the OMS using the REST interface. 
	 * 
	 * @param ommUrl The {@link URL} to the memory, for example <code>http://localhost:10082/rest/MemoryName/</code>. 
	 * @param credentials The {@link OMSCredentials} to access an ACL-protected memory.
	 * @return True, if deletion was successful. 
	 */
	public static boolean deleteOMMViaOMSRestInterface (URL ommUrl, OMSCredentials credentials) {
		
		if (ommUrl == null) return false;
		
		// derive username/password from credentials
		String[] userAndPw = null;
		if (credentials != null) {
			String[] cred = credentials.getOMSCredentialString().split(OWNER_SEPARATOR);
			if (cred.length > 1) {
				userAndPw = cred[1].split(CREDENTIAL_SEPARATOR);
				if (userAndPw.length < 2) return false; // credentials don't work
			}
		}
		
		ClientResource deleteCr = new ClientResource(ommUrl.toString());
		if (userAndPw != null) deleteCr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, userAndPw[0], userAndPw[1]);
		try {
			deleteCr.delete();
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return deleteCr.getStatus().isSuccess();
	}
	
	/** Deletes a memory from the OMS using the REST interface. 
	 * 
	 * @param omm The {@link OMMRestImpl} to delete from the server. 
	 * @param credentials The {@link OMSCredentials} to access an ACL-protected memory.
	 * @return Null, if deletion was successful, else the given OMM.  
	 */
	public static OMMRestImpl deleteOMMViaOMSRestInterface (OMMRestImpl omm, OMSCredentials credentials) {

		URL ommUrl = null;
		try {
			ommUrl = new URL (omm.getHeader().getPrimaryID().getValue().toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		if (deleteOMMViaOMSRestInterface(ommUrl, credentials)) {
			return null;
		}
		
		return omm;
	}
	
	
	/** <p>Loads an OMM from an XML string and a source from which the memory can be loaded when the OMS (re)starts. 
	 * The source can be either a {@link URL} or a {@link File} and has to be described by a {@link OMMSourceType}. </p>
	 * <p>With one source given the other can be null, but not both.</p>
	 * 
	 * @param xml An XMl representation String of the OMM to load. 
	 * @param urlSource Source of the OMM as a {@link URL}. 
	 * @param fileSource Source of the OMM as a {@link File}. 
	 * @param sourceType The {@link OMMSourceType} of the source. 
	 * @return The loaded memory as a {@link OMM}. 
	 */
	public static OMM loadOMMFromXmlFileString(String xml, URL urlSource, File fileSource, OMMSourceType sourceType)
	{
		return OMMXMLConverter.loadFromXmlString(xml, urlSource, fileSource, sourceType);
	}
	
	/** Loads an OMM from a file containing a memory description in XML format. 
	 * 
	 * @param xmlfile The {@link File} from which to load. 
	 * @return The loaded memory as a {@link OMM}. 
	 */
	public static OMM loadOMMFromXmlFile(File xmlfile)
	{		
		return OMMXMLConverter.loadFromXmlFile(xmlfile);
	}
	
	/** Loads an OMM from the OMS using the REST interface. 
	 * 
	 * @param url The {@link URL} to the memory, for example <code>http://localhost:10082/rest/MemoryName/</code>. 
	 * @return The loaded memory as a {@link OMM}.
	 */
	public static OMM loadOMMViaOMSRestInterface(URL url){
		String path = url.toString();
		if(path.endsWith("/")){
			path = path.substring(0,path.length()-1);
		}
		
		OMMRestImpl omm = new OMMRestImpl(path);
		
		return omm;
	}
	
	/** Loads an OMM from the OMS without using the REST interface. 
	 * 
	 * @param primaryID The memory's ID as an {@link URL}.  
	 * @return The loaded memory as a {@link OMM}.
	 */
	public static OMM loadOMMFromOMS(URL primaryID)
	{
		if (primaryID == null || !primaryID.getProtocol().equals("http")) return null;
		
		OMM omm = null;
		
		try
		{
			URL connectorURL = new URL(primaryID.toString()+"?output=xml");
			HttpURLConnection conn = (HttpURLConnection)connectorURL.openConnection();	
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setRequestProperty("User-Agent", "OMS2-Client 1.0");
			Object content = conn.getContent();
			
			if (content instanceof InputStream)
			{
				InputStream is = (InputStream)content;
				InputStream encIS = is;
				
				String encoding = conn.getHeaderField("content-coding");
				if (encoding != null && encoding.contains("gzip"))
				{
					encIS = new GZIPInputStream(is);
				}
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();				
				while(encIS.available() > 0)
				{
					byte[] buffer = new byte[encIS.available()];
					encIS.read(buffer);
					baos.write(buffer);
				}
				encIS.close();
				is.close();				
				baos.flush();
				baos.close();
				String file = new String(baos.toByteArray(), "UTF-8").trim();
				omm = OMMFactory.loadOMMFromXmlFileString(file, primaryID, null, OMMSourceType.OMS);
			}
		}
		catch(Exception e){e.printStackTrace();}
		
		return omm;
	}
	
	/** Saves an OMM to its appointed source. 
	 * 
	 * @param omm The {@link OMM} to save. 
	 * @param withToC True, if the table of contents is to be saved, too. 
	 * @return True, if the memory was saved successfully. 
	 */
	public static boolean saveOMM(OMM omm, boolean withToC)
	{
		try {
			return saveOMM(omm, null, withToC);
		}
		catch(Exception e) { e.printStackTrace(); }
		
		return false;
	}
	
	/** Saves an OMM to its appointed source. 
	 * 
	 * @param omm The {@link OMM} to save. 
	 * @param entity The {@link OMMEntity} issuing the save operation. Must not be null when saving to the OMS. 
	 * @param withToC True, if the table of contents is to be saved, too. 
	 * @return True, if the memory was saved successfully. 
	 * @throws Exception For illegal arguments or save operations.
	 */
	public static boolean saveOMM(OMM omm, OMMEntity entity, boolean withToC) throws Exception
	{
		OMMImpl ommImpl = ((OMMImpl)omm);
		if (ommImpl.getSourceAsFile() == null && ommImpl.getSourceAsURL() == null) throw new IllegalArgumentException("omm has no source");
		
		String xmlDoc = OMMXMLConverter.toXMLFileString(omm, withToC);
		switch(ommImpl.getSourceType())
		{
			case LocalFile:
				try
				{
					System.out.println("WRITE TO: " + ommImpl.getSourceAsFile().toString());
					if (!ommImpl.getSourceAsFile().exists()) 
					{
						ommImpl.getSourceAsFile().getParentFile().mkdirs();
						ommImpl.getSourceAsFile().createNewFile();						
					}
					FileWriter fw = new FileWriter(ommImpl.getSourceAsFile());
					fw.write(xmlDoc);
					fw.flush(); 
					fw.close();
					return true;
				}
				catch(Exception e){ e.printStackTrace(); }
				break;
			case WebFile:
				throw new Exception("NotImplemented");
			case OMS:
				try
				{
					if (entity == null) throw new IllegalArgumentException("Given OMMEntity was null!");
					
					byte[] payload = xmlDoc.getBytes("UTF-8");
					
					/*ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream gos = new GZIPOutputStream(baos);
					gos.write(payload);
					gos.close();
					baos.close();
					payload = baos.toByteArray();*/
					
					String sourceStr = ommImpl.getSourceAsURL().toString();
					if (sourceStr.contains("?")) 
						sourceStr += "&cmd=upload";
					else
						sourceStr += "?cmd=upload";
						
					URL url = new URL(sourceStr);
					HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
					httpCon.setDoOutput(true);
					httpCon.setRequestMethod("POST");
					httpCon.setRequestProperty("Accept-Encoding", "gzip");
					httpCon.setRequestProperty("Content-Type", "application/xml");
					httpCon.setRequestProperty("Content-Length", payload.length+"");					
					httpCon.setRequestProperty("X-OMM-Entity", getOMMEntityForUpload(entity));
					OutputStream os = httpCon.getOutputStream();
					GZIPOutputStream gzos = new GZIPOutputStream(os);
					gzos.write(payload);
					gzos.flush();
					gzos.close();
					httpCon.getInputStream().available(); // data is only sent after input stream is opened
					return true;
				}
				catch(Exception e){ e.printStackTrace(); }
				break;
		}		
		
		return false;
	}
	
	/** Saves an XML memory representation of an OMM to a file.
	 * 
	 * @param omm The {@link OMM} to save. 
	 * @param xmlFile A {@link File} in which to store the memory. 
	 * @param withToC True, if the table of contents is to be saved, too. 
	 * @return True, if the memory was saved successfully. 
	 */
	public static boolean saveOMMToXmlFile(OMM omm, File xmlFile, boolean withToC)
	{
		((OMMImpl)omm).setSourceType(OMMSourceType.LocalFile);
		try
		{
			((OMMImpl)omm).setSource(xmlFile.toURI().toURL());
		} 
		catch (MalformedURLException e1)
		{
			System.err.println("Given file ("+xmlFile+") is invalid! ("+e1.getStackTrace()+")");
			return false;
		}
		
		String xmlDoc = OMMXMLConverter.toXMLFileString(omm, withToC);
		try
		{
			FileWriter fw = new FileWriter(xmlFile);
			fw.write(xmlDoc);
			fw.flush(); 
			fw.close();
			return true;
		}
		catch(Exception e){ e.printStackTrace(); }
		return false;
	}

}
