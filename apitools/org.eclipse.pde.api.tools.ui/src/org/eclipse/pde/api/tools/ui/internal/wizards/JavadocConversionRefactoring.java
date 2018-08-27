/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.markers.ApiQuickFixProcessor;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.ibm.icu.text.MessageFormat;

/**
 * The refactoring that will convert API tools Javadoc tags to the new
 * annotations.
 *
 * @since 1.0.500
 */
public class JavadocConversionRefactoring extends Refactoring {

	static Map<String, String> ALL_API_IMPORTS;
	static {
		ALL_API_IMPORTS = new HashMap<>();
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOEXTEND, "org.eclipse.pde.api.tools.annotations.NoExtend"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOIMPLEMENT, "org.eclipse.pde.api.tools.annotations.NoImplement"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOINSTANTIATE, "org.eclipse.pde.api.tools.annotations.NoInstantiate"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOOVERRIDE, "org.eclipse.pde.api.tools.annotations.NoOverride"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOREFERENCE, "org.eclipse.pde.api.tools.annotations.NoReference"); //$NON-NLS-1$
	}

	/**
	 * The projects to check
	 */
	private HashSet<IProject> projects = new HashSet<>();

	/**
	 * Whether to remove the existing javadoc tags
	 */
	private boolean removeTags = true;

	/**
	 * Constructor
	 */
	public JavadocConversionRefactoring() {
	}

	/**
	 * Whether to remove the existing javadoc tags as part of the refactoring
	 *
	 * @param remove
	 */
	public void setRemoveTags(boolean remove) {
		removeTags = remove;
	}

	/**
	 * Set the projects to run the refactoring on
	 *
	 * @param newProjects
	 */
	public void setProjects(Set<IProject> newProjects) {
		projects.clear();
		if (newProjects != null) {
			projects.addAll(newProjects);
		}
	}

	@Override
	public String getName() {
		return WizardMessages.JavadocConversionRefactoring_convert_tag_to_annotation_refactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		SubMonitor localmonitor = SubMonitor.convert(pm, WizardMessages.JavadocConversionPage_scanning_projects_for_javadoc_tags, projects.size() * 100);
		CompositeChange change = new CompositeChange(WizardMessages.JavadocTagRefactoring_1);
		IProject project = null;
		CompositeChange pchange = null;
		for (Iterator<IProject> iterator = projects.iterator(); iterator.hasNext();) {
			if (localmonitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			project = iterator.next();
			localmonitor.setTaskName(NLS.bind(WizardMessages.JavadocConversionPage_scan_javadoc_to_convert, new Object[] { project.getName() }));
			pchange = new CompositeChange(project.getName());
			IFile build = project.getFile("build.properties"); //$NON-NLS-1$
			if (ApiQuickFixProcessor.needsBuildPropertiesChange(build)) {
				try {
					pchange.add(ApiQuickFixProcessor.createBuildPropertiesChange(build));
				} catch (CoreException ce) {
					// do nothing and continue, the quick fix is
					// available if for some
					// reason we fail to create the change
				}
			}
			// collect the changes for the conversion
			try {
				createChanges(pchange, JavaCore.create(project), removeTags, localmonitor.split(100));
			} catch (CoreException e) {
				ApiUIPlugin.log(e);
			}

			if (localmonitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			if (pchange.getChildren().length > 0) {
				change.add(pchange);
			}
		}
		return change;
	}

	/**
	 * Creates all of the text edit changes for the conversion. The collected
	 * edits are arranged as multi-edits for the one file that they belong to
	 *
	 * @param projectchange the composite change to add to
	 * @param project the project to scan
	 * @param remove if the found tags should also be removed
	 * @param monitor the progress monitor
	 * @throws CoreException
	 */
	RefactoringStatus createChanges(CompositeChange projectchange, IJavaProject project, boolean remove, SubMonitor monitor) throws CoreException {
		HashMap<IFile, Set<TextEdit>> map = new HashMap<>();
		// XXX visit all CU's -> all doc nodes -> create add annotations
		RefactoringStatus status = collectAnnotationEdits(project, map, remove, monitor.split(75));
		if (status.isOK()) {
			IFile file = null;
			TextFileChange change = null;
			MultiTextEdit multiedit = null;
			Set<TextEdit> alledits = null;
			for (Entry<IFile, Set<TextEdit>> entry : map.entrySet()) {
				if (monitor.isCanceled()) {
					return status;
				}
				file = entry.getKey();
				monitor.setTaskName(NLS.bind(WizardMessages.JavadocConversionPage_collect_edits, new Object[] { file.getName() }));
				change = new TextFileChange(MessageFormat.format(WizardMessages.JavadocConversionPage_convert_javadoc_tags_in, file.getName()), file);
				multiedit = new MultiTextEdit();
				change.setEdit(multiedit);
				alledits = entry.getValue();
				if (alledits != null) {
					for (TextEdit edit : alledits) {
						multiedit.addChild(edit);
					}
				}
				projectchange.add(change);
			}
		}
		monitor.setWorkRemaining(0);
		return status;
	}

	/**
	 * Scans the source and collects all of the changes in the given collector
	 *
	 * @param project the project to scan
	 * @param collector the map to collect the edits in
	 * @param remove if the old Javadoc tags should be removed as well
	 * @param monitor the prgress monitor
	 * @throws CoreException
	 */
	RefactoringStatus collectAnnotationEdits(IJavaProject project, Map<IFile, Set<TextEdit>> collector, boolean remove, IProgressMonitor monitor) throws CoreException {
		RefactoringStatus status = new RefactoringStatus();
		IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		if (baseline != null) {
			IApiComponent component = baseline.getApiComponent(project.getProject());
			if (component != null) {
				IApiDescription description = component.getApiDescription();
				AnnotVisitor visitor = new AnnotVisitor(project, component, description, remove, monitor);
				description.accept(visitor, null);
				collector.putAll(visitor.changes);
			}
		}
		return status;
	}

	/**
	 * Visitor for the API description.
	 */
	class AnnotVisitor extends ApiDescriptionVisitor {

		Map<IFile, Set<TextEdit>> changes = new HashMap<>();
		boolean remove = false;
		IJavaProject project = null;
		IApiComponent component = null;
		IApiDescription apidescription = null;
		SubMonitor monitor = null;

		/**
		 * Constructor
		 *
		 * @param project the project context
		 * @param component the backing {@link IApiComponent}
		 * @param description the backing API description
		 * @param remove if the Javadoc tags should be removed
		 * @param monitor the progress monitor
		 */
		public AnnotVisitor(IJavaProject project, IApiComponent component, IApiDescription description, boolean remove, IProgressMonitor monitor) {
			this.project = project;
			this.component = component;
			this.apidescription = description;
			this.remove = remove;
			this.monitor = SubMonitor.convert(monitor, 100);
		}

		@Override
		public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
			if (element.getElementType() == IElementDescriptor.TYPE) {
				// only collect the root types, we will sort out the inner
				// members when we visit the IType
				IType type;
				try {
					type = project.findType(((IReferenceTypeDescriptor) element).getQualifiedName(), new NullProgressMonitor());
					if (type != null) {
						collectUpdates(type, element, apidescription);
					}
				} catch (OperationCanceledException e) {
					return false;
				} catch (JavaModelException e) {
					ApiUIPlugin.log(e);
				} catch (CoreException e) {
					ApiUIPlugin.log(e);
				}
				return false;
			}
			return super.visitElement(element, description);
		}

		/**
		 * Parses the AST for the given {@link IType} and collects edits for
		 * adding annotations and additionally removing Javadoc tags
		 *
		 * @param type the type to scan
		 * @param element the element
		 * @param description the backing API description
		 * @throws CoreException
		 */
		void collectUpdates(IType type, IElementDescriptor element, IApiDescription description) throws CoreException {
			ASTParser parser = ASTParser.newParser(AST.JLS10);
			ICompilationUnit cunit = type.getCompilationUnit();
			if (cunit != null) {
				if (this.monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				this.monitor.worked(1);
				this.monitor.setTaskName(NLS.bind(WizardMessages.JavadocConversionPage_scan_javadoc_to_convert, new Object[] { type.getFullyQualifiedName() }));
				parser.setSource(cunit);
				parser.setResolveBindings(true);
				Map<String, String> options = cunit.getJavaProject().getOptions(true);
				options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				parser.setCompilerOptions(options);
				CompilationUnit cast = (CompilationUnit) parser.createAST(new NullProgressMonitor());
				cast.recordModifications();
				ASTRewrite rewrite = ASTRewrite.create(cast.getAST());
				TagVisitor visitor = new TagVisitor(component, description, rewrite, this.remove);
				cast.accept(visitor);
				ITextFileBufferManager bm = FileBuffers.getTextFileBufferManager();
				IPath path = cast.getJavaElement().getPath();
				try {
					bm.connect(path, LocationKind.IFILE, null);
					ITextFileBuffer tfb = bm.getTextFileBuffer(path, LocationKind.IFILE);
					IDocument document = tfb.getDocument();
					TextEdit edit = rewrite.rewriteAST(document, null);
					if (edit.getChildrenSize() > 0 || edit.getLength() != 0) {
						IFile file = (IFile) cunit.getUnderlyingResource();
						Set<TextEdit> edits = changes.get(file);
						if (edits == null) {
							edits = new HashSet<>(3);
							changes.put(file, edits);
						}
						edits.add(edit);
					}
				} finally {
					bm.disconnect(path, LocationKind.IFILE, null);
				}
			}
		}
	}

	/**
	 * Visits the AST of an {@link IType} and collects annotation additions and
	 * optionally Javadoc tag removals
	 */
	class TagVisitor extends ASTVisitor {

		IApiComponent component = null;
		IApiDescription apidescription = null;
		ASTRewrite rewrite = null;
		boolean remove = false;
		List<String> existingImports = new ArrayList<>();
		List<String> missingImports = new ArrayList<>();

		/**
		 * Constructor
		 *
		 * @param component
		 * @param description
		 * @param rewrite
		 * @param remove
		 */
		public TagVisitor(IApiComponent component, IApiDescription description, ASTRewrite rewrite, boolean remove) {
			this.component = component;
			this.apidescription = description;
			this.rewrite = rewrite;
			this.remove = remove;
		}

		@Override
		public void endVisit(CompilationUnit node) {
			if (missingImports.size() > 0) {
				ListRewrite lrewrite = getListrewrite(node);
				if (lrewrite != null) {
					for (String missing : missingImports) {
						ImportDeclaration imp = node.getAST().newImportDeclaration();
						imp.setName(node.getAST().newName(missing));
						lrewrite.insertLast(imp, null);
					}
				}
			}
			missingImports.clear();
			existingImports.clear();
			super.endVisit(node);
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			String name = node.getName().getFullyQualifiedName();
			if (!ALL_API_IMPORTS.values().contains(name)) {
				existingImports.add(name);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			ITypeBinding binding = node.resolveBinding();
			if (binding != null) {
				IReferenceTypeDescriptor desc = Factory.typeDescriptor(binding.getQualifiedName());
				IApiAnnotations annots = apidescription.resolveAnnotations(desc);
				if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
					updateNode(node, annots);
				}
			}
			return true;
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			ITypeBinding binding = node.resolveBinding();
			if (binding != null) {
				IReferenceTypeDescriptor desc = Factory.typeDescriptor(binding.getQualifiedName());
				IApiAnnotations annots = apidescription.resolveAnnotations(desc);
				if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
					updateNode(node, annots);
				}
			}
			return true;
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			ITypeBinding binding = node.resolveBinding();
			if (binding != null) {
				IReferenceTypeDescriptor desc = Factory.typeDescriptor(binding.getQualifiedName());
				IApiAnnotations annots = apidescription.resolveAnnotations(desc);
				if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
					updateNode(node, annots);
				}
			}
			return true;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			ASTNode parent = node.getParent();
			ITypeBinding binding = null;
			if (parent instanceof AbstractTypeDeclaration) {
				binding = ((AbstractTypeDeclaration) parent).resolveBinding();
			} else if (parent instanceof AnnotationTypeDeclaration) {
				binding = ((AnnotationTypeDeclaration) parent).resolveBinding();
			}
			if (binding != null) {
				List<VariableDeclarationFragment> fragments = node.fragments();
				IFieldDescriptor desc = Factory.fieldDescriptor(binding.getQualifiedName(), fragments.get(0).getName().getIdentifier());
				IApiAnnotations annots = apidescription.resolveAnnotations(desc);
				if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
					updateNode(node, annots);
				}
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			ASTNode parent = node.getParent();
			ITypeBinding binding = null;
			if (parent instanceof AbstractTypeDeclaration) {
				binding = ((AbstractTypeDeclaration) parent).resolveBinding();
			} else if (parent instanceof AnnotationTypeDeclaration) {
				binding = ((AnnotationTypeDeclaration) parent).resolveBinding();
			}
			if (binding != null) {
				IMethodDescriptor method = Factory.methodDescriptor(binding.getQualifiedName(),
						node.getName().getIdentifier(), Signatures.getMethodSignatureFromNode(node, true));
				try {
					method = Factory.resolveMethod(this.component, method);
					IApiAnnotations annots = apidescription.resolveAnnotations(method);
					if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
						updateNode(node, annots);
					}
				} catch (CoreException ce) {
					// do nothing, keep looking
				}
			}
			return true;
		}

		/**
		 * Adds any missing annotations and optionally removes any Javadoc tags
		 *
		 * @param body
		 * @param annotations
		 */
		void updateNode(BodyDeclaration body, IApiAnnotations annotations) {
			ListRewrite lrewrite = getListrewrite(body);
			if (lrewrite != null) {
				AST ast = body.getAST();
				List<IExtendedModifier> mods = body.modifiers();
				if (mods == null) {
					mods = new ArrayList<>();
					rewrite.set(body, body.getModifiersProperty(), mods, null);
				}
				List<String> existing = new ArrayList<>();
				for (IExtendedModifier modifier : mods) {
					if (modifier.isAnnotation()) {
						Annotation annot = (Annotation) modifier;
						String name = annot.getTypeName().getFullyQualifiedName();
						if (JavadocTagManager.ALL_ANNOTATIONS.contains(name)) {
							existing.add(name);
						}
					}
				}
				int restrictions = annotations.getRestrictions();
				if (RestrictionModifiers.isExtendRestriction(restrictions) && !existing.contains(JavadocTagManager.ANNOTATION_NOEXTEND)) {
					MarkerAnnotation newannot = ast.newMarkerAnnotation();
					newannot.setTypeName(ast.newName(JavadocTagManager.ANNOTATION_NOEXTEND));
					lrewrite.insertFirst(newannot, null);
					ensureImport(JavadocTagManager.ANNOTATION_NOEXTEND);
				}
				if (RestrictionModifiers.isImplementRestriction(restrictions) && !existing.contains(JavadocTagManager.ANNOTATION_NOIMPLEMENT)) {
					MarkerAnnotation newannot = ast.newMarkerAnnotation();
					newannot.setTypeName(ast.newName(JavadocTagManager.ANNOTATION_NOIMPLEMENT));
					lrewrite.insertFirst(newannot, null);
					ensureImport(JavadocTagManager.ANNOTATION_NOIMPLEMENT);
				}
				if (RestrictionModifiers.isInstantiateRestriction(restrictions) && !existing.contains(JavadocTagManager.ANNOTATION_NOINSTANTIATE)) {
					MarkerAnnotation newannot = ast.newMarkerAnnotation();
					newannot.setTypeName(ast.newName(JavadocTagManager.ANNOTATION_NOINSTANTIATE));
					lrewrite.insertFirst(newannot, null);
					ensureImport(JavadocTagManager.ANNOTATION_NOINSTANTIATE);
				}
				if (RestrictionModifiers.isOverrideRestriction(restrictions) && !existing.contains(JavadocTagManager.ANNOTATION_NOOVERRIDE)) {
					MarkerAnnotation newannot = ast.newMarkerAnnotation();
					newannot.setTypeName(ast.newName(JavadocTagManager.ANNOTATION_NOOVERRIDE));
					lrewrite.insertFirst(newannot, null);
					ensureImport(JavadocTagManager.ANNOTATION_NOOVERRIDE);
				}
				if (RestrictionModifiers.isReferenceRestriction(restrictions) && !existing.contains(JavadocTagManager.ANNOTATION_NOREFERENCE)) {
					MarkerAnnotation newannot = ast.newMarkerAnnotation();
					newannot.setTypeName(ast.newName(JavadocTagManager.ANNOTATION_NOREFERENCE));
					lrewrite.insertFirst(newannot, null);
					ensureImport(JavadocTagManager.ANNOTATION_NOREFERENCE);
				}
			}
			if (this.remove) {
				// get rid of all API tools tags if the use says so
				Javadoc docnode = body.getJavadoc();
				if (docnode != null) {
					List<TagElement> tags = docnode.tags();
					lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
					for (TagElement tag : tags) {
						if (JavadocTagManager.ALL_TAGS.contains(tag.getTagName())) {
							lrewrite.remove(tag, null);
						}
					}
				}
			}
		}

		/**
		 * Checks to see if the required import is existing for any added
		 * annotation. If any are found to be missing they will be added when we
		 * are finished visiting the {@link CompilationUnit}
		 *
		 * @param added
		 */
		void ensureImport(String added) {
			String annot = ALL_API_IMPORTS.get(added);
			if (annot != null) {
				if (!existingImports.contains(annot)) {
					missingImports.add(annot);
				}
			}
		}

		/**
		 * Return the {@link ListRewrite} to use or <code>null</code> if there
		 * is no suitable one
		 *
		 * @param node
		 * @return the {@link ListRewrite} or <code>null</code>
		 */
		ListRewrite getListrewrite(ASTNode node) {
			switch (node.getNodeType()) {
				case ASTNode.TYPE_DECLARATION: {
					return rewrite.getListRewrite(node, TypeDeclaration.MODIFIERS2_PROPERTY);
				}
				case ASTNode.ANNOTATION_TYPE_DECLARATION: {
					return rewrite.getListRewrite(node, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY);
				}
				case ASTNode.ENUM_DECLARATION: {
					return rewrite.getListRewrite(node, EnumDeclaration.MODIFIERS2_PROPERTY);
				}
				case ASTNode.FIELD_DECLARATION: {
					return rewrite.getListRewrite(node, FieldDeclaration.MODIFIERS2_PROPERTY);
				}
				case ASTNode.METHOD_DECLARATION: {
					return rewrite.getListRewrite(node, MethodDeclaration.MODIFIERS2_PROPERTY);
				}
				case ASTNode.COMPILATION_UNIT:
					return rewrite.getListRewrite(node, CompilationUnit.IMPORTS_PROPERTY);
				default:
					return null;
			}
		}
	}

}
