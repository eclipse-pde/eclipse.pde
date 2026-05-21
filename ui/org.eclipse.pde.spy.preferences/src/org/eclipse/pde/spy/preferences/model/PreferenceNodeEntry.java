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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;

public class PreferenceNodeEntry extends PreferenceEntry {

	private final IObservableSet<PreferenceEntry> preferenceEntries = new WritableSet<>();

	public PreferenceNodeEntry() {
		super();
	}

	public PreferenceNodeEntry(String nodePath) {
		super(nodePath, "", "", "");
	}

	public void addChildren(Collection<PreferenceEntry> entries) {
		preferenceEntries.addAll(entries);
	}

	public boolean addChildren(PreferenceEntry... entry) {
		return preferenceEntries.addAll(Arrays.asList(entry));
	}

	public void removeChildren(Collection<PreferenceEntry> entries) {
		preferenceEntries.removeAll(entries);
	}

	public void removeChildren(PreferenceEntry... entry) {
		preferenceEntries.removeAll(Arrays.asList(entry));
	}

	public IObservableSet<PreferenceEntry> getPreferenceEntries() {
		return preferenceEntries;
	}

}
