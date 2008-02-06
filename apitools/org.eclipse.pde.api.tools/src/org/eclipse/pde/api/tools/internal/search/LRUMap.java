/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
/**
 * LRU cache 
 */
class LRUMap extends LinkedHashMap {
	private static final long serialVersionUID= 1L;
	private int fMaxSize;
	LRUMap(int maxSize) {
		super();
		fMaxSize = maxSize;
	}
	protected boolean removeEldestEntry(Entry eldest) {
		return size() > fMaxSize;
	}	
}