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

import i.INoRefJavadocDefaultInterface;

/**
 * Tests an interface ref for a restricted default method
 */
public class test9 implements INoRefJavadocDefaultInterface {

	public static void main(String[] args) {
		INoRefJavadocDefaultInterface two = new test9();
		two.m1();
	}
}
