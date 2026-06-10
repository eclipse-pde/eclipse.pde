/*******************************************************************************
 *  Copyright (c) 2026 Lars Vogel and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathupdater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.internal.core.ClasspathContainerState;
import org.eclipse.pde.internal.core.ClasspathContainerState.UpdateRequest;
import org.eclipse.pde.internal.core.PDECore;
import org.junit.Test;

/**
 * Tests that {@link ClasspathContainerState} deduplicates queued classpath
 * update requests for the same project, see
 * <a href="https://github.com/eclipse-pde/eclipse.pde/issues/2361">issue
 * 2361</a>.
 */
public class ClasspathContainerStateTest {

	private static final IClasspathContainer SAVED_STATE_1 = container("saved state 1");
	private static final IClasspathContainer SAVED_STATE_2 = container("saved state 2");

	@Test
	public void drainsAllRequestsAndPreservesOrder() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();
		IProject a = project("a");
		IProject b = project("b");
		IProject c = project("c");
		queue.add(new UpdateRequest(a, null));
		queue.add(new UpdateRequest(b, null));
		queue.add(new UpdateRequest(c, null));

		List<UpdateRequest> requests = ClasspathContainerState.drainRequests(queue);

		assertTrue(queue.isEmpty());
		assertEquals(List.of(a, b, c), requests.stream().map(UpdateRequest::project).toList());
	}

	@Test
	public void deduplicatesRequestsForSameProject() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();
		IProject a = project("a");
		IProject b = project("b");
		queue.add(new UpdateRequest(a, null));
		queue.add(new UpdateRequest(b, null));
		queue.add(new UpdateRequest(a, null));
		queue.add(new UpdateRequest(a, null));

		List<UpdateRequest> requests = ClasspathContainerState.drainRequests(queue);

		assertEquals(List.of(a, b), requests.stream().map(UpdateRequest::project).toList());
	}

	@Test
	public void requestWithoutSavedStateWinsOverEarlierSavedState() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();
		IProject a = project("a");
		queue.add(new UpdateRequest(a, SAVED_STATE_1));
		queue.add(new UpdateRequest(a, null));

		List<UpdateRequest> requests = ClasspathContainerState.drainRequests(queue);

		assertEquals(1, requests.size());
		assertNull(requests.get(0).container());
	}

	@Test
	public void requestWithoutSavedStateWinsOverLaterSavedState() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();
		IProject a = project("a");
		queue.add(new UpdateRequest(a, null));
		queue.add(new UpdateRequest(a, SAVED_STATE_1));

		List<UpdateRequest> requests = ClasspathContainerState.drainRequests(queue);

		assertEquals(1, requests.size());
		assertNull(requests.get(0).container());
	}

	@Test
	public void latestSavedStateWins() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();
		IProject a = project("a");
		queue.add(new UpdateRequest(a, SAVED_STATE_1));
		queue.add(new UpdateRequest(a, SAVED_STATE_2));

		List<UpdateRequest> requests = ClasspathContainerState.drainRequests(queue);

		assertEquals(1, requests.size());
		assertSame(SAVED_STATE_2, requests.get(0).container());
	}

	@Test
	public void emptyQueueYieldsNoRequests() {
		Queue<UpdateRequest> queue = new ConcurrentLinkedQueue<>();

		assertTrue(ClasspathContainerState.drainRequests(queue).isEmpty());
	}

	private static IProject project(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	private static IClasspathContainer container(String description) {
		return new IClasspathContainer() {

			@Override
			public IClasspathEntry[] getClasspathEntries() {
				return new IClasspathEntry[0];
			}

			@Override
			public String getDescription() {
				return description;
			}

			@Override
			public int getKind() {
				return IClasspathContainer.K_APPLICATION;
			}

			@Override
			public IPath getPath() {
				return PDECore.REQUIRED_PLUGINS_CONTAINER_PATH;
			}
		};
	}
}
