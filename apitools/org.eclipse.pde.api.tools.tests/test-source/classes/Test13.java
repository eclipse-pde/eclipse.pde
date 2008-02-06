package classes;
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

/**
 * 
 * @since
 */
public class Test13 extends Super {

	public void m1() {
		System.out.println("not empty");
		m2();
		m3();
		m4();
	}
	
	private void m4() {
		System.out.println("not empty");
		Test13A a = new Test13A();
		a.getInteger();
	}
	
	public void m3() {
		System.out.println("not empty");
		Test13A.doSomething();
	}
}

class Super {
	
	protected void m2() {
		System.out.println("not empty");
	}
	
}