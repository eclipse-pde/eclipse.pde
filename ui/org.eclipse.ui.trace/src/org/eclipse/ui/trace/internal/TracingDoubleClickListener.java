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

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;

/**
 * A double-click listener to expand/collapse a root node in the trace component viewer
 */
public class TracingDoubleClickListener implements IDoubleClickListener {

	public void doubleClick(final DoubleClickEvent event) {
		// auto-expand or collapse the selected node
		TreeViewer traceComponentViewer = (TreeViewer) event.getViewer();
		final Object selectedItem = ((IStructuredSelection) traceComponentViewer.getSelection()).getFirstElement();
		final boolean expandedState = traceComponentViewer.getExpandedState(selectedItem);
		if (selectedItem instanceof TracingNode) {
			traceComponentViewer.setExpandedState(selectedItem, !expandedState);
		}
	}

}