/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.apache.xerces.impl.XMLErrorReporter;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;



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
