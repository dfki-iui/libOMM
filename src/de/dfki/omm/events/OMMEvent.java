package de.dfki.omm.events;

import de.dfki.omm.interfaces.OMM;
import de.dfki.omm.interfaces.OMMBlock;
import de.dfki.omm.types.OMMEntity;

/**
 * An OMMEvent which can be fired on changes in an OMM. 
 * Events consist of an {@link OMMEventType}, the entity that triggers them and the OMM or specific block where they are triggered.  
 */
public class OMMEvent
{
	public OMM omm;
	public OMMBlock block;
	public OMMEntity entity; 
	public OMMEventType type;
	
	/**
	 * Constructor.
	 * @param omm The OMM from which this event originates. 
	 * @param block The block from which this event originates. 
	 * @param entity The entity that triggered this event. 
	 * @param type The {@link OMMEventType} of this event. 
	 */
	public OMMEvent(OMM omm, OMMBlock block, OMMEntity entity, OMMEventType type)
	{
		this.omm = omm;
		this.block = block;
		this.entity = entity;
		this.type = type;
	}
}
