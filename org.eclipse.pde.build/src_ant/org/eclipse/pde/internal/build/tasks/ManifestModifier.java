/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Internal task. 
 * Add, change or remove the keys from a manifest.mf.
 * @since 3.0 
 */
public class ManifestModifier extends Task {
	private String manifestLocation;
	private final Map newValues = new HashMap();
	private static String DELIM = "#|"; //$NON-NLS-1$
	private Manifest manifest = null;
	private boolean contentChanged = false;

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
		if (!contentChanged)
			return;

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(manifestLocation));
			try {
				manifest.write(os);
				//work around bug 256787
				os.write(new byte[] {'\n'});
			} finally {
				os.close();
			}
		} catch (IOException e1) {
			throw new BuildException("Problem writing the content of the manifest : " + manifestLocation); //$NON-NLS-1$
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
		//force a manifest version if none exists
		if (!manifest.getMainAttributes().containsKey(Name.MANIFEST_VERSION)) {
			contentChanged = true;
			manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		}

	}

	private void loadManifest() {
		try {
			//work around for bug 256787 
			InputStream is = new SequenceInputStream(new BufferedInputStream(new FileInputStream(manifestLocation)), new ByteArrayInputStream(new byte[] {'\n'}));
			try {
				manifest = new Manifest(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new BuildException("Problem reading the content of the manifest : " + manifestLocation); //$NON-NLS-1$
		}
	}

	private void changeValue(String key, String value) {
		Attributes attributes = manifest.getMainAttributes();
		if (attributes.containsKey(key) && attributes.getValue(key).equals(value))
			return;
		contentChanged = true;
		attributes.put(new Attributes.Name(key), value);
	}

	private void removeAttribute(String key) {
		contentChanged = true;
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
