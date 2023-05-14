/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;

public class NLResourceHelper {
	public static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	public static final String KEY_DOUBLE_PREFIX = "%%"; //$NON-NLS-1$
	private PropertyResourceBundle bundle = null;
	private String fNLFileBasePath;

	public NLResourceHelper(String name, URL[] locations) {
		try (URLClassLoader resourceLoader = new URLClassLoader(locations, null)) {
			try (InputStream stream = getResourceStream(resourceLoader, name);) {
				if (stream != null) {
					bundle = new PropertyResourceBundle(stream);
				}
			}
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public void dispose() {
		bundle = null;
	}

	private InputStream getResourceStream(URLClassLoader resourceLoader, String name) {
		StringTokenizer tokenizer = new StringTokenizer(Platform.getNL(), "_"); //$NON-NLS-1$
		String language = tokenizer.nextToken();
		String country = (tokenizer.hasMoreTokens() ? tokenizer.nextToken() : ""); //$NON-NLS-1$
		String variant = (tokenizer.hasMoreTokens() ? tokenizer.nextToken() : ""); //$NON-NLS-1$

		String suffix1 = "_" + language + "_" + country + "_" + variant; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String suffix2 = "_" + language + "_" + country; //$NON-NLS-1$ //$NON-NLS-2$
		String suffix3 = "_" + language; //$NON-NLS-1$
		String suffix4 = ""; //$NON-NLS-1$

		String[] suffices = new String[] {suffix1, suffix2, suffix3, suffix4};

		InputStream stream = null;
		for (String suffix : suffices) {
			String candidateFileName = name + suffix;
			stream = resourceLoader.getResourceAsStream(candidateFileName + ".properties"); //$NON-NLS-1$
			if (stream != null) {
				fNLFileBasePath = candidateFileName;
				break;
			}
		}
		return stream;
	}

	public String getResourceString(String value) {
		String s = value.trim();

		if (!s.startsWith(KEY_PREFIX)) {
			return s;
		}

		if (s.startsWith(KEY_DOUBLE_PREFIX)) {
			return s.substring(1);
		}

		int ix = s.indexOf(' ');
		String key = ix == -1 ? s : s.substring(0, ix);
		String dflt = ix == -1 ? s : s.substring(ix + 1);

		if (bundle == null) {
			return dflt;
		}

		try {
			return bundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}

	public boolean resourceExists(String value) {
		if (bundle == null) {
			return false;
		}
		try {
			bundle.getString(value.trim().substring(1));
			return true;
		} catch (MissingResourceException e) {
			return false;
		}
	}

	public String getNLFileBasePath() {
		return fNLFileBasePath;
	}

}
