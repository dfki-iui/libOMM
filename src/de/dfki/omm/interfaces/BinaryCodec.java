package de.dfki.omm.interfaces;

/** Binary en- and decryption of a payload. */
public interface BinaryCodec
{
	/** Decodes a payload.
	 * 
	 * @param type Type of encryption, as specified in the implementing class.
	 * @param payload Payload to decode as String.
	 * @return Decoded payload as {@link byte[]}. 
	 */
	byte[] decodePayload(String type, String payload);	
	
	/** Encodes a payload.
	 * 
	 * @param type Type of encryption, as specified in the implementing class.
	 * @param payload Payload to encode as {@link byte[]}.
	 * @return Encoded payload as String. 
	 */
	String encodePayload(String type, byte[] payload);	
}
