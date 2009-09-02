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
package org.eclipse.pde.api.tools.internal.provisional.descriptors;

/**
 * Describes an {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent}
 * <br><br>
 * Component descriptors do not have parent components and do not have a path, therefore both the 
 * {@link #getParent()} and {@link #getPath()} method always yield <code>null</code>
 * 
 * @since 1.0.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IComponentDescriptor extends IElementDescriptor {

	/**
	 * Returns the component id from the descriptor
	 * 
	 * @return the component id
	 */
	public String getId();
	
	/**
	 * Returns the version id for this component or <code>null</code> if
	 * unspecified.
	 * 
	 * @return
	 */
	public String getVersion();
	
}
