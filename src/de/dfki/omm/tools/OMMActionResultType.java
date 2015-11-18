package de.dfki.omm.tools;

/** Enumerator describing various possible results of OMM actions, such as missing verification or addressing errors. 
 */
public enum OMMActionResultType 
{
	OK, UnknownError,
	BlockNotExistent, BlockWithSameIDExists, Forbidden
}
