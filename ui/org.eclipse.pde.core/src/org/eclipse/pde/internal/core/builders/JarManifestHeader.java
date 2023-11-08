/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.osgi.framework.BundleException;

public class JarManifestHeader implements IHeader {
	private final JarManifestErrorReporter fErrorReporter;

	private final int fLineNumber;

	private int fLines;

	private ManifestElement[] fManifestElements;

	private final String fName;

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

	@Override
	public ManifestElement[] getElements() {
		if (fManifestElements == null) {
			if (getValue().trim().length() > 0) {
				try {
					fManifestElements = ManifestElement.parseHeader(getName(), getValue());
				} catch (BundleException be) {
					fManifestElements = new ManifestElement[0];
					if (fErrorReporter != null) {
						String message = NLS.bind(PDECoreMessages.BundleErrorReporter_parseHeader, getName());
						fErrorReporter.report(message, getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					}
				}
			} else {
				fManifestElements = new ManifestElement[0];
			}
		}
		return fManifestElements;
	}

	@Override
	public int getLineNumber() {
		return fLineNumber;
	}

	@Override
	public int getLinesSpan() {
		return fLines;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getValue() {
		return fValue;
	}

	@Override
	public String toString() {
		return fName + "=" + fValue; //$NON-NLS-1$
	}

}
