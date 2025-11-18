/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *
 */
package org.eclipse.pde.internal.junit.runtime;

import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Release specific implementation that using stack walker
 */
public class Caller {

	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
	private static final Bundle LOADER_BUNDLE = FrameworkUtil.getBundle(SPIBundleClassLoader.class);

	static Bundle getBundle(int junitVersion) {
		return WALKER.walk(stream -> stream.map(sf -> FrameworkUtil.getBundle(sf.getDeclaringClass())).filter(Objects::nonNull).filter(b -> b != LOADER_BUNDLE).findFirst().orElse(null));
	}

}
