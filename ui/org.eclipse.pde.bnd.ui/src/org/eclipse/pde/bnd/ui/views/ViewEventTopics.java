/*******************************************************************************
 * Copyright (c) 2023, 2023 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Rueger <chrisrueger@gmail.com> - initial API and implementation
*******************************************************************************/
package org.eclipse.pde.bnd.ui.views;

/**
 * Topics for the EventBroker which is used for communication between different
 * views. For example if View1 sends an event to View2 wants to open a dialog in
 * the other view. .
 */
public enum ViewEventTopics {

	/**
	 * Event to open the advances search of the repositories view.
	 */
	REPOSITORIESVIEW_OPEN_ADVANCED_SEARCH("EVENT/RepositoriesView/openAdvancedSearch");

	private final String eventtype;

	ViewEventTopics(String eventtype) {
		this.eventtype = eventtype;
	}

	public String topic() {
		return eventtype;
	}

	@Override
	public String toString() {
		return eventtype;
	}

}
