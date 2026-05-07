/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.core.target.ITargetHandle;

/**
 * A scheduling rule that conflicts with other instances sharing the same key,
 * serializing jobs that operate on the same target handle.
 */
public record TargetResolveSchedulingRule(String key) implements ISchedulingRule {

	public static ISchedulingRule forHandle(ITargetHandle handle) {
		String key;
		try {
			key = handle.getMemento();
		} catch (CoreException e) {
			key = String.valueOf(System.identityHashCode(handle));
		}
		return new TargetResolveSchedulingRule(key);
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return isConflicting(rule);
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return rule instanceof TargetResolveSchedulingRule other && key.equals(other.key);
	}
}
