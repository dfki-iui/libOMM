package de.dfki.omm.interfaces;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import de.dfki.omm.impl.OMMFactory;
import de.dfki.omm.tools.OMMActionResultType;
import de.dfki.omm.types.OMMEntity;

/***
 * Represents an instance of the OMM (object memory model). <br>
 * Please use the methods of {@link OMMFactory} to create new OMM instances. 
 * @author Jens Haupert (jens.haupert@dfki.de)
 *
 */
public interface OMM extends Serializable
{
	/***
	 * Retrieves the header of this object memory.
	 * @return returns the header as {@link OMMHeader} instance.
	 */
	public OMMHeader getHeader();
	
	/***
	 * Retrieves the table of contents of this object memory.
	 * @return returns the table of contents as {@link Collection} of type {@link OMMToCEntry}.
	 */
	public Collection<OMMToCEntry> getTableOfContents();
	
	/***
	 * Retrieves a specific block by a given block ID.
	 * @param blockID the ID of the requested block.
	 * @return returns the requested block as {@link OMMBlock} instance.
	 */
	public OMMBlock getBlock(String blockID);
	
	/***
	 * Retrieves a collection of all blocks of this object memory.
	 * @return returns all blocks as {@link Collection} of type {@link OMMBlock}.
	 */
	public Collection<OMMBlock> getAllBlocks();
	
	/***
	 * Adds a new block to this memory. This block must not contain any OMMEntity as creator/contributor! The creator is set automatically to the given entity.
	 * @param block the block to add.
	 * @param entity the entity that adds this block
	 * @return returns a {@link OMMActionResultType} that indicates the result of this action.
	 */
	public OMMActionResultType addBlock(OMMBlock block, OMMEntity entity);
	
	/***
	 * Removes the given block from this memory.
	 * @param block the block to remove
	 * @param entity the entity that removes this block
	 * @return returns a {@link OMMActionResultType} that indicates the result of this action.
	 */
	public OMMActionResultType removeBlock(OMMBlock block, OMMEntity entity);
	
	/***
	 * Removes the block with the given ID from this memory.
	 * @param blockID the ID of the block to remove
	 * @param entity the entity that removes this block
	 * @return returns a {@link OMMActionResultType} that indicates the result of this action.
	 */
	public OMMActionResultType removeBlock(String blockID, OMMEntity entity);
	
	/**
	 * Retrieves a list of the IDs of all blocks in this memory
	 * @return the list of all IDs
	 */
	public List<String> getAllBlockIDs();	
}
