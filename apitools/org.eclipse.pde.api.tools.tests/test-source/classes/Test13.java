package classes;
/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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