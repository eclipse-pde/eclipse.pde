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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;

public abstract class BaseSearchAction extends Action {

	public BaseSearchAction(String text) {
		setText(text);
	}

	@Override
	public void run() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(createSearchQuery());
	}

	protected abstract ISearchQuery createSearchQuery();

}
