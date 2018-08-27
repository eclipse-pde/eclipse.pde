/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.IModelChangedListener;

/**
 * This filted is to be used when listeners are copied from
 * model to model. It allows some listeners to be skipped in
 * the process.
 */
public interface IModelChangedListenerFilter {
	/**
	 * Tests if the listener should be accepted.
	 * @param listener the listener to test
	 * @return <code>true</code> if the listener should pass
	 * the filter, <code>false</code> otherwise.
	 */
	public boolean accept(IModelChangedListener listener);

}
