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
 * The class: <code>Inner2</code> will have a no reference tag added to field f1 and
 * a no reference tag to field f2
 * 
 */
public class JavadocTestClass5 {

	class Inner {
		class Inner2{
			protected int f1;
			
			public Object f2;
		}
	}
}
