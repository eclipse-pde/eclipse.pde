package classes;
import java.util.ArrayList;
import java.util.Map;

/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

/**
 * 
 * @since
 */
public class Test8 {

	public ArrayList<String> m1(Map<Integer, Test8Outer> map, Double...doubles) {
		return null;
	}
	
	protected static ArrayList<String> m2(Map<Integer, Test8Outer> map, Double...doubles) {
		return null;
	}
}


class Test8Outer {
	
}