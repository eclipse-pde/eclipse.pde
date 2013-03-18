/*******************************************************************************
 * Copyright (c) Mar 15, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;
import i.INoImpl5;

public class testc12  {
	public void method1() {
		class local2 implements INoImpl5 { //indirect illegal implement
		}
		local2 l2 = new local2();
		System.out.println(l2);
	}
}

class outerc12 {
	class inner2 {
		void method2() {
			class local4 implements INoImpl5 { //indirect illegal implement
			}
			local4 l4 = new local4();
			System.out.println(l4);
		}
	}
}
