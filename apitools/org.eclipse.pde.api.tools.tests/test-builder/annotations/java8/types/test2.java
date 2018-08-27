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
import java.util.ArrayList;

/**
 * Tests type annotations in field decls 
 */
public class test2 {

	ArrayList<String> list = new @test2Annotation ArrayList<>();
	
	String s = (@test2Annotation String) "foo";
}

@Target({ElementType.TYPE_USE})
@interface test2Annotation {
	
}