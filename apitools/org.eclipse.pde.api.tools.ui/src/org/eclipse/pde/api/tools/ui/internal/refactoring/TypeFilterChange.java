/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * A change object for
 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
 * s This change can handle adds / renames and removes.
 *
 * @since 1.0.1
 */
public class TypeFilterChange extends FilterChange {

	String newname = null;
	String newpath = null;

	/**
	 * Constructor
	 *
	 * @param store the store the filter is to be updated in
	 * @param filter the filter being changed
	 * @param primaryname the name of the primary type (the name of the
	 *            resource)
	 * @param newname the new value to set in the filter
	 * @param newpath
	 * @param kind the kind of the change
	 */
	public TypeFilterChange(IApiFilterStore store, IApiProblemFilter filter, String newname, String newpath, int kind) {
		super(store, filter, kind);
		this.newname = newname;
		this.newpath = newpath;
	}

	@Override
	protected Change performAdd() {
		this.store.addFilters(new IApiProblemFilter[] { this.filter });
		return new TypeFilterChange(this.store, this.filter, null, null, DELETE);
	}

	@Override
	protected Change performDelete() {
		if (this.store.removeFilters(new IApiProblemFilter[] { this.filter })) {
			return new TypeFilterChange(this.store, this.filter, null, null, ADD);
		}
		return null;
	}
}
