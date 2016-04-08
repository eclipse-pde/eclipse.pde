/*******************************************************************************
 * Copyright (c) 2015 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class DSAnnotationClasspathContributor implements IClasspathContributor {

	// private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { JavaCore.newAccessRule(new Path("org/osgi/service/component/annotations/*"), IAccessRule.K_DISCOURAGED | IAccessRule.IGNORE_IF_BETTER) };
	private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { };

	private static final IClasspathAttribute[] DS_ATTRS = { JavaCore.newClasspathAttribute(Activator.CP_ATTRIBUTE, Boolean.toString(true)) };

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		if (bundle != null) {
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				IResource resource = model.getUnderlyingResource();
				if (resource != null && !WorkspaceModelManager.isBinaryProject(resource.getProject())) {
					boolean autoClasspath = Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID, Activator.PREF_CLASSPATH, true, new IScopeContext[] { new ProjectScope(resource.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
					if (autoClasspath) {
						try {
							URL fileURL = FileLocator.toFileURL(bundle.getEntry("lib/annotations.jar")); //$NON-NLS-1$
							if ("file".equals(fileURL.getProtocol())) { //$NON-NLS-1$
								URL srcFileURL = FileLocator.toFileURL(bundle.getEntry("lib/annotationssrc.zip")); //$NON-NLS-1$
								IPath srcPath = "file".equals(srcFileURL.getProtocol()) ? new Path(srcFileURL.getPath()) : null; //$NON-NLS-1$
								IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(fileURL.getPath()), srcPath, Path.ROOT, ANNOTATION_ACCESS_RULES, DS_ATTRS, false);
								return Collections.singletonList(entry);
							}
						} catch (IOException e) {
							Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error creating classpath entry.", e)); //$NON-NLS-1$
						}
					}
				}
			}
		}

		return Collections.emptyList();
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		return Collections.emptyList();
	}
}
