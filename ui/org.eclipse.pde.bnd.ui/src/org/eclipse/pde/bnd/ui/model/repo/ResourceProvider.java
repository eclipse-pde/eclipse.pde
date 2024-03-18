/*******************************************************************************
 * Copyright (c) 2020 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - initial API and implementation
 *******************************************************************************/
package bndtools.model.repo;

import org.osgi.resource.Resource;

public interface ResourceProvider {
	Resource getResource();
}
