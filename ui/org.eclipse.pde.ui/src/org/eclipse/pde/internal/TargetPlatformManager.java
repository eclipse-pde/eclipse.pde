/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.model.plugin.*;
import java.util.Vector;
import java.io.*;
import org.eclipse.pde.internal.*;
import java.util.Properties;

/**
 * @version 	1.0
 * @author
 */
public class TargetPlatformManager {
	
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
}