/*******************************************************************************
 * Copyright (c) 2015, 2019 Ecliptical Software Inc. and others.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
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

@SuppressWarnings("restriction")
public class DSAnnotationClasspathContributor implements IClasspathContributor {

	// private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { JavaCore.newAccessRule(new Path("org/osgi/service/component/annotations/*"), IAccessRule.K_DISCOURAGED | IAccessRule.IGNORE_IF_BETTER) };
	private static final IAccessRule[] ANNOTATION_ACCESS_RULES = { };

	private static final IClasspathAttribute[] DS_ATTRS = { JavaCore.newClasspathAttribute(Activator.CP_ATTRIBUTE, Boolean.toString(true)) };

	private static final String ANNOTATIONS_JAR = "annotations.jar"; //$NON-NLS-1$

	private static final String ANNOTATIONS_SRC_ZIP = "annotationssrc.zip"; //$NON-NLS-1$

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		IPluginModelBase model = PluginRegistry.findModel(project);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource == null || WorkspaceModelManager.isBinaryProject(resource.getProject())) {
				return Collections.emptyList();
			}

			IPreferencesService prefs = Platform.getPreferencesService();
			IScopeContext[] scope = new IScopeContext[] { new ProjectScope(resource.getProject()),
					InstanceScope.INSTANCE, DefaultScope.INSTANCE };
			boolean enabled = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_ENABLED, false, scope);
			if (enabled) {
				boolean autoClasspath = prefs.getBoolean(Activator.PLUGIN_ID, Activator.PREF_CLASSPATH, true, scope);
				if (autoClasspath) {
					DSAnnotationVersion specVersion;
					try {
						specVersion = DSAnnotationVersion.valueOf(prefs.getString(Activator.PLUGIN_ID,
								Activator.PREF_SPEC_VERSION, DSAnnotationVersion.V1_3.name(), scope));
					} catch (IllegalArgumentException e) {
						specVersion = DSAnnotationVersion.V1_3;
					}

					String jarDir;
					if (specVersion == DSAnnotationVersion.V1_3) {
						jarDir = "lib/"; //$NON-NLS-1$
					} else {
						jarDir = "lib1_2/"; //$NON-NLS-1$
					}

					IPluginModelBase bundle = PluginRegistry.findModel(Activator.PLUGIN_ID);
					if (bundle != null && bundle.isEnabled()) {
						String location = bundle.getInstallLocation();
						if (location != null) {
							File locationFile = new File(location);
							IPath dirPath;
							if (locationFile.isFile()) {
								String cacheDir = computeBinDirname(location, bundle.getTimeStamp());
								IPath cacheDirPath = Activator.getDefault().getStateLocation().addTrailingSeparator()
										.append("jars/").append(cacheDir); //$NON-NLS-1$
								dirPath = cacheDirPath.addTrailingSeparator().append(jarDir);
								File cacheDirFile = cacheDirPath.toFile();
								if (!cacheDirFile.exists()) {
									deleteCache(cacheDirFile.getParentFile());
									File jarDirFile = new File(cacheDirFile, jarDir);
									try {
										Files.createDirectories(jarDirFile.toPath());
										try (JarFile jarFile = new JarFile(locationFile)) {
											extractJarEntry(jarFile, jarDir + ANNOTATIONS_JAR,
													new File(jarDirFile, ANNOTATIONS_JAR));
											extractJarEntry(jarFile, jarDir + ANNOTATIONS_SRC_ZIP,
													new File(jarDirFile, ANNOTATIONS_SRC_ZIP));
										}
									} catch (IOException e) {
										Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
												"Error creating classpath entry.", e)); //$NON-NLS-1$
									}
								}
							} else {
								dirPath = new Path(location).addTrailingSeparator().append(jarDir);
							}
							
							IPath jarPath = dirPath.append(ANNOTATIONS_JAR);
							IPath srcPath = dirPath.append(ANNOTATIONS_SRC_ZIP);
							IClasspathEntry entry = JavaCore.newLibraryEntry(jarPath, srcPath, Path.ROOT,
									ANNOTATION_ACCESS_RULES, DS_ATTRS, false);
							DSLibPluginModelListener.addProject(JavaCore.create(resource.getProject()));
							return Collections.singletonList(entry);
						}
					}
				}
			}

			DSLibPluginModelListener.removeProject(JavaCore.create(resource.getProject()));
		}

		return Collections.emptyList();
	}

	private void deleteCache(File dir) {
		if (dir.isDirectory()) {
			try (DirectoryStream<java.nio.file.Path> dirs = Files.newDirectoryStream(dir.toPath(),
					path -> path.toFile().isDirectory())) {
				dirs.forEach(path -> {
					try {
						Files.walk(path).sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile)
								.forEach(File::delete);
					} catch (IOException e) {
						Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								"Error deleting stale cache entries.", e)); //$NON-NLS-1$
					}
				});
			} catch (IOException e2) {
				Activator.log(
						new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error enumerating stale cache entries.", e2)); //$NON-NLS-1$
			}
		}
	}

	private void extractJarEntry(JarFile jar, String entryName, File out) throws IOException {
		JarEntry entry = jar.getJarEntry(entryName);
		if (entry != null) {
			try (InputStream in = jar.getInputStream(entry)) {
				Files.copy(in, out.toPath());
			}
		}
	}

	private String computeBinDirname(String location, long timestamp) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
					"Error computing cache directory name.", e)); //$NON-NLS-1$
			return String.format("%d-%d", location.hashCode(), timestamp); //$NON-NLS-1$
		}

		md.update(location.getBytes(StandardCharsets.UTF_8));
		md.update(ByteBuffer.allocate(Long.BYTES).putLong(timestamp).array());
		BigInteger num = new BigInteger(1, md.digest());
		return num.toString(16);
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		return Collections.emptyList();
	}
}
