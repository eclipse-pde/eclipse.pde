/*******************************************************************************
 * Copyright (c) May 16, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import i.INoOverrideJavadocInterface3;

/**
 * Test no overriding (javadoc tag) restricted default methods via extended interfaces
 */
public class test13 implements INoOverrideJavadocInterface3 {


	@Override
	public void m1() {
		int a=0;
	}
}
