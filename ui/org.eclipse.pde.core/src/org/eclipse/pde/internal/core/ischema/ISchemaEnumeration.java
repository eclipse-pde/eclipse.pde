/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
