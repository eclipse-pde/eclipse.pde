package org.eclipse.pde.internal.pluginsview;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.PDEPlugin;

public class ParentElement extends PlatformObject {
	private static final String KEY_WORKSPACE_PLUGINS =
		"PluginsView.workspacePlugins";
	private static final String KEY_EXTERNAL_PLUGINS =
		"PluginsView.externalPlugins";
	private static final String KEY_PLUGIN_PROFILES = "PluginsView.pluginProfiles";

	public static final int WORKSPACE_PLUGINS = 1;
	public static final int EXTERNAL_PLUGINS = 2;
	public static final int PLUGIN_PROFILES = 3;
	private int id;
	private String name;

	public ParentElement(int id) {
		this.id = id;
		switch (id) {
			case WORKSPACE_PLUGINS :
				name = PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS);
				break;
			case EXTERNAL_PLUGINS :
				name = PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS);
				break;
			case PLUGIN_PROFILES :
				name = PDEPlugin.getResourceString(KEY_PLUGIN_PROFILES);
				break;
			default :
				name = "??";
		}
	}

	public int getId() {
		return id;
	}

	public String toString() {
		return name;
	}
}