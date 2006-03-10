package org.eclipse.pde.internal.ui.correction;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.builders.BuildErrorReporter;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddSourceBuildEntryResolution extends BuildEntryMarkerResolution {

	public AddSourceBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddSourceBuildEntryResolution_label, fEntry);
	}
	
	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry)build.getEntry(fEntry);
			boolean unlistedOnly = true;
			if (buildEntry == null) {
				buildEntry = new BuildEntry(fEntry, build.getModel());
				unlistedOnly = false;
			}
			String[] unlisted = getSourcePaths(build, unlistedOnly);
			for (int i = 0; i < unlisted.length; i++) {
				if (unlisted[i] == null)
					break;
				buildEntry.addToken(unlisted[i]);
			}
		} catch (CoreException e) {
		}
	}

	private String[] getSourcePaths(Build build, boolean unlistedOnly) {
		IProject project = build.getModel().getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				ArrayList sourceEntries = new ArrayList();
				IBuildEntry[] entries = build.getBuildEntries();
				if (unlistedOnly)
					for (int i = 0; i < entries.length; i++)
						if (entries[i].getName().startsWith(BuildErrorReporter.SOURCE))
							sourceEntries.add(entries[i]);
				
				IJavaProject jp = JavaCore.create(project);
				IClasspathEntry[] cpes = jp.getRawClasspath();
				return BuildErrorReporter.getUnlistedClasspaths(sourceEntries, project, cpes);
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return null;
	}
}
