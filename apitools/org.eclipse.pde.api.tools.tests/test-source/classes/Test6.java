package classes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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
 * @since
 */
public class Test6<E extends ArrayList<String>> extends Test6Abstract<String> implements Iterable<Map<String, String>> {

	public Iterator<Map<String, String>> iterator() {
		return null;
	}

}

class Test6Abstract<T> {
	
}
