package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.IBuildEntry;

public class BuildEntry extends BuildObject implements IBuildEntry {
	private Vector tokens = new Vector();
	private String name;

public BuildEntry(String name) {
	this.name = name;
}
public void addToken(String token) throws CoreException {
	ensureModelEditable();
	tokens.add(token);
	getModel().fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.INSERT,
			new Object[] { token },
			null));
}
public String getName() {
	return name;
}
public String[] getTokens() {
	String [] result = new String [tokens.size()];
	tokens.copyInto(result);
	return result;
}

public boolean contains(String token) {
	return tokens.contains(token);
}
void processEntry(String value) {
	StringTokenizer stok = new StringTokenizer(value, ",");
	while (stok.hasMoreTokens()) {
		String token = stok.nextToken();
		token = token.trim();
		tokens.add(token);
	}
}
public void removeToken(String token) throws CoreException {
	ensureModelEditable();
	tokens.remove(token);
	getModel().fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.REMOVE,
			new Object[] { token },
			null));
}
public void renameToken(String oldName, String newName) throws CoreException {
	ensureModelEditable();
	for (int i=0; i<tokens.size(); i++) {
		if (tokens.elementAt(i).toString().equals(oldName)) {
			tokens.setElementAt(newName, i);
			break;
		}
	}
	getModel().fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.CHANGE,
			new Object[] { oldName },
			null));
}
public void setName(String name) throws CoreException {
	ensureModelEditable();
	String oldValue = this.name;
	this.name = name;
	getModel().fireModelObjectChanged(this, P_NAME, oldValue, name);
}
public String toString() {
	return name;
}

private String createWritableName(String source) {
	if (source.indexOf(' ')== -1) return source;
	// has blanks 
	StringBuffer writableName = new StringBuffer();
	for (int i=0; i<source.length(); i++) {
		char c = source.charAt(i);
		if (c== ' ') {
			writableName.append("\\ ");
		}
		else writableName.append(c);
	}
	return writableName.toString();
}

public void write(String indent, PrintWriter writer) {
	String writableName = createWritableName(name);
	writer.print(writableName + " = ");
	if (tokens.size()==0) {
		writer.println();
		return;
	}
	int indentLength = name.length() + 3;
	for (int i = 0; i < tokens.size(); i++) {
		writer.print(tokens.elementAt(i).toString());
		if (i < tokens.size() - 1) {
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
