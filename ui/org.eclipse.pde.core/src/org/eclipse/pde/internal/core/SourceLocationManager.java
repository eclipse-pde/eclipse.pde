/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

public class SourceLocationManager implements ICoreConstants {
	private ArrayList userLocations = null;
	private ArrayList extensionLocations = null;

	class SearchResult {
		SearchResult(SourceLocation loc, File file) {
			this.loc = loc;
			this.file = file;
		}
		SourceLocation loc;
		File file;
	}

	public ArrayList getUserLocationArray() {
		initializeUserLocations();
		return userLocations;
	}

	public SourceLocation[] getUserLocations() {
		initializeUserLocations();
		return getLocations(userLocations);
	}

	public SourceLocation[] getExtensionLocations() {
		initializeExtensionLocations();
		return getLocations(extensionLocations);
	}

	private SourceLocation[] getLocations(ArrayList list) {
		return (SourceLocation[]) list.toArray(new SourceLocation[list.size()]);
	}

	public File findSourceFile(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		if (result != null)
			return result.file;
		else
			return null;
	}

	public IPath findPath(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		return result == null ? null : result.loc.getPath().append(relativePath);
	}

	private IPath getRelativePath(IPluginBase pluginBase, IPath sourcePath) {
		PluginVersionIdentifier vid =
			new PluginVersionIdentifier(pluginBase.getVersion());
		String pluginDir = pluginBase.getId() + "_" + vid.toString();
		IPath locationRelativePath = new Path(pluginDir);
		return locationRelativePath.append(sourcePath);
	}

	public SearchResult findSourceLocation(IPluginBase pluginBase, IPath relativePath) {
		initialize();
		SearchResult result = findSourceFile(extensionLocations, relativePath);
		if (result != null)
			return result;
		return findSourceFile(userLocations, relativePath);
	}

	private SearchResult findSourceFile(ArrayList list, IPath sourcePath) {
		for (int i = 0; i < list.size(); i++) {
			SourceLocation location = (SourceLocation) list.get(i);
			if (location.isEnabled() == false)
				continue;
			SearchResult result = findSourcePath(location, sourcePath);
			if (result != null)
				return result;
		}
		return null;
	}

	private SearchResult findSourcePath(SourceLocation location, IPath sourcePath) {
		IPath locationPath = location.getPath();
		IPath fullPath = locationPath.append(sourcePath);
		File file = fullPath.toFile();
		if (file.exists())
			return new SearchResult(location, file);
		else
			return null;
	}

	private void initialize() {
		initializeUserLocations();
		initializeExtensionLocations();
	}

	private void initializeUserLocations() {
		userLocations = new ArrayList();
		String pref =
			PDECore.getDefault().getPluginPreferences().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0)
			parseSavedSourceLocations(pref, userLocations);
	}

	private SourceLocation parseSourceLocation(String text) {
		String path;
		boolean enabled;
		try {
			text = text.trim();
			int commaIndex = text.lastIndexOf(',');
			enabled = text.charAt(commaIndex + 1) == 't';
			int atLoc = text.indexOf('@');
			path =
				(atLoc == -1)
					? text.substring(0, commaIndex)
					: text.substring(atLoc + 1, commaIndex);
		} catch (RuntimeException e) {
			return null;
		}
		return new SourceLocation(new Path(path), enabled);
	}

	private void initializeExtensionLocations() {
		extensionLocations = new ArrayList();
		String pref =
			PDECore.getDefault().getPluginPreferences().getString(P_EXT_LOCATIONS);
		SourceLocation[] storedLocations = getSavedSourceLocations(pref);
		IPluginExtension[] extensions = getRegisteredSourceExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginObject[] children = extensions[i].getChildren();
			for (int j = 0; j < children.length; j++) {
				if (children[j].getName().equals("location")) {
					IPluginElement element = (IPluginElement) children[j];
					String pathValue = element.getAttribute("path").getValue();
					IResource resource = extensions[i].getModel().getUnderlyingResource();
					IPath path;
					if (resource != null && resource.isLinked()) {
						path = resource.getLocation().removeLastSegments(1);
					} else {
						path = new Path(extensions[i].getModel().getInstallLocation());
					}
					SourceLocation location =
						new SourceLocation(path.append(pathValue), true);
					location.setEnabled(
						getSavedState(location.getPath(), storedLocations));
					location.setUserDefined(false);
					if (!extensionLocations.contains(location))
						extensionLocations.add(location);
				}
			}
		}
	}

	private boolean getSavedState(IPath path, SourceLocation[] list) {
		for (int i = 0; i < list.length; i++) {
			SourceLocation saved = list[i];
			if (path.equals(saved.getPath()))
				return saved.isEnabled();
		}
		return true;
	}

	private void parseSavedSourceLocations(String text, ArrayList entries) {
		text = text.replace(File.pathSeparatorChar, ';');
		StringTokenizer stok = new StringTokenizer(text, ";");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			SourceLocation location = parseSourceLocation(token);
			if (location != null)
				entries.add(location);
		}
	}

	private SourceLocation[] getSavedSourceLocations(String text) {
		if (text == null || text.length() == 0)
			return new SourceLocation[0];
		ArrayList entries = new ArrayList();
		parseSavedSourceLocations(text, entries);
		return (SourceLocation[]) entries.toArray(new SourceLocation[entries.size()]);
	}

	private IPluginExtension[] getRegisteredSourceExtensions() {
		Vector result = new Vector();
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginExtension[] extensions = models[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				IPluginExtension extension = extensions[j];
				if (extension.getPoint().equals(PDECore.getPluginId() + ".source")) {
					result.add(extension);
				}
			}
		}
		return (IPluginExtension[]) result.toArray(new IPluginExtension[result.size()]);
	}

}