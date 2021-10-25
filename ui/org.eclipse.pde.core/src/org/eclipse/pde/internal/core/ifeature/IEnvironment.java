/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576890: Ignore included features/plug-ins not matching target-environment
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.target.ITargetDefinition;

public interface IEnvironment {
	String P_OS = "os"; //$NON-NLS-1$
	String P_WS = "ws"; //$NON-NLS-1$
	String P_ARCH = "arch"; //$NON-NLS-1$
	String P_NL = "nl"; //$NON-NLS-1$

	/**
	 * Returns a comma-separated list of the operating systems this plug-in
	 * supports.
	 */
	String getOS();

	/**
	 * Returns a comma-separated list of the window systems this plug-in
	 * supports.
	 */
	String getWS();

	/**
	 * Returns a comma-separated list of the architecture this plug-in supports.
	 */
	String getArch();

	/**
	 * Returns a comma-separated list of the locales this plug-in supports.
	 */
	String getNL();

	/**
	 * Sets a comma-separated list of the operating systems this plug-in
	 * supports.
	 */
	void setOS(String os) throws CoreException;

	/**
	 * Sets a comma-separated list of the window systems this plug-in supports.
	 */
	void setWS(String ws) throws CoreException;

	/**
	 * Sets a comma-separated list of the archiecture this plug-in supports.
	 */
	void setArch(String arch) throws CoreException;

	/**
	 * Sets a comma-separated list of the locales this plug-in supports.
	 */
	void setNL(String nl) throws CoreException;

	/**
	 * Returns true if this environment matches the given
	 * {@link ITargetDefinition target-definition}.
	 * <p>
	 * A environment matches a certain property of the given target if its
	 * corresponding property is either {@code null} or any element of its comma
	 * separated list of values is {@link Object#equals(Object) equals} to the
	 * target's value. If the target's value for a property is {@code null} this
	 * environment's property is instead compared to the corresponding property
	 * of the running {@link Platform} (e.g. {@link Platform#getOS()}.
	 * </p>
	 * <p>
	 * This environment fully matches the target only if all properties of this
	 * environment match each corresponding target property (or the one of the
	 * running platform).
	 * </p>
	 *
	 * @param target
	 *            the target-definition to test
	 * @return true if each property of this environment matches the
	 *         corresponding target (or running platform) property.
	 */
	default boolean matchesEnvironment(ITargetDefinition target) {
		return EnvironmentHelper.matchesTargetEnvironment(this, target);
	}
}
