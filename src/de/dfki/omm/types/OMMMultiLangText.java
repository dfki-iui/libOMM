package de.dfki.omm.types;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;

/** A map of {@link Locale}s and corresponding Strings (of the specified language) for multi language support. */
public class OMMMultiLangText extends HashMap<Locale, String> implements Serializable
{
	private static final long serialVersionUID = -1901194773008779311L;
}
