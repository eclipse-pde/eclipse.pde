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
import org.eclipse.pde.internal.core.plugin.*;

public class SourceLocationManager implements ICoreConstants {
	private ArrayList fUserLocations = new ArrayList();
	private ArrayList fExtensionLocations = new ArrayList();

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
		return fUserLocations;
	}

	public SourceLocation[] getUserLocations() {
		initializeUserLocations();
		return getLocations(fUserLocations);
	}

	public SourceLocation[] getExtensionLocations() {
		initializeExtensionLocations();
		return getLocations(fExtensionLocations);
	}

	private SourceLocation[] getLocations(ArrayList list) {
		return (SourceLocation[]) list.toArray(new SourceLocation[list.size()]);
	}

	public File findSourceFile(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		return (result != null)? result.file : null;
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
		SearchResult result = findSourceFile(fExtensionLocations, relativePath);
		return (result != null) ? result : findSourceFile(fUserLocations, relativePath);
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
		return file.exists() ? new SearchResult(location, file): null;
	}

	private void initialize() {
		initializeUserLocations();
		initializeExtensionLocations();
	}

	private void initializeUserLocations() {
		fUserLocations.clear();
		String pref =
			PDECore.getDefault().getPluginPreferences().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0)
			parseSavedSourceLocations(pref, fUserLocations);
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
		fExtensionLocations.clear();
		String pref =
			PDECore.getDefault().getPluginPreferences().getString(P_EXT_LOCATIONS);
		SourceLocation[] storedLocations = getSavedSourceLocations(pref);
		ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
		for (int i = 0; i < entries.length; i++) {
			processExtensions(entries[i], false);
		}
		for (int i = 0; i < fExtensionLocations.size(); i++) {
			SourceLocation location = (SourceLocation)fExtensionLocations.get(i);
			location.setEnabled(getSavedState(location.getPath(), storedLocations));
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

	private void processExtensions(ModelEntry entry, boolean useExternal) {
		IPluginModelBase model = useExternal ? entry.getExternalModel() : entry.getActiveModel();		
		if (model == null)
			return;
		
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int j = 0; j < extensions.length; j++) {
			IPluginExtension extension = extensions[j];
			if (extension.getPoint().equals(PDECore.getPluginId() + ".source")) {
				int origLength = fExtensionLocations.size();
				processExtension(extension);
				if (fExtensionLocations.size() == origLength && model instanceof WorkspacePluginModelBase) {
					processExtensions(entry, true);					
				}
			}
		}		
	}
	
	private void processExtension(IPluginExtension extension) {
		IPluginObject[] children = extension.getChildren();
		for (int j = 0; j < children.length; j++) {
			if (children[j].getName().equals("location")) {
				IPluginElement element = (IPluginElement) children[j];
				String pathValue = element.getAttribute("path").getValue();
				IResource resource = extension.getModel().getUnderlyingResource();
				IPath path;
				if (resource != null && resource.isLinked()) {
					path = resource.getLocation().removeLastSegments(1).append(pathValue);
				} else {
					path = new Path(extension.getModel().getInstallLocation()).append(pathValue);
				}
				if (path.toFile().exists()) {
					SourceLocation location = new SourceLocation(path, true);
					location.setUserDefined(false);
					if (!fExtensionLocations.contains(location))
						fExtensionLocations.add(location);
				}
			}
		}
	}

}