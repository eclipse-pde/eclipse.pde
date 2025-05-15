/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.pde.internal.build.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.internal.p2.publisher.eclipse.BrandingIron;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ExecutablesDescriptor;

public class BrandTask extends Task {

	BrandingIron iron;
	private String name;
	private String os;
	private File root;

	public BrandTask() {
		iron = new BrandingIron();
	}

	public void setName(String value) {
		name = value;
		iron.setName(value);
	}

	public void setIcons(String value) {
		iron.setIcons(value);
	}

	public void setRoot(String value) {
		root = new File(value);
	}

	public void setOS(String value) {
		os = value;
		iron.setOS(value);
	}

	@Override
	public void execute() throws BuildException {
		try {
			iron.brand(ExecutablesDescriptor.createDescriptor(os, name, root));
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
}
