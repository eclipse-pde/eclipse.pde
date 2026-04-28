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
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.core.target.ITargetHandle;

/**
 * A scheduling rule keyed by a target handle's memento string. Two instances
 * with the same key conflict, which serializes resolve jobs for the same
 * target handle while leaving resolves of different targets independent. This
 * avoids races on the p2 profile lock/unlock pair (see issue #310) and the
 * pile-up of cancelled resolve jobs in the Progress view. Because the rule
 * carries only a lightweight String key and is not cached in a static map,
 * there is no risk of unbounded retention.
 */
public record TargetResolveRule(String key) implements ISchedulingRule {

	public static ISchedulingRule forHandle(ITargetHandle handle) {
		String key;
		try {
			key = handle.getMemento();
		} catch (CoreException e) {
			key = String.valueOf(System.identityHashCode(handle));
		}
		return new TargetResolveRule(key);
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return isConflicting(rule);
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return rule instanceof TargetResolveRule other && key.equals(other.key);
	}
}
