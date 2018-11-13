/*******************************************************************************
 *  Copyright (c) 2018 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class PDEErrorHandler implements org.xml.sax.ErrorHandler {

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		PDECore.log(exception);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		PDECore.log(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		PDECore.log(exception);
	}
}
