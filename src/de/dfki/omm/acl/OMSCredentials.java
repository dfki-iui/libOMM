package de.dfki.omm.acl;

import org.restlet.resource.ClientResource;

/** Abstract class of which credential types for access to the (REST interface of an) object memory can be derived. */
public abstract class OMSCredentials {

	protected String m_cleartextname;

	/**
	 * Returns clear text name as String.
	 * @return clear text name as {@link String}.
	 */
	public String getCleartextname () {
		return m_cleartextname;
	}
	
	/**
	 * Returns credentials as a specifically formatted String.
	 * @return credentials as a specifically formatted String.
	 */
	public abstract String getOMSCredentialString (); 
	
	/**
	 * Given a ClientResource, updates its ChallengeResponse with these credentials.
	 * @param cr A ClientResource in need of a ChallengeResponse.
	 */
	public abstract void updateClientResource (ClientResource cr);
	
}
