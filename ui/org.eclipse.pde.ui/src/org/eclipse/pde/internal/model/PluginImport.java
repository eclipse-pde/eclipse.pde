package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.PluginPrerequisiteModel;
import org.w3c.dom.Node;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;

public class PluginImport extends IdentifiablePluginObject implements IPluginImport {
	private int match = COMPATIBLE;
	private boolean reexported = false;
	private String version;

public PluginImport() {
}
public int getMatch() {
	return match;
}
public java.lang.String getVersion() {
	return null;
}
public boolean isReexported() {
	return false;
}
void load(PluginPrerequisiteModel importModel) {
	this.id = importModel.getPlugin();
	this.reexported = importModel.getExport();
	this.version = importModel.getVersion();
	if (importModel.getMatchByte()==importModel.PREREQ_MATCH_EQUIVALENT)
		this.match = EXACT;
}
void load(Node node) {
	String id = getNodeAttribute(node, "plugin");
	String export = getNodeAttribute(node, "export");
	String version = getNodeAttribute(node, "version");
	String match = getNodeAttribute(node, "match");
	boolean reexport = export != null && export.toLowerCase().equals("true");
	if (match != null && match.toLowerCase().equals("exact"))
		this.match = EXACT;
	this.version = version;
	this.id = id;
	this.reexported = reexport;
	addComments(node);
}
public void setMatch(int match) throws CoreException {
	ensureModelEditable();
	this.match = match;
	firePropertyChanged(P_MATCH);
}
public void setReexported(boolean value) throws CoreException {
	ensureModelEditable();
	this.reexported = value;
	firePropertyChanged(P_REEXPORTED);
}
public void setVersion(String version) throws CoreException {
	ensureModelEditable();
	this.version = version;
	firePropertyChanged(P_VERSION);
}
public void write(String indent, PrintWriter writer) {
	writeComments(writer);
	writer.print(indent);
	writer.print("<import plugin=\"" + getId() + "\"");
	if (isReexported())
		writer.print(" export=\"true\"");
	if (version != null && version.length() > 0)
		writer.print(" version=\"" + version + "\"");
	if (match == EXACT)
		writer.print(" match=\"exact\"");
	writer.println("/>");
}
}
