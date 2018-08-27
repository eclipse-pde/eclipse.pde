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

package a.b.c;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tests type annotations being used in method decls
 */
public class test1 {

	void method() throws @test1Annotation Exception {
		
	}
	
	void method2(@test1Annotation String arg) {
		
	}
	
	@test1Annotation String name() {
		return null;
	}
}

@Target({ElementType.TYPE_USE})
@interface test1Annotation {
	
}
