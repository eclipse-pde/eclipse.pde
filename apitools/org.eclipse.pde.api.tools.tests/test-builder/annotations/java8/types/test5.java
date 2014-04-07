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

import java.util.ArrayList;

import org.eclipse.pde.api.tools.annotations.NoReference;

public class test5 {

	ArrayList<String> list = new @NoReference ArrayList<>();
	
	String s = (@NoReference String) "foo";
}
