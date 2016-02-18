package de.dfki.omm.impl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.dfki.omm.types.*;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.NodeList;
import sun.misc.IOUtils;
import sun.net.www.protocol.http.HttpURLConnection;
import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.impl.rest.OMMRestImpl;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.tools.OMMXMLConverter;

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
	 * @param header {@link OMMHeader} for the empty OMM.
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

//		System.out.println("posting memory string:");
//		System.out.println(stringRep.toString());
		
		try
		{
			cr.post(stringRep, MediaType.APPLICATION_XML);
		}
		catch(ResourceException e) { e.printStackTrace(); return false; }

		return cr.getStatus().isSuccess();
	}

	/** Loads an OMM block from a file containing a block description in binary format,
	 * using standard Java deserialization strategies.
	 *
	 * @param binfile The {@link File} from which to load.
	 * @return The loaded block as a {@link OMMBlock}.
	 */
	public static OMMBlock deserializeBlock(File binfile)
	{
		InputStream fis = null;
		OMMBlock block = null;

		try {
			fis = new FileInputStream(binfile);
			ObjectInputStream o = new ObjectInputStream(fis);
			block = (OMMBlock) o.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally { try { fis.close(); } catch ( Exception e ) { } }

		return block;
	}

	/** Loads an OMM from a file containing a memory description in binary format,
	 * using standard Java deserialization strategies.
	 *
	 * @param binfile The {@link File} from which to load.
	 * @return The loaded memory as a {@link OMM}.
	 */
	public static OMM deserializeOMM(File binfile)
	{
		InputStream fis = null;
		OMM omm = null;

		try {
			fis = new FileInputStream(binfile);
			ObjectInputStream o = new ObjectInputStream(fis);
			omm = (OMM) o.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		finally { try { fis.close(); } catch ( Exception e ) { } }

		return omm;
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

	/**
	 * Gets the next piece of data from a byte buffer when loading a block from binary.
	 * @param byteBuffer the byte buffer from which to read
	 * @return byte[] of the next piece of data
	 */
	public static byte[] getNextDataFromBinary(ByteBuffer byteBuffer) {

		// get the length of the array and reserve space
		byte[] nextData = new byte[byteBuffer.getShort()];

		// read data for given length
		byteBuffer.get(nextData);

		// return result
		return nextData;
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

	/**
	 * Retrieves the ownerBlock from an existing OMM
	 *
	 * @param memoryName name of the OMM
	 * @return
	 */
	public static OMMBlock getOwnerBlockFromOMM (String memoryName) throws FileNotFoundException {

		OMMBlock owner = null;

		// locate info.xml containing the owner information
		final String sep = System.getProperty("file.separator");
		final String infoPath = System.getProperty("user.dir") + sep + "resources" + sep + "memories" + sep + memoryName + sep + "info.xml";
		final File infoFile = new File(infoPath);

		// read info.xml if it exists
		if (infoFile.exists())
		{
			try {

				// get XML data
				Document doc = OMMXMLConverter.getXmlDocumentFromString(new FileInputStream(infoFile));
				Element root = doc.getDocumentElement();
				if (!root.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX + ":oms"))
					throw new IllegalArgumentException("info.xml file is not valid");

				// locate "ownership" node in the document
				NodeList nl = root.getChildNodes();
				for(int i = 0; i < nl.getLength(); i++)
				{
					Object child = nl.item(i);
					if (child instanceof Element)
					{
						Element cElement = (Element)child;
						if (cElement.getNodeName().equals("ownership")) {

							// extract owner block from the ownership node
							NodeList ownershipNodes = cElement.getChildNodes();
							for(int j = 0; j < ownershipNodes.getLength(); j++)
							{
								Object ownershipChild = ownershipNodes.item(j);
								if (ownershipChild instanceof Element)
									return (OMMBlockImpl)OMMXMLConverter.parseBlock((Element)ownershipChild);
							}
						}
					}
				}
			}
			catch(Exception e) {e.printStackTrace();}
		}

		// info.xml could not be found under the default address
		else {
			throw new FileNotFoundException("Memory '" + memoryName + "' not found!");
		}

		return owner;
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


	/** Loads an OMM block from a file containing a block description in custom binary format.
	 *
	 * @param binfile The {@link File} from which to load.
	 * @return The loaded block as a {@link OMMBlock}.
	 */
	public static OMMBlock loadBlockFromBinary (File binfile) {

		OMMBlock block = null;

		// open and read file
		FileInputStream fis = null;
		ByteBuffer byteBuffer = null;
		byte[] fileContent = null;
		try {

			// load file content
			int fileLength = (int) binfile.length();
			fis = new FileInputStream(binfile);
			fileContent = new byte[fileLength];
			fis.read(fileContent);

			// decompress file content
			ByteArrayInputStream byteStream = new ByteArrayInputStream(fileContent);
			GZIPInputStream zipStream = new GZIPInputStream(byteStream);
			java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
			try {
				int readBytes = 0;
				int zipStreamLength = zipStream.toString().length();
				//byte[] buffer = new byte[1024];
				byte[] buffer = new byte[zipStreamLength];
				while (readBytes >= 0) {
					readBytes = zipStream.read(buffer, 0, buffer.length);
					if (readBytes > 0) data.write(buffer, 0, readBytes);
				}
			} catch (IOException e) { e.printStackTrace(); }
			finally {
				try { byteStream.close(); zipStream.close(); }
				catch (IOException e) { e.printStackTrace(); }
			}

			// create byte buffer from read data
			byte[] decompressedData = data.toByteArray();
			byteBuffer = ByteBuffer.allocate(decompressedData.length);
			byteBuffer.put(decompressedData);
			byteBuffer.position(0); // return to start position for reading

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(fis != null) fis.close();
			} catch (IOException e) {}
		}

		// read buffer content
		if (byteBuffer != null) {

			// check prologue, whether file contains a block
//			nextLength = byteBuffer.getShort();
//			nextData = new byte[nextLength];
//			byteBuffer.get(nextData);
//			if (!new String(nextData).equals("OMMBlock")) {
//				System.err.println("File does not contain an OMMBlock");
//				return null;
//			}

			// create block from byte buffer
			block = OMMBlockImpl.createFromBinary(byteBuffer);
		}

		// return finished block
		return block;
	}

	/** Loads an OMM block from a file containing a block description in XML format.
	 *
	 * @param targetFile The {@link File} from which to load.
	 * @return The loaded block as a {@link OMMBlock}.
	 */
	public static OMMBlock loadBlockFromXML (File targetFile) {

		OMMBlock block = null;

		if (targetFile.exists())
		{
			try {
				// get XML data
				Document doc = OMMXMLConverter.getXmlDocumentFromString(new FileInputStream(targetFile));
				Element blockElement = doc.getDocumentElement();
				if (!blockElement.getNodeName().equals(OMMXMLConverter.OMM_NAMESPACE_PREFIX + ":block"))
					throw new IllegalArgumentException(targetFile.getName() + " does not represent a valid OMMBLock");
				block = OMMXMLConverter.parseBlock(blockElement);
			}
			catch(Exception e) {e.printStackTrace();}
		}
		// target could not be found under the default address
		else {
			System.err.println(targetFile.getAbsolutePath() + " could not be found!");
		}

		return block;
	}

	/** Loads an OMM from a file containing a memory description in custom binary format.
	 *
	 * @param binfile The {@link File} from which to load.
	 * @return The loaded memory as a {@link OMM}.
	 */
	public static OMM loadOMMFromBinary (File binfile)
	{

		OMM omm = null;

		// open and read file
		FileInputStream fis = null;
		ByteBuffer byteBuffer = null;
		byte[] fileContent = null;
		try {

			// load file content
			int fileLength = (int) binfile.length();
			fis = new FileInputStream(binfile);
			fileContent = new byte[fileLength];
			fis.read(fileContent);

			System.out.println("reading file: " + binfile.getName());

			// decompress file content, if it is zipped
			if (binfile.getName().endsWith("ommz")) {
				ByteArrayInputStream byteStream = new ByteArrayInputStream(fileContent);
				GZIPInputStream zipStream = new GZIPInputStream(byteStream);
				java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
				try {
					int readBytes = 0;
					int zipStreamLength = zipStream.toString().length();
					//byte[] buffer = new byte[1024];
					byte[] buffer = new byte[zipStreamLength];
					while (readBytes >= 0) {
						readBytes = zipStream.read(buffer, 0, buffer.length);
						if (readBytes > 0) data.write(buffer, 0, readBytes);
					}
				} catch (IOException e) { e.printStackTrace(); }
				finally {
					try { byteStream.close(); zipStream.close(); }
					catch (IOException e) { e.printStackTrace(); }
				}
				fileContent = data.toByteArray();
			}

			// create byte buffer from read data
			byteBuffer = ByteBuffer.allocate(fileContent.length);
			byteBuffer.put(fileContent);
			byteBuffer.position(0); // return to start position for reading

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(fis != null) fis.close();
			} catch (IOException e) {}
		}

		// read buffer content
		if (byteBuffer != null) {

			// check prologue, whether file contains a block
//			nextLength = byteBuffer.getShort();
//			nextData = new byte[nextLength];
//			byteBuffer.get(nextData);
//			if (!new String(nextData).equals("OMM")) {
//				System.err.println("File does not contain an OMM");
//				return null;
//			}

			// read header from byte buffer
			String type = new String(getNextDataFromBinary(byteBuffer));
			String value = new String(getNextDataFromBinary(byteBuffer));
			TypedValue primaryId = new GenericTypedValue(type, value);
			type = new String(getNextDataFromBinary(byteBuffer));
			TypedValue additionalBlocks = null;
			if (type.length() > 0) {
				value = new String(getNextDataFromBinary(byteBuffer));
				if (value.length() > 0) additionalBlocks = new GenericTypedValue(type, value);
			}
			OMMHeader header = OMMHeaderImpl.create(primaryId, additionalBlocks);

			// read owner from byte buffer
			OMMBlock owner = null;
			String ownerFollows = new String(getNextDataFromBinary(byteBuffer));
			if (ownerFollows.length() > 0) {
				owner = OMMBlockImpl.createFromBinary(byteBuffer);
			}

			// create new OMM on the OMS and add loaded blocks
			if (header != null) {

				// create empty OMM to add blocks to
				String memoryUrl = header.getPrimaryID().getValue().toString();
				String omsUrl = memoryUrl.substring(0, memoryUrl.indexOf("rest/"));
				OMMFactory.createOMMViaOMSRestInterface(omsUrl + "mgmt/createMemory", header, owner);
				omm = new OMMRestImpl(memoryUrl, OMMRestAccessMode.CompleteDownloadUnlimited);

				// read memory blocks
				int blockNumber = byteBuffer.getInt();
				for (int i = 0; i < blockNumber; i++) {
					OMMBlock block = OMMBlockImpl.createFromBinary(byteBuffer);
					if (block != null)
						omm.addBlock(block, block.getCreator());
				}
			}
		}

		// return finished OMM
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

	/** Saves a binary representation of an OMM block to a file,
	 * using a customly created binary format and
	 * serizalizing all block contents.
	 *
	 * @param block The {@link OMMBlock} to save.
	 * @param binFile A {@link File} in which to store the block.
	 * @return True, if the block was saved successfully.
	 */
	public static boolean saveBlockToBinary(OMMBlock block, File binFile) {
		return saveBlockToBinary(block, binFile, true, true, true, true, true, true, true, true, true);
	}


	/** Saves a binary representation of an OMM block to a file,
	 * using a customly created binary format and
	 * only serialising the selected information in order to save resources.
	 * (Mandatory information like ID, creator or title cannot be omitted.)
	 *
	 * @param block The {@link OMMBlock} to save.
	 * @param binFile A {@link File} in which to store the block.
	 * @param savePrimaryID true if the block`s primaryID should be saved.
	 * @param saveNamespace true if the block`s namespace should be saved.
	 * @param saveType true if the block`s type should be saved.
	 * @param saveDescription true if the block`s description should be saved.
	 * @param saveContributors true if the block`s contributors should be saved.
	 * @param saveFormat true if the block`s format should be saved.
	 * @param saveSubject true if the block`s subject should be saved.
	 * @param savePayload true if the block`s payload should be saved.
	 * @param saveLink true if the block`s link should be saved.
	 * @return True, if the block was saved successfully.
	 */
	public static boolean saveBlockToBinary(OMMBlock block, File binFile,
		boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription,
		boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload,
		boolean saveLink)
	{

//		System.out.println("saving block " + block.getID() + " of type " + block.getClass().getName());
//		System.out.println("into " + binFile);

		// create byte buffer
		int estimatedBlockLength = 1024;
		if (block.getPayload() != null)
			estimatedBlockLength += block.getPayload().getValue().toString().length();
		ByteBuffer byteBuffer = ByteBuffer.allocate(estimatedBlockLength);

		// save a prologue stating the serialized class (OMMBlock)
//		byteBuffer.putShort((short) 8);
//		byteBuffer.put("OMMBlock".getBytes(StandardCharsets.UTF_8));

		// write block
		writeBlockToBinary(byteBuffer, block, savePrimaryID, saveNamespace, saveType, saveDescription, saveContributors, saveFormat, saveSubject, savePayload, saveLink);

		// reduce buffer size if possible
		int contentLength = byteBuffer.position();
		byte[] writeBytes = new byte[contentLength];
		byteBuffer.position(0);
		byteBuffer.get(writeBytes, 0, contentLength);

		// compress buffer content
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(writeBytes.length);
		try {
			GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
			try { zipStream.write(writeBytes); }
			finally { zipStream.close(); }
		} catch (IOException e) { e.printStackTrace(); }
		finally {
			try { byteStream.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}
		byte[] compressedData = byteStream.toByteArray();

		// open and write output file
		FileOutputStream fos = null;
		try {
			// write collected block data
			fos = new FileOutputStream(binFile);
			//fos.write(writeBytes);
			fos.write(compressedData);
			fos.flush();
			// return true on success
			return true;
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		finally {
			try { if (fos == null) fos.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}

		// return false on error
		return false;
	}

	/**
	 * Writes a given block and its selected contents to a byte buffer
	 * @param byteBuffer the byte buffer to write
	 * @param block The {@link OMMBlock} to save.
	 * @param savePrimaryID true if the block`s primaryID should be saved.
	 * @param saveNamespace true if the block`s namespace should be saved.
	 * @param saveType true if the block`s type should be saved.
	 * @param saveDescription true if the block`s description should be saved.
	 * @param saveContributors true if the block`s contributors should be saved.
	 * @param saveFormat true if the block`s format should be saved.
	 * @param saveSubject true if the block`s subject should be saved.
	 * @param savePayload true if the block`s payload should be saved.
	 * @param saveLink true if the block`s link should be saved.
	 */
	private static void writeBlockToBinary(ByteBuffer byteBuffer, OMMBlock block,
		 boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription,
		 boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload,
		 boolean saveLink)
	{

		// write ID to byte buffer (mandatory)
		String id = block.getID();
		byteBuffer.putShort((short) OMMBlockImpl.ID_CODE.length());
		byteBuffer.put(OMMBlockImpl.ID_CODE.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putShort((short) id.length());
		byteBuffer.put(id.getBytes(StandardCharsets.UTF_8));

		// write title to byte buffer (mandatory)
		OMMMultiLangText titles = block.getTitle();				// possible to save space by only writing one title
		byteBuffer.putShort((short) OMMBlockImpl.TITLE_CODE.length());
		byteBuffer.put(OMMBlockImpl.TITLE_CODE.getBytes(StandardCharsets.UTF_8));
		for (Map.Entry<Locale,String> title : titles.entrySet()) {
			String language = title.getKey().toString();
			String localizedTitle = title.getValue();
			byteBuffer.putShort((short) OMMBlockImpl.TITLELOCALE_CODE.length());
			byteBuffer.put(OMMBlockImpl.TITLELOCALE_CODE.getBytes(StandardCharsets.UTF_8));
			byteBuffer.putShort((short) language.length());
			byteBuffer.put(language.getBytes(StandardCharsets.UTF_8));
			byteBuffer.putShort((short) localizedTitle.length());
			byteBuffer.put(localizedTitle.getBytes(StandardCharsets.UTF_8));
		}

		// write creator to byte buffer (mandatory)
		OMMEntity creator = block.getCreator();
		byteBuffer.putShort((short) OMMBlockImpl.CREATOR_CODE.length());
		byteBuffer.put(OMMBlockImpl.CREATOR_CODE.getBytes(StandardCharsets.UTF_8));
		String creatortype = creator.getType();
		String creatorvalue = creator.getValue();
		String creatordate = creator.getDateAsISO8601();
		byteBuffer.putShort((short) creatortype.length());
		byteBuffer.put(creatortype.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putShort((short) creatorvalue.length());
		byteBuffer.put(creatorvalue.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putShort((short) creatordate.length());
		byteBuffer.put(creatordate.getBytes(StandardCharsets.UTF_8));

		// write primary ID to byte buffer (if selected)
		if (savePrimaryID) {
			TypedValue primaryID = block.getPrimaryID();
			if (primaryID != null) {
				byteBuffer.putShort((short) OMMBlockImpl.PRIMARYID_CODE.length());
				byteBuffer.put(OMMBlockImpl.PRIMARYID_CODE.getBytes(StandardCharsets.UTF_8));
				String type = primaryID.getType();
				String value = primaryID.getValue().toString();
				byteBuffer.putShort((short) type.length());
				byteBuffer.put(type.getBytes(StandardCharsets.UTF_8));
				byteBuffer.putShort((short) value.length());
				byteBuffer.put(value.getBytes(StandardCharsets.UTF_8));
			}
		}

		// write namespace to byte buffer (if selected)
		if (saveNamespace) {
			URI namespace = block.getNamespace();
			if (namespace != null) {
				byteBuffer.putShort((short) OMMBlockImpl.NAMESPACE_CODE.length());
				byteBuffer.put(OMMBlockImpl.NAMESPACE_CODE.getBytes(StandardCharsets.UTF_8));
				String ns = namespace.toString();
				byteBuffer.putShort((short) ns.length());
				byteBuffer.put(ns.getBytes(StandardCharsets.UTF_8));
			}
		}

		// write type to byte buffer (if selected)
		if (saveType) {
			URL type = block.getType();
			if (type != null) {
				byteBuffer.putShort((short) OMMBlockImpl.TYPE_CODE.length());
				byteBuffer.put(OMMBlockImpl.TYPE_CODE.getBytes(StandardCharsets.UTF_8));
				String ts = type.toString();
				byteBuffer.putShort((short) ts.length());
				byteBuffer.put(ts.getBytes(StandardCharsets.UTF_8));
			}
		}

		// write description to byte buffer (if selected)
		if (saveDescription) {
			OMMMultiLangText descriptions = block.getDescription();
			if (descriptions != null) {
				byteBuffer.putShort((short) OMMBlockImpl.DESCRIPTION_CODE.length());
				byteBuffer.put(OMMBlockImpl.DESCRIPTION_CODE.getBytes(StandardCharsets.UTF_8));
				for (Map.Entry<Locale,String> description : descriptions.entrySet()) {
					String language = description.getKey().toString();
					String localizedDescription= description.getValue();
					byteBuffer.putShort((short) OMMBlockImpl.DESCRIPTIONLOCALE_CODE.length());
					byteBuffer.put(OMMBlockImpl.DESCRIPTIONLOCALE_CODE.getBytes(StandardCharsets.UTF_8));
					byteBuffer.putShort((short) language.length());
					byteBuffer.put(language.getBytes(StandardCharsets.UTF_8));
					byteBuffer.putShort((short) localizedDescription.length());
					byteBuffer.put(localizedDescription.getBytes(StandardCharsets.UTF_8));
				}
			}
		}

		// write contributors to byte buffer (if selected)
		if (saveContributors) {
			OMMEntityCollection contributors = block.getContributors();
			if (contributors != null) {
				byteBuffer.putShort((short) OMMBlockImpl.CONTRIBUTORS_CODE.length());
				byteBuffer.put(OMMBlockImpl.CONTRIBUTORS_CODE.getBytes(StandardCharsets.UTF_8));
				for (OMMEntity con : contributors) {
					byteBuffer.putShort((short) OMMBlockImpl.CONTRIBUTOR_CODE.length());
					byteBuffer.put(OMMBlockImpl.CONTRIBUTOR_CODE.getBytes(StandardCharsets.UTF_8));
					String contributortype = con.getType();
					String contributorvalue = con.getValue();
					String contributordate = con.getDateAsISO8601();
					byteBuffer.putShort((short) contributortype.length());
					byteBuffer.put(contributortype.getBytes(StandardCharsets.UTF_8));
					byteBuffer.putShort((short) contributorvalue.length());
					byteBuffer.put(contributorvalue.getBytes(StandardCharsets.UTF_8));
					byteBuffer.putShort((short) contributordate.length());
					byteBuffer.put(contributordate.getBytes(StandardCharsets.UTF_8));
				}
			}
		}

		// write format to byte buffer (if selected)
		if (saveFormat) {
			OMMFormat format = block.getFormat();
			if (format != null) {
				byteBuffer.putShort((short) OMMBlockImpl.FORMAT_CODE.length());
				byteBuffer.put(OMMBlockImpl.FORMAT_CODE.getBytes(StandardCharsets.UTF_8));
				String mimetype = format.getMIMEType();
				URL schema = format.getSchema();
				String schemastr = null;
				if (schema != null) { schemastr = schema.toString(); }
				String encoding = format.getEncryption();
				byteBuffer.putShort((short) mimetype.length());
				byteBuffer.put(mimetype.getBytes(StandardCharsets.UTF_8));
				if (schemastr != null) {
					byteBuffer.putShort((short) schemastr.length());
					byteBuffer.put(schemastr.getBytes(StandardCharsets.UTF_8));
				}
				else byteBuffer.putShort((short)0);
				if (encoding != null) {
					byteBuffer.putShort((short) encoding.length());
					byteBuffer.put(encoding.getBytes(StandardCharsets.UTF_8));
				}
				else byteBuffer.putShort((short)0);
			}
		}

		// write subject to byte buffer (if selected)
		if (saveSubject) {
			OMMSubjectCollection subject = block.getSubject();
			if (subject != null) {
				byteBuffer.putShort((short) OMMBlockImpl.SUBJECT_CODE.length());
				byteBuffer.put(OMMBlockImpl.SUBJECT_CODE.getBytes(StandardCharsets.UTF_8));
				for (OMMSubjectTag tag : subject) {
					byteBuffer.putShort((short) OMMBlockImpl.SUBJECTTAG_CODE.length());
					byteBuffer.put(OMMBlockImpl.SUBJECTTAG_CODE.getBytes(StandardCharsets.UTF_8));
					writeSubjectTagToBinary(tag, byteBuffer);
				}
			}
		}

		// write payload to byte buffer (if selected)
		if (savePayload) {
			TypedValue payload = block.getPayload();
			if (payload != null) {
				byteBuffer.putShort((short) OMMBlockImpl.PAYLOAD_CODE.length());
				byteBuffer.put(OMMBlockImpl.PAYLOAD_CODE.getBytes(StandardCharsets.UTF_8));
				String payloadtype = payload.getType();
				String payloadvalue = payload.getValue().toString();
				byteBuffer.putShort((short) payloadtype.length());
				byteBuffer.put(payloadtype.getBytes(StandardCharsets.UTF_8));
				byteBuffer.putShort((short) payloadvalue.length());
				byteBuffer.put(payloadvalue.getBytes(StandardCharsets.UTF_8));
			}
		}

		// write payload to byte buffer (if selected)
		if (saveLink) {
			TypedValue link = block.getLink();
			if (link != null) {
				byteBuffer.putShort((short) OMMBlockImpl.LINK_CODE.length());
				byteBuffer.put(OMMBlockImpl.LINK_CODE.getBytes(StandardCharsets.UTF_8));
				String linktype = link.getType();
				String linkvalue = link.getValue().toString();
				byteBuffer.putShort((short) linktype.length());
				byteBuffer.put(linktype.getBytes(StandardCharsets.UTF_8));
				byteBuffer.putShort((short) linkvalue.length());
				byteBuffer.put(linkvalue.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	/**
	 * Writes an OMMSubjectTag and its potential children to the given byte buffer for serialization.
	 * @param tag the subject tag to serialize
	 * @param byteBuffer the byte buffer to write
	 */
	private static void writeSubjectTagToBinary(OMMSubjectTag tag, ByteBuffer byteBuffer) {

		// get data to write
		String tagtype = tag.getType().toString();
		String tagtext = tag.getValue();
		OMMSubjectTag child = tag.getChild();

		// write tag type and text
		byteBuffer.putShort((short) tagtype.length());
		byteBuffer.put(tagtype.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putShort((short) tagtext.length());
		byteBuffer.put(tagtext.getBytes(StandardCharsets.UTF_8));

		// write child
		if (child == null) byteBuffer.putShort((short) 0);
		else {
			byteBuffer.putShort((short) OMMBlockImpl.SUBJECTTAG_CODE.length());
			byteBuffer.put(OMMBlockImpl.SUBJECTTAG_CODE.getBytes(StandardCharsets.UTF_8));
			writeSubjectTagToBinary(child, byteBuffer);
		}

	}

	/** Saves an XML representation of an OMM block to a file,
	 * using a customly created binary format and
	 * serizalizing all block contents.
	 *
	 * @param block The {@link OMMBlock} to save.
	 * @param targetFile A {@link File} in which to store the block.
	 * @return True, if the block was saved successfully.
	 */
	public static boolean saveBlockToXML(OMMBlock block, File targetFile) {

		String content = OMMXMLConverter.toXMLFileString(OMMXMLConverter.generateCompleteBlock(block, true));

		FileWriter fw = null;
		try
		{
			fw = new FileWriter(targetFile);
			fw.write(content);
			return true;
		}
		catch(Exception e){ e.printStackTrace(); }
		finally {
			try {
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// return false on error
		return false;
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

	/** Saves a binary memory representation of an OMM to a file,
	 * using a customly created binary format.
	 *
	 * The format is structured as follows:
	 *
	 * (- an optional prologue ("OMM") stating that an OMM is saved)
	 * - complete header information
	 * - complete owner information
	 * - all blocks in the memory
	 *
	 * To allow any application to read the stored information the data
	 * structures to be saved are split up into their atomic values.
	 *
	 * For example, a header consists of a primary Id and a field for
	 * additional blocks (both again consisting of type and value each).
	 *
	 * Additionally, every atomic item is saved as a pair of values:
	 * - first one short containing the length of the following information
	 * - second the information itself converted to bytes
	 *
	 * For example, an OMM prologue at the beginning of a file would look
	 * like this: 3|OMM|...
	 *
	 * @param omm The {@link OMM} to save.
	 * @param binFile A {@link File} in which to store the memory.
	 * @param compressData True if the saved data should be compressed to save disk space (saves as *.ommz).
	 * @return True, if the memory was saved successfully.
	 */
	public static boolean saveOMMToBinary(OMM omm, File binFile, boolean compressData) {

		// create byte buffer
		// TODO too big?
		int estimatedOMMLength= 1024;
		Collection<OMMBlock> blocks = omm.getAllBlocks();
		if (blocks != null) {
			for (OMMBlock b : blocks) {
				if (b.getPayload() != null) {
					estimatedOMMLength += b.getPayload().getValue().toString().length() + 1024;
				}
			}
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(estimatedOMMLength);

		// save a prologue stating the serialized class (OMM)
//		byteBuffer.putShort((short) 3);
//		byteBuffer.put("OMM".getBytes(StandardCharsets.UTF_8));


		// write header
		OMMHeader header = omm.getHeader();
		TypedValue primaryId = header.getPrimaryID();
		String type = primaryId.getType();
		String value = primaryId.getValue().toString();
		byteBuffer.putShort((short) type.length());
		byteBuffer.put(type.getBytes(StandardCharsets.UTF_8));
		byteBuffer.putShort((short) value.length());
		byteBuffer.put(value.getBytes(StandardCharsets.UTF_8));
		TypedValue additionalBlocks = header.getAdditionalBlocks();
		if (additionalBlocks != null) {
			type = additionalBlocks.getType();
			value = additionalBlocks.getValue().toString();
			byteBuffer.putShort((short) type.length());
			byteBuffer.put(type.getBytes(StandardCharsets.UTF_8));
			byteBuffer.putShort((short) value.length());
			byteBuffer.put(value.getBytes(StandardCharsets.UTF_8));
		}
		else byteBuffer.putShort((short) 0);

		// write owner
		String[] primaryIdUrl = omm.getHeader().getPrimaryID().getValue().toString().split("/");
		String memoryName = primaryIdUrl[primaryIdUrl.length-1];
		OMMBlock owner = null;
		try { owner = getOwnerBlockFromOMM(memoryName); }
		catch (FileNotFoundException e) { e.printStackTrace(); }
		if (owner != null) {
			byteBuffer.putShort((short) "o".length());
			byteBuffer.put("o".getBytes(StandardCharsets.UTF_8));
			writeBlockToBinary(byteBuffer, owner, true, true, true, true, true, true, true, true, true);
			byteBuffer.putShort((short) OMMBlockImpl.NEW_BLOCK_CODE.length());
			byteBuffer.put(OMMBlockImpl.NEW_BLOCK_CODE.getBytes(StandardCharsets.UTF_8));
		}
		else byteBuffer.putShort((short) 0);

		// write blocks
		Collection<OMMBlock> allBlocks = omm.getAllBlocks();
		if (allBlocks != null) {
			byteBuffer.putInt(allBlocks.size());	// write number of blocks
			for (OMMBlock block : blocks) {         // and blocks themselves
				writeBlockToBinary(byteBuffer, block, true, true, true, true, true, true, true, true, true);
				byteBuffer.putShort((short) OMMBlockImpl.NEW_BLOCK_CODE.length());
				byteBuffer.put(OMMBlockImpl.NEW_BLOCK_CODE.getBytes(StandardCharsets.UTF_8));
			}
		}
		else byteBuffer.putInt(0); // zero blocks

		// reduce buffer size if possible
		int contentLength = byteBuffer.position();
		byte[] writeBytes = new byte[contentLength];
		byteBuffer.position(0);
		byteBuffer.get(writeBytes, 0, contentLength);

		// compress buffer content if desired
		if (compressData) {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream(writeBytes.length);
			try {
				GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
				try { zipStream.write(writeBytes); }
				finally { zipStream.close(); }
			} catch (IOException e) { e.printStackTrace(); }
			finally {
				try { byteStream.close(); }
				catch (IOException e) { e.printStackTrace(); }
			}
			writeBytes = byteStream.toByteArray();
			if (!binFile.getName().endsWith("ommz")) binFile = new File(binFile.getAbsolutePath() + "z");
		}

		// open and write output file
		FileOutputStream fos = null;
		try {
			// write collected block data
			System.out.println("writing to: " + binFile.getName());
			fos = new FileOutputStream(binFile);
			fos.write(writeBytes);
			fos.flush();
			// return true on success
			return true;
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		finally {
			try { if (fos == null) fos.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}

		// return false on error
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
		// set memory source to given file
		if (omm instanceof OMMImpl) {
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
		}

		// save OMM into file by converting to XML
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

	/** Saves a binary representation of an OMM block to a file,
	 * using the built-in Java serialization and
	 * serizalizing all block contents.
	 *
	 * @param block The {@link OMMBlock} to save.
	 * @param binFile A {@link File} in which to store the block.
	 * @return True, if the block was saved successfully.
	 */
	public static boolean serializeBlock(OMMBlock block, File binFile)
	{
		return serializeBlock(block, binFile, true, true, true, true, true, true, true, true, true);
	}

	/** Saves a binary representation of an OMM block to a file,
	 * using the built-in Java serialization and
	 * only serizalizing the selected information in order to save resources.
	 * (Mandatory information ID, creator and title cannot be omitted,
	 * at least one of namespace and format have to be given.)
	 *
	 * @param block The {@link OMMBlock} to save.
	 * @param binFile A {@link File} in which to store the block.
	 * @param savePrimaryID true if the block`s primaryID should be saved.
	 * @param saveNamespace true if the block`s namespace should be saved.
	 * @param saveType true if the block`s type should be saved.
	 * @param saveDescription true if the block`s description should be saved.
	 * @param saveContributors true if the block`s contributors should be saved.
	 * @param saveFormat true if the block`s format should be saved.
	 * @param saveSubject true if the block`s subject should be saved.
	 * @param savePayload true if the block`s payload should be saved.
	 * @param saveLink true if the block`s link should be saved.
	 * @return True, if the block was saved successfully.
	 */
	public static boolean serializeBlock(OMMBlock block, File binFile,
			 boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription,
			 boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload,
			 boolean saveLink)
	{

		//		System.out.println("saving block " + block.getID() + " of type " + block.getClass().getName());
		//		System.out.println("into " + binFile);

		// create a new block only containing the selected information
		String id = block.getID();
		TypedValue primaryID = null;
		URI namespace = null;
		URL type = null;
		OMMMultiLangText title = block.getTitle();
		OMMMultiLangText description = null;
		OMMEntityCollection contributors = null;
		OMMEntity creator = block.getCreator();
		OMMFormat format = null;
		OMMSubjectCollection subject = null;
		TypedValue payload = null;
		TypedValue link = null;
		if (savePrimaryID) primaryID = block.getPrimaryID();
		if (saveNamespace) namespace = block.getNamespace();
		if (saveType) type = block.getType();
		if (saveDescription) description = block.getDescription();
		if (saveContributors) contributors = block.getContributors();
		if (saveFormat) format = block.getFormat();
		if (saveSubject) subject = block.getSubject();
		if (savePayload) payload = block.getPayload();
		if (saveLink) link = block.getLink();
		OMMBlock blockToSave = OMMBlockImpl.create(id, primaryID, namespace, type, title, description,
				contributors, creator, format, subject, payload, null, link, null);

		// save block into file by serializing with ObjectOutputStream
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(binFile);
			ObjectOutputStream outputStream = new ObjectOutputStream(fos);
			outputStream.writeObject(blockToSave);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// in case of an error
		return false;
	}

	/** Saves a binary memory representation of an OMM to a file,
	 * using the built-in Java serialization.
	 *
	 * @param omm The {@link OMM} to save.
	 * @param binFile A {@link File} in which to store the memory.
	 * @return True, if the memory was saved successfully.
	 */
	public static boolean serializeOMM (OMM omm, File binFile)
	{

//		System.out.println("saving memory " + omm.getHeader().getPrimaryID() + " of type " + omm.getClass().getName());
//		System.out.println("into " + binFile + ", containing ");
//		if (omm.getAllBlockIDs() != null)
//			System.out.println(omm.getAllBlockIDs().size() + " blocks");
//		else System.out.println("no blocks, apparently");

		// save OMM into file by serializing with ObjectOutputStream
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(binFile);
			ObjectOutputStream outputStream = new ObjectOutputStream(fos);
			outputStream.writeObject(omm);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// in case of an error
		return false;
	}

}
