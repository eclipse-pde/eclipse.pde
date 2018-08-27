/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 *     Rafael Oliveira N√≥brega <rafael.oliveira@gmail.com> - bug 230232
 *******************************************************************************/

package org.eclipse.pde.internal.ds.core.builders;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.xml.sax.SAXException;

public class DefaultSAXParser {

	public static void parse(IFile file, XMLErrorReporter reporter) {
		SAXParserWrapper parser = null;
		try {
			parser = new SAXParserWrapper();
			try (InputStream stream = new BufferedInputStream(file.getContents())) {
				parser.parse(stream, reporter);
			}
		} catch (CoreException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		}
	}
}
