package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginBase;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourceLocationManager implements ICoreConstants {
	private ArrayList userLocations = null;
	private ArrayList extensionLocations = null;
	private ArrayList orphanedExtensionLocations = null;

	class VariableTask {
		public VariableTask(String name, IPath path) {
			this.name = name;
			this.path = path;
		}
		String name; // name of the variable to set
		IPath path; // path for the path to set or null deleted
	}
	
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
		if (result!=null)
			return result.file;
		else
			return null;
	}
	
	public IPath findVariableRelativePath(IPluginBase pluginBase, IPath sourcePath) {
		IPath relativePath = getRelativePath(pluginBase, sourcePath);
		SearchResult result = findSourceLocation(pluginBase, relativePath);
		if (result!=null) {
			Path path = new Path(result.loc.getName());
			return path.append(relativePath);
		}
		else
			return null;
	}
	
	private IPath getRelativePath(IPluginBase pluginBase, IPath sourcePath) {
		String pluginDir = pluginBase.getId() + "_" + pluginBase.getVersion();
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
		String pref = PDECore.getDefault().getPluginPreferences().getString(P_SOURCE_LOCATIONS);
		parseSavedSourceLocations(pref, userLocations);
	}
	
	public void initializeClasspathVariables(IProgressMonitor monitor) {
		initialize();
		String[] variableNames = JavaCore.getClasspathVariableNames();
		ArrayList tasks = new ArrayList();
		addOrphanedLocations(variableNames, tasks);
		addNewOrChangedLocations(variableNames, tasks);
		if (tasks.size() == 0)
			return;
		String[] names = new String[tasks.size()];
		IPath[] paths = new IPath[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			VariableTask task = (VariableTask) tasks.get(i);
			names[i] = task.name;
			paths[i] = task.path;
		}
		try {
			JavaCore.setClasspathVariables(names, paths, monitor);
			orphanedExtensionLocations = null;
		} catch (JavaModelException e) {
			PDECore.log(e);
		}
	}

	private String encodeSourceLocations(ArrayList locations) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < locations.size(); i++) {
			SourceLocation loc = (SourceLocation) locations.get(i);
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			buf.append(encodeSourceLocation(loc));
		}
		return buf.toString();
	}

	private String encodeSourceLocation(SourceLocation location) {
		return location.getName()
			+ "@"
			+ location.getPath().toOSString()
			+ ","
			+ (location.isEnabled() ? "t" : "f");
	}

	private SourceLocation parseSourceLocation(String text) {
		String name = "";
		String path = "";
		boolean enabled = true;
		int atLoc = text.indexOf('@');
		if (atLoc != -1)
			name = text.substring(0, atLoc);
		else
			atLoc = 0;
		int commaLoc = text.lastIndexOf(',');
		if (commaLoc != -1) {
			String state = text.substring(commaLoc + 1);
			if (state.equals("f"))
				enabled = false;
			path = text.substring(atLoc + 1, commaLoc);
		} else
			path = text.substring(atLoc + 1);
		return new SourceLocation(name, new Path(path), enabled);
	}

	private void initializeExtensionLocations() {
		extensionLocations = new ArrayList();
		String pref = PDECore.getDefault().getPluginPreferences().getString(P_EXT_LOCATIONS);
		IPluginRegistry registry = Platform.getPluginRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(
				PDECore.getPluginId(),
				"source");
		SourceLocation[] storedLocations = getSavedSourceLocations(pref);
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("location")) {
				SourceLocation location = new SourceLocation(element);
				location.setEnabled(getSavedState(location.getName(), storedLocations));
				extensionLocations.add(location);
			}
		}
		computeOrphanedLocations(storedLocations);
	}



	private void addOrphanedLocations(String[] variables, ArrayList tasks) {
		if (orphanedExtensionLocations == null)
			return;
		for (int i = 0; i < orphanedExtensionLocations.size(); i++) {
			SourceLocation orphanedLocation =
				(SourceLocation) orphanedExtensionLocations.get(i);
			if (isOnTheList(orphanedLocation.getName(), variables)) {
				tasks.add(new VariableTask(orphanedLocation.getName(), null));
			}
		}
	}

	private void addNewOrChangedLocations(
		String[] variables,
		ArrayList tasks) {
		addNewOrChangedLocations(variables, extensionLocations, tasks);
		addNewOrChangedLocations(variables, userLocations, tasks);
	}

	private void addNewOrChangedLocations(
		String[] variables,
		ArrayList locations,
		ArrayList tasks) {
		for (int i = 0; i < locations.size(); i++) {
			SourceLocation location = (SourceLocation) locations.get(i);
			IPath varPath = JavaCore.getClasspathVariable(location.getName());
			IPath locPath = location.getPath();
			if (varPath == null || !varPath.equals(locPath)) {
				tasks.add(
					new VariableTask(location.getName(), location.getPath()));
			}
		}
	}

	private boolean isOnTheList(String name, String[] list) {
		for (int i = 0; i < list.length; i++) {
			if (name.equals(list[i]))
				return true;
		}
		return false;
	}

	private boolean getSavedState(String name, SourceLocation[] list) {
		for (int i = 0; i < list.length; i++) {
			SourceLocation saved = list[i];
			if (name.equals(saved.getName()))
				return saved.isEnabled();
		}
		return true;
	}

	private void parseSavedSourceLocations(String text, ArrayList entries) {
		StringTokenizer stok = new StringTokenizer(text, File.pathSeparator);
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			SourceLocation location = parseSourceLocation(token);
			entries.add(location);
		}
	}

	private SourceLocation[] getSavedSourceLocations(String text) {
		if (text == null || text.length() == 0)
			return new SourceLocation[0];
		ArrayList entries = new ArrayList();
		parseSavedSourceLocations(text, entries);
		return (SourceLocation[]) entries.toArray(
			new SourceLocation[entries.size()]);
	}

	private void computeOrphanedLocations(SourceLocation[] storedLocations) {
		if (orphanedExtensionLocations != null)
			orphanedExtensionLocations.clear();
		for (int i = 0; i < storedLocations.length; i++) {
			SourceLocation storedLoc = storedLocations[i];
			boolean found = false;
			for (int j = 0; j < extensionLocations.size(); j++) {
				SourceLocation loc = (SourceLocation) extensionLocations.get(j);
				if (storedLoc.getName().equals(loc.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				if (orphanedExtensionLocations == null)
					orphanedExtensionLocations = new ArrayList();
				orphanedExtensionLocations.add(storedLoc);
			}
		}
	}
}