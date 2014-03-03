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
