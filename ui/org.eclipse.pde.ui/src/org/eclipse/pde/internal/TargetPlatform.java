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

import java.util.Properties;

/**
 * @version 	1.0
 * @author
 */
public class TargetPlatform {
	
	public static File createPropertiesFile() throws CoreException {
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
		return createPropertiesFile(array);
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

				if (curr instanceof IPluginModel) {
					IPlugin plugin = ((IPluginModel) curr).getPlugin();
					properties.setProperty(plugin.getId(), prefix + "plugin.xml");
				} else if (curr instanceof IFragmentModel) {
					IFragment fragment = ((IFragmentModel) curr).getFragment();
					properties.setProperty(fragment.getId(), prefix + "fragment.xml");
				}
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
		return choices;
	}
}