/*******************************************************************************
 * Copyright (c) May 16, 2014 IBM Corporation and others.
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
