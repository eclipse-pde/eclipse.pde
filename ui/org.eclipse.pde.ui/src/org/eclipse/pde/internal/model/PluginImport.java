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
	private int match = NONE;
	private boolean reexported = false;
	private String version;

public PluginImport() {
}

public int getMatch() {
	return match;
}

public String getVersion() {
	return version;
}

public boolean isReexported() {
	return reexported;
}

void load(PluginPrerequisiteModel importModel) {
	this.id = importModel.getPlugin();
	this.reexported = importModel.getExport();
	this.version = importModel.getVersion();
	switch (importModel.getMatchByte()) {
		case PluginPrerequisiteModel.PREREQ_MATCH_PERFECT:
			this.match = PERFECT;
			break;
		case PluginPrerequisiteModel.PREREQ_MATCH_EQUIVALENT:
			this.match = EQUIVALENT;
			break;
		case PluginPrerequisiteModel.PREREQ_MATCH_COMPATIBLE:
			this.match = COMPATIBLE;
			break;
		case PluginPrerequisiteModel.PREREQ_MATCH_GREATER_OR_EQUAL:
			this.match = GREATER_OR_EQUAL;
			break;
	}
}

void load(Node node) {
	String id = getNodeAttribute(node, "plugin");
	String export = getNodeAttribute(node, "export");
	String version = getNodeAttribute(node, "version");
	String match = getNodeAttribute(node, "match");
	boolean reexport = export != null && export.toLowerCase().equals("true");
	if (match != null) {
		String lmatch = match.toLowerCase();
		if (lmatch.equals("perfect")) {
			this.match = PERFECT;
		}
		else if (lmatch.equals("equivalent")) {
			this.match = EQUIVALENT;
		}
		else if (lmatch.equals("compatible")) {
			this.match = COMPATIBLE;
		}
		else if (lmatch.equals("greaterOrEqual")) {
			this.match = GREATER_OR_EQUAL;
		}
		else
		    this.match = NONE;
	}
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
	switch (match) {
		case PERFECT:
			writer.print(" match=\"perfect\"");
			break;
		case EQUIVALENT:
			writer.print(" match=\"equivalent\"");
			break;
			
		case COMPATIBLE:
			writer.print(" match=\"compatible\"");
			break;
		case GREATER_OR_EQUAL:
			writer.print(" match=\"greaterOrEqual\"");
			break;
	}
	writer.println("/>");
}
}
