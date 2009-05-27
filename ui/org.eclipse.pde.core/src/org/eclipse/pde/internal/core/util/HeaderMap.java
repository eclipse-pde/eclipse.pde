/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * 
 * Designed as a Map to contain Manifest headers and values.  Returns 
 * the value of a header if it matches (case insensitive) to the key.  
 *
 */
public class HeaderMap extends TreeMap {

	private static final long serialVersionUID = 1L;

	private static class HeaderComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			String header0 = (String) arg0;
			String header1 = (String) arg1;
			return header0.compareToIgnoreCase(header1);
		}
	}

	public HeaderMap() {
		super(new HeaderComparator());
	}

}
