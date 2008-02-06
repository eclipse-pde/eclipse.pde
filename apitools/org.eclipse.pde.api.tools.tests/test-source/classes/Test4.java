package classes;

/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * 
 */
public class Test4<List> {
	static class Inner<String> {
		class Inner2<E> {
			
		}
	}
	
	class Inner2<Map> {
		
	}
}
class Test4Outer<Integer> {
	static class Inner<Double> {
		
	}
}