/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/

package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;

import org.eclipse.pde.internal.build.ant.AntScript;

/**
 * Utility class to convert buildmanifest.properties files into fetch.xml files.
 */
public class BuildManifestToFetchConverter extends AbstractApplication {

	protected String manifestLocation;
	protected String fetchXMLLocation;
	protected String fetchBatchFileLocation;

public static void main(String[] args) throws Exception {
	BuildManifestToFetchConverter converter = new BuildManifestToFetchConverter();
	converter.run(args);
}

public void run() {
	generateXML();
	generateBatchFile();
}

protected void generateXML() {
	try {
		Properties manifest = readManifest();
		AntScript script = new AntScript(new FileOutputStream(fetchXMLLocation));
		int n = "pluging@".length() - 1;
		try {
			script.printProjectDeclaration("fetch", "fetch", ".");
			int tab = 1;
			script.printTargetDeclaration(tab, "fetch", null, null, null, null);
			String cvsroot = ":pserver:anonymous@dev.eclipse.org:/home/eclipse";
			String dest = "plugins/";
			for (Iterator iterator = manifest.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String key = (String) entry.getKey();
				if (!key.startsWith("plugin@"))
					continue;
				String tag = (String) entry.getValue();
				String module = key.substring(n);
				script.printCVSTask(tab, null, cvsroot, dest, module, tag, "quiet", null);
			}
			script.printEndTag(tab, "target");
			script.printEndTag(tab, "project");
		} finally {
			script.close();
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
}

protected void generateBatchFile() {
	try {
		Properties manifest = readManifest();
		FileOutputStream output = new FileOutputStream(fetchBatchFileLocation);
		PrintWriter writer = new PrintWriter(output);
		int n = "pluging@".length() - 1;
		try {
			String cvsroot = ":pserver:anonymous@dev.eclipse.org:/home/eclipse";
			for (Iterator iterator = manifest.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String key = (String) entry.getKey();
				if (!key.startsWith("plugin@"))
					continue;
				String tag = (String) entry.getValue();
				String module = key.substring(n);
				writer.print("cvs -d ");
				writer.print(cvsroot);
				writer.print(" checkout -r ");
				writer.print(tag);
				writer.print(" ");
				writer.println(module);
			}
		} finally {
			writer.flush();
			writer.close();
			output.close();
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
}

protected Properties readManifest() throws IOException {
	Properties result = new Properties();
	File file = new File(manifestLocation);
	InputStream is = new FileInputStream(file);
	try {
		result.load(is);
	} finally {
		is.close();
	}
	return result;
}

protected void processCommandLine(List commands) {
	// looks for flag-like commands
//	if (commands.remove(ARG_USAGE)) 
//		usage = true;

	// looks for param/arg-like commands
	String[] arguments = getArguments(commands, "-manifest");
	this.manifestLocation = arguments[0]; // only consider one location
	arguments = getArguments(commands, "-fetch");
	this.fetchXMLLocation = arguments[0] + ".xml"; // only consider one location
	this.fetchBatchFileLocation = arguments[0] + ".sh"; // only consider one location
}
}