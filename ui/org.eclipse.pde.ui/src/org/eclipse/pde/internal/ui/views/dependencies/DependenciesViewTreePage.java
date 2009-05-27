/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			super.run();
			fTreeViewer.collapseAll();
		}
	}

	class OptionalFilter extends ViewerFilter {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.view.DependenciesViewPage#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		fTreeViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(true);
		fTreeViewer.setLabelProvider(labelProvider);
		fTreeViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				labelProvider.dispose();
			}
		});
		fTreeViewer.setAutoExpandLevel(2);

		return fTreeViewer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.Page#makeContributions(org.eclipse.jface.action.IMenuManager,
	 *      org.eclipse.jface.action.IToolBarManager,
	 *      org.eclipse.jface.action.IStatusLineManager)
	 */
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		if (toolBarManager.find(DependenciesView.TREE_ACTION_GROUP) != null)
			toolBarManager.prependToGroup(DependenciesView.TREE_ACTION_GROUP, new CollapseAllAction());
		else
			toolBarManager.add(new CollapseAllAction());
	}

	protected void handleShowOptional(boolean isChecked, boolean refreshIfNecessary) {
		if (isChecked)
			fTreeViewer.removeFilter(fHideOptionalFilter);
		else
			fTreeViewer.addFilter(fHideOptionalFilter);
		// filter automatically refreshes tree, therefore can ignore refreshIfNecessary
	}

	protected boolean isShowingOptional() {
		ViewerFilter[] filters = fTreeViewer.getFilters();
		for (int i = 0; i < filters.length; i++)
			if (filters[i].equals(fHideOptionalFilter))
				return false;
		return true;
	}

}
