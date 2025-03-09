/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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

package org.eclipse.pde.internal.build.publisher.compatibility;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;

/**
 * A class to enable carrying GeneratorResults across multiple invocations of the Generator.
 * Done here in the bundle instead of in GeneratorTask because of the way org.eclipse.ant.core.AntRunner uses class loaders.
 * @since 1.0
 */

public class IncrementalGenerator {
	private static String MODE_INCREMENTAL = "incremental"; //$NON-NLS-1$
	private String mode = null;
	static private PublisherResult result = null;
	static private ArrayList<String> configs = null;
	static private ArrayList<IPublisherAdvice> advice = null;

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void run(GeneratorApplication generator, PublisherInfo provider) throws Exception {
		if (MODE_INCREMENTAL.equals(mode)) {
			initialize();
			generator.setIncrementalResult(result);
		} else if ("final".equals(mode) && result != null) { //$NON-NLS-1$
			generator.setIncrementalResult(result);
			if (configs != null) {
				provider.setConfigurations(configs.toArray(new String[configs.size()]));
			}
			if (advice != null) {
				for (IPublisherAdvice iPublisherAdvice : advice) {
					provider.addAdvice(iPublisherAdvice);
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
		if (result == null) {
			result = new PublisherResult();
		}
		if (configs == null) {
			configs = new ArrayList<>();
		}
		if (advice == null) {
			advice = new ArrayList<>();
		}
	}

}
