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

	public IPath findSourceArchive(IPath libraryPath) {
		initialize();
		IPath path = findSourceArchive(libraryPath, userLocations);
		if (path != null)
			return path;
		return findSourceArchive(libraryPath, extensionLocations);
	}

	private void initialize() {
		initializeUserLocations();
		initializeExtensionLocations();
	}

	private IPath findSourceArchive(IPath libraryPath, ArrayList list) {
		for (int i = 0; i < list.size(); i++) {
			SourceLocation location = (SourceLocation) list.get(i);
			IPath path = findSourceArchive(libraryPath, location);
			if (path != null)
				return path;
		}
		return null;
	}

	private IPath findSourceArchive(
		IPath libraryPath,
		SourceLocation location) {
		return null;
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
			userLocations.add(new SourceLocation(new Path(entries[i])));
		}
	}

	public void setUserLocations(ArrayList locations) {
		userLocations = locations;
		storeSourceLocations();
		PDEPlugin.getDefault().savePluginPreferences();
	}

	private String[] parseSourceLocations(String text) {
		ArrayList entries = new ArrayList();
		StringTokenizer stok = new StringTokenizer(text, File.pathSeparator);
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			entries.add(token);
		}
		return (String[]) entries.toArray(new String[entries.size()]);
	}

	public void storeSourceLocations() {
		if (userLocations == null)
			return;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < userLocations.size(); i++) {
			if (i > 0)
				buf.append(File.pathSeparatorChar);
			SourceLocation location = (SourceLocation) userLocations.get(i);
			IPath path = location.getPath();
			buf.append(path.toOSString());
		}
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		pstore.setValue(P_SOURCE_LOCATIONS, buf.toString());
	}

	private void initializeExtensionLocations() {
		if (extensionLocations != null)
			return;
		extensionLocations = new ArrayList();
		IPluginRegistry registry = Platform.getPluginRegistry();
		IConfigurationElement[] elements =
			registry.getConfigurationElementsFor(
				PDEPlugin.getPluginId(),
				"source");
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("location")) {
				SourceLocation location = new SourceLocation(element);
				extensionLocations.add(location);
			}
		}
	}
}