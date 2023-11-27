/*******************************************************************************
 * Copyright (c) 2013, 2022 OPCoach and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.spy.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import jakarta.inject.Inject;

@SuppressWarnings("restriction")
/**
 * This label and treecontent provider is used by ContextSpyPart to display
 * available contexts.
 *
 * @author olivier
 */
public class ContextSpyProvider extends LabelProvider implements ITreeContentProvider, IColorProvider {

	@Inject
	private ContextDataFilter contextFilter;

	@Inject
	public ContextSpyProvider() {

	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof MApplication) {
			return new Object[] { ((MApplication) inputElement).getContext().getParent() };
		} else if (inputElement instanceof Collection) {
			return ((Collection<?>) inputElement).toArray();
		}

		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		List<EclipseContext> children = Collections.emptyList();

		if (parentElement instanceof EclipseContext) {
			Iterable<EclipseContext> it = ((EclipseContext) parentElement).getChildren();
			children = new ArrayList<>();
			it.forEach(children::add);
		}
		return children.toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof EclipseContext) {
			return ((EclipseContext) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public String getText(Object element) {
		return super.getText(element);
	}

	@Override
	public Color getForeground(Object element) {
		// Return a color if a text contained in this node contains the text.
		if (element instanceof IEclipseContext && contextFilter.containsText((IEclipseContext) element)) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}

}
