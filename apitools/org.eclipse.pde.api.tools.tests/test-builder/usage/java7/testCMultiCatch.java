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

public class testCMultiCatch {

	public void m1(){
		MultipleThrowableClass c = new MultipleThrowableClass();
		try {
			c.m1();
			ExceptionA a = new ExceptionA();
		} catch (ExceptionA | ExceptionB e) {
			ExceptionA a = new ExceptionA();
		}
	}
}
