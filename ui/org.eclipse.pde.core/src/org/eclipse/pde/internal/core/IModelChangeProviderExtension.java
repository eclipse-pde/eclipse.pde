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

import org.eclipse.pde.core.IModelChangeProvider;

/**
 *
 */
public interface IModelChangeProviderExtension extends IModelChangeProvider {
	/**
	 * Passes all the listeners to the target change provider.
	 * @param target the target provider
	 * @param filter if not <code>null</code>, the filter will be used to
	 * filter listeners that need to be transfered. Listeners that
	 * do not pass the filter will be exempt from the transfer.
	 */
	void transferListenersTo(IModelChangeProviderExtension target, IModelChangedListenerFilter filter);
}
