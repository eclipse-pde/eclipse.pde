/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.providers;

import java.util.Collection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;

/**
 * An {@link ITreeContentProvider} implementation for providing the content to display in the tracing viewer
 */
public class TracingComponentContentProvider implements ITreeContentProvider {

	public TracingNode[] getChildren(final Object parentElement) {

		TracingNode[] children = null;
		if (parentElement instanceof TracingNode) {
			final TracingNode node = (TracingNode) parentElement;
			children = node.getChildren();
		}
		return children;
	}

	public boolean hasChildren(final Object element) {

		boolean hasChildren = false;
		if ((element != null) && (element instanceof TracingNode)) {
			hasChildren = ((TracingNode) element).hasChildren();
		}
		return hasChildren;
	}

	public Object[] getElements(final Object inputElement) {

		TracingNode results[] = null;
		if (inputElement instanceof TracingNode) {
			results = new TracingNode[] {(TracingNode) inputElement};
		} else if (inputElement instanceof TracingNode[]) {
			results = (TracingNode[]) inputElement;
		} else if (inputElement instanceof Collection<?>) {
			Collection<?> collectionElement = (Collection<?>) inputElement;
			results = collectionElement.toArray(new TracingNode[collectionElement.size()]);
		}
		return results;
	}

	public Object getParent(final Object element) {

		TracingNode node = null;
		if ((element != null) && (element instanceof TracingNode)) {
			node = ((TracingNode) element).getParent();
		}
		return node;
	}

}