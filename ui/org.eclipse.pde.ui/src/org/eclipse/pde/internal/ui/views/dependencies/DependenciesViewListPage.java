/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class DependenciesViewListPage extends DependenciesViewPage {
	
	private static ViewerComparator fComparator = null;

	/**
	 * 
	 */
	public DependenciesViewListPage(DependenciesView view,
			IContentProvider contentProvider) {
		super(view, contentProvider);
	}
	
	static class DependenciesComparator extends ViewerComparator {

		public int compare(Viewer viewer, Object e1, Object e2) {
			return getId(e1).compareTo(getId(e2));
		}
		
		private String getId(Object obj) {
			BundleDescription desc = null;
			if (obj instanceof ImportPackageSpecification) {
				desc = ((ExportPackageDescription)((ImportPackageSpecification)obj).getSupplier()).getSupplier();
			} else if (obj instanceof BundleSpecification) {
				desc = (BundleDescription)((BundleSpecification)obj).getSupplier();
			} else if (obj instanceof BundleDescription)
				desc = (BundleDescription)obj;
			if (desc != null)
				return desc.getSymbolicName();
			return ""; //$NON-NLS-1$
		}
		
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
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(
				false);
		fViewer.setLabelProvider(labelProvider);
		fViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				labelProvider.dispose();
			}
		});
		fViewer.setComparator(getComparator());

		return fViewer;
	}
	
	private ViewerComparator getComparator() {
		if (fComparator == null)
			fComparator = new DependenciesComparator();
		return fComparator;
	}
}
