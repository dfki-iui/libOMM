package de.dfki.omm.types;

import java.io.Serializable;

/** Enumerator listing the known types of previous blocks. */
public enum OMMPreviousBlockType implements Serializable
{
	Previous, Supersedes, Removes 
}
