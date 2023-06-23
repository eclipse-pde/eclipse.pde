/*******************************************************************************
 *  Copyright (c) 2023 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlDocumentBuilderFactory {
	private XmlDocumentBuilderFactory() {
		// static Utility only
	}

	public static DocumentBuilderFactory createDocumentBuilderFactoryWithErrorOnDOCTYPE() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// completely disable DOCTYPE declaration:
		try {
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return factory;
	}


}
