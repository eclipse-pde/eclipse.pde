/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.standalone.parser;

import org.apache.xerces.impl.*;
import org.apache.xerces.xni.*;



public class XEErrorReporter extends XMLErrorReporter {

	/**
	 * Constructor for XEErrorReporter.
	 */
	public XEErrorReporter() {
		super();
	}

	/*
	 * @see org.apache.xerces.impl.XMLErrorReporter#reportError(org.apache.xerces.xni.XMLLocator, java.lang.String, java.lang.String, java.lang.Object, short)
	 */
	public void reportError(XMLLocator location, String domain, String key, Object[] arguments, short severity) throws XNIException {
		super.reportError(location, domain, key == null ? "BadMessageKey" : key, arguments, severity);
	}
}
