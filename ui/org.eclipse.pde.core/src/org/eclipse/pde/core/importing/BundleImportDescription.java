/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.importing;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes how a bundle import will be executed. A bundle importer delegate
 * creates bundle import descriptions when it validates bundle manifests for
 * importing. When asked to import bundles, it is passed back the instances
 * of bundle import descriptions is created. However, the target project
 * may have been modified and properties may have been modified.
 * <p>
 * Clients may instantiate this class. Clients may subclass this class to
 * implement model specific behavior and include model specific data in import
 * descriptions.
 * </p> 
 * @since 3.6
 */
public class BundleImportDescription {

	private String project;
	private Map manifest;
	private Map properties;

	/**
	 * Constructs a bundle import description with given project and manifest.
	 * 
	 * @param project the project the bundle should be imported into which may
	 *  or may not exist
	 * @param manifest bundle manifest headers and values
	 */
	public BundleImportDescription(String project, Map manifest) {
		this.project = project;
		this.manifest = manifest;
	}

	/**
	 * Sets or removes a client property.
	 * 
	 * @param key property key
	 * @param value property value or <code>null</code> to remove the property
	 */
	public synchronized void setProperty(String key, Object value) {
		if (properties == null) {
			properties = new HashMap();
		}
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}

	}

	/**
	 * Returns the specified client property, or <code>null</code> if none.
	 * 
	 * @param key property key
	 * @return property value or <code>null</code>
	 */
	public synchronized Object getProperty(String key) {
		if (properties == null) {
			return null;
		}
		return properties.get(key);
	}

	/**
	 * Returns the project name the bundle will be imported into. The project
	 * may or may not exist before the import. However, when the import operation
	 * beings, the project will not exist.
	 * 
	 * @return target project
	 */
	public synchronized String getProject() {
		return project;
	}

	/**
	 * Returns the manifest of the bundle to be imported.
	 * 
	 * @return bundle manifest keys and values
	 */
	public Map getManifest() {
		return manifest;
	}

	/**
	 * Sets the project name that is the target of the import operation.
	 * 
	 * @param project target project
	 */
	public synchronized void setProject(String project) {
		this.project = project;
	}

}
