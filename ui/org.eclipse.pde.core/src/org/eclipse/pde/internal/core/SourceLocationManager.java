package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.plugin.Plugin;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourceLocationManager implements ICoreConstants {
	private ArrayList userLocations = null;
	private ArrayList extensionLocations = null;

	
	class SearchResult {
		SourceLocation loc;
		File file;
		SearchResult(SourceLocation loc, File file) {
			this.loc = loc;
			this.file = file;
		}
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
			
		// undo old behavior:
		// delete classpath variables as we no longer use them for anything.
		IPath value = JavaCore.getClasspathVariable(name);
		if (value != null && value.toOSString().equals(path))
			JavaCore.removeClasspathVariable(name, new NullProgressMonitor());
		
		return new SourceLocation(name, new Path(path), enabled);
	}

	private void initializeExtensionLocations() {
		extensionLocations = new ArrayList();
		String pref =
			PDECore.getDefault().getPluginPreferences().getString(
				P_EXT_LOCATIONS);
		SourceLocation[] storedLocations = getSavedSourceLocations(pref);
		IPluginExtension[] extensions = getRegisteredSourceExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginObject[] children = extensions[i].getChildren();
			for (int j = 0; j < children.length; j++) {
				if (children[j].getName().equals("location")) {
					IPluginElement element = (IPluginElement) children[j];
					String pathValue = element.getAttribute("path").getValue();
					SourceLocation location =
						new SourceLocation(
							getComputedName(extensions[i], pathValue),
							new Path(
								extensions[i].getModel().getInstallLocation()
									+ Path.SEPARATOR
									+ pathValue),
							true);
					location.setEnabled(
						getSavedState(location.getName(), storedLocations));
					location.setUserDefined(false);
					extensionLocations.add(location);
				}
			}
		}
	}

	private String getComputedName(IPluginExtension extension, String pathValue) {
		String name = ((Plugin)extension.getParent()).getId() + "_" + pathValue;
		return name.replace('.','_').toUpperCase();
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

	private IPluginExtension[] getRegisteredSourceExtensions() {
		Vector result = new Vector();
		IPluginModel[] models = PDECore.getDefault().getExternalModelManager().getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginExtension[] extensions = models[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				IPluginExtension extension = extensions[j];
				if (extension.getPoint().equals(PDECore.getPluginId() + ".source")) {
					result.add(extension);
				}
			}
		}
		IPluginExtension[] extensions = new IPluginExtension[result.size()];
		result.copyInto(extensions);
		return extensions;
	}
	
}