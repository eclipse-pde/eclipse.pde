/*******************************************************************************
 * Copyright (c) 2020, 2021 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Kriens <Peter.Kriens@aqute.biz> - initial API and implementation
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.autocomplete;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Descriptors;

public class AddBundleCompletionProposal extends WorkspaceJob implements IJavaCompletionProposal {

	final BundleId					bundle;
	final String					displayString;
	final int						relevance;
	final IInvocationContext		context;
	final Project					project;
	final String					pathtype;
	final Map<String, Boolean>		classes;

	public AddBundleCompletionProposal(BundleId bundle, Map<String, Boolean> classes, int relevance,
		IInvocationContext context, Project project, String pathtype) {
		super("Adding '" + bundle.getBsn() + "' to " + project + " " + pathtype);
		this.classes = classes;
		this.bundle = bundle;
		this.relevance = relevance;
		this.context = context;
		this.project = project;
		this.pathtype = pathtype;
		this.displayString = String.format("Add %s to %s (found %s)", bundle.getBsn(), pathtype,
				classes.keySet()
			.stream()
			.sorted()
			.collect(Collectors.joining(", ")));
	}

	@Override
	public void apply(org.eclipse.jface.text.IDocument document) {
		schedule();
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	@Override
	public Point getSelection(org.eclipse.jface.text.IDocument document) {
		return new Point(context.getSelectionOffset(), context.getSelectionLength());
	}

	@Override
	public String getAdditionalProposalInfo() {
		// As far as I can tell, this method is not called for quick fixes -
		// only for content assists.
		return displayString;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Image getImage() {
		return Resources.getImage("/icons/bundle.png");
	}

	@Override
	public IContextInformation getContextInformation() {
		// Not called for quick fixes.
		return null;
	}

	@Override
	public int getRelevance() {
		return relevance;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		try {
			Workspace ws = project.getWorkspace();
			IStatus status = ws.readLocked(() -> {

				BndEditModel model = new BndEditModel(project);
				model.load();
//TODO
//				BndPreferences prefs = new BndPreferences();
				VersionedClause vc;
//				switch (prefs.getQuickFixVersioning()) {
//					case latest:
//						vc = new VersionedClause(bundle.getBsn() + ";version=latest");
//						break;
//					case noversion :
//					default :
						vc = new VersionedClause(bundle.getBsn());
//						break;
//				}
				model.addPath(vc, pathtype);
				model.saveChanges();
				refreshFile(project.getPropertiesFile());
				return Status.OK_STATUS;

			}, monitor::isCanceled);

			classes.entrySet()
				.stream()
				.filter(Entry::getValue)
				.forEach(pair -> {
					String fqn = pair.getKey();
					String[] determine = Descriptors.determine(fqn)
						.unwrap();

					assert determine[0] != null : "We must have found a package";
					try {
						if (determine[1] == null) {
							context.getCompilationUnit()
								.createImport(fqn + ".*", null, monitor);
						} else {
							context.getCompilationUnit()
								.createImport(fqn, null, monitor);
						}
					} catch (JavaModelException jme) {
							ILog.get().error("Couldn't add import for " + fqn, jme);
					}
				});
			return status;
		} catch (Exception e) {
			return new Status(IStatus.ERROR, "bndtools.core.services",
				"Failed to add bundle " + bundle + " to " + pathtype, e);
		}
	}

	private void refreshFile(File propertiesFile) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(IPath.fromOSString(propertiesFile.getAbsolutePath()));
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			// if we can't refresh we can't do anything here...
		}

	}
}
