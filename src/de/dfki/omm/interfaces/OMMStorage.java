package de.dfki.omm.interfaces;

public interface OMMStorage 
{
	/**
	 * Writes the given OMM-based memory to internal storage. The storage has to estimate the changes in the memory by its own!
	 * @param omm The memory to store.
	 */
	void WriteOMM(OMM omm);
	
	/**
	 * Reads an OMM-based memory from the internal storage.
	 * @return The loaded memory.
	 */
	OMM ReadOMM();
	
	/**
	 * Writes the given OMM-based memory header to internal storage. The storage has to estimate the changes in the memory header by its own!
	 * @param header The memory header to store.
	 */
	void WriteHeader(OMMHeader header);
	
	/**
	 * Reads an OMM-based memory header from the internal storage.
	 * @return The loaded memory header.
	 */
	OMMHeader ReadHeader();
	
	/**
	 * Writes the given OMM-based memory block to internal storage. The storage has to estimate the changes in the memory block by its own!
	 * @param block The memory block to store.
	 */
	void WriteBlock(OMMBlock block);
	
	/**
	 * Reads an OMM-based memory block from the internal storage.
	 * @param blockID The ID of the block to read.
	 * @return The loaded memory block.
	 */
	OMMBlock ReadBlock(String blockID);
	
	/**
	 * Deletes the given block from the memory.
	 * @param blockID The ID of the block to delete.
	 */
	void DeleteBlock(String blockID);        
}
