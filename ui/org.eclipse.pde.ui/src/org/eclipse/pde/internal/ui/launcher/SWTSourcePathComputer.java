package org.eclipse.pde.internal.ui.launcher;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.sourcelookup.*;
import org.eclipse.debug.core.sourcelookup.containers.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.sourcelookup.containers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public class SWTSourcePathComputer extends JavaSourcePathComputer {
	
	private static final String ID = "org.eclipse.pde.ui.swtSourcePathComputer"; //$NON-NLS-1$

	public String getId() {
		return ID;
	}
	
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		ISourceContainer[] containers =  super.computeSourceContainers(configuration, monitor);

		IFragment fragment = SWTLaunchConfiguration.findFragment();
		if (fragment == null)
			return containers;
		
		IPath fragmentPath = new Path(fragment.getModel().getInstallLocation());
		for (int i = 0; i < containers.length; i++) {
			if (containers[i] instanceof PackageFragmentRootSourceContainer) {
				PackageFragmentRootSourceContainer container = (PackageFragmentRootSourceContainer)containers[i];
				IPackageFragmentRoot root = container.getPackageFragmentRoot();
				if (root.getSourceAttachmentPath() == null) {
					int matchCount = fragmentPath.matchingFirstSegments(root.getPath());
					if (matchCount == fragmentPath.segmentCount()) {
						IPath libPath = root.getPath().removeFirstSegments(matchCount);
						String libLocation = getLibrarySourceLocation(fragment, libPath.toString());
						if (libLocation != null) {
							containers[i] = new ExternalArchiveSourceContainer(libLocation, false);
						}
					}
				}
			}
		}	
		return containers;
	}
	
	private String getLibrarySourceLocation(IFragment fragment, String library) {
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		int dot = library.lastIndexOf('.');
		if (dot != -1) {
			library = library.substring(0, dot) + "src.zip"; //$NON-NLS-1$
		}
		File file = manager.findSourceFile(fragment, new Path(library));
		return file == null ? null : file.getAbsolutePath();
	}
}
