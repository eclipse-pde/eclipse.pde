/*******************************************************************************
 * Copyright (c) 2013, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.repo;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository;

public class SearchableRepositoryTreeContentProvider extends RepositoryTreeContentProvider {

	@Override
	Object[] getRepositoryBundles(RepositoryPlugin repo) {
		Object[] bundles = super.getRepositoryBundles(repo);
		Object[] result = bundles;

		if (repo instanceof SearchableRepository) {
			String filter = getFilter();
			if (filter != null && filter.length() > 0) {
				ContinueSearchElement newElem = new ContinueSearchElement(filter, (SearchableRepository) repo);
				if (bundles != null) {
					result = new Object[bundles.length + 1];
					System.arraycopy(bundles, 0, result, 0, bundles.length);
					result[bundles.length] = newElem;
				} else {
					result = new Object[] {
						newElem
					};
				}
			}
		}

		return result;
	}
}
