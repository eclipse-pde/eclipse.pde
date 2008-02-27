/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.api.tools.internal.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SourceDefaultHandler extends DefaultHandler {
	private static final String ORG_ECLIPSE_PDE_CORE_SOURCE_EXTENSION_POINT_NAME = "org.eclipse.pde.core.source"; //$NON-NLS-1$
	private static final String EXTENSION_NAME = "extension"; //$NON-NLS-1$
	private static final String ECLIPSE_POINT_ATTRIBUTE_NAME = "point"; //$NON-NLS-1$
	boolean isSource = false;
	public void error(SAXParseException e) throws SAXException {
		e.printStackTrace();
	}
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		if (this.isSource) return;
		this.isSource = EXTENSION_NAME.equals(name)
				&& attributes.getLength() == 1
				&& (ECLIPSE_POINT_ATTRIBUTE_NAME.equals(attributes.getQName(0))
						|| ECLIPSE_POINT_ATTRIBUTE_NAME.equals(attributes.getLocalName(0)))
				&& ORG_ECLIPSE_PDE_CORE_SOURCE_EXTENSION_POINT_NAME.equals(attributes.getValue(0));
	}
	public boolean isSource() {
		return this.isSource;
	}
}