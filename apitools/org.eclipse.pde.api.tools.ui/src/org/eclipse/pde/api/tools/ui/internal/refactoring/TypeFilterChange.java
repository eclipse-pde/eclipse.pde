/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * A change object for {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}s
 * This change can handle adds / renames and removes.
 * 
 * @since 1.0.1
 */
public class TypeFilterChange extends FilterChange {
	
	String newname = null;
	String newpath = null;
	
	/**
	 * Constructor
	 * @param store the store the filter is to be updated in
	 * @param filter the filter being changed
	 * @param primaryname the name of the primary type (the name of the resource)
	 * @param newname the new value to set in the filter
	 * @param newpath
	 * @param kind the kind of the change
	 */
	public TypeFilterChange(IApiFilterStore store, IApiProblemFilter filter, String newname, String newpath, int kind) {
		super(store, filter, kind);
		this.newname = newname;
		this.newpath = newpath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.internal.refactoring.FilterChange#performAdd()
	 */
	protected Change performAdd() {
		this.store.addFilters(new IApiProblemFilter[] {this.filter});
		return new TypeFilterChange(this.store, this.filter, null, null, DELETE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.ui.internal.refactoring.FilterChange#performDelete()
	 */
	protected Change performDelete() {
		if(this.store.removeFilters(new IApiProblemFilter[] {this.filter})) {
			return new TypeFilterChange(this.store, this.filter, null, null, ADD);
		}
		return null;
	}
}
