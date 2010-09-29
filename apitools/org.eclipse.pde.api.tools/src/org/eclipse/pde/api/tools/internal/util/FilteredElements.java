/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FilteredElements {

	private Set exactMatches;
	private Set partialMatches;

	public Set getExactMatches() {
		if (this.exactMatches == null) {
			return Collections.EMPTY_SET;
		}
		return this.exactMatches;
	}

	public Set getPartialMatches() {
		if (this.partialMatches == null) {
			return Collections.EMPTY_SET;
		}
		return this.partialMatches;
	}

	public boolean containsPartialMatch(String componentId) {
		if (this.partialMatches == null) return false;
		if (this.partialMatches.contains(componentId)) {
			return true;
		}
		for (Iterator iterator = this.partialMatches.iterator(); iterator.hasNext(); ) {
			String partialMatch = (String) iterator.next();
			if (componentId.startsWith(partialMatch)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsExactMatch(String key) {
		if (this.exactMatches == null) return false;
		return this.exactMatches.contains(key);
	}

	public void addPartialMatch(String componentid) {
		if (this.partialMatches == null) {
			this.partialMatches = new HashSet();
		}
		this.partialMatches.add(componentid);
	}

	public void addExactMatch(String match) {
		if (this.exactMatches == null) {
			this.exactMatches = new HashSet();
		}
		this.exactMatches.add(match);
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		printSet(buffer, this.exactMatches, "exact matches"); //$NON-NLS-1$
		printSet(buffer, this.partialMatches, "partial matches"); //$NON-NLS-1$
		return String.valueOf(buffer);
	}
	
	private void printSet(StringBuffer buffer, Set set, String title) {
		final String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer
			.append(title)
			.append(lineSeparator)
			.append("================================================================") //$NON-NLS-1$
			.append(lineSeparator);
		if (set != null) {
			final int max = set.size();
			String[] allEntries = new String[max];
			set.toArray(allEntries);
			Arrays.sort(allEntries);
			for (int i = 0; i < max; i++) {
				buffer.append(allEntries[i]).append(lineSeparator);
			}
			buffer.append(lineSeparator);
		}
		buffer
			.append("================================================================") //$NON-NLS-1$
			.append(lineSeparator);
	}
	
	public boolean isEmpty(){
		return (exactMatches == null || exactMatches.isEmpty()) && (partialMatches == null || partialMatches.isEmpty());
	}
}
