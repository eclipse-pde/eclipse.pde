package org.eclipse.pde.internal.ui;

import java.io.File;
import java.util.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourceLocationManager {
	public static final String P_SOURCE_LOCATIONS = "source_locations";
	public static final String P_EXT_LOCATIONS = "ext_locations";
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

	public File findSourceFile(IPath sourcePath) {
		initialize();
		File file = findSourceFile(extensionLocations, sourcePath);
		if (file != null)
			return file;
		file = findSourceFile(userLocations, sourcePath);
		return file;
	}
	
	private File findSourceFile(ArrayList list, IPath sourcePath) {
		for (int i = 0; i < list.size(); i++) {
			SourceLocation location = (SourceLocation) list.get(i);
			File file = findSourcePath(location, sourcePath);
			if (file != null)
				return file;
		}
		return null;
	}

	private File findSourcePath(SourceLocation location, IPath libraryPath) {
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
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		String pref = pstore.getString(P_SOURCE_LOCATIONS);
		if (pref == null)
			return;
		String[] entries = parseSourceLocations(pref);
		for (int i = 0; i < entries.length; i++) {
			String entry = entries[i];
			boolean enabled = true;
			int commaLoc = entry.lastIndexOf(',');
			if (commaLoc != -1) {
				enabled = entry.substring(commaLoc + 1).equals("t");
				entry = entry.substring(0, commaLoc);
			}
			userLocations.add(new SourceLocation(new Path(entry), enabled));
		}
	}

	public void setUserLocations(ArrayList locations) {
		userLocations = locations;
		storeSourceLocations();
		PDEPlugin.getDefault().savePluginPreferences();
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
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		if (extensionLocations != null) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < extensionLocations.size(); i++) {
				SourceLocation loc = (SourceLocation) extensionLocations.get(i);
				if (i > 0)
					buf.append(File.pathSeparatorChar);
				if (!loc.isEnabled())
					buf.append(loc.getPath().toOSString());
			}
			pstore.setValue(P_EXT_LOCATIONS, buf.toString());
		}
		if (userLocations == null)
			return;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < userLocations.size(); i++) {
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			SourceLocation location = (SourceLocation) userLocations.get(i);
			IPath path = location.getPath();
			buf.append(path.toOSString());
			if (location.isEnabled())
				buf.append(",t");
			else
				buf.append(",f");
		}
		pstore.setValue(P_SOURCE_LOCATIONS, buf.toString());
	}

	private void initializeExtensionLocations() {
		if (extensionLocations != null)
			return;
		extensionLocations = new ArrayList();
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		String pref = pstore.getString(P_EXT_LOCATIONS);
		String[] entries = parseSourceLocations(pref);
		IPluginRegistry registry = Platform.getPluginRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(
				PDEPlugin.getPluginId(),
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