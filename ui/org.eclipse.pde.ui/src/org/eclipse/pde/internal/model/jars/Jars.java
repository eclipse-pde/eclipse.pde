package org.eclipse.pde.internal.model.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.*;

public class Jars extends JarsObject implements IJars {
	protected Vector entries = new Vector();

public Jars() {
}
public void add(IJarEntry entry) throws CoreException {
	ensureModelEditable();
	entries.add(entry);
	getModel().fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.INSERT, new Object[] { entry }, null));
}
public IJarEntry getEntry(String name) {
	for (int i=0; i<entries.size(); i++) {
		IJarEntry entry = (IJarEntry)entries.elementAt(i);
		if (entry.getName().equals(name)) return entry;
	}
	return null;
}
public IJarEntry[] getJarEntries() {
	IJarEntry [] result = new IJarEntry[entries.size()];
	entries.copyInto(result);
	return result;
}
public void processEntry(String name, String value) {
	JarEntry entry = (JarEntry)getModel().getFactory().createEntry(name);
	entries.add(entry);
	entry.processEntry(value);
}
public void remove(IJarEntry entry) throws CoreException {
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
		IJarEntry entry = (IJarEntry)entries.elementAt(i);
		entry.write("", writer);
	}
}
}
