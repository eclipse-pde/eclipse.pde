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

public class ProjectProposal {

	private String folder;
	private List<String> importAsList;

	public ProjectProposal(String folder) {
		this.folder = folder;
		this.importAsList = new ArrayList<>();
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public List<String> getImportAsList() {
		return importAsList;
	}

	public void addImportAs(String importType){
		importAsList.add(importType);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ProjectProposal){
			ProjectProposal proposal = (ProjectProposal) obj;
			if (proposal.getFolder().equals(folder)){
				return proposal.getImportAsList().equals(this.getImportAsList());
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Folder: "+folder+", ");
		sb.append(importAsList.stream().collect(Collectors.joining(",")));
		return sb.toString();
	}

}
