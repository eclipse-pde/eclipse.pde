package classes;
/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

/**
 * This class is compiled to 1.4
 * @since
 */
public class Test12 {
	public void test() {
		Class c = Integer.class;
		Double.class.desiredAssertionStatus();
		String.class.equals(new Object());
	}
}
