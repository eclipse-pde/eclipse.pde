/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Does container specific actions for the BundleContainerTable.  Allows the resolve
 * operation to run differently (job or dialog) if the table is being used in an editor
 * or a wizard page.  Allows changes to the bundle containers to be reported properly.
 *
 * @see BundleContainerTable
 */
public interface IBundleContainerTableReporter {

	/**
	 * Runs the given operation in a container appropriate fashion.
	 * <p>
	 * This method may be called from the UI thread so implementors should
	 * not block.
	 * </p>
	 * @param operation the operation to run
	 */
	public void runResolveOperation(IRunnableWithProgress operation);

	/**
	 * Informs the container that the contents of the target's bundle 
	 * containers have changed.  Used to mark the editor dirty.
	 */
	public void contentsChanged();

}
