/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.model.plugin.*;
import java.util.Vector;
import java.io.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.util.Choice;
import org.eclipse.core.boot.BootLoader;
import java.util.Locale;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.preferences.TargetEnvironmentPreferencePage;
import java.util.Properties;
import java.net.*;

/**
 * @version 	1.0
 * @author
 */
public class TargetPlatform implements IEnvironmentVariables {

	public static File createPropertiesFile() throws CoreException {
		return createPropertiesFile(getVisibleModels());
	}

	public static URL[] createPluginPath() throws CoreException {
		IPluginModelBase[] models = getVisibleModels();
		URL urls[] = new URL[models.length];
		for (int i = 0; i < urls.length; i++) {
			IPluginModelBase model = models[i];
			String urlName = createURL(model);
			try {
				urls[i] = new URL(urlName);
			} catch (MalformedURLException e) {
				PDEPlugin.logException(e);
				return new URL[0];
			}
		}
		return urls;
	}

	private static IPluginModelBase[] getVisibleModels() {
		Vector result = new Vector();
		WorkspaceModelManager wmanager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] wplugins = wmanager.getWorkspacePluginModels();
		IPluginModelBase[] wfragments = wmanager.getWorkspaceFragmentModels();
		IPluginModelBase[] eplugins =
			PDEPlugin.getDefault().getExternalModelManager().getModels();

		addFromList(result, wplugins);
		addFromList(result, wfragments);
		addFromList(result, eplugins);
		IPluginModelBase[] array =
			(IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
		return array;
	}

	private static void addFromList(Vector result, IPluginModelBase[] list) {
		for (int i = 0; i < list.length; i++) {
			IPluginModelBase model = list[i];
			if (model.isEnabled())
				result.add(list[i]);
		}
	}

	public static File createPropertiesFile(IPluginModelBase[] plugins)
		throws CoreException {
		try {
			File file = File.createTempFile(PDEPlugin.getPluginId(), ".properties");
			file.deleteOnExit();
			Properties properties = new Properties();

			for (int i = 0; i < plugins.length; i++) {
				IPluginModelBase curr = plugins[i];
				String prefix = "file:" + curr.getInstallLocation() + File.separator;
				String id = curr.getPluginBase().getId();
				properties.setProperty(id, createURL(curr));
			}

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				properties.store(fos, null);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
			return file;
		} catch (IOException e) {
			throw new CoreException(
				new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", e));
		}
	}

	private static String createURL(IPluginModelBase model) {
		String prefix = "file:" + model.getInstallLocation() + File.separator;

		if (model instanceof IPluginModel) {
			return prefix + "plugin.xml";
		} else if (model instanceof IFragmentModel) {
			return prefix + "fragment.xml";
		} else
			return "";
	}

	public static String getOS() {
		initializeDefaults();
		return getProperty(OS);
	}

	public static String getWS() {
		initializeDefaults();
		return getProperty(WS);
	}

	public static String getNL() {
		initializeDefaults();
		return getProperty(NL);
	}

	public static String getOSArch() {
		initializeDefaults();
		return getProperty(ARCH);
	}

	private static String getProperty(String key) {
		return PDEPlugin.getDefault().getPreferenceStore().getString(key);
	}

	public static void initializeDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		store.setDefault(OS, BootLoader.getOS());
		store.setDefault(WS, BootLoader.getWS());
		store.setDefault(NL, BootLoader.getNL());
		store.setDefault(ARCH, BootLoader.getOSArch());
	}

	public static Choice[] getOSChoices() {
		return new Choice[] {
			new Choice(BootLoader.OS_WIN32, BootLoader.OS_WIN32),
			new Choice(BootLoader.OS_LINUX, BootLoader.OS_LINUX),
			new Choice(BootLoader.OS_AIX, BootLoader.OS_AIX),
			new Choice(BootLoader.OS_HPUX, BootLoader.OS_HPUX),
			new Choice(BootLoader.OS_QNX, BootLoader.OS_QNX),
			new Choice(BootLoader.OS_SOLARIS, BootLoader.OS_SOLARIS)};
	}

	public static Choice[] getWSChoices() {
		return new Choice[] {
			new Choice(BootLoader.WS_WIN32, BootLoader.WS_WIN32),
			new Choice(BootLoader.WS_MOTIF, BootLoader.WS_MOTIF),
			new Choice(BootLoader.WS_GTK, BootLoader.WS_GTK),
			new Choice(BootLoader.WS_PHOTON, BootLoader.WS_PHOTON)};
	}

	public static Choice[] getNLChoices() {
		Locale[] locales = Locale.getAvailableLocales();
		Choice[] choices = new Choice[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			choices[i] = new Choice(locale.toString(), locale.getDisplayName());
		}
		ArraySorter.INSTANCE.sortInPlace(choices);
		return choices;
	}
	public static Choice[] getArchChoices() {
		return new Choice[] { new Choice(BootLoader.ARCH_X86, BootLoader.ARCH_X86)};
	}
}