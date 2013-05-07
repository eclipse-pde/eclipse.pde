package x.y.z;

import c.NoRefClass;

/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


public class testC14 {
	
	class Inner {
		
		void method2() {
			NoRefClass clazz = new NoRefClass();
			String field = clazz.fNoRefClassField;
			clazz.noRefClassMethod();
		}
		
	}
	
	void method1() {
		NoRefClass clazz = new NoRefClass();
		String field = clazz.fNoRefClassField;
		clazz.noRefClassMethod();
	}
}