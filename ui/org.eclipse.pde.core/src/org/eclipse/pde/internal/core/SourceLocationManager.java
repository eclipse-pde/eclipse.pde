package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.*;
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
		initialize();
		String pluginDir = pluginBase.getId()+"_"+pluginBase.getVersion();
		IPath locationRelativePath = new Path(pluginDir);
		locationRelativePath = locationRelativePath.append(sourcePath);
		File file = findSourceFile(extensionLocations, locationRelativePath);
		if (file != null)
			return file;
		file = findSourceFile(userLocations, sourcePath);
		return file;
	}
	
	private File findSourceFile(ArrayList list, IPath sourcePath) {
		for (int i = 0; i < list.size(); i++) {
			SourceLocation location = (SourceLocation) list.get(i);
			if (location.isEnabled() == false)
				continue;
			File file = findSourcePath(location, sourcePath);
			if (file != null)
				return file;
		}
		return null;
	}

	private File findSourcePath(SourceLocation location, IPath sourcePath) {
		IPath locationPath = location.getPath();
		IPath fullPath = locationPath.append(sourcePath);
		File file = fullPath.toFile();
		if (file.exists())
			return file;
		else
			return null;
	}

	public SourceLocation[] getAllLocations() {
		initialize();
		int size = userLocations.size() + extensionLocations.size();
		SourceLocation[] array = new SourceLocation[size];
		for (int i = 0; i < extensionLocations.size(); i++) {
			array[i] = (SourceLocation) extensionLocations.get(i);
		}
		int offset = extensionLocations.size();
		for (int i = 0; i < userLocations.size(); i++) {
			array[i + offset] = (SourceLocation) userLocations.get(i);
		}
		return array;
	}

	private void initialize() {
		initializeUserLocations();
		initializeExtensionLocations();
	}

	private void initializeUserLocations() {
		if (userLocations != null)
			return;
		userLocations = new ArrayList();
		CoreSettings settings = PDECore.getDefault().getSettings();
		String pref = settings.getString(P_SOURCE_LOCATIONS);
		if (pref == null)
			return;
		String[] entries = parseSourceLocations(pref);
		for (int i = 0; i < entries.length; i++) {
			String entry = entries[i];
			boolean enabled = true;
			String name = "?";
			int atLoc = entry.indexOf('@');
			if (atLoc!= -1) {
				name = entry.substring(0, atLoc);
			}
			else atLoc = 0;
			int commaLoc = entry.lastIndexOf(',');
			if (commaLoc != -1) {
				enabled = entry.substring(commaLoc + 1).equals("t");
				entry = entry.substring(atLoc+1, commaLoc);
			}
			userLocations.add(new SourceLocation(name, new Path(entry), enabled));
		}
	}

	public void setUserLocations(ArrayList locations) {
		userLocations = locations;
		storeSourceLocations();
	}

	private String[] parseSourceLocations(String text) {
		ArrayList entries = new ArrayList();
		if (text != null) {
			StringTokenizer stok =
				new StringTokenizer(text, File.pathSeparator);
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken();
				entries.add(token);
			}
		}
		return (String[]) entries.toArray(new String[entries.size()]);
	}

	public void storeSourceLocations() {
		CoreSettings settings = PDECore.getDefault().getSettings();
		if (extensionLocations != null) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < extensionLocations.size(); i++) {
				SourceLocation loc = (SourceLocation) extensionLocations.get(i);
				if (i > 0)
					buf.append(File.pathSeparatorChar);
				if (!loc.isEnabled())
					buf.append(loc.getPath().toOSString());
			}
			settings.setValue(P_EXT_LOCATIONS, buf.toString());
		}
		if (userLocations == null)
			return;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < userLocations.size(); i++) {
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			SourceLocation location = (SourceLocation) userLocations.get(i);
			String name = location.getName();
			buf.append(name+"@");
			IPath path = location.getPath();
			buf.append(path.toOSString());
			if (location.isEnabled())
				buf.append(",t");
			else
				buf.append(",f");
		}
		settings.setValue(P_SOURCE_LOCATIONS, buf.toString());
		settings.store();
		
	}

	private void initializeExtensionLocations() {
		if (extensionLocations != null)
			return;
		extensionLocations = new ArrayList();
		CoreSettings settings = PDECore.getDefault().getSettings();
		String pref = settings.getString(P_EXT_LOCATIONS);
		String[] entries = parseSourceLocations(pref);
		IPluginRegistry registry = Platform.getPluginRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(
				PDECore.getPluginId(),
				"source");
		String[] disabledLocations = parseSourceLocations(pref);
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("location")) {
				SourceLocation location = new SourceLocation(element);
				if (isOnTheList(location, disabledLocations))
					location.setEnabled(false);
				extensionLocations.add(location);
			}
		}
	}
	private boolean isOnTheList(SourceLocation loc, String[] list) {
		String pathName = loc.getPath().toOSString();
		for (int i = 0; i < list.length; i++) {
			if (pathName.equals(list[i]))
				return true;
		}
		return false;
	}
}