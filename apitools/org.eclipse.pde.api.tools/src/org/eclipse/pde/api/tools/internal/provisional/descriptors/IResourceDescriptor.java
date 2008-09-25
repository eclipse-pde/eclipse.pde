/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 * Descriptor for an {@link IResource}
 * 
 * @since 1.0.0
 */
public interface IResourceDescriptor extends IElementDescriptor {

	/**
	 * Returns the name of this member.
	 * 
	 * @return member name
	 */
	public String getName();
	
	/**
	 * Returns the type of the resource, which will be one of:
	 * <ul>
	 * <li>{@link IResource#FILE}</li>
	 * <li>{@link IResource#FOLDER}</li>
	 * <li>{@link IResource#PROJECT}</li>
	 * </ul>
	 * @return the type of the resource
	 */
	public int getResourceType();
}
