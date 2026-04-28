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
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetResolveRule;
import org.junit.Test;

/**
 * Pins the contract of {@link TargetResolveRule}: resolves of the same target
 * handle must serialize, resolves of distinct handles must not. Regression
 * coverage for issue #310, where parallel resolves raced on the p2 profile
 * lock and produced "ProfileLock.unlock() because lock is null" errors.
 */
public class TargetResolveRuleTest {

	@Test
	public void sameKeyConflicts() {
		ISchedulingRule a = new TargetResolveRule("target-A");
		ISchedulingRule b = new TargetResolveRule("target-A");

		assertTrue("rules with the same key must conflict", a.isConflicting(b));
		assertTrue("isConflicting must be symmetric", b.isConflicting(a));
		assertTrue("contains must mirror isConflicting", a.contains(b));
	}

	@Test
	public void differentKeysDoNotConflict() {
		ISchedulingRule a = new TargetResolveRule("target-A");
		ISchedulingRule b = new TargetResolveRule("target-B");

		assertFalse(a.isConflicting(b));
		assertFalse(b.isConflicting(a));
		assertFalse(a.contains(b));
	}

	@Test
	public void doesNotConflictWithForeignRule() {
		ISchedulingRule rule = new TargetResolveRule("target-A");
		ISchedulingRule foreign = new ISchedulingRule() {
			@Override
			public boolean contains(ISchedulingRule r) {
				return r == this;
			}

			@Override
			public boolean isConflicting(ISchedulingRule r) {
				return r == this;
			}
		};

		assertFalse("must only conflict with other TargetResolveRule instances",
				rule.isConflicting(foreign));
	}

	@Test
	public void forHandleSerializesByMemento() throws Exception {
		ITargetHandle h1 = mock(ITargetHandle.class);
		ITargetHandle h2 = mock(ITargetHandle.class);
		when(h1.getMemento()).thenReturn("memento-X");
		when(h2.getMemento()).thenReturn("memento-X");

		assertTrue("two handles with the same memento must serialize",
				TargetResolveRule.forHandle(h1).isConflicting(TargetResolveRule.forHandle(h2)));
	}

	@Test
	public void forHandleKeepsDistinctTargetsIndependent() throws Exception {
		ITargetHandle h1 = mock(ITargetHandle.class);
		ITargetHandle h2 = mock(ITargetHandle.class);
		when(h1.getMemento()).thenReturn("memento-X");
		when(h2.getMemento()).thenReturn("memento-Y");

		assertFalse("handles with different mementos must not block each other",
				TargetResolveRule.forHandle(h1).isConflicting(TargetResolveRule.forHandle(h2)));
	}

	@Test
	public void forHandleFallsBackWhenMementoFails() throws Exception {
		ITargetHandle broken = mock(ITargetHandle.class);
		when(broken.getMemento()).thenThrow(new CoreException(Status.error("boom")));

		ISchedulingRule first = TargetResolveRule.forHandle(broken);
		ISchedulingRule second = TargetResolveRule.forHandle(broken);

		assertTrue("a second resolve of the same handle instance must still serialize",
				first.isConflicting(second));

		ITargetHandle otherBroken = mock(ITargetHandle.class);
		when(otherBroken.getMemento()).thenThrow(new CoreException(Status.error("boom")));
		assertNotEquals("distinct broken handles must not collide on the fallback key",
				((TargetResolveRule) first).key(),
				((TargetResolveRule) TargetResolveRule.forHandle(otherBroken)).key());
	}
}
