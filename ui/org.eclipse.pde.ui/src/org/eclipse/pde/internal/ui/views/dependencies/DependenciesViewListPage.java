/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class DependenciesViewListPage extends DependenciesViewPage {

	/**
	 *
	 */
	public DependenciesViewListPage(DependenciesView view, IContentProvider contentProvider) {
		super(view, contentProvider);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		fViewer = new TableViewer(table);
		fViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(false);
		fViewer.setLabelProvider(labelProvider);
		fViewer.getControl().addDisposeListener(e -> labelProvider.dispose());

		return fViewer;
	}

	@Override
	protected void handleShowOptional(boolean isChecked, boolean refreshIfNecessary) {
		if (fContentProvider instanceof CalleesListContentProvider) {
			((CalleesListContentProvider) fContentProvider).setShowOptional(isChecked);
			if (refreshIfNecessary)
				fViewer.refresh();
		}
	}

	@Override
	protected boolean isShowingOptional() {
		if (fContentProvider instanceof CalleesListContentProvider) {
			return ((CalleesListContentProvider) fContentProvider).getShowOptional();
		}
		return true;
	}
}
