/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;

public class Build extends BuildObject implements IBuild {
	protected Vector entries = new Vector();

public Build() {
}
public void add(IBuildEntry entry) throws CoreException {
	ensureModelEditable();
	entries.add(entry);
	((BuildEntry)entry).setInTheModel(true);
	getModel().fireModelChanged(
		new ModelChangedEvent(getModel(), IModelChangedEvent.INSERT, new Object[] { entry }, null));
}
public IBuildEntry[] getBuildEntries() {
	IBuildEntry [] result = new IBuildEntry[entries.size()];
	entries.copyInto(result);
	return result;
}
public IBuildEntry getEntry(String name) {
	for (int i=0; i<entries.size(); i++) {
		IBuildEntry entry = (IBuildEntry)entries.elementAt(i);
		if (entry.getName().equals(name)) return entry;
	}
	return null;
}
public void processEntry(String name, String value) {
	BuildEntry entry = (BuildEntry)getModel().getFactory().createEntry(name);
	entries.add(entry);
	entry.processEntry(value);
}
public void remove(IBuildEntry entry) throws CoreException {
	ensureModelEditable();
	entries.remove(entry);
	getModel().fireModelChanged(
		new ModelChangedEvent(getModel(), IModelChangedEvent.REMOVE, new Object[] { entry }, null));
}
public void reset() {
	entries.clear();
}
public void write(String indent, PrintWriter writer) {
	for (int i=0; i<entries.size(); i++) {
		IBuildEntry entry = (IBuildEntry)entries.elementAt(i);
		entry.write("", writer); //$NON-NLS-1$
	}
}
}
