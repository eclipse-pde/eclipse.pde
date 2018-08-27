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
package a.b.c;

/**
 * Tests that methods do not inherit restrictions that do not apply to methods
 * @noinstantiate
 * @since
 */
public class TestMethod19 {
	public void one() {
		
	}
}

/**
 * @noinstantiate
 * @since
 */
class TestMethod19Outer {
	protected void two() {
		
	}
}
