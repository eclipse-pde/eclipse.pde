/*******************************************************************************
 *  Copyright (c) 2021, 2022 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.target.ITargetDefinition;

class EnvironmentHelper {
	private EnvironmentHelper() {
	}

	/** @see IEnvironment#matchesEnvironment(ITargetDefinition) */
	static boolean matchesTargetEnvironment(IEnvironment environment, ITargetDefinition target) {
		return matchesProperty(environment.getOS(), target, ITargetDefinition::getOS, Platform::getOS)
				&& matchesProperty(environment.getWS(), target, ITargetDefinition::getWS, Platform::getWS)
				&& matchesProperty(environment.getArch(), target, ITargetDefinition::getArch, Platform::getOSArch)
				&& matchesProperty(environment.getNL(), target, ITargetDefinition::getNL, Platform::getNL);
	}

	private static final Pattern ENVIRONMENT_FILTER_ELEMENT_SEPARATOR = Pattern.compile(","); //$NON-NLS-1$

	private static boolean matchesProperty(String filter, ITargetDefinition target,
			Function<ITargetDefinition, String> targetGetter, Supplier<String> defaultValue) {
		if (filter == null) {
			return true;
		}
		String targetEnvironment = targetGetter.apply(target);
		if (targetEnvironment == null) {
			targetEnvironment = defaultValue.get();
		}
		return ENVIRONMENT_FILTER_ELEMENT_SEPARATOR.splitAsStream(filter).map(String::strip)
				.anyMatch(targetEnvironment::equals);
	}
}
