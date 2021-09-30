/*******************************************************************************
 * Copyright (c) 2011, 2016 Manumitting Technologies, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Brian de Alwis (MT) - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 509506
 *******************************************************************************/
package org.eclipse.pde.spy.css;

public class Util {

	public static void join(StringBuilder sb, String[] elements, String glue) {
		for (int i = 0; i < elements.length; i++) {
			sb.append(elements[i]);
			if (i < elements.length - 1) {
				sb.append(glue);
			}
		}
	}
}
