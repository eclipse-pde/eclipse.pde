/******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 *
 * This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.ui.internal.views.log.jdt;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.internal.views.log.LogView;

/**
 * A property tester for controlling the enablement of the Show In Console
 * command.
 */
public class ShowInConsolePropertyTester extends PropertyTester {
	private static final String HAS_SELECTED_STACK_PROPERTY = "hasSelectedStack"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (HAS_SELECTED_STACK_PROPERTY.equals(property)) {
			if (receiver instanceof LogView) {
				LogView logView = (LogView) receiver;
				if (logView.getSelectedStack() != null) {
					return true;
				}
				return false;
			}
		}
		return false;
	}
}
