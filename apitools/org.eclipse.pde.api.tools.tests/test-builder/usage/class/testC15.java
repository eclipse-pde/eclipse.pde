/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package x.y.z;

import c.NoRefClass;

public class testC15 {

	void method1() {
		class Local {
			void lm() {
				NoRefClass clazz = new NoRefClass();
				String str = clazz.fNoRefClassField;
				clazz.noRefClassMethod();
			}
		}
		
		NoRefClass clazz = new NoRefClass() {
			public void noRefClassMethod() {
				super.noRefClassMethod();
				String str = super.fNoRefClassField;
			}
		};
	}
}
