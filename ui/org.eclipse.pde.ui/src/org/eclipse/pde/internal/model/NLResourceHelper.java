package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;
import java.util.*;

public class NLResourceHelper {
	protected ResourceBundle bundle = null; // abc.properties
	private Locale locale = null; // bundle locale
	private String name = null; // abc
	private URL [] locations = null;
	private boolean notFound = false; // marker to prevent unnecessary lookups

	public static final String KEY_PREFIX = "%";
	public static final String KEY_DOUBLE_PREFIX = "%%";

	public NLResourceHelper(String name, URL [] locations) {
		this.name = name;
		this.locations = locations;
	}
	public ResourceBundle getResourceBundle()
		throws MissingResourceException {

		return getResourceBundle(Locale.getDefault());
	}
	public ResourceBundle getResourceBundle(Locale locale)
		throws MissingResourceException {

		// we cache the bundle for a single locale 
		if (bundle != null && this.locale.equals(locale))
			return bundle;

		// check if we already tried and failed
		if (notFound)
			throw new MissingResourceException(
				"resourceNotFound" + name + "_" + locale,
				name + "_" + locale,
				"");

		ClassLoader resourceLoader = new URLClassLoader(locations);
		try {
			this.bundle = ResourceBundle.getBundle(name, locale, resourceLoader);
			this.locale = locale;
			notFound = false;
		} catch (MissingResourceException e) {
			notFound = true;
			this.bundle = null;
			this.locale = null;
			throw e;
		}
		return bundle;
	}

	public void dispose() {
	}

	public String getResourceString(String value) {
		ResourceBundle b = null;
		try {
			b = getResourceBundle();
		} catch (MissingResourceException e) {
		};
		return getResourceString(value, b);
	}
	public String getResourceString(String value, ResourceBundle b) {

		String s = value.trim();

		if (!s.startsWith(KEY_PREFIX))
			return s;

		if (s.startsWith(KEY_DOUBLE_PREFIX))
			return s.substring(1);

		int ix = s.indexOf(" ");
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (b == null)
			return dflt;

		try {
			return b.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}
}