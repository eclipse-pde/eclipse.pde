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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

public class DeltaVisitor {

	/**
	 * Visit the given delta
	 *
	 * @param delta the given delta
	 * @return true if the children of the given delta should also be processed
	 */
	public boolean visit(IDelta delta) {
		return true;
	}

	/**
	 * Callback called when the delta visitor is exiting the given delta
	 *
	 * @param delta the given delta
	 */
	public void endVisit(IDelta delta) {
		//
	}
}
