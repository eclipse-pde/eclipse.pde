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

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;

public class XmlTransformerFactory {
	private XmlTransformerFactory() {
		// static Utility only
	}

	public static TransformerFactory createTransformerFactoryWithErrorOnDOCTYPE() {
		TransformerFactory factory = TransformerFactory.newInstance();
		// prohibit the use of all protocols by external entities:
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); //$NON-NLS-1$
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); //$NON-NLS-1$
		return factory;
	}


}
