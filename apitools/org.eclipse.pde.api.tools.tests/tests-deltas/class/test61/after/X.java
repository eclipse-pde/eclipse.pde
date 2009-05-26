/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class X {
	private int i;
	
	public int foo() {
		class C {
			int value;
			C(int value) {
				this.value = value + i;
			}
			int getValue() {
				return this.value;
			}
		}
		return new C(0).getValue();
	}
}