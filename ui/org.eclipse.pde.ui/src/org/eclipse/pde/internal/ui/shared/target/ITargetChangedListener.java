/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.pde.core.target.ITargetDefinition;

import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;

/**
 * Listener for UI components that modify the contents of target definitions.
 * Whenever the contents of the target changes the listener will be called so
 * the target can be resolved and the UI updated.
 * 
 * @see TargetLocationsGroup
 * @see TargetContentsGroup
 * @see TargetDefinitionContentPage
 */
public interface ITargetChangedListener {

	/**
	 * Informs the listener that the contents of the target have changed
	 * and UI elements may need to be refreshed.
	 * @param definition target definition whose contents have changed or <code>null</code> if the definition is not available
	 * @param source the ui part that changed the contents, used to avoid unnecessary refreshes
	 * @param resolve whether the definition must be resolved to catch up to all changes
	 * @param forceResolve whether the definition should be resolved even if all contents are marked as resolved
	 */
	public void contentsChanged(ITargetDefinition definition, Object source, boolean resolve, boolean forceResolve);

}
