/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.List;

import org.eclipse.pde.core.IModelChangeProvider;


/**
 *
 */
public interface IModelChangeProviderExtension extends IModelChangeProvider {
/**
 * Passes all the listeners to the target change provider.
 * @param target
 */
	void transferListenersTo(IModelChangeProviderExtension target);
/**
 * Accepts all the listeners from the source change provider.
 * @param target
 */
	void acceptListenersFrom(IModelChangeProviderExtension source);
	
	List getListeners();
}