/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.pde.spy.preferences.constants.PreferenceSpyEventTopics;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.pde.spy.preferences.model.PreferenceNodeEntry;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;

public class ShowAllPreferencesHandler {
	@Execute
	public void execute(Shell shell, IEventBroker eventBroker) {
		Map<String, PreferenceNodeEntry> preferenceEntries = new HashMap<>();
		IEclipsePreferences bundleDefaultsScopePreferences = BundleDefaultsScope.INSTANCE.getNode("");
		IEclipsePreferences configurationScopePreferences = ConfigurationScope.INSTANCE.getNode("");
		IEclipsePreferences defaultScopePreferences = DefaultScope.INSTANCE.getNode("");
		IEclipsePreferences instanceScopePreferences = InstanceScope.INSTANCE.getNode("");
		try {
			bundleDefaultsScopePreferences.accept(new PrefereneGatherer(preferenceEntries));
			configurationScopePreferences.accept(new PrefereneGatherer(preferenceEntries));
			defaultScopePreferences.accept(new PrefereneGatherer(preferenceEntries));
			instanceScopePreferences.accept(new PrefereneGatherer(preferenceEntries));
		} catch (BackingStoreException e) {
			ErrorDialog.openError(shell, "BackingStoreException", e.getLocalizedMessage(),
					Status.error(e.getMessage()));
		}
		eventBroker.post(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_SHOW, preferenceEntries.values());
	}

	private class PrefereneGatherer implements IPreferenceNodeVisitor {

		private final Map<String, PreferenceNodeEntry> preferenceEntries;

		public PrefereneGatherer(Map<String, PreferenceNodeEntry> preferenceEntries) {
			this.preferenceEntries = preferenceEntries;
		}

		@Override
		public boolean visit(IEclipsePreferences node) throws BackingStoreException {
			// only show nodes, which have changed keys
			String[] keys = node.keys();
			if (keys.length <= 0) {
				return true;
			}
			PreferenceNodeEntry preferenceNodeEntry = preferenceEntries.get(node.absolutePath());
			if (null == preferenceNodeEntry) {
				preferenceNodeEntry = new PreferenceNodeEntry(node.absolutePath());
				preferenceEntries.put(node.absolutePath(), preferenceNodeEntry);
			}
			for (String key : keys) {
				String value = node.get(key, "*default*");
				PreferenceEntry preferenceEntry = new PreferenceEntry(node.absolutePath(), key, value, value);
				preferenceNodeEntry.addChildren(preferenceEntry);
			}
			return true;
		}
	}

}