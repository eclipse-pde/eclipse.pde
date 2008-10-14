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
package org.eclipse.pde.api.tools.internal.util;

import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;

/**
 * Describes the result of a search for a component that provides a specific class file.
 * @see Util#getComponent(IApiComponent[], String)
 * @since 1.1
 */
public class ClassFileResult {
	private IApiComponent ac;
	private IClassFile cf;
	ClassFileResult(IApiComponent component, IClassFile classFile) {
		cf = classFile;
		ac = component;
	}
	public IClassFile getClassFile() {
		return cf;
	}
	
	public IApiComponent getComponent() {
		return ac;
	}
}

