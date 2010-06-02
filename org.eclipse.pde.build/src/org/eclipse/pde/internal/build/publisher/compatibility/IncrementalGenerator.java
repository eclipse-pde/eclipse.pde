/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.publisher.compatibility;

import java.util.*;
import org.eclipse.equinox.p2.publisher.*;

/**
 * A class to enable carrying GeneratorResults across multiple invocations of the Generator.
 * Done here in the bundle instead of in GeneratorTask because of the way org.eclipse.ant.core.AntRunner uses class loaders.
 * @since 1.0
 */

public class IncrementalGenerator {
	private static String MODE_INCREMENTAL = "incremental"; //$NON-NLS-1$
	private String mode = null;
	static private PublisherResult result = null;
	static private ArrayList configs = null;
	static private ArrayList advice = null;

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void run(GeneratorApplication generator, PublisherInfo provider) throws Exception {
		if (MODE_INCREMENTAL.equals(mode)) {
			initialize();
			generator.setIncrementalResult(result);
		} else if ("final".equals(mode) && result != null) { //$NON-NLS-1$
			generator.setIncrementalResult(result);
			if (configs != null)
				provider.setConfigurations((String[]) configs.toArray(new String[configs.size()]));
			if (advice != null) {
				for (Iterator iterator = advice.iterator(); iterator.hasNext();) {
					provider.addAdvice((IPublisherAdvice) iterator.next());
				}
			}
		}

		generator.run(provider);

		if (MODE_INCREMENTAL.equals(mode)) {
			configs.addAll(Arrays.asList(provider.getConfigurations()));
			advice.addAll(provider.getAdvice());
		} else {
			result = null;
			configs = null;
			advice = null;
		}
	}

	private void initialize() {
		if (result == null)
			result = new PublisherResult();
		if (configs == null)
			configs = new ArrayList();
		if (advice == null)
			advice = new ArrayList();
	}

}
