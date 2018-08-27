package classes;
import java.util.ArrayList;
import java.util.Iterator;
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
public class Test6<E extends ArrayList<String>> extends Test6Abstract<String> implements Iterable<Map<String, String>> {

	public Iterator<Map<String, String>> iterator() {
		return null;
	}

}

class Test6Abstract<T> {
	
}
