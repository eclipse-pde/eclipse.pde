/*******************************************************************************
 *  Copyright (c) 2007, 2025 IBM Corporation and others.
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

import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTable;
import org.eclipse.e4.ui.dialogs.filteredtree.PatternFilter;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class DependenciesViewListPage extends DependenciesViewPage {
	private FilteredTable fFilteredViewer;

	public DependenciesViewListPage(DependenciesView view, IContentProvider contentProvider) {
		super(view, contentProvider);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		fFilteredViewer = new FilteredTable(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, new PatternFilter());

		fViewer = fFilteredViewer.getViewer();
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
			if (refreshIfNecessary) {
				fViewer.refresh();
			}
		}
	}

	@Override
	protected boolean isShowingOptional() {
		if (fContentProvider instanceof CalleesListContentProvider) {
			return ((CalleesListContentProvider) fContentProvider).getShowOptional();
		}
		return true;
	}

	@Override
	public Control getControl() {
		return fFilteredViewer;
	}
}
