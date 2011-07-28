/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import c.MultipleThrowableClass;
import c.MultipleThrowableClass.ExceptionA;
import c.MultipleThrowableClass.ExceptionB;

public class testFMultiCatch {

	public void m1(){
		MultipleThrowableClass c = new MultipleThrowableClass();
		int i = 0;
		if (i == 0);
		try {
			c.m1();
			i = c.f1;
		} catch (ExceptionA | ExceptionB e) {
			i = c.f1;
		}
	}
}