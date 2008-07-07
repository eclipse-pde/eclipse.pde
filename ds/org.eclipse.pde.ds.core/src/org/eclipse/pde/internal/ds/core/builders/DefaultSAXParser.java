/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		InputStream stream = null;
		SAXParserWrapper parser = null;
		try {
			parser = new SAXParserWrapper();
			stream = new BufferedInputStream(file.getContents());
			parser.parse(stream, reporter);
		} catch (CoreException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}
		}
	}
}
