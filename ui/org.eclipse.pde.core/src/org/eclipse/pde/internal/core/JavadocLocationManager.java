/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class JavadocLocationManager {

	public static final String JAVADOC_ID = "org.eclipse.pde.core.javadoc"; //$NON-NLS-1$

	private HashMap fLocations;

	public String getJavadocLocation(IPluginModelBase model) {
		try {
			File file = new File(model.getInstallLocation());
			if (file.isDirectory()) {
				File doc = new File(file, "doc"); //$NON-NLS-1$
				if (new File(doc, "package-list").exists()) //$NON-NLS-1$
					return doc.toURL().toString();
			} else if (CoreUtility.jarContainsResource(file, "doc/package-list", false)) { //$NON-NLS-1$
				return "jar:" + file.toURL().toString() + "!/doc"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return getEntry(model);
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		}
	}

	private synchronized String getEntry(IPluginModelBase model) {
		initialize();
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			HostSpecification host = desc.getHost();
			String id = host == null ? desc.getSymbolicName() : host.getName();
			if (id != null) {
				Iterator iter = fLocations.keySet().iterator();
				while (iter.hasNext()) {
					String location = iter.next().toString();
					Set set = (Set) fLocations.get(location);
					if (set.contains(id))
						return location;
				}
			}
		}
		return null;
	}

	private synchronized void initialize() {
		if (fLocations != null)
			return;
		fLocations = new HashMap();

		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions(JAVADOC_ID, false);
		for (int i = 0; i < extensions.length; i++) {
			IPluginModelBase base = PluginRegistry.findModel(extensions[i].getContributor().getName());
			// only search external models
			if (base == null || base.getUnderlyingResource() != null)
				continue;
			processExtension(extensions[i], base);
		}
	}

	private void processExtension(IExtension extension, IPluginModelBase base) {
		IConfigurationElement[] children = extension.getConfigurationElements();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getName().equals("javadoc")) { //$NON-NLS-1$
				String path = children[i].getAttribute("path"); //$NON-NLS-1$
				if (path == null)
					continue;
				try {
					new URL(path);
					processPlugins(path, children[i].getChildren());
				} catch (MalformedURLException e) {
					String attr = children[i].getAttribute("archive"); //$NON-NLS-1$
					boolean archive = attr == null ? false : "true".equals(attr); //$NON-NLS-1$

					IPath modelPath = new Path(base.getInstallLocation());
					StringBuffer buffer = new StringBuffer();
					File file = modelPath.toFile();
					if (file.exists()) {
						try {
							buffer.append(file.toURI().toURL());
						} catch (MalformedURLException e1) {
							buffer.append("file:/"); //$NON-NLS-1$
							buffer.append(modelPath.toPortableString());
						}
						if (file.isFile()) {
							buffer.append("!/"); //$NON-NLS-1$
							archive = true;
						}
					}
					buffer.append(path);
					if (archive) {
						buffer.insert(0, "jar:"); //$NON-NLS-1$
						if (buffer.indexOf("!") == -1) { //$NON-NLS-1$
							buffer.append("!/"); //$NON-NLS-1$
						}
					}
					processPlugins(buffer.toString(), children[i].getChildren());
				}
			}
		}
	}

	private void processPlugins(String path, IConfigurationElement[] plugins) {
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].getName().equals("plugin")) { //$NON-NLS-1$
				String id = plugins[i].getAttribute("id"); //$NON-NLS-1$
				if (id == null)
					continue;
				Set set = (Set) fLocations.get(path);
				if (set == null) {
					set = new HashSet();
					fLocations.put(path, set);
				}
				set.add(id);
			}
		}
	}

	public synchronized void reset() {
		fLocations = null;
	}

}
