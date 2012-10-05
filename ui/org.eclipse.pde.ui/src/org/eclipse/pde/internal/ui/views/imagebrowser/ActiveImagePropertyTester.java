/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser;

import org.eclipse.core.expressions.PropertyTester;

/**
 * Property tester for the active (selected) image in the plug-in image browser view.  Allows the
 * command handlers to test if a valid image has been selected in the view and enable actions such
 * as {@link SaveToWorkspace}.
 *
 */
public class ActiveImagePropertyTester extends PropertyTester {

	private static final String EXISTS = "exists"; //$NON-NLS-1$

	public ActiveImagePropertyTester() {
	}

	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (EXISTS.equals(property))
			return receiver != null;

		return false;
	}
}
