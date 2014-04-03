/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

import org.eclipse.pde.api.tools.annotations.*;

/**
 * Valid annotations on a functional interface with an inner functional interface
 */
@NoImplement
@NoExtend
@NoReference
@FunctionalInterface
public interface test2 {
	@NoReference
	int m1();
	
	@FunctionalInterface
	interface inner {
		@NoReference
		int m1();
	}
}


