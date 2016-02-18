package de.dfki.omm.impl.rest;

import org.json.JSONException;
import org.json.JSONObject;

import de.dfki.omm.acl.OMSCredentials;
import de.dfki.omm.interfaces.JSONOutput;
import de.dfki.omm.tools.DownloadHelper;

import java.io.Serializable;

/** Basic information about an OMM and its REST interface, such as the address to the block storage or the management node. */
public class OMMRestNegotiationData implements JSONOutput, Serializable {
	
	private int m_version = -1;
	private OMMRestNegotiationStorage m_storage = null;
	private OMMRestNegotiationManagement m_management = null;

	private OMMRestNegotiationData() {
	}

	/** Creates new REST negotiation data, given all necessary attributes.
	 * 
	 * @param version The version as int. 
	 * @param storage Information about the object memory's block storage node as {@link OMMRestNegotiationStorage}.
	 * @param man Information about the object memory's management node as {@link OMMRestNegotiationManagement}.
	 * @return The created {@link OMMRestNegotiationData} object. 
	 */
	public static OMMRestNegotiationData create(int version, OMMRestNegotiationStorage storage, OMMRestNegotiationManagement man) {
		OMMRestNegotiationData data = new OMMRestNegotiationData();
		data.m_version = version;
		data.m_storage = storage;
		data.m_management = man;
		return data;
	}

	/** Parses and creates new REST negotiation data from the REST interface, given a memory's URL and credentials.
	 * 
	 * @param restURL The OMM's address.
	 * @param credentials The credentials to use when trying to access the OMM.
	 * @return The created {@link OMMRestNegotiationData} object. 
	 */
	public static OMMRestNegotiationData downloadAndCreate(String restURL, OMSCredentials credentials) {
		try {
			String data = DownloadHelper.downloadData(restURL, credentials);
			if (data == null || data.length() < 1)
				return null;

			JSONObject json = new JSONObject(data);

			OMMRestNegotiationData retVal = new OMMRestNegotiationData();
			retVal.m_storage = OMMRestNegotiationStorage.createFromJSON(json.getJSONObject("STORAGE"));
			retVal.m_management = OMMRestNegotiationManagement.createFromJSON(json.getJSONObject("MANAGEMENT"));
			retVal.m_version = json.getInt("VERSION");
			return retVal;
		} catch (Exception e) {
			System.err.println("Unable to download data from REST URL '" + restURL + "'!");
			e.printStackTrace();
		}
		return null;
	}

	/** Retrieves information about the block storage node.
	 * 
	 * @return The used storage as {@link OMMRestNegotiationStorage}.
	 */
	public OMMRestNegotiationStorage getStorage() {
		return m_storage;
	}

	/**
	 * Retrieves information about the management node.
	 * 
	 * @return The used storage as {@link OMMRestNegotiationManagement}.
	 */
	public OMMRestNegotiationManagement getManagement() {
		return m_management;
	}

	/**
	 * Retrieves the version of the negotiation data.
	 * 
	 * @return The version as int.
	 */
	public int getVersion() {
		return m_version;
	}

	@Override
	public String toString() {
		return "Version: " + getVersion() + "\r\n" + "Storage: "
				+ getStorage().toString() + "\r\n" + "Management: "
				+ getManagement().toString();
	}

	@Override
	public String toJSONString() {
		JSONObject obj = toJSONObject();
		if (obj == null) return null;
		return obj.toString();
	}

	@Override
	public JSONObject toJSONObject() {

		JSONObject feature = new JSONObject();

		try {
			feature.put("VERSION", m_version);
			feature.put("STORAGE", m_storage.toJSONObject());
			feature.put("MANAGEMENT", m_management.toJSONObject());
			return feature;
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}
}
