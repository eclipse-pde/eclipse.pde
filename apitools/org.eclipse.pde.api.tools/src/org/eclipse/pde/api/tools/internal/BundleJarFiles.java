/*******************************************************************************
 * Copyright (c) 2020 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Alexander Fedorov (ArSysOp)
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class BundleJarFiles {

	// File is typically the output of `mvn dependency:list
	// -DoutputAbsoluteArtifactFilename=true -DoutputScope=false -DoutputFile=...`
	// like
	// ```
	// The following files have been resolved:
	// p2.eclipse-plugin:org.eclipse.equinoxz.event:jar:1.5.0.v20181008-1938:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.equinox.event/1.5.0.v20181008-1938/org.eclipse.equinox.event-1.5.0.v20181008-1938.jar
	// p2.eclipse-plugin:org.eclipse.equinox.p2.core:jar:2.6.0.v20190215-2242:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.equinox.p2.core/2.6.0.v20190215-2242/org.eclipse.equinox.p2.core-2.6.0.v20190215-2242.jar
	// p2.eclipse-plugin:org.eclipse.osgi.compatibility.state:jar:1.1.400.v20190208-1533:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.osgi.compatibility.state/1.1.400.v20190208-1533/org.eclipse.osgi.compatibility.state-1.1.400.v20190208-1533.jar
	// ```
	// or output from "tycho-dependency-tools-plugin" (dependencies-list.txt)
	// like
	// ```
	// C:\work\ef\passage-master\git\passage\bundles\org.eclipse.passage.lic.api\target\org.eclipse.passage.lic.api-1.0.0-SNAPSHOT.jar
	// ```
	private final File dependencies;

	public BundleJarFiles(File dependencies) {
		this.dependencies = dependencies;
	}

	public List<File> list() throws IOException {
		return Files.readAllLines(dependencies.toPath()).stream()//
				.filter(line -> line.contains("jar")) //$NON-NLS-1$
				.map(line -> recognize(line))//
				.filter(Predicate.not(Optional::isEmpty))//
				.map(Optional::get)//
				.collect(Collectors.toList());
	}

	private Optional<File> recognize(String line) {
		Optional<File> whole = file(line);
		if (whole.isPresent()) {
			return whole;
		}
		String[] split = line.split(":", 2); //$NON-NLS-1$
		if (split.length != 2) {
			return Optional.empty();
		}
		return recognize(split[1]);
	}

	private Optional<File> file(String segment) {
		return Optional.of(segment.trim())//
				.filter(Predicate.not(String::isEmpty))//
				.map(File::new)//
				.filter(File::isAbsolute)//
				.filter(File::isFile);
	}

}
