package de.dfki.omm.events;

import java.io.Serializable;

/**
 * Interface for classes that can react to {@link OMMEvent}s. 
 */
public interface OMMEventListener
{
	/** Reacts to an event.
	 * @param event The {@link OMMEvent} to which to react. 
	 */
	public void eventOccured(OMMEvent event);
}
