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
		IPreferenceNodeVisitor gatherer = node -> {
			// only show nodes, which have changed keys
			String[] keys = node.keys();
			if (keys.length <= 0) {
				return true;
			}
			PreferenceNodeEntry preferenceNodeEntry = preferenceEntries.computeIfAbsent(node.absolutePath(),
					PreferenceNodeEntry::new);
			for (String key : keys) {
				String value = node.get(key, "*default*");
				preferenceNodeEntry.addChildren(new PreferenceEntry(node.absolutePath(), key, value, value));
			}
			return true;
		};
		try {
			BundleDefaultsScope.INSTANCE.getNode("").accept(gatherer);
			ConfigurationScope.INSTANCE.getNode("").accept(gatherer);
			DefaultScope.INSTANCE.getNode("").accept(gatherer);
			InstanceScope.INSTANCE.getNode("").accept(gatherer);
		} catch (BackingStoreException e) {
			ErrorDialog.openError(shell, "BackingStoreException", e.getLocalizedMessage(),
					Status.error(e.getMessage()));
		}
		eventBroker.post(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_SHOW, preferenceEntries.values());
	}

}