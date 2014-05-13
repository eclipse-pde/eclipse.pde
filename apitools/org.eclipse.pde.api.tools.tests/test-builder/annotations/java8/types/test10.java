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
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tests there are no problems with type annotation whose attribute is not set
 *  andif it  has a default value
 */
public class test10 {
	
	@Annotation4
	public int fValue = 9;

}

@Target(ElementType.FIELD)
@interface Annotation4 {
	 int    id2()  default 99;
}