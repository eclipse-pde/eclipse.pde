/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

public class SourceLocationManager implements ICoreConstants {
	private SourceLocation[] fExtensionLocations = null;

	class SearchResult {
		SearchResult(SourceLocation loc, File file) {
			this.loc = loc;
			this.file = file;
		}
		SourceLocation loc;
		File file;
	}

	public SourceLocation[] getUserLocations() {
		ArrayList userLocations = new ArrayList();
		String pref = PDECore.getDefault().getPluginPreferences().getString(P_SOURCE_LOCATIONS);
		if (pref.length() > 0)
			parseSavedSourceLocations(pref, userLocations);
		return (SourceLocation[]) userLocations.toArray(new SourceLocation[userLocations.size()]);
	}

	public SourceLocation[] getExtensionLocations() {
		if (fExtensionLocations == null) {
			ArrayList list = new ArrayList();
			ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
			for (int i = 0; i < entries.length; i++) {
				IPluginModelBase model = entries[i].getExternalModel();
				if (model != null)
					processExtensions(model, list);
			}
			fExtensionLocations = (SourceLocation[]) list.toArray(new SourceLocation[list.size()]);
		}
		return fExtensionLocations;
	}
	
	public void setExtensionLocations(SourceLocation[] locations) {
		fExtensionLocations = locations;
	}

	public File findSourceFile(IPluginBase pluginBase, IPath sourcePath) {
		IStatus status = PluginVersionIdentifier.validateVersion(pluginBase.getVersion());
		if (status.getSeverity() != IStatus.OK)
			return null;
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		return (result != null)? result.file : null;
	}

	public IPath findSourcePath(IPluginBase pluginBase, IPath sourcePath) {
		IStatus status = PluginVersionIdentifier.validateVersion(pluginBase.getVersion());
		if (status.getSeverity() != IStatus.OK)
			return null;
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		return result == null ? null : result.loc.getPath().append(relativePath);
	}

	private IPath getRelativePath(IPluginBase pluginBase, IPath sourcePath) {
		PluginVersionIdentifier vid = new PluginVersionIdentifier(pluginBase.getVersion());
		String pluginDir = pluginBase.getId() + "_" + vid.toString(); //$NON-NLS-1$
		return new Path(pluginDir).append(sourcePath);
	}

	public SearchResult findSourceLocation(IPluginBase pluginBase, IPath relativePath) {
		SearchResult result = findSearchResult(getUserLocations(), relativePath);
		return (result != null) ? result : findSearchResult(getExtensionLocations(), relativePath);
	}

	private SearchResult findSearchResult(SourceLocation[] locations, IPath sourcePath) {
		for (int i = 0; i < locations.length; i++) {
			IPath fullPath = locations[i].getPath().append(sourcePath);
			File file = fullPath.toFile();
			if (file.exists())
				return new SearchResult(locations[i], file);
		}
		return null;
	}

	private SourceLocation parseSourceLocation(String text) {
		String path;
		try {
			text = text.trim();
			int commaIndex = text.lastIndexOf(',');
			if (commaIndex == -1)
				return new SourceLocation(new Path(text));
			
			int atLoc = text.indexOf('@');
			path =
				(atLoc == -1)
					? text.substring(0, commaIndex)
					: text.substring(atLoc + 1, commaIndex);
		} catch (RuntimeException e) {
			return null;
		}
		return new SourceLocation(new Path(path));
	}

	private void parseSavedSourceLocations(String text, ArrayList entries) {
		text = text.replace(File.pathSeparatorChar, ';');
		StringTokenizer stok = new StringTokenizer(text, ";"); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			SourceLocation location = parseSourceLocation(token);
			if (location != null)
				entries.add(location);
		}
	}

	public static SourceLocation[] computeSourceLocations(IPluginModelBase[] models) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			processExtensions(models[i], result);
		}
		return (SourceLocation[])result.toArray(new SourceLocation[result.size()]);
	}
	
	private static void processExtensions(IPluginModelBase model, ArrayList result) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int j = 0; j < extensions.length; j++) {
			IPluginExtension extension = extensions[j];
			if ((PDECore.getPluginId() + ".source").equals(extension.getPoint())) { //$NON-NLS-1$
				processExtension(extension, result);
			}
		}				
	}
	
	private static  void processExtension(IPluginExtension extension, ArrayList result) {
		IPluginObject[] children = extension.getChildren();
		for (int j = 0; j < children.length; j++) {
			if (children[j].getName().equals("location")) { //$NON-NLS-1$
				IPluginElement element = (IPluginElement) children[j];
				String pathValue = element.getAttribute("path").getValue(); //$NON-NLS-1$b	
				ISharedPluginModel model = extension.getModel();
				IPath path = new Path(model.getInstallLocation()).append(pathValue);
				if (path.toFile().exists()) {
					SourceLocation location = new SourceLocation(path);
					location.setUserDefined(false);
					if (!result.contains(location))
						result.add(location);
				}
			}
		}
	}

}
