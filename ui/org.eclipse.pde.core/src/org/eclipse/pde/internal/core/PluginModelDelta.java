/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;

public class PluginModelDelta {
	public static final int ADDED = 1;
	public static final int REMOVED = 2;
	public static final int CHANGED = 4;

	private ArrayList added;
	private ArrayList removed;
	private ArrayList changed;

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
	private ModelEntry[] getEntries(ArrayList list) {
		if (list == null)
			return new ModelEntry[0];
		return (ModelEntry[]) list.toArray(new ModelEntry[list.size()]);
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
	private ArrayList addEntry(ArrayList list, ModelEntry entry) {
		if (list == null)
			list = new ArrayList();
		list.add(entry);
		return list;
	}
}
