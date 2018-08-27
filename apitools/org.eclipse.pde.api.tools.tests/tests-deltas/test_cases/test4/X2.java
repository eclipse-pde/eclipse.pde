/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.io.Serializable;

public class X2<T extends Object & Serializable> {
		T t;
    public static <U extends Exception> void foo(U u) {}
    public static <U extends Object & Serializable> void foo(String s, int i, U u) {}
    public static void foo(String s, int i, String[] tab) {}
    
    public void bar() {
    	foo("", 0, null);
    }
}