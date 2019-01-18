/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import org.eclipse.pde.core.plugin.ModelEntry;

public class PluginModelDelta {
	public static final int ADDED = 1;
	public static final int REMOVED = 2;
	public static final int CHANGED = 4;

	private ArrayList<ModelEntry> added;
	private ArrayList<ModelEntry> removed;
	private ArrayList<ModelEntry> changed;

	private int kind = 0;

	public PluginModelDelta() {
	}

	public int getKind() {
		return kind;
	}

	public ModelEntry[] getAddedEntries() {
		return getEntries(added);
	}

	public ModelEntry[] getRemovedEntries() {
		return getEntries(removed);
	}

	public ModelEntry[] getChangedEntries() {
		return getEntries(changed);
	}

	private ModelEntry[] getEntries(ArrayList<ModelEntry> list) {
		if (list == null) {
			return new ModelEntry[0];
		}
		return list.toArray(new ModelEntry[list.size()]);
	}

	void addEntry(ModelEntry entry, int type) {
		switch (type) {
			case ADDED :
				added = addEntry(added, entry);
				break;
			case REMOVED :
				removed = addEntry(removed, entry);
				break;
			case CHANGED :
				changed = addEntry(changed, entry);
				break;
		}
		kind |= type;
	}

	private ArrayList<ModelEntry> addEntry(ArrayList<ModelEntry> list, ModelEntry entry) {
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(entry);
		return list;
	}
}
