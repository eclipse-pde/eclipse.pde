/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import javax.xml.parsers.FactoryConfigurationError;

/**
 * PDEXMLHelper
 *
 */
public class PDEXMLHelper {

	protected static PDEXMLHelper fPinstance;
	protected static int fSAXPoolLimit;
	protected static int fDOMPoolLimit;
	protected static final int FMAXPOOLLIMIT = 1;

	protected PDEXMLHelper() throws FactoryConfigurationError {
		fSAXPoolLimit = FMAXPOOLLIMIT;
		fDOMPoolLimit = FMAXPOOLLIMIT;
	}

	public static PDEXMLHelper Instance() throws FactoryConfigurationError {
		if (fPinstance == null) {
			fPinstance = new PDEXMLHelper();
		}
		return fPinstance;
	}

	public static String getWritableString(String source) {
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	public static String getWritableAttributeString(String source) {
		// Ensure source is defined
		if (source == null) {
			return ""; //$NON-NLS-1$
		}
		// Trim the leading and trailing whitespace if any
		source = source.trim();
		// Translate source using a buffer
		StringBuilder buffer = new StringBuilder();
		// Translate source character by character
		for (int i = 0; i < source.length(); i++) {
			char character = source.charAt(i);
			switch (character) {
				case '&' :
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buffer.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buffer.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buffer.append("&quot;"); //$NON-NLS-1$
					break;
				case '\r' :
					buffer.append("&#x0D;"); //$NON-NLS-1$
					break;
				case '\n' :
					buffer.append("&#x0A;"); //$NON-NLS-1$
					break;
				default :
					buffer.append(character);
					break;
			}
		}
		return buffer.toString();
	}

	public static int getSAXPoolLimit() {
		return fSAXPoolLimit;
	}

	public static void setSAXPoolLimit(int poolLimit) {
		fSAXPoolLimit = poolLimit;
	}

	public static int getDOMPoolLimit() {
		return fDOMPoolLimit;
	}

	public static void setDOMPoolLimit(int poolLimit) {
		fDOMPoolLimit = poolLimit;
	}

}
