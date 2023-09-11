/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.model;

import java.util.HashMap;
import java.util.Map;

public class PreferenceEntryManager extends PreferenceNodeEntry {

	private final Map<String, PreferenceNodeEntry> recentPreferenceEntries = new HashMap<>();

	public PreferenceEntryManager() {
	}

	public PreferenceNodeEntry getRecentPreferenceNodeEntry(String nodePath) {
		return recentPreferenceEntries.get(nodePath);
	}

	public PreferenceNodeEntry removeRecentPreferenceNodeEntry(String nodePath) {
		return recentPreferenceEntries.remove(nodePath);
	}

	public void clearRecentPreferenceNodeEntry() {
		recentPreferenceEntries.clear();
	}

	public void putRecentPreferenceEntry(String nodePath, PreferenceNodeEntry preferenceNodeEntry) {
		recentPreferenceEntries.put(nodePath, preferenceNodeEntry);
	}

}
