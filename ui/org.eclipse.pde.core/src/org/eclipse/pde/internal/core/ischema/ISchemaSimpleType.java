/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

import org.eclipse.pde.core.IWritable;

/**
 * Classes that implement this interface define
 * simple types. Simple types do not have compositors.
 * They consist of the base type whose name is
 * available by calling 'getName()' and an optional
 * restriction that can narrow the value space for the type.
 */
public interface ISchemaSimpleType extends ISchemaType, IWritable {
	/**
	 * Returns the restriction that narrows the value space of
	 * this type.
	 *
	 * @return restriction for this simple type, or <samp>null</samp> if there
	 * is no restriction.
	 */
	ISchemaRestriction getRestriction();
}
