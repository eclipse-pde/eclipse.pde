/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nina Rinskaya
 *     		Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=172820.
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests.util;

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchResult;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;

/**
 * Utility methods for api tools test suite
 * @since 1.0.0
 */
public class Util {
	
	/**
	 * Returns the next available port number on the local host.
	 */
	public static int getFreePort() {
	    ServerSocket socket = null;
	    try {
	        socket = new ServerSocket(0);
	        return socket.getLocalPort();
	    } catch (IOException e) {
	        // ignore
	    } finally {
	        if (socket != null) {
	            try {
	                socket.close();
	            } catch (IOException e) {
	                // ignore
	            }
	        }
	    }
	    return -1;
	}
	
	/**
	 * Returns all references in the given search results.
	 * 
	 * @param results
	 * @return
	 */
	public static IReference[] getReferences(IApiSearchResult[] results) {
		if (results.length == 1) {
			return results[0].getReferences();
		}
		if (results.length == 0) {
			return new IReference[0];
		}
		int size = 0;
		for (int i = 0; i < results.length; i++) {
			IApiSearchResult result = results[i];
			size += result.getReferences().length;
		}
		IReference[] refs = new IReference[size];
		int index = 0;
		for (int i = 0; i < results.length; i++) {
			IApiSearchResult result = results[i];
			IReference[] references = result.getReferences();
			System.arraycopy(references, 0, refs, index, references.length);
			index = index + references.length;
		}
		return refs;
	}

}
