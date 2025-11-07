/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package x.y.z;

/**
 * Test case for static inner class implementing parent interface.
 * This pattern should NOT produce a leak warning as the inner class
 * is referencing its enclosing type which has the same visibility.
 */
public interface Etest15 {
	
	/**
	 * Static inner class implementing the parent interface.
	 * This should not be reported as leaking non-API interface.
	 */
	static class Etest15Impl implements Etest15 {
		// Implementation
	}
}
