/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class JavadocLocationManager {
	
	public static final String JAVADOC_ID = "org.eclipse.pde.core.javadoc"; //$NON-NLS-1$

	private HashMap fLocations;
	
	public String getJavadocLocation(IPluginModelBase model) {
		File file = new File(model.getInstallLocation());			
		if (file.isDirectory()) {
			File doc = new File(file, "doc"); //$NON-NLS-1$
			if (new File(doc, "package-list").exists()) //$NON-NLS-1$
				return doc.getAbsolutePath();
		} else if (CoreUtility.jarContainsResource(file, "doc/package-list", false)) { //$NON-NLS-1$
			return file.getAbsolutePath() + "!/doc"; //$NON-NLS-1$
		}		
		return getEntry(model);
	}

	private String getEntry(IPluginModelBase model) {
		initialize();
		String id = model.getPluginBase().getId();
		if (id == null)
			return null;
		Iterator iter = fLocations.keySet().iterator();
		while (iter.hasNext()) {
			String location = iter.next().toString();
			Set set = (Set)fLocations.get(location);
			if (set.contains(id))
				return location;
		}
		return null;
	}
	
	private synchronized void initialize() {
		if (fLocations != null) return;
		fLocations = new HashMap();
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getExternalModels();
		for (int i = 0; i < models.length; i++) {
			IPluginExtension[] extensions = models[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				if (JAVADOC_ID.equals(extensions[j].getPoint())) 
					processExtension(extensions[j]);
			}				
		}
	}

	private void processExtension(IPluginExtension extension) {
		IPluginObject[] children = extension.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getName().equals("javadoc")) { //$NON-NLS-1$
				IPluginElement javadoc = (IPluginElement) children[i];
				IPluginAttribute attr = javadoc.getAttribute("path"); //$NON-NLS-1$
				String path = (attr == null) ? null : attr.getValue();
				if (path == null)
					continue;
				try {
					new URL(path);
					processPlugins(path, javadoc.getChildren());
				} catch (MalformedURLException e) {
					attr = javadoc.getAttribute("archive"); //$NON-NLS-1$
					boolean archive = attr == null ? false : "true".equals(attr.getValue()); //$NON-NLS-1$

					IPath modelPath = new Path(extension.getModel().getInstallLocation());
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
					if (archive)
						buffer.insert(0, "jar:"); //$NON-NLS-1$
					processPlugins(buffer.toString(), javadoc.getChildren()); //$NON-NLS-1$				
				}
			}
		}
	}
	
	private void processPlugins(String path, IPluginObject[] plugins) {
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].getName().equals("plugin")) { //$NON-NLS-1$
				IPluginElement plugin = (IPluginElement)plugins[i];
				IPluginAttribute attr = plugin.getAttribute("id"); //$NON-NLS-1$
				String id = attr == null ? null : attr.getValue();
				if (id == null)
					continue;
				Set set = (Set)fLocations.get(path);
				if (set == null) {
					set = new HashSet();
					fLocations.put(path, set);
				}
				set.add(id);
			}
		}		
	}
	
	public void reset() {
		fLocations = null;
	}

}
