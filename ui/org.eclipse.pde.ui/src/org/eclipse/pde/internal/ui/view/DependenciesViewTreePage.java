/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

public class DependenciesViewTreePage extends DependenciesViewPage {
	class CollapseAllAction extends Action {
		public CollapseAllAction() {
			super();
			setText(PDEPlugin
					.getResourceString("DependenciesViewTreePage.CollapseAllAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesViewTreePage.CollapseAllAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesViewTreePage.CollapseAllAction.tooltip")); //$NON-NLS-1$
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

	ITreeContentProvider fContentProvider;

	TreeViewer fTreeViewer;

	public DependenciesViewTreePage(DependenciesView view,
			ITreeContentProvider contentProvider) {
		super(view);
		fContentProvider = contentProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.view.DependenciesViewPage#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL
				| SWT.H_SCROLL);
		fTreeViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(
				true);
		fTreeViewer.setLabelProvider(labelProvider);
		fTreeViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				labelProvider.dispose();
			}
		});
		fTreeViewer.setSorter(ListUtil.PLUGIN_SORTER);
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
	public void makeContributions(IMenuManager menuManager,
			IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		if (toolBarManager.find(DependenciesView.TREE_ACTION_GROUP) != null)
			toolBarManager.prependToGroup(DependenciesView.TREE_ACTION_GROUP,
					new CollapseAllAction());
		else
			toolBarManager.add(new CollapseAllAction());
	}
}
