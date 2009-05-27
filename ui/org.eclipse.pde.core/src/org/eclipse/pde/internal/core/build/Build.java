/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
	protected ArrayList fEntries = new ArrayList();

	public void add(IBuildEntry entry) throws CoreException {
		ensureModelEditable();
		fEntries.add(entry);
		((BuildEntry) entry).setInTheModel(true);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.INSERT, new Object[] {entry}, null));
	}

	public IBuildEntry[] getBuildEntries() {
		return (IBuildEntry[]) fEntries.toArray(new IBuildEntry[fEntries.size()]);
	}

	public IBuildEntry getEntry(String name) {
		for (int i = 0; i < fEntries.size(); i++) {
			IBuildEntry entry = (IBuildEntry) fEntries.get(i);
			if (entry.getName().equals(name))
				return entry;
		}
		return null;
	}

	public void processEntry(String name, String value) {
		BuildEntry entry = (BuildEntry) getModel().getFactory().createEntry(name);
		fEntries.add(entry);
		entry.processEntry(value);
	}

	public void remove(IBuildEntry entry) throws CoreException {
		ensureModelEditable();
		fEntries.remove(entry);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.REMOVE, new Object[] {entry}, null));
	}

	public void reset() {
		fEntries.clear();
	}

	public void write(String indent, PrintWriter writer) {
		for (int i = 0; i < fEntries.size(); i++) {
			IBuildEntry entry = (IBuildEntry) fEntries.get(i);
			entry.write("", writer); //$NON-NLS-1$
		}
	}
}
