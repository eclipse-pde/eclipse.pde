/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.pde.internal.build.BrandingIron;

/**
 *
 */
public class BrandTask extends Task {

	BrandingIron iron;

	public BrandTask() {
		iron = new BrandingIron();
	}

	public void setName(String value) {
		iron.setName(value);
	}

	public void setIcons(String value) {
		iron.setIcons(value);
	}

	public void setRoot(String value) {
		iron.setRoot(value);
	}

	public void setOS(String value) {
		iron.setOS(value);
	}

	public void execute() throws BuildException {
		try {
			iron.brand();
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
}
