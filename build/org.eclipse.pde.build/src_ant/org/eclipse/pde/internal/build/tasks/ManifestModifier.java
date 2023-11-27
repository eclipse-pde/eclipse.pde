/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
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
	private final Map<String, String> newValues = new HashMap<>();
	private static String DELIM = "#|"; //$NON-NLS-1$
	private Manifest manifest = null;
	private boolean contentChanged = false;

	/**
	 * Indicate new values to add to the manifest. The format of the parameter is key|value#key|value#...
	 * If a value is specified to null, the key will be removed from the manifest. 
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

	@Override
	public void execute() {
		loadManifest();

		applyChanges();

		writeManifest();
	}

	private void writeManifest() {
		if (!contentChanged)
			return;

		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(manifestLocation))) {
			manifest.write(os);
			//work around bug 256787
			os.write(new byte[] {'\n'});
		} catch (IOException e1) {
			throw new BuildException("Problem writing the content of the manifest : " + manifestLocation); //$NON-NLS-1$
		}
	}

	private void applyChanges() {
		for (Entry<String, String> entry : newValues.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
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
		try (InputStream is = new SequenceInputStream(new BufferedInputStream(new FileInputStream(manifestLocation)), new ByteArrayInputStream(new byte[] {'\n'}))) {
			//work around for bug 256787 
			manifest = new Manifest(is);
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

	public void setManifestLocation(String path) {
		manifestLocation = path;
	}
}
