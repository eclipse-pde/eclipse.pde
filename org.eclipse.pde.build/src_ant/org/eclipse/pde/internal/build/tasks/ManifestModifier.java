/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Internal task. 
 * Add, change or remove the keys from a manifest.mf.
 * @since 3.0 
 */
public class ManifestModifier extends Task {
	private String manifestLocation;
	private Map newValues = new HashMap();
	private static String DELIM = "#|"; //$NON-NLS-1$
	private Manifest manifest = null;

	/**
	 * Indicate new values to add to the manifest. The format of the parameter is key|value#key|value#...
	 * If a value is specified to null, the key will be removed from the manifest. 
	 * @param values
	 */
	public void setKeyValue(String values) {
		StringTokenizer tokenizer = new StringTokenizer(values, DELIM, false);
		while (tokenizer.hasMoreElements()) {
			String key = tokenizer.nextToken();
			String value = tokenizer.nextToken();
			if (value.equals("null")) //$NON-NLS-1$
				value = null;
			newValues.put(key, value);
		}
	}

	public void execute() {
		loadManifest();

		applyChanges();

		writeManifest();
	}

	private void writeManifest() {
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(manifestLocation));
			try {
				manifest.write(os);
			} finally {
				os.close();
			}
		} catch (IOException e1) {
			new BuildException("Problem writing the content of the manifest : " + manifestLocation); //$NON-NLS-1$
		}
	}

	private void applyChanges() {
		for (Iterator iter = newValues.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (value == null) {
				removeAttribute(key);
			} else {
				changeValue(key, value);
			}
		}
	}

	private void loadManifest() {
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(manifestLocation));
			try {
				manifest = new Manifest(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			new BuildException("Problem reading the content of the manifest : " + manifestLocation); //$NON-NLS-1$
		}
	}

	private void changeValue(String key, String value) {
		//		log("key : " + key + " becomes " + value, Project.MSG_VERBOSE);
		manifest.getMainAttributes().put(new Attributes.Name(key), value);
	}

	private void removeAttribute(String key) {
		//		log("key : " + key + " removed", Project.MSG_VERBOSE);
		manifest.getMainAttributes().remove(new Attributes.Name(key));
	}

	/**
	 * 
	 * @param path
	 */
	public void setManifestLocation(String path) {
		manifestLocation = path;
	}
}