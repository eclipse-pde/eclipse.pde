package org.eclipse.pde.internal.model.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;

public class JarEntry extends JarsObject implements IJarEntry {
	private Vector folderNames = new Vector();
	private String name;

public JarEntry(String name) {
	this.name = name;
}
public void addFolderName(String folderName) throws CoreException {
	ensureModelEditable();
	folderNames.add(folderName);
	getModel().fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.INSERT,
			new Object[] { folderName },
			null));
}
public String[] getFolderNames() {
	String [] result = new String [folderNames.size()];
	folderNames.copyInto(result);
	return result;
}
public String getName() {
	return name;
}
void processEntry(String value) {
	StringTokenizer stok = new StringTokenizer(value, ",");
	while (stok.hasMoreTokens()) {
		String token = stok.nextToken();
		token = token.trim();
		folderNames.add(token);
	}
}
public void removeFolderName(String folderName) throws CoreException {
	ensureModelEditable();
	folderNames.remove(folderName);
	getModel().fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.REMOVE,
			new Object[] { folderName },
			null));
}
public void renameFolder(String oldName, String newName) throws CoreException {
	for (int i=0; i<folderNames.size(); i++) {
		if (folderNames.elementAt(i).toString().equals(oldName)) {
			folderNames.setElementAt(newName, i);
			break;
		}
	}
}
public void setName(String name) throws CoreException {
	ensureModelEditable();
	this.name = name;
	getModel().fireModelObjectChanged(this, P_NAME);
}
public String toString() {
	return name;
}
public void write(String indent, PrintWriter writer) {
	writer.print(name + " = ");
	if (folderNames.size()==0) {
		writer.println();
		return;
	}
	int indentLength = name.length() + 3;
	for (int i = 0; i < folderNames.size(); i++) {
		writer.print(folderNames.elementAt(i).toString());
		if (i < folderNames.size() - 1) {
			writer.println(",\\");
			for (int j=0; j<indentLength; j++) {
				writer.print(" ");
			}
		}
		else
			writer.println("");
	}
}
}
