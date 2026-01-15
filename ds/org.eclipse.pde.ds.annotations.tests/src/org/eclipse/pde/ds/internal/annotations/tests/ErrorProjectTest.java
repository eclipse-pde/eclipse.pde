package org.eclipse.pde.ds.internal.annotations.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Simply doesn't work in the new build environment yet")
public class ErrorProjectTest extends CompilationParticipantTest {

	private static final IPath PATH_PREFIX = IPath.fromOSString("src/ds/annotations/test2/");

	@Override
	protected String getTestProjectName() {
		return "ds.annotations.test2";
	}

	private IFile getFixture(String simpleClassName) {
		IFile file = testProject.getFile(PATH_PREFIX.append(simpleClassName).addFileExtension("java"));
		assertTrue(file.exists());
		return file;
	}

	@Test
	public void missingImplicitDynamicReferenceUnbindMethodError() throws Exception {
		IResource cu = getFixture("MissingDynamicReferenceUnbindMethodComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("No implicit unbind method named 'unsetDynamicReference' found in implementation class.", markers[0].getAttribute(IMarker.MESSAGE));
	}

	@Test
	public void duplicateConfigurationPidError() throws Exception {
		IResource cu = getFixture("DuplicateConfigurationPidComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(2, markers.length);
		for (IMarker marker : markers) {
			assertEquals("Duplicate configuration PID.", marker.getAttribute(IMarker.MESSAGE));
		}
	}

	@Test
	public void factoryImmediateError() throws Exception {
		IResource cu = getFixture("FactoryImmediateComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("Factory component cannot be immediate.", markers[0].getAttribute(IMarker.MESSAGE));
	}

	@Test
	public void delayedWithNoServicesError() throws Exception {
		IResource cu = getFixture("DelayedWithNoServicesComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("Component that does not register services must be immediate.", markers[0].getAttribute(IMarker.MESSAGE));
	}

	@Test
	public void nonSingletonFactoryOrImmediateError() throws Exception {
		IResource cu = getFixture("NonSingletonFactoryOrImmediateComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(2, markers.length);
		for (IMarker marker : markers) {
			assertEquals("Factory or immediate component must have singleton scope.", marker.getAttribute(IMarker.MESSAGE));
		}
	}

	@Test
	public void scopeWithNoServicesError() throws Exception {
		IResource cu = getFixture("ScopeWithNoServicesComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("Scope is not applicable to component with no registered services.", markers[0].getAttribute(IMarker.MESSAGE));
	}


	@Test
	public void factoryOrImmediateServiceFactoryError() throws Exception {
		IResource cu = getFixture("FactoryOrImmediateServiceFactoryComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(2, markers.length);
		for (IMarker marker : markers) {
			assertEquals("Factory or immediate component cannot be a service factory.", marker.getAttribute(IMarker.MESSAGE));
		}
	}

	@Test
	public void serviceFactoryIgnoredError() throws Exception {
		IResource cu = getFixture("ServiceFactoryWithScopeComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("Property 'servicefactory' is ignored when non-default scope is specified.", markers[0].getAttribute(IMarker.MESSAGE));
	}

	@Test
	public void serviceFactoryWithNoServicesError() throws Exception {
		IResource cu = getFixture("ServiceFactoryWithNoServicesComponent");
		IMarker[] markers = cu.findMarkers(DS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
		assertEquals(1, markers.length);
		assertEquals("Component that does not register services cannot be a service factory.", markers[0].getAttribute(IMarker.MESSAGE));
	}
}
