/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredTree;

/**
 * A {@link TracingComponentTreeViewer} provides a tree viewer with support for filtering the content based on user
 * input in a {@link Text} field. Filtering is done via {@link TracingComponentViewerFilter}.
 */
public class TracingComponentTreeViewer extends FilteredTree {

	/**
	 * Create a new {@link TracingComponentTreeViewer} instance
	 * 
	 * @param parent
	 *            The parent composite
	 */
	public TracingComponentTreeViewer(final Composite parent) {

		super(parent, SWT.NONE, new TracingComponentViewerFilter(), true);
		setInitialText(Messages.filterSearchText);
	}

	@Override
	protected TreeViewer doCreateTreeViewer(final Composite treeViewerParentComposite, final int style) {

		return new TreeViewer(treeViewerParentComposite, style | SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
	}

	@Override
	public void setEnabled(final boolean enabled) {

		filterComposite.setEnabled(enabled);
		getViewer().getTree().setEnabled(enabled);
		getFilterControl().setEnabled(enabled);
	}
}