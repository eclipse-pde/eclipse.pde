/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Objects of this type are carrying one enumeration choice
 * when defining enumeration restrictions for 'string' types.
 * For example, in restriction {"one", "two", "three"},
 * each choice is stored as one object of this type.
 * Choice name can be obtained by inherited 'getName()' method.
 */
public interface ISchemaEnumeration extends ISchemaObject {
}
