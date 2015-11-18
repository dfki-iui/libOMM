package de.dfki.omm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import de.dfki.omm.acl.OMSCredentials;

/** Tool to request data from the REST interface and provide it internally. */
public class DownloadHelper {

	static TrustManager[] trustAllCerts = null;
	
	static
	{
		// Create a new trust manager that trust all certificates
		trustAllCerts = new TrustManager[]{
		    new X509TrustManager() {
		        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return null;
		        }
		        public void checkClientTrusted(
		            java.security.cert.X509Certificate[] certs, String authType) {
		        }
		        public void checkServerTrusted(
		            java.security.cert.X509Certificate[] certs, String authType) {
		        }
		    }
		};
		
		try 
		{
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} 
		catch (Exception e) { e.printStackTrace(); }
		
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier( new javax.net.ssl.HostnameVerifier()
		{ 
			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) 
			{ 
				if (hostname.equals("localhost")) { return true; } return false; } 
			}
		); 
	}
	
	/** Requests data from a given REST node and converts it to String. 
	 * @param restURL URL to the node from which to download. 
	 * @param credentials {@link OMSCredentials} to be used to access the node. 
	 * @return Retrieved data as String.
	 */
	public static String downloadData(String restURL, OMSCredentials credentials)
	{		

	    InputStream is = null;
	    String line;
	    StringBuffer buffer = new StringBuffer();

	    try {
	    	ClientResource c = new ClientResource(restURL);
			if (credentials != null) credentials.updateClientResource(c);
			Representation representation = c.get();
			BufferedReader br = null;
			Reader reader = null;
			if (representation == null) return null;
			else {
				reader = representation.getReader();
				if (reader == null) return null;
			}
			br = new BufferedReader(reader);

	        while ((line = br.readLine()) != null) {
	        	buffer.append(line);
	        }
	    } catch (Exception ioe) {
	    	System.out.println("---- that went wrong: restUrl="+restURL+", credentials="+credentials.getOMSCredentialString());
	         ioe.printStackTrace();
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
	    }
	    
	    return buffer.toString();
	}
	
	
//	/** Unused version of downloadData.  
//	 * @param restURL URL to the node from which to download. 
//	 * @return Retrieved data as String. 
//	 */
//	public static String downloadDataREST(String restURL)
//	{
//		try {			
//			ClientResource c = new ClientResource(restURL);
//			Representation r = c.get();
//			BufferedReader br = new BufferedReader(r.getReader());
//			StringBuilder sb = new StringBuilder();
//			String line;
//			while ((line = br.readLine()) != null) {
//				sb.append(line);
//		    }
//			br.close();			
//			return sb.toString();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
	
	
//	/** Former version of dowloadData.
//	 * @param restURL URL to the node from which to download. 
//	 * @return Retrieved data as String. 
//	 */
//	public static String downloadDataOld (String restURL)
//	{		
//		URL url;
//	    InputStream is = null;
//	    BufferedReader br;
//	    String line;
//	    StringBuffer buffer = new StringBuffer();
//
//	    try {
//	        url = new URL(restURL);
//	        is = url.openStream();  // throws an IOException
//	        br = new BufferedReader(new InputStreamReader(is));
//
//	        while ((line = br.readLine()) != null) {
//	        	buffer.append(line);
//	        }
//	    } catch (Exception ioe) {
//	         ioe.printStackTrace();
//	    } finally {
//	        try {
//	            if (is != null) is.close();
//	        } catch (IOException ioe) {
//	            // nothing to see here
//	        }
//	    }
//	    
//	    return buffer.toString();
//	}
	
}
