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
package org.eclipse.pde.api.tools.builder.tests.leak;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that an API method leaking an internal type via a parameter
 * is correctly detected
 * 
 * @since 1.0
 */
public class MethodParameterLeak extends LeakTest {

	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public MethodParameterLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_METHOD_PARAMETER);
		}
		return pid;
	}

}
