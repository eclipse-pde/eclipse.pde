/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.ui.launcher;

/**
 * Generates a source lookup path for all PDE-based launch configurations
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 * @deprecated use {@link org.eclipse.pde.launching.PDESourcePathProvider} instead.
 * @see org.eclipse.pde.launching.PDESourcePathProvider
 */
@Deprecated
public class PDESourcePathProvider extends org.eclipse.pde.launching.PDESourcePathProvider {

}