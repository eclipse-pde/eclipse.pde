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
package org.eclipse.pde.spy.preferences.addon;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.pde.spy.preferences.constants.PreferenceConstants;
import org.eclipse.pde.spy.preferences.constants.PreferenceSpyEventTopics;
import org.osgi.service.prefs.BackingStoreException;

import jakarta.inject.Inject;

/**
 * This model addon is used to register an IPreferenceChangeListener for all
 * {@link EclipsePreferences} and it fires an
 * {@link PreferenceSpyEventTopics#PREFERENCESPY_PREFERENCE_CHANGED} event via
 * the {@link IEventBroker}.<br/>
 * The Object, which is send within the
 * {@link PreferenceSpyEventTopics#PREFERENCESPY_PREFERENCE_CHANGED} event is a
 * PreferenceChangeEvent.
 */
@SuppressWarnings("restriction")
public class PreferenceSpyAddon {

	@Inject
	private Logger LOG;

	@Inject
	private IEventBroker eventBroker;

	private final IEclipsePreferences bundleDefaultsScopePreferences = BundleDefaultsScope.INSTANCE.getNode("");
	private final IEclipsePreferences configurationScopePreferences = ConfigurationScope.INSTANCE.getNode("");
	private final IEclipsePreferences defaultScopePreferences = DefaultScope.INSTANCE.getNode("");
	private final IEclipsePreferences instanceScopePreferences = InstanceScope.INSTANCE.getNode("");

	private final ChangedPreferenceListener preferenceChangedListener = new ChangedPreferenceListener();

	@Inject
	@Optional
	public void initialzePreferenceSpy(
			@Preference(value = PreferenceConstants.TRACE_PREFERENCES) boolean tracePreferences) {
		if (tracePreferences) {
			registerVisitors();
		} else {
			deregisterVisitors();
		}
	}

	private void registerVisitors() {
		addPreferenceListener(bundleDefaultsScopePreferences);
		addPreferenceListener(configurationScopePreferences);
		addPreferenceListener(defaultScopePreferences);
		addPreferenceListener(instanceScopePreferences);
	}

	private void addPreferenceListener(IEclipsePreferences rootPreference) {
		try {
			rootPreference.accept(node -> {
				node.addPreferenceChangeListener(preferenceChangedListener);
				return true;
			});
		} catch (BackingStoreException e) {
			LOG.error(e);
		}
	}

	private void deregisterVisitors() {
		removePreferenceListener(bundleDefaultsScopePreferences);
		removePreferenceListener(configurationScopePreferences);
		removePreferenceListener(defaultScopePreferences);
		removePreferenceListener(instanceScopePreferences);
	}

	private void removePreferenceListener(IEclipsePreferences rootPreference) {
		try {
			rootPreference.accept(node -> {
				node.removePreferenceChangeListener(preferenceChangedListener);
				return true;
			});
		} catch (BackingStoreException e) {
			LOG.error(e);
		}
	}

	private final class ChangedPreferenceListener implements IPreferenceChangeListener {
		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			eventBroker.post(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_CHANGED, event);
		}
	}
}
