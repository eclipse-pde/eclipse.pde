/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class TestBrandTask extends Task {

	public String name;
	public String icons;
	public String root;
	public String os;

	public TestBrandTask() {
	}

	public void setName(String value) {
		name = value;
	}

	public void setIcons(String value) {
		icons = value;
	}

	public void setRoot(String value) {
		root = value;
	}

	public void setOS(String value) {
		os = value;
	}

	@Override
	public void execute() throws BuildException {
		// nothing
	}
}
