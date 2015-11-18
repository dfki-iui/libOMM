package de.dfki.omm.impl;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dfki.omm.events.OMMEvent;
import de.dfki.omm.events.OMMEventListener;
import de.dfki.omm.events.OMMEventType;
import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.interfaces.OMMHeader;
import de.dfki.omm.interfaces.OMMToCEntry;
import de.dfki.omm.tools.OMMActionResultType;
import de.dfki.omm.types.OMMEntity;
import de.dfki.omm.types.OMMSourceType;
import de.dfki.omm.types.TypedValue;

/** Implementation of {@link OMM}. */
public class OMMImpl implements OMM
{	
	public static int VERSION = 1;
	protected OMMHeader m_header = null;
	protected LinkedHashMap<String, OMMBlock> m_blocks = null;
	protected URL m_sourceURL = null;
	protected File m_sourceFile = null;
	protected OMMSourceType m_sourceType = null;
	protected HashSet<OMMEventListener> m_listener = null;
	protected ExecutorService exService;
	
	protected OMMImpl() 
	{	
		m_listener = new HashSet<OMMEventListener>();
		exService = Executors.newCachedThreadPool();
	}
	
	/** Creates a new empty OMM. 
	 * 
	 * @param primaryID {@link TypedValue} containing the primary ID of the empty OMM. 
	 * @return The created memory as {@link OMM}. 
	 */
	public static OMM create(TypedValue primaryID)
	{
		OMMImpl retVal = new OMMImpl();
		retVal.m_header = OMMHeaderImpl.create(primaryID, null);
		retVal.m_blocks = new LinkedHashMap<String, OMMBlock>();
		return retVal;
	}
	
	/** Creates a new empty OMM. 
	 * 
	 * @param secureHeader An {@link OMMSecurityHeader} for the new OMM. 
	 * @return The created memory as {@link OMM}. 
	 */
	public static OMM create(OMMHeader secureHeader)
	{
		OMMImpl retVal = new OMMImpl();
		retVal.m_header = secureHeader;
		retVal.m_blocks = new LinkedHashMap<String, OMMBlock>();
		return retVal;
	}

	/** Creates a new OMM with blocks and a source. 
	 * 
	 * @param header An {@link OMMHeader} for the new OMM. 
	 * @param blocks A {@link Collection} of the OMM's blocks as {@link OMMBlock}s.
	 * @param source The source for the OMM as a {@link URL}. 
	 * @param sourceType {@link OMMSourceType} of the given source. 
	 * @return The created memory as {@link OMM}. 
	 */
	public static OMM create(OMMHeader header, Collection<OMMBlock> blocks, URL source, OMMSourceType sourceType)
	{
		OMMImpl omm = new OMMImpl();
		
		omm.m_header = header;
		omm.m_blocks = new LinkedHashMap<String, OMMBlock>();
		omm.m_sourceURL = source;
		omm.m_sourceType = sourceType;
		
		for(OMMBlock block : blocks)
		{
			omm.m_blocks.put(block.getID(), block);
		}
		
		return omm;
	}
	
	/** Creates a new OMM with blocks and a source. 
	 * 
	 * @param header An {@link OMMHeader} for the new OMM. 
	 * @param blocks A {@link Collection} of the OMM's blocks as {@link OMMBlock}s.
	 * @param source The source for the OMM as a {@link File}. 
	 * @param sourceType {@link OMMSourceType} of the given source. 
	 * @return The created memory as {@link OMM}. 
	 */
	public static OMM create(OMMHeader header, Collection<OMMBlock> blocks, File source, OMMSourceType sourceType)
	{
		OMMImpl omm = new OMMImpl();
		
		omm.m_header = header;
		omm.m_blocks = new LinkedHashMap<String, OMMBlock>();
		omm.m_sourceFile = source;
		omm.m_sourceType = sourceType;
		
		for(OMMBlock block : blocks)
		{
			omm.m_blocks.put(block.getID(), block);
		}
		
		return omm;
	}
	
	/** Adds an event listener to the OMM. 
	 * @param listener The {@link OMMEventListener} to add. 
	 */
	public void addEventListener(OMMEventListener listener)
	{
		if (!m_listener.contains(listener))
		{
			m_listener.add(listener);
		}
	}
	
	/** Removes an event listener from the OMM. 
	 * @param listener The {@link OMMEventListener} to remove. 
	 */
	public void removeEventListener(OMMEventListener listener)
	{
		if (m_listener.contains(listener)) m_listener.remove(listener);
	}
	
	public OMMHeader getHeader()
	{
		return m_header;
	}
	
	public Collection<OMMToCEntry> getTableOfContents()
	{
		Collection<OMMBlock> blocks = getAllBlocks();
		Collection<OMMToCEntry> toc = new LinkedList<OMMToCEntry>();
		
		for (OMMBlock block : blocks)
		{
			toc.add(block);
		}
		
		return toc;
	}
	
	public OMMBlock getBlock(String blockID)
	{
		if (m_blocks.containsKey(blockID))
		{
			return m_blocks.get(blockID);
		}
		
		return null;
	}
	
	public Collection<OMMBlock> getAllBlocks()
	{
		return m_blocks.values();
	}


	/** Retrieves the object memory's source. 
	 * @return The source as {@link URL}. 
	 */
	public URL getSourceAsURL() { return m_sourceURL; }
	
	/** Retrieves the object memory's source. 
	 * @return The source as {@link File}. 
	 */
	public File getSourceAsFile() { return m_sourceFile; }
	
	/** Retrieves the object memory's source type. 
	 * @return The object memory's {@link OMMSourceType}. 
	 */
	public OMMSourceType getSourceType() { return m_sourceType; }
	
	/** Sets the object memory's source, overwriting an existing one. 
	 * @param newSource The source as {@link URL}. 
	 */
	public void setSource(URL newSource) { m_sourceFile = null; m_sourceURL = newSource; }
	
	/** Sets the object memory's source, overwriting an existing one. 
	 * @param newSource The source as {@link File}. 
	 */
	public void setSource(File newSource) { m_sourceURL = null; m_sourceFile = newSource; }
	
	/** Sets the object memory's source, overwriting an existing one. 
	 * @param newSourceType The new {@link setSourceType}. 
	 */
	public void setSourceType(OMMSourceType newSourceType) { m_sourceType = newSourceType; }
	
	public OMMActionResultType addBlock(OMMBlock block, OMMEntity entity) 
	{
		for(String id : m_blocks.keySet())
		{
			if (id.equals(block.getID())) return OMMActionResultType.BlockWithSameIDExists;
		}
		
		((OMMBlockImpl)block).setCreator(entity);
		
		m_blocks.put(block.getID(), block);
		
		((OMMBlockImpl)block).setParentOMM(this);
			
		fireOMMEvent(new OMMEvent(this, block, entity, OMMEventType.BLOCK_ADDED));
		
		return OMMActionResultType.OK;
	}
	
	/** Adds a new block to this memory without setting the creator automatically. 
	 * 
	 * @param block The {@link OMMBlock} to add. 
	 * @param entity The {@link OMMEntity} that adds or created this block. 
	 * @return An {@link OMMActionResultType} that indicates the result of this action.
	 */
	public OMMActionResultType addBlockWithoutChanges(OMMBlock block, OMMEntity entity) 
	{
		for(String id : m_blocks.keySet())
		{
			if (id.equals(block.getID())) return OMMActionResultType.BlockWithSameIDExists;
		}
		
		m_blocks.put(block.getID(), block);
		
		((OMMBlockImpl)block).setParentOMM(this);
		
		fireOMMEvent(new OMMEvent(this, block, entity, OMMEventType.BLOCK_ADDED));
		
		return OMMActionResultType.OK;
	}

	public OMMActionResultType removeBlock(OMMBlock block, OMMEntity entity) 
	{
		return removeBlock(block.getID(), entity);
	}

	public OMMActionResultType removeBlock(String blockID, OMMEntity entity) 
	{
		if (!m_blocks.containsKey(blockID)) return OMMActionResultType.BlockNotExistent;

		// get block and remove it 
		OMMBlock block = m_blocks.get(blockID);
		m_blocks.remove(blockID);
		((OMMBlockImpl)block).setParentOMM(null);
		fireOMMEvent(new OMMEvent(this, block, entity, OMMEventType.BLOCK_REMOVED));

		return OMMActionResultType.OK;
	}

	public List<String> getAllBlockIDs()
	{
		List<String> retVal = new Vector<String>(m_blocks.size());
		for(OMMBlock block : m_blocks.values())
		{
			retVal.add(block.getID());
		}
		return retVal;
	}
	
	public String toString()
	{
		String retVal = "OMM:\n\t"+m_header.toString()+"\n";
		
		retVal += "\tOMM-ToC:\n";
		
		int counter = 1;
		for(OMMToCEntry toc : getTableOfContents())
		{
			retVal += "\t("+counter+") "+toc.toString()+"\n";
			counter++;					
		}
		
		retVal += "\tOMM-Block:\n";
		
		counter = 1;
		for(OMMBlock block : m_blocks.values())
		{
			String blockData = block.toString();
			retVal += "\t("+counter+") "+blockData+"\n";
			counter++;					
		}
		
		return retVal;
	}

	/** Hands an event to the memory's {@link OMMEventListener} (if set). 
	 * 
	 * @param event The {@link OMMEvent} to be fired. 
	 */
	public void fireOMMEvent (final OMMEvent event)
	{
		if (event.entity != null && event.block != null)
		{
			/*if (((event.block.getContributors() != null && event.block.getContributors().size() > 0) || 
				!event.block.getCreator().equals(event.entity)) && !(event.block.getContributors() != null && event.block.getContributors().contains(event.entity)))*/
			if (event.block.getContributors() != null)
			{
				for(OMMEntity cEntity : event.block.getContributors())
				{
					if (event.entity.equalsTypeAndValue(cEntity)) // entity has contributed earlier -> update contributor value
					{
						((OMMBlockImpl)event.block).getContributors().remove(cEntity);												
					}
				}
				((OMMBlockImpl)event.block).addContributor(event.entity);	
			}			
		}
		
		//System.out.println("fireOMMEvent()");
		
		for(final OMMEventListener l : m_listener)
		{
			System.out.println("OMMEvent to "+l);
			exService.execute(new Runnable()
			{				
				public void run()
				{
					l.eventOccured(event);					
				}
			});			
		}
	}

}
