/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.eclipse.core.runtime.IStatus;

public class TaskHelper {
	public static StringBuffer statusToString(IStatus status, StringBuffer b) {
		IStatus[] nestedStatus = status.getChildren();
		if (b == null)
			b = new StringBuffer();
		b.append(status.getMessage());
		for (int i = 0; i < nestedStatus.length; i++) {
			b.append('\n');
			b.append(statusToString(nestedStatus[i], b));
		}
		return b;
	}
}
