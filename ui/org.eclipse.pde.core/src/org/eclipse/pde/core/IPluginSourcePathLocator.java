/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IPluginBase;

/**
 * A plugin source path locator is capable of locating the path of the source
 * for a given plugin.
 * <p>
 * A plugin source path locator is declared as an extension
 * (<code>org.eclipse.pde.core.source</code>).
 * </p>
 *
 * @since 3.16
 */
public interface IPluginSourcePathLocator {

	IPath locateSource(IPluginBase plugin);
}
