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
import i.INoOverrideInterface4;

/**
 * Test no overriding (javadoc) restricted default methods via multiple inheritance
 */
public class test14 implements INoOverrideInterface4,INoOverrideJavadocInterface3 {


	@Override
	public void m1() {
		int a=0;
	}
}
