/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Bundle;

public abstract class TextUtil {

	private static URL fJavaDocStyleSheet = null;

	public static String createMultiLine(String text, int limit) {
		return createMultiLine(text, limit, false);
	}

	public static String createMultiLine(String text, int limit, boolean ignoreNewLine) {
		StringBuffer buffer = new StringBuffer();
		int counter = 0;
		boolean preformatted = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			counter++;
			if (c == '<') {
				if (isPreStart(text, i)) {
					preformatted = true;
				} else if (isPreEnd(text, i)) {
					preformatted = false;
				} else if (isParagraph(text, i)) {
					buffer.append("\n<p>\n"); //$NON-NLS-1$
					counter = 0;
					i += 2;
					continue;
				}
			}
			if (preformatted) {
				if (c == '\n')
					counter = 0;
				buffer.append(c);
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (counter == 1) {
					counter = 0;
					continue; // skip
				} else if (counter > limit) {
					buffer.append('\n');
					counter = 0;
					i--;
					continue;
				}
			}
			if (c == '\n') {
				if (ignoreNewLine)
					c = ' ';
				else
					counter = 0;
			}
			buffer.append(c);
		}
		return buffer.toString();
	}

	private static boolean isParagraph(String text, int loc) {
		if (text.charAt(loc) != '<')
			return false;
		if (loc + 2 >= text.length())
			return false;
		if (text.charAt(loc + 1) != 'p')
			return false;
		if (text.charAt(loc + 2) != '>')
			return false;
		return true;
	}

	private static boolean isPreEnd(String text, int loc) {
		if (text.charAt(loc) != '<')
			return false;
		if (loc + 5 >= text.length())
			return false;
		if (text.charAt(loc + 1) != '/')
			return false;
		if (text.charAt(loc + 2) != 'p')
			return false;
		if (text.charAt(loc + 3) != 'r')
			return false;
		if (text.charAt(loc + 4) != 'e')
			return false;
		if (text.charAt(loc + 5) != '>')
			return false;
		return true;
	}

	private static boolean isPreStart(String text, int loc) {
		if (text.charAt(loc) != '<')
			return false;
		if (loc + 4 >= text.length())
			return false;
		if (text.charAt(loc + 1) != 'p')
			return false;
		if (text.charAt(loc + 2) != 'r')
			return false;
		if (text.charAt(loc + 3) != 'e')
			return false;
		if (text.charAt(loc + 4) != '>')
			return false;
		return true;
	}

	public static URL getJavaDocStyleSheerURL() {
		if (fJavaDocStyleSheet == null) {
			Bundle bundle = Platform.getBundle(IPDEUIConstants.PLUGIN_ID);
			fJavaDocStyleSheet = bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
			if (fJavaDocStyleSheet != null) {
				try {
					fJavaDocStyleSheet = FileLocator.toFileURL(fJavaDocStyleSheet);
				} catch (IOException ex) {
					PDEPlugin.log(ex);
				}
			}
		}
		return fJavaDocStyleSheet;
	}

	public static String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}

}
