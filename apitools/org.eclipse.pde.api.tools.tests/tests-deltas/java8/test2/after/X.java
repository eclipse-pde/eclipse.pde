/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
public class X {

	public interface Intf {
		void method2();

	}

	public class ClassA {
		
		void method1(){
			
		}

	}
	void func(){

		ClassA a = new ClassA();
		Intf b = a::method1;
		b.method2();
		
	}

}