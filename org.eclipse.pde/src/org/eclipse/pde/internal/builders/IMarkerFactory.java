package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 *
 */
public interface IMarkerFactory {
	IMarker createMarker(IFile file) throws CoreException;
}
