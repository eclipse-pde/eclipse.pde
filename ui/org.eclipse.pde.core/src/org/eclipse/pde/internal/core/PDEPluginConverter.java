package org.eclipse.pde.internal.core;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.osgi.util.tracker.*;

public class PDEPluginConverter {
	
	public static void convertToOSGIFormat(IProject project, String filename, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().append(filename).toOSString());
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			converter.convertManifest(inputFile, outputFile, false, null);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			IFile file = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
			file.setCharset("UTF-8"); //$NON-NLS-1$
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}

}
