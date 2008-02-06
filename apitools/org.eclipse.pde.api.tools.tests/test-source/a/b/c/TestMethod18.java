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
 * Tests that method correctly inherit restrictions that are valid
 * @since
 * @noextend
 */
public class TestMethod18 {
	public void one() {
		
	}
}

/**
 * @noextend
 * @since
 */
class TestMethod18Outer {
	protected void two() {
		
	}
	
}
