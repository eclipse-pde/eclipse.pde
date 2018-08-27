/*******************************************************************************
 * Copyright (c) Sep 26, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.refactoring.CreateFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.progress.UIJob;

/**
 * Default quick fix processor for API tools
 *
 * @since 1.0.500
 */
public class ApiQuickFixProcessor implements IQuickFixProcessor {

	class UnknownAnnotationQuickFix implements IJavaCompletionProposal {

		ICompilationUnit fUnit = null;
		String fName = null;

		public UnknownAnnotationQuickFix(ICompilationUnit unit, String qualifiedname) {
			fUnit = unit;
			fName = qualifiedname;
		}

		@Override
		public void apply(IDocument document) {
			UIJob job = new UIJob("Update project to use API Tools annotations") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						Change changes = createChange(fUnit, fName);
						changes.perform(monitor);

					} catch (CoreException e) {
						ApiUIPlugin.log(e);
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.INTERACTIVE);
			job.schedule();
		}

		@Override
		public Point getSelection(IDocument document) {
			return null;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return MarkerMessages.UnknownAnnotationResolution_0;
		}

		@Override
		public String getDisplayString() {
			return NLS.bind(MarkerMessages.UnknownAnnotationResolution_1, new Object[] {
					Signatures.getSimpleTypeName(fName),
					Signatures.getPackageName(fName) });
		}

		@Override
		public Image getImage() {
			return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_CHANGE_CORRECTION);
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public int getRelevance() {
			return 0;
		}

	}

	static final String JAVA_ELEMENT_DELIMITERS = TextProcessor.getDefaultDelimiters() + "<>(),?{} "; //$NON-NLS-1$

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return problemId == IProblem.UndefinedType;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		List<IJavaCompletionProposal> proposals = new ArrayList<>();
		ICompilationUnit unit = context.getCompilationUnit();
		IProject project = unit.getJavaProject().getProject();
		if (!project.hasNature(ApiPlugin.NATURE_ID)) {
			return new IJavaCompletionProposal[0];
		}
		IFile build = project.getFile("build.properties"); //$NON-NLS-1$
		if (needsBuildPropertiesChange(build)) {
			for (IProblemLocation location : locations) {
				if (location.getProblemId() == IProblem.UndefinedType) {
					String[] args = location.getProblemArguments();
					if (args.length == 1) {
						// only one argument in the missing annotation problem
						for (String arg : args) {
							String name = ApiPlugin.getJavadocTagManager().getQualifiedNameForAnnotation(arg);
							if (name != null) {
								proposals.add(new UnknownAnnotationQuickFix(unit, name));
							}
						}
					}
				}
			}
		}
		return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
	}

	/**
	 * Creates the set of changes to update the compilation unit to use API
	 * Tools annotations
	 *
	 * @param unit
	 * @param qualifiedname
	 * @return the {@link Change}, never <code>null</code>
	 * @throws CoreException
	 */
	public static Change createChange(ICompilationUnit unit, String qualifiedname) throws CoreException {
		IProject project = unit.getJavaProject().getProject();
		IFile buildProperties = project.getFile("build.properties"); //$NON-NLS-1$
		boolean isBundle = project.hasNature(ApiPlugin.NATURE_ID);
		if (!isBundle) {
			return new NullChange();
		}
		return new CompositeChange(MarkerMessages.UnknownAnnotationResolution_3, new Change[] {
				createBuildPropertiesChange(buildProperties),
				createAddImportChange(unit, qualifiedname) });
	}

	/**
	 * Create the {@link Change} object for adding / updating the
	 * build.properties file as needed
	 *
	 * @param build
	 * @return the {@link Change} object, never <code>null</code>
	 * @throws CoreException
	 */
	public static Change createBuildPropertiesChange(IFile build) throws CoreException {
		String buildPropertiesEntry = "additional.bundles = org.eclipse.pde.api.tools.annotations"; //$NON-NLS-1$
		if (!build.exists()) {
			return new CreateFileChange(build.getFullPath(), buildPropertiesEntry, null);
		} else {
			TextFileChange change = new TextFileChange(MarkerMessages.UnknownAnnotationResolution_2, build);
			change.setEdit(new MultiTextEdit());
			IPath filepath = build.getFullPath();
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(filepath, LocationKind.IFILE, null);
			try {
				ITextFileBuffer textFileBuffer = manager.getTextFileBuffer(filepath, LocationKind.IFILE);
				IDocument document = textFileBuffer.getDocument();
				String lineDelim = TextUtilities.getDefaultLineDelimiter(document);
				IRegion match = new FindReplaceDocumentAdapter(document).find(0, "additional\\.bundles\\s*=\\s*", true, false, false, true); //$NON-NLS-1$
				if (match != null) {
					StringBuilder buf = new StringBuilder("org.eclipse.pde.api.tools.annotations,\\").append(lineDelim); //$NON-NLS-1$
					int spaces = match.getOffset() + match.getLength() - document.getLineOffset(document.getLineOfOffset(match.getOffset()));
					while (spaces-- > 0) {
						buf.append(' ');
					}
					change.addEdit(new InsertEdit(match.getOffset() + match.getLength(), buf.toString()));
				} else {
					String entry = buildPropertiesEntry + lineDelim;
					int len = document.getLength();
					if (len > 0 && document.getLineInformation(document.getNumberOfLines() - 1).getLength() != 0) {
						entry = lineDelim + entry;
					}
					change.addEdit(new InsertEdit(len, entry));
				}
				return change;
			} catch (BadLocationException e) {
				ApiUIPlugin.log(e);
				return new NullChange();
			} finally {
				manager.disconnect(filepath, LocationKind.IFILE, null);
			}
		}
	}

	public static boolean needsBuildPropertiesChange(IFile file) {
		if (file.exists()) {
			Properties props = new Properties();
			InputStream stream = null;
			try {
				stream = file.getContents();
				props.load(file.getContents());
				String entry = (String) props.get("additional.bundles"); //$NON-NLS-1$
				if (entry != null) {
					if (entry.contains("org.eclipse.pde.api.tools.annotations")) { //$NON-NLS-1$
						return false;
					}
				}
			} catch (CoreException ce) {
				ApiUIPlugin.log(ce);
			} catch (IOException e) {
				ApiUIPlugin.log(e);
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						ApiUIPlugin.log(e);
					}
				}
			}
		}
		return true;
	}

	/**
	 * Creates a new {@link Change}
	 *
	 * @param cu
	 * @param fullyQualifiedName
	 * @return
	 * @throws CoreException
	 */
	public static CompilationUnitChange createAddImportChange(ICompilationUnit cu, String fullyQualifiedName) throws CoreException {
		String[] args = {
				TextProcessor.process(Signature.getSimpleName(fullyQualifiedName), JAVA_ELEMENT_DELIMITERS),
				TextProcessor.process(Signature.getQualifier(fullyQualifiedName), JAVA_ELEMENT_DELIMITERS) };
		String label = NLS.bind(MarkerMessages.UnknownAnnotationResolution_4, args);
		CompilationUnitChange cuChange = new CompilationUnitChange(label, cu);
		ImportRewrite rewrite = CodeStyleConfiguration.createImportRewrite(cu, true);
		rewrite.setUseContextToFilterImplicitImports(true);
		rewrite.addImport(fullyQualifiedName);
		cuChange.setEdit(rewrite.rewriteImports(null));
		return cuChange;
	}
}
