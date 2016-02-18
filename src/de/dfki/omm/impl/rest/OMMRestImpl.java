/**
 * 
 */
package de.dfki.omm.impl.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.dfki.omm.impl.OMMFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.interfaces.OMMRestInterface;
import de.dfki.omm.interfaces.OMMToCEntry;
import de.dfki.omm.tools.OMMActionResultType;
import de.dfki.omm.tools.OMMXMLConverter;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMRestAccessMode;

/** 
 * @author samuel
 */
public class OMMRestImpl implements OMM, OMMRestInterface {

	public static int REST_CACHE_TIME_IN_SECONDS = 20;
	
	protected String	restURL;
	protected OMMRestAccessMode mode;
	protected List<String> blockIDsCache;
	protected Long lastAccess = 0L;
	protected OMMRestNegotiationData negDataCache = null;
	protected OMSCredentials m_credentials = null; 

	/** Creates a new OMM using the OMS-RESTful interface
	 * 
	 * @param restUrl The OMS memory URL 
	 * @param mode The accessing mode
	 * @param credentials The accessing credentials
	 */
	public OMMRestImpl(String restUrl, OMMRestAccessMode mode, OMSCredentials credentials) {
		this.restURL = restUrl;
		this.mode = mode;
		this.m_credentials = credentials;
	}
	
	/**
	 * Creates a new OMM using the OMS-RESTful interface
	 * @param restUrl The OMS memory URL 
	 * @param mode The accessing mode
	 */
	public OMMRestImpl(String restUrl, OMMRestAccessMode mode) {
		this.restURL = restUrl;
		this.mode = mode;
	}
	
	/**
	 * Creates a new OMM using the OMS-RESTful interface with SingleAccess mode
	 * @param restUrl RESTful address of the memory, like in: <code>http://localhost:10082/rest/MemoryName/</code>
	 */
	public OMMRestImpl(String restUrl) {
		this.restURL = restUrl;
		this.mode = OMMRestAccessMode.SingleAccess;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#addBlock(de.dfki.omm.interfaces.OMMBlock, de.dfki.omm.types.OMMEntity)
	 */
	public OMMActionResultType addBlock(OMMBlock block, OMMEntity entity) {

		try {
			String blockString = OMMXMLConverter.toXMLFileString(OMMXMLConverter.generateCompleteBlock(block, true));
			Response rep = this.postBlock(blockString);
			if (rep.getStatus().equals(Status.SUCCESS_CREATED))
			{
				String newBlockID = rep.getEntityAsText();
				block.setID(newBlockID);
				return OMMActionResultType.OK;
			}
			else if (rep.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) return OMMActionResultType.Forbidden;
			return OMMActionResultType.UnknownError;
		} catch (ResourceException e) {
			e.printStackTrace();
			if (e.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) return OMMActionResultType.Forbidden;
			return OMMActionResultType.UnknownError;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#getAllBlockIDs()
	 */
	public List<String> getAllBlockIDs()
	{
		long now = new GregorianCalendar().getTime().getTime();
		
		switch(mode)
		{
			case CompleteDownloadUnlimited:
				if (blockIDsCache != null) return blockIDsCache;
				break;
			case CompleteDownloadLimitedLifetime:				
				if (blockIDsCache != null && (now - lastAccess) <= OMMRestImpl.REST_CACHE_TIME_IN_SECONDS) return blockIDsCache;
				break;
			default:
				// do nothing here
				break;
		}
		
		try		
		{
			ClientResource c = new ClientResource(getStorageURL() +"/block_ids");
			if (m_credentials != null) m_credentials.updateClientResource(c);
			Representation representation = c.get();
			if (representation == null) return null;
			BufferedReader br = new BufferedReader(representation.getReader());
			String line;
			StringBuilder sb = new StringBuilder();
			
		    while ((line = br.readLine()) != null) 
		    {
		        sb.append(line);
		    }
		    
		    JSONObject jsonO = new JSONObject(sb.toString());
		    JSONArray array = (JSONArray)jsonO.get("IDs");
		    
		    List<String> retVal = new Vector<String>(array.length());
		    for(int i = 0; i < array.length(); i++)
		    {
		    	retVal.add(array.get(i).toString());
		    }
		    
		    if (mode != OMMRestAccessMode.SingleAccess ) blockIDsCache = retVal;
		    lastAccess = now;
		    
		    return retVal;
		}
		catch(Exception e){ e.printStackTrace(); }
		
		return null;
		
		/*Collection<OMMBlock> blocks = getAllBlocks();
		LinkedList<String> ids = new LinkedList<String>();
		for (OMMBlock b : blocks) {
			ids.add(b.getID());
		}
		return ids;*/
	}

	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#getAllBlocks()
	 */
	public Collection<OMMBlock> getAllBlocks() {
		
		List<String> blockIDs = getAllBlockIDs();
		if (blockIDs == null) return null;
		
		Collection<OMMBlock> retVal = new Vector<OMMBlock>(blockIDs.size());
		
		for(String id : blockIDs)
		{
			retVal.add(new OMMBlockRestImpl(id, getStorageURL(), mode, this));
		}
		
		return retVal;		
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#getBlock(java.lang.String)
	 */
	public OMMBlock getBlock(String blockID) {

		return new OMMBlockRestImpl(blockID, getStorageURL(), mode, this);

	}
	
	/** Retrieves the credentials used by this OMM. 
	 * @return Credentials as {@link OMSCredentials}. 
	 */
	public OMSCredentials getCredentials () {
		return m_credentials;
	}
	
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#getHeader()
	 */
	public OMMHeader getHeader() {
		
		ClientResource cr = new ClientResource(getStorageURL() +"/header");
		if (m_credentials != null) m_credentials.updateClientResource(cr);
		Representation representation = cr.get();
		if (representation == null) return null;

		try 
		{
			BufferedReader br = new BufferedReader(representation.getReader());
			int c;
			StringBuilder sb = new StringBuilder();
			
		    while ((c = br.read()) > -1) 
		    {
		        sb.append((char)c);
		    }
		    
		    Document doc = OMMXMLConverter.getXmlDocumentFromString(OMMXMLConverter.getInputStreamFromText(sb.toString()));

			return OMMXMLConverter.parseHeader(doc.getDocumentElement());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMRestInterface#getNegotiationData()
	 */
	public OMMRestNegotiationData getNegotiationData()
	{		
		//switch(mode)
		//{
			//case CompleteDownloadUnlimited:
				if (negDataCache == null) negDataCache = OMMRestNegotiationData.downloadAndCreate(restURL, m_credentials);
				return negDataCache;
			/*case CompleteDownloadLimitedLifetime:
				long now = new GregorianCalendar().getTime().getTime();
				if (negDataCache == null || (now - lastAccess) > OMMRestImpl.REST_CACHE_TIME_IN_SECONDS) negDataCache = OMMRestNegotiationData.downloadAndCreate(restURL);
				return negDataCache;
			default:
				return OMMRestNegotiationData.downloadAndCreate(restURL);*/
		//}		
	}	
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMRestInterface#getRestAccessMode()
	 */
	public OMMRestAccessMode getRestAccessMode()
	{
		return mode;
	}
	
	/** Retrieves the address of the OMM's storage node. 
	 * @return Storage URL as String. 
	 */
	protected String getStorageURL()
	{
		OMMRestNegotiationData data = getNegotiationData();
		if (data == null) return null;
		return data.getStorage().getLink();
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#getTableOfContents()
	 */
	public Collection<OMMToCEntry> getTableOfContents() {
		Collection<OMMToCEntry> entries = new LinkedList<OMMToCEntry>();
		entries.addAll(this.getAllBlocks());
		return entries;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMRestInterface#invalidateCache()
	 */
	public void invalidateCache()
	{		
		for(OMMBlock block : getAllBlocks())
		{
			OMMBlockRestImpl rBlock = (OMMBlockRestImpl)block;
			rBlock.invalidateCache();
		}
		
		blockIDsCache = null;
		
		negDataCache = null;
	}
	
	/** HTTP-POSTs a block to this OMM's block storage.
	 *   
	 * @param xml The block to post as an XML String. 
	 * @return The {@link Response} to the POST command.
	 * @throws ResourceException If there is a problem accessing the REST interface.
	 */
	protected Response postBlock (String xml) throws ResourceException {
		ClientResource c = new ClientResource(getStorageURL() + "/block");
		if (m_credentials != null) m_credentials.updateClientResource(c);
		c.post(xml);
		return c.getResponse();
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#removeBlock(de.dfki.omm.interfaces.OMMBlock, de.dfki.omm.types.OMMEntity)
	 */
	public OMMActionResultType removeBlock(OMMBlock block, OMMEntity entity) {
		return this.removeBlock(block.getID(), entity);
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMM#removeBlock(java.lang.String, de.dfki.omm.types.OMMEntity)
	 */
	public OMMActionResultType removeBlock(String blockID, OMMEntity entity) {
		ClientResource c = new ClientResource(getStorageURL() + "/block/"+blockID);
		if (m_credentials != null) m_credentials.updateClientResource(c);
		try 
		{
			c.delete();
			if (c.getStatus().equals(Status.SUCCESS_OK)) return OMMActionResultType.OK;
			else if (c.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) return OMMActionResultType.Forbidden;
		}
		catch (ResourceException e)
		{ 
			if (e.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) return OMMActionResultType.Forbidden;
			e.printStackTrace();			
		}
		return OMMActionResultType.UnknownError;
	}
	
	/**
	 * Sets user credentials to a given value. Different users may have different access rights on an OMM. (Overwrites existing credentials.)
	 * @param credentials New credentials
	 */
	public void setCredentials (OMSCredentials credentials) {
		m_credentials = credentials;
	}
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.OMMRestInterface#setRestAccessMode(de.dfki.omm.types.OMMRestAccessMode)
	 */
	public void setRestAccessMode(OMMRestAccessMode newMode)
	{
		mode = newMode;
	}




	/**
	 * Custom method to serialize OMMRestImpls and their blocks properly
	 *
	 * @param outputStream Stream to write to
	 * @throws IOException
	 */
	private synchronized void writeObject(java.io.ObjectOutputStream outputStream) throws IOException {

		// write basic memory information
		outputStream.defaultWriteObject();

		// write header
		outputStream.writeObject(getHeader());

		// write owner
		String[] primaryID = getHeader().getPrimaryID().getValue().toString().split("/");
		String memoryName = primaryID[primaryID.length-1];
		outputStream.writeObject(OMMFactory.getOwnerBlockFromOMM(memoryName));

		// write memory blocks
		// (order of written objects will be maintained when reading)
		Collection<OMMBlock> blocks = this.getAllBlocks();
		if (blocks != null) {
			outputStream.writeObject(blocks.size()); 			// write number of blocks
			for (OMMBlock block: blocks) {						// write blocks
				if (block instanceof OMMBlockRestImpl)
					outputStream.writeObject(((OMMBlockRestImpl) block).getAsRegularBlock());
				else
					outputStream.writeObject(block);
			}
		}
		else outputStream.writeObject(0);						// if memory does not contain blocks

	}

	/**
	 * Custom method to deserialize OMMRestImpls and their blocks properly
	 *
	 * @param inputStream Stream to read from
	 * @throws IOException
	 */
	private synchronized void readObject(java.io.ObjectInputStream inputStream) throws IOException, ClassNotFoundException {

		// read basic memory information
		inputStream.defaultReadObject();

		// read header
		OMMHeader header = null;
		Object loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMHeader) {
			header = (OMMHeader) loadedInfo;
		}

		// read owner
		OMMBlock owner = null;
		loadedInfo = inputStream.readObject();
		if (loadedInfo instanceof OMMBlock) {
			owner = (OMMBlock) loadedInfo;
		}

		// create new OMM on the OMS and add loaded blocks
		if (header != null) {

			// create empty OMM to add blocks to
			String omsUrl = header.getPrimaryID().getValue().toString();
			omsUrl = omsUrl.substring(0, omsUrl.indexOf("rest/"));
			OMMFactory.createOMMViaOMSRestInterface(omsUrl + "mgmt/createMemory", header, owner);
			//OMMFactory.createOMMViaOMSRestInterface(omsUrl + "mgmt/createMemory", header, null);

			// read memory blocks
			loadedInfo = inputStream.readObject(); 		// read number of blocks
			int numberOfBlocks = -1;
			if (loadedInfo instanceof Integer) {
				numberOfBlocks = (int) loadedInfo;
			}
			for (int i = 0; i < numberOfBlocks; i++) {			// read blocks
				loadedInfo = inputStream.readObject();
				if (loadedInfo instanceof OMMBlock) {
					OMMBlock block = (OMMBlock) loadedInfo;
					this.addBlock(block, block.getCreator());
				}
			}
		}
	}

}
