package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;

public class Build extends BuildObject implements IBuild {
	protected Vector entries = new Vector();

public Build() {
}
public void add(IBuildEntry entry) throws CoreException {
	ensureModelEditable();
	entries.add(entry);
	getModel().fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { entry }, null));
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
		new ModelChangedEvent(IModelChangedEvent.REMOVE, new Object[] { entry }, null));
}
public void reset() {
	entries.clear();
}
public void write(String indent, PrintWriter writer) {
	for (int i=0; i<entries.size(); i++) {
		IBuildEntry entry = (IBuildEntry)entries.elementAt(i);
		entry.write("", writer);
	}
}
}
