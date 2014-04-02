/*******************************************************************************
 * Copyright (c) Apr 2, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package defaultmethods;

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
