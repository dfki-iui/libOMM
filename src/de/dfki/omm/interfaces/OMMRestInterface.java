package de.dfki.omm.interfaces;

import de.dfki.omm.impl.rest.OMMRestNegotiationData;
import de.dfki.omm.types.OMMRestAccessMode;

/** Interface for RESTful representations of OMMs, controlling cached data and access modes. */
public interface OMMRestInterface {

	/** Deletes cached block data, forcing a reload on the next access. */
	public void invalidateCache();
	/** Retrieves the negotiation data of this OMM. 
	 * @return The memory's (cached) {@link OMMRestNegotiationData}. */
	public OMMRestNegotiationData getNegotiationData();
	
	/** Allows to specify a (new) access mode for the OMM. 
	 * @param newMode {@link OMMRestAccessMode} to use for this memory. */
	public void setRestAccessMode(OMMRestAccessMode newMode);
	/** Retrieves the currently used access mode of the OMM.
	 * @return {@link OMMRestAccessMode} that is used for this memory. */
	public OMMRestAccessMode getRestAccessMode();

}
