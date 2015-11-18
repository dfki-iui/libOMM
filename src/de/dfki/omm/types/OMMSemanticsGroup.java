package de.dfki.omm.types;

import java.util.Vector;


/** A Vector of {@link OMMSemanticsInfo}s to collect all semantic information of a semantic block. */
public class OMMSemanticsGroup extends Vector<OMMSemanticsInfo>
{
	private static final long serialVersionUID = 3841651914566574720L;
	
	/** Checks whether a specified semantic relation is present in the group. 
	 * @param relation The name of the relation, for example "urn:omm:structure:isConnectedWith".
	 * @return True, if the relation is found in the group.
	 */
	public boolean hasRelation(String relation)
	{
		for(OMMSemanticsInfo info : this)
		{
			if (info.getRelation().equals(relation)) return true;
		}
		
		return false;
	}
}
