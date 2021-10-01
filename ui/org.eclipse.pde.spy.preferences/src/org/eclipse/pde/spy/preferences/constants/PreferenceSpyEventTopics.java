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
package org.eclipse.pde.spy.preferences.constants;

public interface PreferenceSpyEventTopics {

	public static final String PREFERENCESPY_PREFERENCE_ENTRIES_DELETE_ALL = "TOPIC_PREFERENCESPY/PREFERENCE_ENTRIES/DELETE_ALL";
	public static final String PREFERENCESPY_PREFERENCE_ENTRIES_DELETE = "TOPIC_PREFERENCESPY/PREFERENCE_ENTRIES/DELETE";

	public static final String PREFERENCESPY_PREFERENCE_CHANGED = "TOPIC_PREFERENCESPY/PREFERENCE/CHANGED";
	public static final String PREFERENCESPY_PREFERENCE_SHOW = "TOPIC_PREFERENCESPY/PREFERENCE/SHOW";
}
