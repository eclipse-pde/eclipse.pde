/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.*;


public class NLResourceHelper {
	public static final String KEY_PREFIX = "%";
	public static final String KEY_DOUBLE_PREFIX = "%%";
	private PropertyResourceBundle bundle = null;

	public NLResourceHelper(String name, URL[] locations) {
		try {
			InputStream stream = getResourceStream(name, locations);
			if (stream != null) {
				bundle = new PropertyResourceBundle(stream);
				stream.close();
			}
		} catch (IOException e) {
		}
	}
	
	public void dispose() {
		bundle = null;
	}

	private InputStream getResourceStream(String name, URL[] locations) {
		URLClassLoader resourceLoader = new URLClassLoader(locations);
		
		StringTokenizer tokenizer = new StringTokenizer(BootLoader.getNL(), "_");
		String language = tokenizer.nextToken();
		String country = (tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "");
		String variant = (tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "");
		
		String suffix1 = "_" + language + "_" + country + "_" + variant;
		String suffix2 = "_" + language + "_" + country;
		String suffix3 = "_" + language;
		String suffix4 = "";

		String[] suffices = new String[] { suffix1, suffix2, suffix3, suffix4 };

		InputStream stream = null;
		for (int i = 0; i < suffices.length; i++) {
			stream =
				resourceLoader.getResourceAsStream(
					name + suffices[i] + ".properties");
			if (stream != null)
				break;
		}
		return stream;
	}

	public String getResourceString(String value) {
		String s = value.trim();

		if (!s.startsWith(KEY_PREFIX))
			return s;

		if (s.startsWith(KEY_DOUBLE_PREFIX))
			return s.substring(1);

		int ix = s.indexOf(" ");
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (bundle == null)
			return dflt;

		try {
			return bundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}

}
