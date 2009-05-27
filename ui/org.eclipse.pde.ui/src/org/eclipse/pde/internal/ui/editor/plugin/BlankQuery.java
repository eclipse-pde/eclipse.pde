/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		monitor.done();
		return new Status(IStatus.OK, IPDEUIConstants.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

	public String getLabel() {
		return '\'' + fObject.getName() + '\'';
	}

	public boolean canRerun() {
		return true;
	}

	public boolean canRunInBackground() {
		return true;
	}

	public ISearchResult getSearchResult() {
		return new SearchResult(this);
	}

}
