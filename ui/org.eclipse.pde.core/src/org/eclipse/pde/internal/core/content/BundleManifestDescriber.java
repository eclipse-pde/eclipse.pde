/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.content;

import java.io.*;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;
import org.osgi.framework.Constants;

public class BundleManifestDescriber implements ITextContentDescriber {

	private final static String[] HEADERS = {Constants.BUNDLE_MANIFESTVERSION, Constants.BUNDLE_NAME, Constants.BUNDLE_VERSION, Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VENDOR, Constants.BUNDLE_ACTIVATOR};
	private final static int LINES = 50;

	private final static QualifiedName[] SUPPORTED_OPTIONS = {IContentDescription.BYTE_ORDER_MARK};

	/* (Intentionally not included in javadoc)
	 * @see IContentDescriber#describe(InputStream, IContentDescription)
	 */
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		byte[] bom = getByteOrderMark(contents);
		contents.reset();
		String charset = "UTF-8"; //$NON-NLS-1$
		if (bom != null) {
			// has a bom
			// remember to skip it
			contents.skip(bom.length);
			// compute a corresponding charset
			if (bom == IContentDescription.BOM_UTF_8)
				charset = "UTF-8"; //$NON-NLS-1$
			else if (bom == IContentDescription.BOM_UTF_16BE || bom == IContentDescription.BOM_UTF_16LE)
				// UTF-16 will properly recognize the BOM
				charset = "UTF-16"; //$NON-NLS-1$
			// fill description if requested
			if (description != null && description.isRequested(IContentDescription.BYTE_ORDER_MARK))
				description.setProperty(IContentDescription.BYTE_ORDER_MARK, bom);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(contents, charset));
		String line;
		for (int i = 0; ((line = reader.readLine()) != null) && i < LINES; i++)
			if (matches(line))
				// found signature
				return VALID;
		// could not find signature
		return INDETERMINATE;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.content.ITextContentDescriber#describe(java.io.Reader, org.eclipse.core.runtime.content.IContentDescription)
	 */
	public int describe(Reader contents, IContentDescription description) throws IOException {
		BufferedReader reader = new BufferedReader(contents);
		String line;
		for (int i = 0; ((line = reader.readLine()) != null) && i < LINES; i++)
			if (matches(line))
				return VALID;
		return INDETERMINATE;
	}

	byte[] getByteOrderMark(InputStream input) throws IOException {
		int first = (input.read() & 0xFF);//converts unsigned byte to int
		int second = (input.read() & 0xFF);
		if (first == -1 || second == -1)
			return null;
		//look for the UTF-16 Byte Order Mark (BOM)
		if (first == 0xFE && second == 0xFF)
			return IContentDescription.BOM_UTF_16BE;
		if (first == 0xFF && second == 0xFE)
			return IContentDescription.BOM_UTF_16LE;
		int third = (input.read() & 0xFF);
		if (third == -1)
			return null;
		//look for the UTF-8 BOM
		if (first == 0xEF && second == 0xBB && third == 0xBF)
			return IContentDescription.BOM_UTF_8;
		return null;
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.core.runtime.content.IContentDescriber#getSupportedOptions()
	 */
	public QualifiedName[] getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	private boolean matches(String line) {
		for (int i = 0; i < HEADERS.length; i++) {
			int length = HEADERS[i].length();
			if (line.length() >= length)
				if (line.substring(0, length).equalsIgnoreCase(HEADERS[i]))
					return true;
		}
		return false;
	}
}
