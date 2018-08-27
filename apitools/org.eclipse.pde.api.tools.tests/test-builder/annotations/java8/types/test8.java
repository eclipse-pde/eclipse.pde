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
 * Tests there are problems with type annotation whose location is different
*  than defined in the target
 */

public class test8 {
	
	@Annotation4(id2 = 0)
	public int fValue = 9;

}

@Target(ElementType.METHOD)
@interface Annotation4 {
	 int    id2();
}
