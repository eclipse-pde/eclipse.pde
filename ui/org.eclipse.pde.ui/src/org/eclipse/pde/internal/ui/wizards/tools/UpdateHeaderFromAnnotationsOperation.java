/*******************************************************************************
 *  Copyright (c) 2025 Keith Bui and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Keith Bui <buikeith112@gmail.com> - Issue #1816
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Capability;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.CapReqBuilder;

/*
 * Utility to enhance bundle MANIFEST.MF headers from source annotations,
 * similar to Tycho's deriveHeaderFromSource feature. Currently, only Require-Capability
 * items are processed.
 */
public class UpdateHeaderFromAnnotationsOperation {

	public static void updateHeadersFromAnnotations(IProject project, IBundle bundle) {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null || !javaProject.exists()) {
			return;
		}

		try {
			IPath outputLocation = javaProject.getOutputLocation();
			IPath fullPath = project.getWorkspace().getRoot().getFolder(outputLocation).getLocation();
			File outputDir = fullPath.toFile();
			if (!outputDir.exists()) {
				return;
			}

			File manifestFile = new File(project.getLocation().toFile(), "META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest();

			if (manifestFile.exists()) {
				try (var is = new java.io.FileInputStream(manifestFile)) {
					manifest = new Manifest(is);
				}
			}

			// Copy PDE bundle headers into working manifest (to sync PDE model
			// state)
			for (Object key : bundle.getManifestHeaders().keySet()) {
				manifest.getMainAttributes().putValue(
					key.toString(),
					bundle.getManifestHeaders().get(key).toString()
				);
			}

			try (Jar jar = new Jar(project.getName(), outputDir);
				Analyzer analyzer = new Analyzer(jar)) {
				// Add project classpath
				for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
					File f = entry.getPath().toFile();
					if (f.exists() && f.length() > 0) {
						try {
							analyzer.addClasspath(f);
						} catch (IOException e) {
							// ignore bad entries
						}
					}
				}

				Manifest calcManifest = analyzer.calcManifest();
				String reqCap = calcManifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY);
				mergeRequireCapability(manifest.getMainAttributes(), calcManifest.getMainAttributes());
			}
			IFile manifestIFile = project.getFile("META-INF/MANFIEST.MF");
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				manifest.write(baos);
				byte[] bytes = baos.toByteArray();
				if (manifestIFile.exists()) {
					manifestIFile.setContents(new ByteArrayInputStream(bytes), true, false, null);
				} else {
					manifestIFile.create(new ByteArrayInputStream(bytes), true, null);
				}
				manifestIFile.refreshLocal(IResource.DEPTH_ZERO, null); // sync
																		// Eclipse
																		// workspace
			}

			// Update only the headers we touched in PDE's IBundle model
			String reqCapValue = manifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY);
			if (reqCapValue != null) {
				bundle.setHeader(Constants.REQUIRE_CAPABILITY, reqCapValue);
			}
			// remove BRE if replaced by osgi.ee
			manifest.getMainAttributes().remove(new Attributes.Name(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));

		} catch (Exception e) {
			String message = "Cannot derive header from source";
			System.out.println(message);
			System.out.println(e);
		}
	}

	@SuppressWarnings("deprecation")
	private static void mergeRequireCapability(Attributes mainAttributes, Attributes calcAttributes) {
		String existingValue = mainAttributes.getValue(Constants.REQUIRE_CAPABILITY);
		String newValue = calcAttributes.getValue(Constants.REQUIRE_CAPABILITY);

		if (newValue == null) {
			return;
		}

		Parameters additional = OSGiHeader.parseHeader(newValue);
		if (additional.containsKey(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)) {
			// remove deprecated header but use the EE namespace
			mainAttributes.remove(new Attributes.Name(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT));
		}

		if (existingValue == null) {
			mainAttributes.putValue(Constants.REQUIRE_CAPABILITY, newValue);
		} else {
			Parameters current = OSGiHeader.parseHeader(existingValue);
			if (current.containsKey(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)) {
				// strip duplicate osgi.ee
				additional.remove(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			}

			List<Capability> initialCapabilities = CapReqBuilder.getCapabilitiesFrom(current);
			List<Capability> newCapabilities = CapReqBuilder.getCapabilitiesFrom(additional);
			if (newCapabilities.isEmpty()) {
				return;
			}

			Set<Capability> mergedCapabilities = new LinkedHashSet<>();
			mergedCapabilities.addAll(initialCapabilities);
			mergedCapabilities.addAll(newCapabilities);

			String merged = mergedCapabilities.stream().map(cap -> String.valueOf(cap).replace("'", "\"")) //$NON-NLS-1$ //$NON-NLS-2$
					.collect(Collectors.joining(",")); //$NON-NLS-1$
			mainAttributes.putValue(Constants.REQUIRE_CAPABILITY, merged);
		}
	}
}