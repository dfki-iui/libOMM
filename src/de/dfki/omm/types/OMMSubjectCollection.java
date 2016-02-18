package de.dfki.omm.types;

import java.io.Serializable;
import java.util.LinkedList;

/** A List of {@link OMMSubjectTag}s to represent an OMM block's collected subjects. */
public class OMMSubjectCollection extends LinkedList<OMMSubjectTag> implements Serializable
{
	private static final long serialVersionUID = -9223278529589404902L;
}
