/*******************************************************************************
 * Copyright (c) Apr 2, 2014 IBM Corporation and others.
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

import i.INoRefDefaultInterface;

/**
 * Tests an interface ref for a restricted default method
 */
public class test2 implements INoRefDefaultInterface {

	public static void main(String[] args) {
		INoRefDefaultInterface two = new test2();
		two.m1();
	}
}
