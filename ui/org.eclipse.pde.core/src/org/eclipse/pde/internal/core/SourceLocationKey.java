/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import org.osgi.framework.Version;

/**
 * Used as the key for the bundle manifest location map.  Contains
 * both the bundle name and the version.  The version attribute can
 * be null.
 * @since 3.4
 */
public record SourceLocationKey(String fBundleName, Version fVersion) {
}