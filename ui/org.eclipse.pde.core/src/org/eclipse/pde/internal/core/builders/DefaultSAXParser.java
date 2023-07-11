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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXException;

public class DefaultSAXParser {

	public static void parse(IFile file, XMLErrorReporter reporter) {
		try (InputStream stream = new BufferedInputStream(file.getContents())) {
			@SuppressWarnings("restriction")
			SAXParser parser = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createSAXParserWithErrorOnDOCTYPE();
			parser.parse(stream, reporter);
		} catch (CoreException | SAXException | IOException | ParserConfigurationException e) {
		}
	}

}
