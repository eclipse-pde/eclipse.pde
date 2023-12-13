/*******************************************************************************
 * Copyright (c) 2015 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation (bug #451116)
 *******************************************************************************/
package org.eclipse.pde.spy.bundle.internal;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.pde.spy.bundle.BundleSpyPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

import jakarta.inject.Inject;

/**
 * The column Label and content Provider used to display information in context
 * data TreeViewer. Two instances for label provider are created : one for key,
 * one for values
 */
public class BundleDataProvider extends ColumnLabelProvider {

	public static final int COL_NAME = 0;
	public static final int COL_VERSION = 1;
	public static final int COL_STATE = 2;

	private static final Color COLOR_IF_FOUND = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

	@Inject
	private ImageRegistry imgReg;

	// Only one bundle filter, injected for all columns.
	@Inject
	private BundleDataFilter bundleFilter;

	// The column number this provider manages.
	private int column;

	@Inject
	public BundleDataProvider() {
		super();
	}

	@Override
	public String getText(Object element) {
		// Received element is a bundle...Text depends on column.
		Bundle b = (Bundle) element;
		String result = getText(b, column);
		return (result == null) ? super.getText(element) : result;

	}

	public static String getText(Bundle b, int col) {
		switch (col) {
		case COL_NAME:
			return b.getSymbolicName();
		case COL_VERSION:
			return b.getVersion().toString();
		case COL_STATE:
			return ""; // No text for state (see tooltip) //$NON-NLS-1$

		}
		return null;
	}

	@Override
	public Color getForeground(Object element) {
		// Return magenta color if the value could not be yet computed (for
		// context functions)
		String s = getText(element);

		// Return blue color if the string matches the search
		return ((bundleFilter != null) && (bundleFilter.matchText(s))) ? COLOR_IF_FOUND : null;
	}

	@Override
	public Image getImage(Object element) {
		Bundle b = (Bundle) element;
		if (column == COL_STATE) {

			switch (b.getState()) {
			case Bundle.ACTIVE:
				return imgReg.get(BundleSpyPart.ICON_STATE_ACTIVE);
			case Bundle.INSTALLED:
				return imgReg.get(BundleSpyPart.ICON_STATE_INSTALLED);
			case Bundle.RESOLVED:
				return imgReg.get(BundleSpyPart.ICON_STATE_RESOLVED);
			case Bundle.STARTING:
				return imgReg.get(BundleSpyPart.ICON_STATE_STARTING);
			case Bundle.STOPPING:
				return imgReg.get(BundleSpyPart.ICON_STATE_STOPPING);
			case Bundle.UNINSTALLED:
				return imgReg.get(BundleSpyPart.ICON_STATE_UNINSTALLED);

			}
		}
		return null;

	}

	@Override
	public String getToolTipText(Object element) {
		Bundle b = (Bundle) element;

		switch (b.getState()) {
		case Bundle.ACTIVE:
			return Messages.BundleDataProvider_1;
		case Bundle.INSTALLED:
			return Messages.BundleDataProvider_2;
		case Bundle.RESOLVED:
			return Messages.BundleDataProvider_3;
		case Bundle.STARTING:
			return Messages.BundleDataProvider_4;
		case Bundle.STOPPING:
			return Messages.BundleDataProvider_5;
		case Bundle.UNINSTALLED:
			return Messages.BundleDataProvider_6;

		}

		return Messages.BundleDataProvider_7 + b.getState();

	}

	@Override
	public Image getToolTipImage(Object object) {
		return getImage(object);
	}

	@Override
	public int getToolTipStyle(Object object) {
		return SWT.SHADOW_OUT;
	}

	public void setColumn(int col) {
		column = col;

	}

}
