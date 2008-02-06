/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
