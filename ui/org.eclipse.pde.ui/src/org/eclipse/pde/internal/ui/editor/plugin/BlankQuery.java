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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.search.SearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public class BlankQuery implements ISearchQuery {

	private PackageObject fObject;

	BlankQuery(PackageObject object) {
		fObject = object;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		monitor.done();
		return new Status(IStatus.OK, IPDEUIConstants.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

	@Override
	public String getLabel() {
		return '\'' + fObject.getName() + '\'';
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
		return new SearchResult(this);
	}

}
