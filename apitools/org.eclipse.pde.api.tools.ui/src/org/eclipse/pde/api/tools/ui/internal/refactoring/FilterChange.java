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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Generic {@link Change} for {@link IApiProblemFilter}s
 * 
 * @since 1.0.1
 */
public abstract class FilterChange extends Change {
	
	static final int DELETE = 1;
	static final int ADD = 2;
	
	IApiFilterStore store = null;
	IApiProblemFilter filter = null;
	int kind = 0;
	
	/**
	 * Constructor
	 */
	public FilterChange(IApiFilterStore store, IApiProblemFilter filter, int kind) {
		this.store = store;
		this.filter = filter;
		this.kind = kind;
	}
	
	/**
	 * Performs the delete operation and returns an undo change or <code>null</code>
	 * @return an undo change or <code>null</code>
	 */
	protected abstract Change performDelete();
	
	/**
	 * Performs the add operation and returns an undo change or <code>null</code>
	 * @return an undo change or <code>null</code>
	 */
	protected abstract Change performAdd();
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		switch(this.kind) {
			case DELETE: {
				return performDelete();
			}
			case ADD: {
				return performAdd();
			}
		}
		return null;
	}
	
	/**
	 * Returns the name to use for an {@link #ADD} change operation
	 * @return the name for an {@link #ADD} change
	 */
	protected String getAddName() {
		return NLS.bind(RefactoringMessages.FilterChange_add_filter, this.filter.toString());
	}
	
	/**
	 * Returns the name to use for a {@link #DELETE} change operation
	 * @return the name for a {@link #DELETE} change
	 */
	protected String getDeleteName() {
		IApiProblem problem = this.filter.getUnderlyingProblem();
		return NLS.bind(RefactoringMessages.FilterChange_remove_used_filter, problem.getMessage());
	}
	
	/**
	 * Returns the name to use for a {@link #RENAME} change operation
	 * @return the name for a {@link #RENAME} change
	 */
	protected String getRenameName() {
		IApiProblem problem = this.filter.getUnderlyingProblem();
		return NLS.bind(RefactoringMessages.FilterChange_remove_used_filter, new Object[] {problem.getMessage()}); 
	}
	
	/**
	 * Returns the name to use for a {@link #MOVE} change operation
	 * @return the name for a {@link #MOVE} change
	 */
	protected String getMoveName() {
		IApiProblem problem = this.filter.getUnderlyingProblem();
		return NLS.bind(RefactoringMessages.FilterChange_remove_used_filter, new Object[] {problem.getMessage()});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getName()
	 */
	public String getName() {
		switch(this.kind) {
			case ADD: {
				return getAddName();
			}
			case DELETE: {
				return getDeleteName();
			}
		} 
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void initializeValidationData(IProgressMonitor pm) {}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
	 */
	public Object getModifiedElement() {
		return this.filter;
	}
}
