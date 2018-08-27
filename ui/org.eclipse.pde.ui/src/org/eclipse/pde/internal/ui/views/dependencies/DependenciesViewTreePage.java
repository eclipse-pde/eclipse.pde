/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.Constants;

public class DependenciesViewTreePage extends DependenciesViewPage {
	class CollapseAllAction extends Action {
		public CollapseAllAction() {
			super();
			setText(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_label);
			setDescription(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_description);
			setToolTipText(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
			setDisabledImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		}

		@Override
		public void run() {
			super.run();
			fTreeViewer.collapseAll();
		}
	}

	class OptionalFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof BundleSpecification) {
				return !((BundleSpecification) element).isOptional();
			} else if (element instanceof ImportPackageSpecification)
				return !Constants.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) element).getDirective(Constants.RESOLUTION_DIRECTIVE));
			return true;
		}
	}

	TreeViewer fTreeViewer;
	private OptionalFilter fHideOptionalFilter = new OptionalFilter();

	public DependenciesViewTreePage(DependenciesView view, ITreeContentProvider contentProvider) {
		super(view, contentProvider);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		fTreeViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(true);
		fTreeViewer.setLabelProvider(labelProvider);
		fTreeViewer.getControl().addDisposeListener(e -> labelProvider.dispose());
		fTreeViewer.setAutoExpandLevel(2);

		return fTreeViewer;
	}

	@Override
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		if (toolBarManager.find(DependenciesView.TREE_ACTION_GROUP) != null)
			toolBarManager.prependToGroup(DependenciesView.TREE_ACTION_GROUP, new CollapseAllAction());
		else
			toolBarManager.add(new CollapseAllAction());
	}

	@Override
	protected void handleShowOptional(boolean isChecked, boolean refreshIfNecessary) {
		if (isChecked)
			fTreeViewer.removeFilter(fHideOptionalFilter);
		else
			fTreeViewer.addFilter(fHideOptionalFilter);
		// filter automatically refreshes tree, therefore can ignore refreshIfNecessary
	}

	@Override
	protected boolean isShowingOptional() {
		ViewerFilter[] filters = fTreeViewer.getFilters();
		for (ViewerFilter filter : filters)
			if (filter.equals(fHideOptionalFilter))
				return false;
		return true;
	}

}
