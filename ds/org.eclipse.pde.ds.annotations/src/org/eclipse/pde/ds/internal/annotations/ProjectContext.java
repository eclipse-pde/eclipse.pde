/*******************************************************************************
 * Copyright (c) 2012, 2016 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Collection;
import java.util.HashSet;

public class ProjectContext {

	private final ProjectState state;

	// DS files abandoned since last run
	private final Collection<String> abandoned = new HashSet<>();

	// CUs not processed in this run
	private final Collection<String> unprocessed;

	private final ProjectState oldState;

	public ProjectContext(ProjectState state) {
		this.state = state;

		// track unprocessed CUs from the start
		unprocessed = new HashSet<>(state.getCompilationUnits());

		// clone existing state so later we can determine if changed
		oldState = state.clone();
	}

	public boolean isChanged() {
		return !oldState.equals(state);
	}

	public ProjectState getState() {
		return state;
	}

	public Collection<String> getAbandoned() {
		return abandoned;
	}

	public Collection<String> getUnprocessed() {
		return unprocessed;
	}
}