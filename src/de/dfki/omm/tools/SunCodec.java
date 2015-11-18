package de.dfki.omm.tools;

import java.io.IOException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.misc.UUDecoder;
import sun.misc.UUEncoder;
import de.dfki.omm.interfaces.BinaryCodec;

/** Implementation of {@link BinaryCodec} that uses Sun BASE64 and UU codification. */
public class SunCodec implements BinaryCodec
{
	public static final String BASE64_TYPE = "base64";
	public static final String UUENCODE_TYPE = "base64"; // TODO actually do use UU codification?
	
	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.BinaryCodec#decodePayload(java.lang.String, java.lang.String)
	 */
	public byte[] decodePayload(String type, String payload)
	{
		if (type.toLowerCase().equals(BASE64_TYPE))
		{
			try 
			{	
				return new BASE64Decoder().decodeBuffer(payload);				
			} 
			catch (IOException e) { e.printStackTrace(); }
		}
		else if (type.toLowerCase().equals(UUENCODE_TYPE))
		{
			try 
			{				
				return new UUDecoder().decodeBuffer(payload);
			} 
			catch (IOException e) { e.printStackTrace(); }
		}
		else
		{
			return payload.getBytes();
		} 
		
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dfki.omm.interfaces.BinaryCodec#encodePayload(java.lang.String, byte[])
	 */
	public String encodePayload(String type, byte[] payload)
	{
		if (type.toLowerCase().equals(BASE64_TYPE))
		{
			return new BASE64Encoder().encode(payload);
		}
		else if (type.toLowerCase().equals(UUENCODE_TYPE))
		{
			return new UUEncoder().encode(payload);
		}
		else
		{
			return new String(payload);
		}
	}
}
