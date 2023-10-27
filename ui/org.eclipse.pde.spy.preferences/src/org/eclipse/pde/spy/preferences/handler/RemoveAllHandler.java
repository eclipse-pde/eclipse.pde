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

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.pde.spy.preferences.constants.PreferenceSpyEventTopics;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;

import jakarta.inject.Named;

public class RemoveAllHandler {
	@Execute
	public void execute(IEventBroker eventBroker,
			@Optional @Named(IServiceConstants.ACTIVE_SELECTION) List<PreferenceEntry> preferenceEntries) {
		eventBroker.post(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_ENTRIES_DELETE_ALL, preferenceEntries);
	}

}