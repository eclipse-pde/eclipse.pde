/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.bnd;

import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Stream;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;

public class DelegateRepositoryListener implements RepositoryListenerPlugin {

	private Supplier<Stream<RepositoryListenerPlugin>> delegateSupplier;

	public DelegateRepositoryListener(Supplier<Stream<RepositoryListenerPlugin>> delegateSupplier) {
		this.delegateSupplier = delegateSupplier;
	}

	@Override
	public void bundleAdded(RepositoryPlugin repository, Jar jar, File file) {
		delegateSupplier.get().forEach(listener -> listener.bundleAdded(repository, jar, file));

	}

	@Override
	public void bundleRemoved(RepositoryPlugin repository, Jar jar, File file) {
		delegateSupplier.get().forEach(listener -> listener.bundleRemoved(repository, jar, file));
	}

	@Override
	public void repositoryRefreshed(RepositoryPlugin repository) {
		delegateSupplier.get().forEach(listener -> listener.repositoryRefreshed(repository));

	}

	@Override
	public void repositoriesRefreshed() {
		delegateSupplier.get().forEach(listener -> listener.repositoriesRefreshed());
	}

}
