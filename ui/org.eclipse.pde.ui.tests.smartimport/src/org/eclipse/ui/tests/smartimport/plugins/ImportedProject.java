/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.tests.smartimport.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents imported project as it is displayed after import in {@link EasymportJobReportDialog}.
 */
public class ImportedProject {

	private String projectName;
	private List<String> importedAs;
	private String relativePath;

	public ImportedProject(String projectName, String relativePath) {
		this.projectName = projectName;
		this.relativePath = relativePath;
		importedAs = new ArrayList<>();
	}

	public void addImportedAs(String type){
		importedAs.add(type);
	}

	public String getProjectName() {
		return projectName;
	}

	public List<String> getImportedAsList() {
		return importedAs;
	}

	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ImportedProject){
			ImportedProject project = (ImportedProject) obj;
			if (project.getProjectName().equals(projectName) && project.getRelativePath().equals(relativePath)){
				return project.getImportedAsList().equals(this.getImportedAsList());
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ProjectName: "+projectName+", ");
		sb.append("relative path: \""+relativePath+"\", ");
		sb.append(importedAs.stream().collect(Collectors.joining(",")));
		return sb.toString();
	}

}
