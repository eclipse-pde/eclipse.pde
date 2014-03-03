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