/*******************************************************************************
 * Copyright (c) 2015, 2023 bndtools project and others.
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
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.resource.SupportingResource;

public abstract class BndBuilderCapReqLoader implements CapReqLoader {

	protected final File							file;
	private Map<String, Collection<Capability>> loadCapabilities;
	private Map<String, Collection<Requirement>> loadRequirements;

	public BndBuilderCapReqLoader(File file) {
		this.file = file;
	}

	@Override
	public String getShortLabel() {
		return file.getName();
	}

	@Override
	public String getLongLabel() {
		return file.getName() + " - " + file.getParentFile()
			.getAbsolutePath();
	}

	protected abstract Builder getBuilder() throws Exception;

	private void load() throws Exception {
		if ((loadCapabilities != null) && (loadRequirements != null)) {
			return;
		}

		Builder builder = getBuilder();
		if (builder == null) {
			loadCapabilities = Collections.emptyMap();
			loadRequirements = Collections.emptyMap();
			return;
		}

		Jar jar = builder.getJar();
		if (jar == null) {
			loadCapabilities = Collections.emptyMap();
			loadRequirements = Collections.emptyMap();
			return;
		}

		ResourceBuilder rb = new ResourceBuilder();
		rb.addJar(jar);
		SupportingResource sr = rb.build();
		List<Capability> capabilities = new ArrayList<>();
		List<Requirement> requirements = new ArrayList<>();

		for (Resource resource : sr.all()) {
			capabilities.addAll(resource.getCapabilities(null));
			requirements.addAll(resource.getRequirements(null));
		}
		loadRequirements = requirements.stream().map(r -> toRequirementWrapper(r))
				.collect(groupingBy(Requirement::getNamespace, Collectors.toCollection(ArrayList::new)));
		loadCapabilities = capabilities.stream()
				.collect(groupingBy(Capability::getNamespace, Collectors.toCollection(ArrayList::new)));
	}

	@Override
	public CapReq loadCapReq(IProgressMonitor monitor) throws Exception {
		load();
		return new CapReq(loadCapabilities, loadRequirements);
	}

	private Requirement toRequirementWrapper(Requirement req) {
		if (req.getNamespace()
			.equals(PackageNamespace.PACKAGE_NAMESPACE)) {
			String pkgName = (String) req.getAttributes()
				.get(PackageNamespace.PACKAGE_NAMESPACE);
			try {
				List<Clazz> importingClasses = findImportingClasses(pkgName);
				if (!importingClasses.isEmpty()) {
					return new RequirementWithChildren(req, importingClasses);
				}
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}
		return req;
	}

	private List<Clazz> findImportingClasses(String pkgName) throws Exception {
		List<Clazz> classes = new LinkedList<>();
		Collection<Clazz> importers = getBuilder().getClasses("", "IMPORTING", pkgName);

		// Remove *this* package
		for (Clazz clazz : importers) {
			String fqn = clazz.getFQN();
			int dot = fqn.lastIndexOf('.');
			if (dot >= 0) {
				String pkg = fqn.substring(0, dot);
				if (!pkgName.equals(pkg)) {
					classes.add(clazz);
				}
			}
		}
		return classes;
	}

	public File getFile() {
		return file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BndBuilderCapReqLoader other = (BndBuilderCapReqLoader) obj;
		return Objects.equals(file, other.file);
	}



}
