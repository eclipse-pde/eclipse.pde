/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class DependenciesViewListPage extends DependenciesViewPage {
	private IContentProvider fContentProvider;

	/**
	 * 
	 */
	public DependenciesViewListPage(DependenciesView view,
			IContentProvider contentProvider) {
		super(view);
		fContentProvider = contentProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.view.DependenciesViewPage#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected StructuredViewer createViewer(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		fViewer = new TableViewer(table);
		fViewer.setContentProvider(fContentProvider);
		fViewer.setLabelProvider(new DependenciesLabelProvider(false));
		fViewer.setSorter(ListUtil.PLUGIN_SORTER);

		return fViewer;
	}
}
