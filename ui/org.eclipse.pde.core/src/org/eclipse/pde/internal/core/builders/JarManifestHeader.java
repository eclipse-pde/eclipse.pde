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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.osgi.framework.BundleException;

public class JarManifestHeader implements IHeader {
	private JarManifestErrorReporter fErrorReporter;

	private int fLineNumber;

	private int fLines;

	private ManifestElement[] fManifestElements;

	private String fName;

	private String fValue;

	/**
	 * 
	 * @param name
	 * @param value
	 * @param lineNumber
	 * @param errorReporter
	 *            JarManinfestErrorReporter or null
	 */
	public JarManifestHeader(String name, String value, int lineNumber, JarManifestErrorReporter errorReporter) {
		fName = name;
		fValue = value;
		fLineNumber = lineNumber;
		fErrorReporter = errorReporter;
		fLines = 1;
	}

	public void append(String value) {
		fValue += value;
		fLines++;
	}

	public ManifestElement[] getElements() {
		if (fManifestElements == null) {
			if (getValue().trim().length() > 0) {
				try {
					fManifestElements = ManifestElement.parseHeader(getName(), getValue());
				} catch (BundleException be) {
					fManifestElements = new ManifestElement[0];
					if (fErrorReporter != null) {
						String message = NLS.bind(PDECoreMessages.BundleErrorReporter_parseHeader, getName());
						fErrorReporter.report(message, getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					}
				}
			} else {
				fManifestElements = new ManifestElement[0];
			}
		}
		return fManifestElements;
	}

	public int getLineNumber() {
		return fLineNumber;
	}

	public int getLinesSpan() {
		return fLines;
	}

	public String getName() {
		return fName;
	}

	public String getValue() {
		return fValue;
	}

	public String toString() {
		return fName + "=" + fValue; //$NON-NLS-1$
	}

}
