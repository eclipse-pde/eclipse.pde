package invokedynamic;
import java.util.Arrays;

/*******************************************************************************
 * Copyright (c) Mar 26, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Tests an invoke dynamic reference in a lambda method call
 */
public class test6 {
	void m1() {
		String[] array = {"one"};
		Arrays.sort(array, (s1, s2) -> s1.compareToIgnoreCase(s2));
	}
}
