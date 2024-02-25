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

import java.net.URI;

import aQute.bnd.service.repository.SearchableRepository;

public class ContinueSearchElement {
	private final String				filter;
	private final SearchableRepository	repository;

	public ContinueSearchElement(String filter, SearchableRepository repository) {
		this.filter = filter;
		this.repository = repository;
	}

	public String getFilter() {
		return filter;
	}

	public SearchableRepository getRepository() {
		return repository;
	}

	public URI browse() throws Exception {
		return repository.browse(filter);
	}

}
