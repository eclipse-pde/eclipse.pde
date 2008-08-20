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
package a.classes.hierarchy;

/**
 *
 */
public class ChangeInterfaceFromAtoB implements InterfaceB {

	/* (non-Javadoc)
	 * @see a.classes.hierarchy.InterfaceA#methodA()
	 */
	public int methodA() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see a.classes.hierarchy.InterfaceB#methodB()
	 */
	public String methodB() {
		return null;
	}

}
