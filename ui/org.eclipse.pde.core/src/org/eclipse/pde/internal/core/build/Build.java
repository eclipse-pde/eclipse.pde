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
package org.eclipse.pde.internal.core.build;

import java.io.PrintWriter;
import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;

public class Build extends BuildObject implements IBuild {
	protected ArrayList<IBuildEntry> fEntries = new ArrayList<>();

	@Override
	public void add(IBuildEntry entry) throws CoreException {
		ensureModelEditable();
		fEntries.add(entry);
		((BuildEntry) entry).setInTheModel(true);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.INSERT, new Object[] {entry}, null));
	}

	@Override
	public IBuildEntry[] getBuildEntries() {
		return fEntries.toArray(new IBuildEntry[fEntries.size()]);
	}

	@Override
	public IBuildEntry getEntry(String name) {
		for (int i = 0; i < fEntries.size(); i++) {
			IBuildEntry entry = fEntries.get(i);
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	public void processEntry(String name, String value) {
		BuildEntry entry = (BuildEntry) getModel().getFactory().createEntry(name);
		fEntries.add(entry);
		entry.processEntry(value);
	}

	@Override
	public void remove(IBuildEntry entry) throws CoreException {
		ensureModelEditable();
		fEntries.remove(entry);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.REMOVE, new Object[] {entry}, null));
	}

	public void reset() {
		fEntries.clear();
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		for (int i = 0; i < fEntries.size(); i++) {
			IBuildEntry entry = fEntries.get(i);
			entry.write("", writer); //$NON-NLS-1$
		}
	}
}
