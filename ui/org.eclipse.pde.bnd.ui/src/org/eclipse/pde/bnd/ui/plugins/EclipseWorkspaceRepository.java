/*******************************************************************************
 * Copyright (c) 2020, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     BJ Hargrave <bj@hargrave.dev> - initial API and implementation
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.plugins;

import static aQute.bnd.exceptions.SupplierWithException.asSupplierOrElse;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Adapters;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.repository.WorkspaceRepositoryMarker;
import aQute.bnd.osgi.resource.ResourceBuilder;

public class EclipseWorkspaceRepository extends AbstractIndexingRepository<IProject, File>
	implements WorkspaceRepositoryMarker {

	private static final Map<IWorkspace, EclipseWorkspaceRepository> repositoryMap = new ConcurrentHashMap<>();
	private boolean initialized;
	private final IWorkspace workspace;

	EclipseWorkspaceRepository(IWorkspace workspace) {
		this.workspace = workspace;
	}

	synchronized void initialize() throws Exception {
		if (initialized) {
			return;
		}
		initialized = true;
		List<IProject> projects = Arrays.stream(workspace
			.getRoot()
			.getProjects())
			.collect(toList());
		for (IProject project : projects) {
			Project model = Adapters.adapt(project, Project.class);
			if (model == null) {
				continue;
			}
			File target = model.getTargetDir();
			File buildfiles = new File(target, Constants.BUILDFILES);
			if (buildfiles.isFile()) {
				index(project, asSupplierOrElse(() -> {
					try (BufferedReader rdr = Files.newBufferedReader(buildfiles.toPath(), StandardCharsets.UTF_8)) {
						return rdr.lines()
								.map(line -> new File(target, line.trim()))
							.filter(File::isFile)
							.collect(toList());
					}
				}, Collections.emptyList()));
			}
		}
	}

	@Override
	protected boolean isValid(IProject project) {
		try {
			return project.isOpen() && (Adapters.adapt(project, Project.class) != null);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected BiFunction<ResourceBuilder, File, ResourceBuilder> indexer(IProject project) {
		String name = project.getName();
		return (rb, file) -> {
			rb = fileIndexer(rb, file);
			if (rb == null) {
				return null; // file is not a file
			}
			// Add a capability specific to the workspace so that we can
			// identify this fact later during resource processing.
			rb.addWorkspaceNamespace(name);
			return rb;
		};
	}

	@Override
	public String toString() {
		return NAME;
	}

	public static EclipseWorkspaceRepository get(IWorkspace workspace) throws Exception {
		EclipseWorkspaceRepository repository = repositoryMap.computeIfAbsent(workspace,
				EclipseWorkspaceRepository::new);
		repository.initialize();
		return repository;
	}
}
