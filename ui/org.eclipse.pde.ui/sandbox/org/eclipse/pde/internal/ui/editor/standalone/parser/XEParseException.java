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

import org.apache.xerces.xni.*;
import org.apache.xerces.xni.parser.*;


public class XEParseException extends XMLParseException {
	
	private String fKey;

	/**
	 * Constructor for XEParseException.
	 * @param locator
	 * @param message
	 */
	public XEParseException(XMLLocator locator, String message, String key) {
		super(locator, message);
		fKey= key;
	}

	/**
	 * Constructor for XEParseException.
	 * @param locator
	 * @param message
	 * @param exception
	 */
	public XEParseException(XMLLocator locator, String message, Exception exception, String key) {
		super(locator, message, exception);
		fKey= key;
	}

	/**
	 * Returns the fKey.
	 * @return String
	 */
	public String getKey() {
		return fKey;
	}
}
