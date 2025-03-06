/*******************************************************************************
 * Copyright (c) 2012, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.preferences;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.pde.bnd.ui.TeamUtils;

import aQute.bnd.build.Workspace;

public class BndPreferences {

	private static final boolean DEFAULT_PREF_USE_ALIAS_REQUIREMENTS = true;
	private static final boolean DEFAULT_PREF_PARALLEL = false;
	private static final boolean DEFAULT_PREF_WORKSPACE_OFFLINE = false;
	private static final String DEFAULT_PREF_TEMPLATE_REPO_URI_LIST = "https://raw.githubusercontent.com/bndtools/bundle-hub/master/index.xml.gz";
	private static final boolean DEFAULT_PREF_ENABLE_TEMPLATE_REPO = false;
	private static final boolean DEFAULT_PREF_VCS_IGNORES_CREATE = true;
	private static final String DEFAULT_PREF_HEADLESS_BUILD_PLUGINS = "";
	private static final boolean DEFAULT_PREF_HEADLESS_BUILD_CREATE = true;
	private static final boolean DEFAULT_PREF_BUILDBEFORELAUNCH = true;
	private static final boolean DEFAULT_PREF_WARN_EXISTING_LAUNCH = true;
	private static final String DEFAULT_PREF_VCS_IGNORES_PLUGINS = "";
	private static final String DEFAULT_PREF_EXPLORER_PROMPT = "";
	private static final String PREF_ENABLE_SUB_BUNDLES = "enableSubBundles";
	private static final String PREF_NOASK_PACKAGEINFO = "noAskPackageInfo";
	private static final String PREF_USE_ALIAS_REQUIREMENTS = "useAliasRequirements";
	private static final String PREF_HIDE_INITIALISE_CNF_WIZARD = "hideInitialiseCnfWizard";
	private static final String PREF_HIDE_INITIALISE_CNF_ADVICE = "hideInitialiseCnfAdvice";
	private static final String PREF_WARN_EXISTING_LAUNCH = "warnExistingLaunch";
	private static final String PREF_HIDE_WARNING_EXTERNAL_FILE = "hideExternalFileWarning";
	private static final String PREF_BUILD_LOGGING = "buildLogging";
	private static final String PREF_EDITOR_OPEN_SOURCE_TAB = "editorOpenSourceTab";
	private static final String PREF_HEADLESS_BUILD_CREATE = "headlessBuildCreate";
	private static final String PREF_HEADLESS_BUILD_PLUGINS = "headlessBuildPlugins";
	private static final String PREF_VCS_IGNORES_CREATE = "versionControlIgnoresCreate";
	private static final String PREF_VCS_IGNORES_PLUGINS = "versionControlIgnoresPlugins";
	private static final String PREF_BUILDBEFORELAUNCH = "buildBeforeLaunch";
	private static final String PREF_ENABLE_TEMPLATE_REPO = "enableTemplateRepo";
	private static final String PREF_TEMPLATE_REPO_URI_LIST = "templateRepoUriList";
	private static final String PREF_EXPLORER_PROMPT = "prompt";
	private static final String PREF_PARALLEL = "parallel";

	static final String PREF_WORKSPACE_OFFLINE = "workspaceIsOffline";
	private static final boolean DEFAULT_PREF_NOASK_PACKAGEINFO = false;
	private static final boolean DEFAULT_PREF_HIDE_INITIALISE_CNF_WIZARD = false;
	private static final String DEFAULT_PREF_ENABLE_SUB_BUNDLES = null;
	private static final int DEFAULT_PREF_BUILD_LOGGING = 0;
	private static final boolean DEFAULT_PREF_HIDE_INITIALISE_CNF_ADVICE = false;
	private static final boolean DEFAULT_PREF_HIDE_WARNING_EXTERNAL_FILE = false;
	private static final boolean DEFAULT_PREF_EDITOR_OPEN_SOURCE_TAB = false;

	private final IPreferenceStore	store;
	private final IProject project;

	public BndPreferences(IProject project, IPreferenceStore store) {
		this.store = store;
		this.project = project;
		if (store != null) {
			// Defaults...
			store.setDefault(PREF_WARN_EXISTING_LAUNCH, DEFAULT_PREF_WARN_EXISTING_LAUNCH);
			store.setDefault(PREF_BUILDBEFORELAUNCH, DEFAULT_PREF_BUILDBEFORELAUNCH);
			store.setDefault(PREF_HEADLESS_BUILD_CREATE, DEFAULT_PREF_HEADLESS_BUILD_CREATE);
			store.setDefault(PREF_HEADLESS_BUILD_PLUGINS, DEFAULT_PREF_HEADLESS_BUILD_PLUGINS);
			store.setDefault(PREF_VCS_IGNORES_CREATE, DEFAULT_PREF_VCS_IGNORES_CREATE);
			store.setDefault(PREF_VCS_IGNORES_PLUGINS, DEFAULT_PREF_VCS_IGNORES_PLUGINS);
			store.setDefault(PREF_ENABLE_TEMPLATE_REPO, DEFAULT_PREF_ENABLE_TEMPLATE_REPO);
			store.setDefault(PREF_TEMPLATE_REPO_URI_LIST,
					DEFAULT_PREF_TEMPLATE_REPO_URI_LIST);
			store.setDefault(PREF_WORKSPACE_OFFLINE, DEFAULT_PREF_WORKSPACE_OFFLINE);
			store.setDefault(PREF_PARALLEL, DEFAULT_PREF_PARALLEL);
			store.setDefault(PREF_USE_ALIAS_REQUIREMENTS, DEFAULT_PREF_USE_ALIAS_REQUIREMENTS);
			store.setDefault(QuickFixVersioning.PREFERENCE_KEY, QuickFixVersioning.DEFAULT.toString());
			store.setDefault(PREF_EXPLORER_PROMPT, DEFAULT_PREF_EXPLORER_PROMPT);
		}
	}

	private String mapToPreference(Map<String, Boolean> names) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Boolean> nameEntry : names.entrySet()) {
			if (nameEntry.getValue()
				.booleanValue()) {
				if (sb.length() > 0) {
					sb.append("|");
				}
				sb.append(nameEntry.getKey());
			}
		}
		return sb.toString();
	}

	private Map<String, Boolean> preferenceToMap(String preference,
		Collection<? extends NamedPlugin> allPluginsInformation, boolean onlyEnabled) {
		List<String> names = null;
		if (preference != null && !preference.isEmpty()) {
			names = Arrays.asList(preference.split("\\|"));
		}

		boolean atLeastOneEnabled = false;

		Map<String, Boolean> map = new TreeMap<>();
		for (NamedPlugin info : allPluginsInformation) {
			boolean enabled = (names == null) ? (info.isEnabledByDefault() && !info.isDeprecated())
				: names.contains(info.getName());
			map.put(info.getName(), enabled);
			atLeastOneEnabled = atLeastOneEnabled || enabled;
		}

		if (!atLeastOneEnabled && (map.size() > 0)) {
			for (String name : map.keySet()) {
				map.put(name, Boolean.TRUE);
			}
		}

		if (onlyEnabled) {
			Set<String> pluginsToRemove = new HashSet<>();
			for (Map.Entry<String, Boolean> entry : map.entrySet()) {
				if (!entry.getValue()
					.booleanValue()) {
					pluginsToRemove.add(entry.getKey());
				}
			}
			for (String plugin : pluginsToRemove) {
				map.remove(plugin);
			}
		}

		return map;
	}

	public void setNoAskPackageInfo(boolean noAskPackageInfo) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_NOASK_PACKAGEINFO, noAskPackageInfo);
	}

	public void setUseAliasRequirements(boolean useAliases) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_USE_ALIAS_REQUIREMENTS, useAliases);
	}

	public boolean getNoAskPackageInfo() {
		if (store == null) {
			return DEFAULT_PREF_NOASK_PACKAGEINFO;
		}
		return store.getBoolean(PREF_NOASK_PACKAGEINFO);
	}

	public boolean getUseAliasRequirements() {
		if (store == null) {
			return DEFAULT_PREF_USE_ALIAS_REQUIREMENTS;
		}
		return store.getBoolean(PREF_USE_ALIAS_REQUIREMENTS);
	}

	public void setHideInitCnfWizard(boolean hide) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_HIDE_INITIALISE_CNF_WIZARD, hide);
	}

	public boolean getHideInitCnfWizard() {
		if (store == null) {
			return DEFAULT_PREF_HIDE_INITIALISE_CNF_WIZARD;
		}
		return store.getBoolean(PREF_HIDE_INITIALISE_CNF_WIZARD);
	}

	public void setWarnExistingLaunch(boolean warnExistingLaunch) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_WARN_EXISTING_LAUNCH, warnExistingLaunch);
	}

	public boolean getWarnExistingLaunches() {
		if (store == null) {
			return DEFAULT_PREF_WARN_EXISTING_LAUNCH;
		}
		return store.getBoolean(PREF_WARN_EXISTING_LAUNCH);
	}

	public void setEnableSubBundles(String enableSubs) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_ENABLE_SUB_BUNDLES, enableSubs);
	}

	public String getEnableSubBundles() {
		if (store == null) {
			return DEFAULT_PREF_ENABLE_SUB_BUNDLES;
		}
		return store.getString(PREF_ENABLE_SUB_BUNDLES);
	}

	public void setBuildLogging(int buildLogging) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_BUILD_LOGGING, buildLogging);
	}

	public int getBuildLogging() {
		if (store == null) {
			return DEFAULT_PREF_BUILD_LOGGING;
		}
		return store.getInt(PREF_BUILD_LOGGING);
	}

	public void setHideInitCnfAdvice(boolean hide) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_HIDE_INITIALISE_CNF_ADVICE, hide);
	}

	public boolean getHideInitCnfAdvice() {
		if (store == null) {
			return DEFAULT_PREF_HIDE_INITIALISE_CNF_ADVICE;
		}
		return store.getBoolean(PREF_HIDE_INITIALISE_CNF_ADVICE);
	}

	public void setHideWarningExternalFile(boolean hide) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_HIDE_WARNING_EXTERNAL_FILE, hide);
	}

	public boolean getHideWarningExternalFile() {
		if (store == null) {
			return DEFAULT_PREF_HIDE_WARNING_EXTERNAL_FILE;
		}
		return store.getBoolean(PREF_HIDE_WARNING_EXTERNAL_FILE);
	}

	public boolean getEnableTemplateRepo() {
		if (store == null) {
			return DEFAULT_PREF_ENABLE_TEMPLATE_REPO;
		}
		return store.getBoolean(PREF_ENABLE_TEMPLATE_REPO);
	}

	public void setEnableTemplateRepo(boolean enable) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_ENABLE_TEMPLATE_REPO, enable);
	}

	public List<String> getTemplateRepoUriList() {
		if (store == null) {
			return List.of(DEFAULT_PREF_TEMPLATE_REPO_URI_LIST);
		}
		String urisStr = store.getString(PREF_TEMPLATE_REPO_URI_LIST);
		return Arrays.asList(urisStr.split("\\s"));
	}

	public void setTemplateRepoUriList(List<String> uris) {
		if (store == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> i = uris.iterator(); i.hasNext();) {
			sb.append(i.next());
			if (i.hasNext()) {
				sb.append(' ');
			}
		}
		store.setValue(PREF_TEMPLATE_REPO_URI_LIST, sb.toString());
	}

	public IPreferenceStore getStore() {
		return store;
	}

	public void setEditorOpenSourceTab(boolean editorOpenSourceTab) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_EDITOR_OPEN_SOURCE_TAB, editorOpenSourceTab);
	}

	public boolean getEditorOpenSourceTab() {
		if (store == null) {
			return DEFAULT_PREF_EDITOR_OPEN_SOURCE_TAB;
		}
		return store.getBoolean(PREF_EDITOR_OPEN_SOURCE_TAB);
	}

	public void setHeadlessBuildCreate(boolean headlessCreate) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_HEADLESS_BUILD_CREATE, headlessCreate);
	}

	public boolean getHeadlessBuildCreate() {
		if (store == null) {
			return DEFAULT_PREF_HEADLESS_BUILD_CREATE;
		}
		return store.getBoolean(PREF_HEADLESS_BUILD_CREATE);
	}

	public void setHeadlessBuildPlugins(Map<String, Boolean> names) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_HEADLESS_BUILD_PLUGINS, mapToPreference(names));
	}

	public Map<String, Boolean> getHeadlessBuildPlugins(Collection<? extends NamedPlugin> allPluginsInformation,
		boolean onlyEnabled) {
		if (!getHeadlessBuildCreate() || store == null) {
			return Collections.emptyMap();
		}

		return preferenceToMap(store.getString(PREF_HEADLESS_BUILD_PLUGINS), allPluginsInformation, onlyEnabled);
	}

	/**
	 * Return the enabled headless build plugins.
	 * <ul>
	 * <li>When plugins is not null and not empty then plugins itself is
	 * returned</li>
	 * <li>Otherwise this method determines from the preferences which plugins
	 * are enabled</li>
	 * </ul>
	 *
	 * @param manager the headless build manager
	 * @param plugins the plugins, can be null or empty.
	 * @return the enabled plugins
	 */
	public Set<String> getHeadlessBuildPluginsEnabled(HeadlessBuildManager manager, Set<String> plugins) {
		if (plugins != null && !plugins.isEmpty()) {
			return plugins;
		}

		return getHeadlessBuildPlugins(manager.getAllPluginsInformation(), true).keySet();
	}

	public void setVersionControlIgnoresCreate(boolean versionControlIgnoresCreate) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_VCS_IGNORES_CREATE, versionControlIgnoresCreate);
	}

	public boolean getVersionControlIgnoresCreate() {
		if (store == null) {
			return DEFAULT_PREF_VCS_IGNORES_CREATE;
		}
		return store.getBoolean(PREF_VCS_IGNORES_CREATE);
	}

	public void setVersionControlIgnoresPlugins(Map<String, Boolean> names) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_VCS_IGNORES_PLUGINS, mapToPreference(names));
	}

	public Map<String, Boolean> getVersionControlIgnoresPlugins(Collection<? extends NamedPlugin> allPluginsInformation,
		boolean onlyEnabled) {
		if (!getVersionControlIgnoresCreate() || store == null) {
			return Collections.emptyMap();
		}

		return preferenceToMap(store.getString(PREF_VCS_IGNORES_PLUGINS), allPluginsInformation, onlyEnabled);
	}

	public void setPrompt(String prompt) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_EXPLORER_PROMPT, prompt);
	}

	public String getPrompt() {
		if (store == null) {
			return DEFAULT_PREF_EXPLORER_PROMPT;
		}
		return store.getString(PREF_EXPLORER_PROMPT);
	}

	public Closeable onPrompt(Consumer<String> listener) {
		return onString(PREF_EXPLORER_PROMPT, listener);
	}

	public void setParallel(boolean parallel) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_PARALLEL, parallel);
	}

	public boolean isParallel() {
		if (store == null) {
			return DEFAULT_PREF_PARALLEL;
		}
		return store.getBoolean(PREF_PARALLEL);
	}


	public Closeable onString(String key, Consumer<String> listener) {
		if (store == null) {
			return () -> {
			};
		}
		IPropertyChangeListener l = e -> {
			if (e.getProperty()
				.equals(key)) {
				listener.accept((String) e.getNewValue());
			}
		};
		addPropertyChangeListener(l);
		listener.accept(store.getString(key));
		return () -> removePropertyChangeListener(l);
	}

	/**
	 * Return the enabled version control ignores plugins.
	 * <ul>
	 * <li>When plugins is not null and not empty then plugins itself is
	 * returned</li>
	 * <li>Otherwise, when the files in the project are already managed by a
	 * version control system, this method tries to detect which plugins can
	 * apply ignores for the version control system</li>
	 * <li>Otherwise this method determines from the preferences which plugins
	 * are enabled</li>
	 * </ul>
	 *
	 * @param manager the version control ignores manager
	 * @param project the project (can be null to ignore it)
	 * @param plugins the plugins, can be null or empty.
	 * @return the enabled plugins
	 */
	public Set<String> getVersionControlIgnoresPluginsEnabled(VersionControlIgnoresManager manager,
		IJavaProject project, Set<String> plugins) {
		if (plugins != null && !plugins.isEmpty()) {
			return plugins;
		}

		if (project != null) {
			String repositoryProviderId = TeamUtils.getProjectRepositoryProviderId(project);
			if (repositoryProviderId != null) {
				VersionControlIgnoresManager versionControlIgnoresManager = Adapters.adapt(project.getProject(),
						VersionControlIgnoresManager.class);
				if (versionControlIgnoresManager != null) {
					Set<String> managingPlugins = versionControlIgnoresManager
							.getPluginsForProjectRepositoryProviderId(repositoryProviderId);
					if (managingPlugins != null && !managingPlugins.isEmpty()) {
						return managingPlugins;
					}
				}
			}
		}

		return getVersionControlIgnoresPlugins(manager.getAllPluginsInformation(), true).keySet();
	}

	public boolean getBuildBeforeLaunch() {
		if (store == null) {
			return DEFAULT_PREF_BUILDBEFORELAUNCH;
		}
		return store.getBoolean(PREF_BUILDBEFORELAUNCH);
	}

	public void setBuildBeforeLaunch(boolean b) {
		if (store == null) {
			return;
		}
		store.setValue(PREF_BUILDBEFORELAUNCH, b);
	}

	public QuickFixVersioning getQuickFixVersioning() {
		if (store == null) {
			return QuickFixVersioning.DEFAULT;
		}
		return QuickFixVersioning.parse(store.getString(QuickFixVersioning.PREFERENCE_KEY));
	}

	public void setQuickFixVersioning(QuickFixVersioning qfv) {
		if (store == null) {
			return;
		}
		if (qfv == null) {
			qfv = QuickFixVersioning.DEFAULT;
		}
		store.setValue(QuickFixVersioning.PREFERENCE_KEY, qfv.toString());
	}

	public boolean isWorkspaceOffline() {
		if (store == null) {
			return DEFAULT_PREF_WORKSPACE_OFFLINE;
		}
		return store.getBoolean(PREF_WORKSPACE_OFFLINE);
	}

	public void setWorkspaceOffline(boolean b) {
		Workspace workspace = Adapters.adapt(project, Workspace.class);
		if (workspace != null) {
			workspace.setOffline(b);
		}
		if (store == null) {
			return;
		}
		store.setValue(PREF_WORKSPACE_OFFLINE, b);
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		store.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		store.removePropertyChangeListener(listener);
	}

	@Override
	public int hashCode() {
		return Objects.hash(project, store);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BndPreferences other = (BndPreferences) obj;
		return Objects.equals(project, other.project) && Objects.equals(store, other.store);
	}
}
