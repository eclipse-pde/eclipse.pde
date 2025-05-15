/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.eclipse.core.runtime.IStatus;

public class TaskHelper {
	private TaskHelper() {
	}

	public static String statusToString(IStatus status) {
		return addNested(status, new StringBuilder()).toString();
	}

	private static StringBuilder addNested(IStatus status, StringBuilder b) {
		IStatus[] nestedStatus = status.getChildren();
		b.append(status.getMessage());
		for (IStatus element : nestedStatus) {
			b.append('\n');
			b.append(addNested(element, b));
		}
		return b;
	}
}
