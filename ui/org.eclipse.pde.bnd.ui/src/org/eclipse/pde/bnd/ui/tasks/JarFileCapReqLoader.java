/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
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
*******************************************************************************/
package org.eclipse.pde.bnd.ui.tasks;

import java.io.File;
import java.io.IOException;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class JarFileCapReqLoader extends BndBuilderCapReqLoader {

	private Builder builder;

	public JarFileCapReqLoader(File jarFile) {
		super(jarFile);
	}

	@Override
	protected synchronized Builder getBuilder() throws Exception {
		if (builder == null) {
			Builder b = new Builder();
			Jar jar = new Jar(file);
			b.setJar(jar);
			b.analyze();

			builder = b;
		}
		return builder;
	}

	@Override
	public synchronized void close() throws IOException {
		if (builder != null)
			builder.close();
		builder = null;
	}

}
