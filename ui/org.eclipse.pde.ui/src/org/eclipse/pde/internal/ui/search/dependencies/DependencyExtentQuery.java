/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.search.ui.*;
import org.eclipse.search.ui.text.*;


public class DependencyExtentQuery implements ISearchQuery {
	
	private ISearchResult fSearchResult;
	private IProject fProject;
	private String fImportID;
	
	public DependencyExtentQuery(IProject project, String importID) {
		fProject = project;
		fImportID = importID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) {
		final AbstractTextSearchResult result = (AbstractTextSearchResult) getSearchResult();
		result.removeAll();
		DependencyExtentOperation op = new DependencyExtentOperation(fProject, fImportID, result);
		op.execute(monitor);
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel() {
		return PDEPlugin.getResourceString("DependencyExtentQuery.label") + " " + fImportID; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	public boolean canRerun() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	public boolean canRunInBackground() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	public ISearchResult getSearchResult() {
		if (fSearchResult == null)
			fSearchResult = new DependencyExtentSearchResult(this);
		return fSearchResult;
	}

}
