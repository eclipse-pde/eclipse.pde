/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 215232, 250334
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleUtil {

	public static String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		String[] result = new String[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			StringBuffer buffer = new StringBuffer();
			buffer.append(locale.toString());
			buffer.append(" - "); //$NON-NLS-1$
			buffer.append(locale.getDisplayName());
			result[i] = buffer.toString();
		}
		return result;
	}

	public static String expandLocaleName(String name) {
		String language = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$

		StringTokenizer tokenizer = new StringTokenizer(name, "_"); //$NON-NLS-1$
		if (tokenizer.hasMoreTokens())
			language = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			country = tokenizer.nextToken();
		if (tokenizer.hasMoreTokens())
			variant = tokenizer.nextToken();

		Locale locale = new Locale(language, country, variant);
		return locale.toString() + " - " + locale.getDisplayName(); //$NON-NLS-1$
	}

	/**
	 * Smartly trims a {@link String} representing a <code>Bundle-Localization</code> key removing trailing locale and/or .properties extension<br>
	 * <em>e.g.</em> <code>"bundle_es.properties  "</code> --> <code>"bundle"</code>
	 * @param localization the {@link String} to trim
	 * @return the trimmed {@link String}
	 */
	public static String trimLocalization(String localization) {
		String sTrim = localization.trim();
		if (sTrim.endsWith(".properties")) //$NON-NLS-1$
			sTrim = sTrim.replaceAll(".properties", ""); //$NON-NLS-1$ //$NON-NLS-2$
		Pattern p = Pattern.compile(".*(_[a-z]{2}(_[A-Z]{2})?)$"); //$NON-NLS-1$
		Matcher m = p.matcher(sTrim);
		if (m.matches())
			sTrim = sTrim.substring(0, m.start(1));
		return sTrim;
	}

}