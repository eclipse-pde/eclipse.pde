/*******************************************************************************
 * Copyright (c) 2014, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.team.core.RepositoryProvider;

public class TeamUtils {
	/**
	 * Get the id of the repository provider that is managing the project
	 *
	 * @param project the project
	 * @return null when project is null or when the project is not managed by a
	 *         version control plugin
	 */
	static public String getProjectRepositoryProviderId(IJavaProject project) {
		if (project == null) {
			return null;
		}

		RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(project.getProject());
		if (repositoryProvider != null) {
			return repositoryProvider.getID();
		}

		return null;
	}
}
