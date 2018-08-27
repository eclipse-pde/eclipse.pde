/*******************************************************************************
 * Copyright (c) 2015, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@SuppressWarnings("restriction")
public class BuildPathMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	private static final String[] ANNOTATIONS_LIB_BUNDLES = { "org.eclipse.pde.ds.lib", "org.eclipse.pde.ds1_2.lib" }; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String[] ANNOTATIONS_JAR_URLS = { "platform:/plugin/org.eclipse.pde.ds.lib/annotations.jar", "platform:/plugin/org.eclipse.pde.ds1_2.lib/annotations.jar" }; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String[] ANNOTATIONS_PACKAGE_VERSION_RANGES = { "[1.3.0,2.0.0)", "[1.2.0,2.0.0)" }; //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			return DSAnnotationCompilationParticipant.BUILDPATH_PROBLEM_MARKER.equals(marker.getType());
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		IPreferencesService prefs = Platform.getPreferencesService();
		IScopeContext[] scope = new IScopeContext[] { new ProjectScope(marker.getResource().getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE };
		DSAnnotationVersion specVersion;
		try {
			specVersion = DSAnnotationVersion.valueOf(prefs.getString(Activator.PLUGIN_ID, Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name(), scope));
		} catch (IllegalArgumentException e) {
			specVersion = DSAnnotationVersion.V1_3;
		}

		int entryIndex = Math.min(DSAnnotationVersion.V1_3.ordinal() - specVersion.ordinal(), ANNOTATIONS_LIB_BUNDLES.length);
		final String libBundle = ANNOTATIONS_LIB_BUNDLES[entryIndex];
		final String jarUrl = ANNOTATIONS_JAR_URLS[entryIndex];
		final String packageVersionRange = ANNOTATIONS_PACKAGE_VERSION_RANGES[entryIndex];

		IMarkerResolution[] resolutions = new IMarkerResolution[3];
		resolutions[0] = new BuildPathMarkerResolution(
				Messages.BuildPathMarkerResolutionGenerator_additionalBundleResolution_label,
				NLS.bind(Messages.BuildPathMarkerResolutionGenerator_additionalBundleResolution_description, libBundle),
				PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ)) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(PDEProject.getBuildProperties(project)) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBuildModel) {
							addAdditionalBundle((IBuildModel) model, libBundle);
						}
					}
				};
			}
		};

		resolutions[1] = new BuildPathMarkerResolution(
				Messages.BuildPathMarkerResolutionGenerator_extraLibraryResolution_label,
				NLS.bind(Messages.BuildPathMarkerResolutionGenerator_extraLibraryResolution_description, libBundle),
				JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_JAR_WITH_SOURCE)) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(PDEProject.getBuildProperties(project)) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBuildModel) {
							addExtraLibrary((IBuildModel) model, jarUrl);
						}
					}
				};
			}
		};

		resolutions[2] = new BuildPathMarkerResolution(
				Messages.BuildPathMarkerResolutionGenerator_packageImportResolution_label,
				NLS.bind(Messages.BuildPathMarkerResolutionGenerator_packageImportResolution_description, DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE),
				PDEPluginImages.get(PDEPluginImages.OBJ_DESC_PACKAGE)) {
			@Override
			protected ModelModification createModification(IMarker marker) {
				IProject project = (IProject) marker.getResource();
				return new ModelModification(project) {
					@Override
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (model instanceof IBundlePluginModelBase) {
							addPackageImport((IBundlePluginModelBase) model, packageVersionRange);
						}
					}
				};
			}
		};

		return resolutions;
	}

	private void addAdditionalBundle(IBuildModel model, String libBundle) throws CoreException {
		IBuild build = model.getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		if (entry == null) {
			entry = model.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			build.add(entry);
		}

		if (!entry.contains(libBundle)) {
			entry.addToken(libBundle);
		}
	}

	private void addExtraLibrary(IBuildModel model, String jarUrl) throws CoreException {
		IBuild build = model.getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null) {
			entry = model.getFactory().createEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			build.add(entry);
		}

		if (!entry.contains(jarUrl)) {
			entry.addToken(jarUrl);
		}
	}

	private void addPackageImport(IBundlePluginModelBase model, String packageVersionRange) throws CoreException {
		IBundleModel bundleModel = model.getBundleModel();
		IBundle bundle = bundleModel.getBundle();
		ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		boolean changed = false;
		if (header == null) {
			header = (ImportPackageHeader) bundleModel.getFactory().createHeader(Constants.IMPORT_PACKAGE, ""); //$NON-NLS-1$
			changed = true;
		}

		ImportPackageObject pkg = header.getPackage(DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE);
		if (pkg == null) {
			pkg = header.addPackage(DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE);
			pkg.setVersion(packageVersionRange);
			pkg.setOptional(true);
			changed = true;
		} else if (pkg.getVersion() != null && !new VersionRange(pkg.getVersion()).includes(new Version(1, 2, 0))) {
			pkg.setVersion(packageVersionRange);
			changed = true;
		}

		if (changed) {
			header.update();
			bundle.setHeader(Constants.IMPORT_PACKAGE, header.getValue());
		}
	}
}
