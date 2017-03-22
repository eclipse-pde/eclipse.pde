package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.junit.Test;

public class ErrorProjectTest extends CompilationParticipantTest {

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test2";
	}

	@Test
	public void missingImplicitDynamicReferenceUnbindMethodError() throws Exception {
		IMarker[] markers = testProject.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		assertEquals(1, markers.length);
		assertEquals("No implicit unbind method named 'unsetDynamicReference' found in implementation class.", markers[0].getAttribute(IMarker.MESSAGE));
	}
}
