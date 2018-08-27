package org.eclipse.tools.layout.spy.internal.displayfilter;
/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

public class LayoutIssuesDebugFilter implements Listener {
	static LayoutIssuesDebugFilter filter;

	private int extraCompositeMargin;
	private boolean toolTip;

	public static final String IGNORE_BY_LAYOUT_ISSUES_DEBUG_FILTER = "IGNORE_BY_LayoutIssuesDebugFilter";

	public static void activate(boolean activateColoring, boolean showTooltips, int extraCompositeMargin) {
		if (filter != null) {
			Display.getDefault().removeFilter(SWT.Move, filter);
			Display.getDefault().removeFilter(SWT.Resize, filter);
			Display.getDefault().removeFilter(SWT.Show, filter);
		}

		if (!activateColoring) {
			return;
		} else {

			// called with activate == true

			if (filter == null) {
				filter = new LayoutIssuesDebugFilter(showTooltips, extraCompositeMargin);
			}
			Display.getDefault().addFilter(SWT.Move, filter);
			Display.getDefault().addFilter(SWT.Resize, filter);
			Display.getDefault().addFilter(SWT.Show, filter);
		}
	}

	private LayoutIssuesDebugFilter(boolean showTooltips, int extraCompositeMargin) {
		this.extraCompositeMargin = extraCompositeMargin;
		this.toolTip = showTooltips;
	}

	@Override
	public void handleEvent(Event event) {
		Widget widget = event.widget;
		if (widget instanceof Control) {
			Painter.decorate((Control) widget, extraCompositeMargin, toolTip);
		}
	}
}
