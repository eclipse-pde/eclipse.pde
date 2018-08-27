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
package javadoc;

/**
 * Test class that will be annotated with javadoc tags from the testing component.xml
 * file. 
 * 
 * The class: <code>Inner2</code> will have a no extend tag added to the method m1 and have a 
 * no reference tag added to the method m2
 * 
 */
public class JavadocTestClass6 {

	class Inner {
		class Inner2 {
			protected void m1() {
				
			}
			
			public void m2() {
				
			}
		}
	}
}
