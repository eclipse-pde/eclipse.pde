/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools.change;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.Document;

public class BuildToBndChange extends Change {

	@SuppressWarnings("restriction")
	private static final String DS_CONTENT_TYPE_ID = org.eclipse.pde.internal.ds.core.Activator.CONTENT_TYPE_ID;
	private IBuildModel model;
	private IProject project;
	private IFile instructionfile;

	public BuildToBndChange(IProject project, IBuildModel model, IFile instructionfile) {
		this.project = project;
		this.model = model;
		this.instructionfile = instructionfile;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ProjectUpdateChange_convert_build_to_bnd;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {

	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		if (model != null) {
			Document document;
			if (instructionfile.exists()) {
				try (InputStream contents = instructionfile.getContents()) {
					document = new Document(new String(contents.readAllBytes(), StandardCharsets.UTF_8));
				} catch (IOException e) {
					throw new CoreException(Status.error("Reading file content failed", e)); //$NON-NLS-1$
				}
			} else {
				document = new Document(""); //$NON-NLS-1$
			}
			BndEditModel editModel;
			try {
				editModel = new BndEditModel(document);
			} catch (IOException e) {
				throw new CoreException(Status.error("Reading document failed", e)); //$NON-NLS-1$
			}
			IBuild build = model.getBuild();
			processBinIncludes(build, editModel);
			processAdditionalBundles(build, editModel);
			processExtraClasspath(build, editModel);
			processJars(build, editModel);
			editModel.saveChangesTo(document);
			if (instructionfile.exists()) {
				instructionfile.setContents(new ByteArrayInputStream(document.get().getBytes(StandardCharsets.UTF_8)),
						true, true, pm);
			} else {
				instructionfile.create(new ByteArrayInputStream(document.get().getBytes(StandardCharsets.UTF_8)), true,
						pm);
			}
		}
		return null;
	}

	private void processJars(IBuild build, BndEditModel editModel) {
		List<String> includeResource = new ArrayList<>(editModel.getIncludeResource());
		List<String> makeJars = new ArrayList<>();
		for (IBuildEntry buildEntry : build.getBuildEntries()) {
			String name = buildEntry.getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX)) {
				String jarName = name.substring(IBuildEntry.JAR_PREFIX.length());
				if (jarName.equals(".")) { //$NON-NLS-1$
					continue;
				}
				String outputFolder = Optional.ofNullable(build.getEntry(IBuildEntry.OUTPUT_PREFIX + jarName)).stream()
						.flatMap(entry -> Arrays.stream(entry.getTokens())).findFirst()
						.orElseGet(() -> getOutputFolderFromJava(buildEntry.getTokens()));
				String outputForInclude = getIncludeResourceForBinInclude(jarName);
				includeResource.remove(outputForInclude);
				includeResource.add(outputForInclude + ";lib:=true"); //$NON-NLS-1$
				makeJars.add(String.format("%s;type=jar;input=\"%s\"", jarName, outputFolder)); //$NON-NLS-1$
			}
		}
		if (!makeJars.isEmpty()) {
			// See https://github.com/bndtools/bnd/issues/5919
			// TODO need to use generic set instead of
			// editModel.setIncludeResource(includeResource);
			editModel.genericSet(Constants.INCLUDERESOURCE, includeResource);
			editModel.genericSet(Constants.MAKE, makeJars.stream().collect(Collectors.joining(", "))); //$NON-NLS-1$
		}

	}

	private String getOutputFolderFromJava(String[] sourceTokens) {
		try {
			IJavaProject javaProject = JavaCore.create(project);
			IPath outputLocation = javaProject.getOutputLocation();
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (String token : sourceTokens) {
						if (IPath.fromPortableString(token).equals(entry.getPath())) {
							IPath srcLoc = entry.getOutputLocation();
							if (srcLoc != null) {
								outputLocation = srcLoc;
							}
							break;
						}
					}
				}
			}
			return project.getWorkspace().getRoot().getFolder(outputLocation).getProjectRelativePath()
					.toPortableString();
		} catch (CoreException e) {
			// this should never happen but just in case return a sensible
			// default...
			return "bin/"; //$NON-NLS-1$
		}
	}

	private void processBinIncludes(IBuild build, BndEditModel editModel) {
		IBuildEntry entry = build.getEntry(IBuildEntry.BIN_INCLUDES);
		if (entry == null) {
			return;
		}
		List<String> list = Arrays.stream(entry.getTokens()).filter(str -> isCustomResource(str, build))
				.map(str -> getIncludeResourceForBinInclude(str)).toList();
		// can't use editModel.addIncludeResource because of
		// https://github.com/bndtools/bnd/pull/5904
		editModel.genericSet(Constants.INCLUDERESOURCE, list);
	}


	private boolean isCustomResource(String str, IBuild build) {
		if (".".equals(str)) { //$NON-NLS-1$
			// this is the default jar inclusion...
			return false;
		}
		if (JarFile.MANIFEST_NAME.equals(str)) {
			// the manifest we generate!
			return false;
		}
		if ("META-INF/".equals(str) || "META-INF".equals(str)) { //$NON-NLS-1$ //$NON-NLS-2$
			IFolder folder = project.getFolder(str);
			try {
				IResource[] members = folder.members();
				if (members.length == 0) {
					// empty folder with manifest previously inside it
					return false;
				}
				if (members.length == 1 && "MANIFEST.MF".equals(members[0].getName())) { //$NON-NLS-1$
					// a manifest either generated or not yet deleted... but we
					// don't want to try include it
					return false;
				}
			} catch (CoreException e) {
			}
		}
		if ("OSGI-INF/".equals(str) || "OSGI-INF".equals(str)) { //$NON-NLS-1$ //$NON-NLS-2$
			IFolder folder = project.getFolder(str);
			try {
				IResource[] members = folder.members();
				if (members.length == 0) {
					// empty folder then assume it is of no use...
				}
				for (IResource member : members) {
					if (member.getName().startsWith(".")) { //$NON-NLS-1$
						continue;
					}
					if (member instanceof IFile file) {
						IContentDescription description = file.getContentDescription();
						if (description != null) {
							IContentType contentType = description.getContentType();
							if (contentType != null && DS_CONTENT_TYPE_ID.equals(contentType.getId())) {
								// a DS component... these will be generated so
								// we can ignore it
								continue;
							}
						}
						return true;
					}
					if (member instanceof IFolder) {
						// some subfolder e.g. i10n ... we need to include it
						return true;
					}
				}
				return false;
			} catch (CoreException e) {
			}
		}
		return true;
	}

	private void processAdditionalBundles(IBuild build, BndEditModel editModel) {
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		if (entry == null) {
			return;
		}
		Arrays.stream(entry.getTokens())
				.forEach(additional -> editModel.addPath(new VersionedClause(additional), Constants.BUILDPATH));
	}

	private void processExtraClasspath(IBuild build, BndEditModel editModel) {
		IBuildEntry entry = build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
		if (entry == null) {
			return;
		}
		editModel.setClassPath(Arrays.asList(entry.getTokens()));
	}

	@Override
	public Object getModifiedElement() {
		return instructionfile;
	}

	private static String getIncludeResourceForBinInclude(String str) {
		return str.contains("/") ? (String.format("%s=%s", str, str)) : str;//$NON-NLS-1$ //$NON-NLS-2$
	}

}
