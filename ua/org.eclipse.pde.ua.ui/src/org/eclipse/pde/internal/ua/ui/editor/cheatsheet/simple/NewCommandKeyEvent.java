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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import java.util.EventObject;

/**
 * NewCommandKeyEvent
 *
 */
public class NewCommandKeyEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	private String fKey;

	private String fValue;

	/**
	 * @param source
	 * @param key
	 * @param value
	 */
	public NewCommandKeyEvent(Object source, String key, String value) {
		super(source);
		fKey = key;
		fValue = value;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return fKey;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return fValue;
	}

}
