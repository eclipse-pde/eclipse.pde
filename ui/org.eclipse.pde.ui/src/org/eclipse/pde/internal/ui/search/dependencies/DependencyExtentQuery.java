/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jdt.core.IPackageFragmentRoot;

import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

public class DependencyExtentQuery implements ISearchQuery {

	private ISearchResult fSearchResult;
	private IProject fProject;
	private String fImportID;

	public DependencyExtentQuery(IProject project, String importID) {
		fProject = project;
		fImportID = importID;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		final AbstractTextSearchResult result = (AbstractTextSearchResult) getSearchResult();
		result.removeAll();
		DependencyExtentOperation op = new DependencyExtentOperation(fProject, fImportID, result);
		op.execute(monitor);
		return Status.OK_STATUS;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.DependencyExtentQuery_label + " " + fImportID; //$NON-NLS-1$
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (fSearchResult == null)
			fSearchResult = new DependencyExtentSearchResult(this);
		return fSearchResult;
	}

	public IPackageFragmentRoot[] getDirectRoots() {
		ArrayList<IPackageFragmentRoot> result = new ArrayList<>();
		try {
			IPackageFragmentRoot[] roots = JavaCore.create(fProject).getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE || (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return result.toArray(new IPackageFragmentRoot[result.size()]);
	}

}
