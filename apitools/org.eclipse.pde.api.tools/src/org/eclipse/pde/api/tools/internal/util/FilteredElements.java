/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FilteredElements {

	private Set<String> exactMatches;
	private Set<String> partialMatches;

	public Set<String> getExactMatches() {
		if (this.exactMatches == null) {
			return Collections.EMPTY_SET;
		}
		return this.exactMatches;
	}

	public Set<String> getPartialMatches() {
		if (this.partialMatches == null) {
			return Collections.EMPTY_SET;
		}
		return this.partialMatches;
	}

	public boolean containsPartialMatch(String componentId) {
		if (partialMatches == null) {
			return false;
		}
		if (partialMatches.contains(componentId)) {
			return true;
		}
		for (String match : partialMatches) {
			if (componentId.startsWith(match)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsExactMatch(String key) {
		if (exactMatches == null) {
			return false;
		}
		return this.exactMatches.contains(key);
	}

	public void addPartialMatch(String componentid) {
		if (this.partialMatches == null) {
			this.partialMatches = new HashSet<>();
		}
		this.partialMatches.add(componentid);
	}

	public void addExactMatch(String match) {
		if (this.exactMatches == null) {
			this.exactMatches = new HashSet<>();
		}
		this.exactMatches.add(match);
	}

	@Override
	public String toString() {
		final String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuilder buffer = new StringBuilder();
		buffer.append("==============================================================================").append(lineSeparator); //$NON-NLS-1$
		printSet(buffer, this.exactMatches, "Exact matches:"); //$NON-NLS-1$
		printSet(buffer, this.partialMatches, "Partial matches:"); //$NON-NLS-1$
		buffer.append("==============================================================================").append(lineSeparator); //$NON-NLS-1$
		return String.valueOf(buffer);
	}

	private void printSet(StringBuilder buffer, Set<String> set, String title) {
		final String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer.append(title).append(lineSeparator);
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
	}

	public boolean isEmpty() {
		return (exactMatches == null || exactMatches.isEmpty()) && (partialMatches == null || partialMatches.isEmpty());
	}
}
