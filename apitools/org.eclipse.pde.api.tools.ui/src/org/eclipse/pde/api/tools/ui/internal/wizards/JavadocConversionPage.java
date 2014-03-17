/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
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
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.markers.ApiQuickFixProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.WorkbenchJob;

import com.ibm.icu.text.MessageFormat;

/**
 * The wizard page for performing the conversion
 * 
 * @since 1.0.500
 */
public class JavadocConversionPage extends UserInputWizardPage {

	/**
	 * Visitor for the API description.
	 */
	class AnnotVisitor extends ApiDescriptionVisitor {

		Map<IFile, Set<TextEdit>> changes = new HashMap<IFile, Set<TextEdit>>();
		boolean remove = false;
		IJavaProject project = null;
		IApiDescription apidescription = null;
		SubMonitor monitor = null;

		/**
		 * Constructor
		 * 
		 * @param project the project context
		 * @param the backing API description
		 * @param remove if the Javadoc tags should be removed
		 * @param monitor the progress monitor
		 */
		public AnnotVisitor(IJavaProject project, IApiDescription description, boolean remove, IProgressMonitor monitor) {
			this.project = project;
			this.apidescription = description;
			this.remove = remove;
			this.monitor = SubMonitor.convert(monitor, 1);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor
		 * #visitElement
		 * (org.eclipse.pde.api.tools.internal.provisional.descriptors
		 * .IElementDescriptor,
		 * org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
		 */
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
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			ICompilationUnit cunit = type.getCompilationUnit();
			if (cunit != null) {
				this.monitor.setTaskName(NLS.bind(WizardMessages.JavadocConversionPage_scan_javadoc_to_convert, new Object[] { type.getFullyQualifiedName() }));
				parser.setSource(cunit);
				parser.setResolveBindings(true);
				Map<String, String> options = cunit.getJavaProject().getOptions(true);
				options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				parser.setCompilerOptions(options);
				CompilationUnit cast = (CompilationUnit) parser.createAST(new NullProgressMonitor());
				cast.recordModifications();
				ASTRewrite rewrite = ASTRewrite.create(cast.getAST());
				TagVisitor visitor = new TagVisitor(description, rewrite, this.remove);
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
							edits = new HashSet<TextEdit>(3);
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

		IApiDescription apidescription = null;
		ASTRewrite rewrite = null;
		boolean remove = false;
		List<String> existingImports = new ArrayList<String>();
		List<String> missingImports = new ArrayList<String>();

		/**
		 * Constructor
		 * 
		 * @param description
		 * @param rewrite
		 * @param remove
		 */
		public TagVisitor(IApiDescription description, ASTRewrite rewrite, boolean remove) {
			this.apidescription = description;
			this.rewrite = rewrite;
			this.remove = remove;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core
		 * .dom.CompilationUnit)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .ImportDeclaration)
		 */
		@Override
		public boolean visit(ImportDeclaration node) {
			String name = node.getName().getFullyQualifiedName();
			if (!ALL_API_IMPORTS.values().contains(name)) {
				existingImports.add(name);
			}
			return super.visit(node);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .TypeDeclaration)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .AnnotationTypeDearation)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .EnumDeclaration)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .FieldDeclaration)
		 */
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

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom
		 * .MethodDeclaration)
		 */
		@Override
		public boolean visit(MethodDeclaration node) {
			ASTNode parent = node.getParent();
			String typename = null;
			if (parent instanceof AbstractTypeDeclaration) {
				typename = ((AbstractTypeDeclaration) parent).getName().getFullyQualifiedName();
			} else if (parent instanceof AnnotationTypeDeclaration) {
				typename = ((AnnotationTypeDeclaration) parent).getName().getFullyQualifiedName();
			}
			if (typename != null) {
				IMethodDescriptor desc = Factory.methodDescriptor(typename, node.getName().getIdentifier(), Signatures.getMethodSignatureFromNode(node));
				IApiAnnotations annots = apidescription.resolveAnnotations(desc);
				if (annots != null && !RestrictionModifiers.isUnrestricted(annots.getRestrictions())) {
					updateNode(node, annots);
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
					mods = new ArrayList<IExtendedModifier>();
					rewrite.set(body, body.getModifiersProperty(), mods, null);
				}
				List<String> existing = new ArrayList<String>();
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

	/**
	 * Job for updating the filtering on the table viewer
	 */
	class UpdateJob extends WorkbenchJob {

		private String pattern = null;

		/**
		 * Constructor
		 */
		public UpdateJob() {
			super(WizardMessages.ApiToolingSetupWizardPage_filter_update_job);
			setSystem(true);
		}

		/**
		 * Sets the current text filter to use
		 * 
		 * @param filter
		 */
		public synchronized void setFilter(String pattern) {
			this.pattern = pattern;
		}

		/**
		 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (tableviewer != null) {
				try {
					tableviewer.getTable().setRedraw(false);
					synchronized (this) {
						filter.setPattern(pattern + '*');
					}
					tableviewer.refresh(true);
					tableviewer.setCheckedElements(checkedset.toArray());
				} finally {
					tableviewer.getTable().setRedraw(true);
				}
			}
			return Status.OK_STATUS;
		}

	}

	private static final String SETTINGS_SECTION = "JavadocTagConversionWizardPage"; //$NON-NLS-1$
	private static final String SETTINGS_REMOVE_TAGS = "remove_tags"; //$NON-NLS-1$

	static Map<String, String> ALL_API_IMPORTS;

	static {
		ALL_API_IMPORTS = new HashMap<String, String>();
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOEXTEND, "org.eclipse.pde.api.tools.annotations.NoExtend"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOIMPLEMENT, "org.eclipse.pde.api.tools.annotations.NoImplement"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOINSTANTIATE, "org.eclipse.pde.api.tools.annotations.NoInstantiate"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOOVERRIDE, "org.eclipse.pde.api.tools.annotations.NoOverride"); //$NON-NLS-1$
		ALL_API_IMPORTS.put(JavadocTagManager.ANNOTATION_NOREFERENCE, "org.eclipse.pde.api.tools.annotations.NoReference"); //$NON-NLS-1$
	}
	Button removetags = null;
	CheckboxTableViewer tableviewer = null;
	HashSet<Object> checkedset = new HashSet<Object>();
	UpdateJob updatejob = new UpdateJob();
	StringFilter filter = new StringFilter();
	private Text checkcount = null;

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public JavadocConversionPage() {
		super(WizardMessages.JavadocConversionWizard_0);
		setTitle(WizardMessages.JavadocConversionWizard_0);
		setDescription(WizardMessages.JavadocConversionPage_convert_tags_to_annotations_description);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.JAVADOC_CONVERSION_WIZARD_PAGE);
		SWTFactory.createWrapLabel(comp, WizardMessages.JavadocConversionPage_select_pjs_to_convert, 1, 100);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiToolingSetupWizardPage_6, 1, 50);

		final Text text = SWTFactory.createText(comp, SWT.BORDER, 1);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updatejob.setFilter(text.getText().trim());
				updatejob.cancel();
				updatejob.schedule();
			}
		});
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (tableviewer != null) {
						tableviewer.getTable().setFocus();
					}
				}
			}
		});

		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_8, 1, 50);

		Table table = new Table(comp, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		table.setLayoutData(gd);
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new WorkbenchLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		IProject[] input = Util.getApiProjectsMinSourceLevel(JavaCore.VERSION_1_5);
		if (input == null) {
			setMessage(WizardMessages.JavadocConversionPage_0, IMessageProvider.WARNING);
		} else {
			tableviewer.setInput(input);
		}
		tableviewer.setComparator(new ViewerComparator());
		tableviewer.addFilter(filter);
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					checkedset.add(event.getElement());
				} else {
					checkedset.remove(event.getElement());
				}
				setPageComplete(pageValid());
			}
		});
		Composite bcomp = SWTFactory.createComposite(comp, 3, 1, GridData.FILL_HORIZONTAL | GridData.END, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_10, null);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(true);
				checkedset.addAll(Arrays.asList(tableviewer.getCheckedElements()));
				setPageComplete(pageValid());
			}
		});
		button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_11, null);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(false);
				TableItem[] items = tableviewer.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					checkedset.remove(items[i].getData());
				}
				setPageComplete(pageValid());
			}
		});

		checkcount = SWTFactory.createText(bcomp, SWT.FLAT | SWT.READ_ONLY, 1, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		checkcount.setBackground(bcomp.getBackground());

		Object[] selected = getWorkbenchSelection();
		if (selected.length > 0) {
			tableviewer.setCheckedElements(selected);
			checkedset.addAll(Arrays.asList(selected));
		}
		setPageComplete(tableviewer.getCheckedElements().length > 0);

		SWTFactory.createVerticalSpacer(comp, 1);
		removetags = SWTFactory.createCheckButton(comp, WizardMessages.JavadocConversionPage_delete_tags_during_conversion, null, true, 1);

		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if (settings != null) {
			removetags.setSelection(settings.getBoolean(SETTINGS_REMOVE_TAGS));
		}
	}

	/**
	 * @return if the page is valid or not, this method also sets error messages
	 */
	protected boolean pageValid() {
		if (checkedset.size() < 1) {
			setErrorMessage(WizardMessages.UpdateJavadocTagsWizardPage_12);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	/**
	 * Called by the {@link ApiToolingSetupWizard} when finishing the wizard
	 * 
	 * @return true if the page finished normally, false otherwise
	 */
	public boolean finish() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		settings.put(SETTINGS_REMOVE_TAGS, removetags.getSelection());
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage() {
		// always have to collect changes again in the event the user goes back
		// and forth,
		// as a change cannot ever have more than one parent - EVER
		collectChanges();
		IWizardPage page = super.getNextPage();
		if (page != null) {
			page.setDescription(WizardMessages.JavadocConversionPage_changes_required_for_conversion);
		}
		return page;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
	 */
	@Override
	protected boolean performFinish() {
		collectChanges();
		return super.performFinish();
	}

	/**
	 * @return the current selection from the workbench as an array of objects
	 */
	protected Object[] getWorkbenchSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.getActivePart();
				if (part != null) {
					IWorkbenchSite site = part.getSite();
					if (site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if (provider != null) {
							ISelection selection = provider.getSelection();
							if (selection instanceof IStructuredSelection) {
								Object[] jps = ((IStructuredSelection) provider.getSelection()).toArray();
								ArrayList<IProject> pjs = new ArrayList<IProject>();
								for (int i = 0; i < jps.length; i++) {
									if (jps[i] instanceof IAdaptable) {
										IAdaptable adapt = (IAdaptable) jps[i];
										IProject pj = (IProject) adapt.getAdapter(IProject.class);
										if (Util.isApiProject(pj)) {
											pjs.add(pj);
										}
									}
								}
								return pjs.toArray();
							}
						}
					}
				}
			}
		}
		return new Object[0];
	}

	/**
	 * @return the mapping of text edits to the IFile they occur on
	 */
	private void collectChanges() {
		final JavadocConversionRefactoring refactoring = (JavadocConversionRefactoring) getRefactoring();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				Object[] projects = checkedset.toArray(new IProject[checkedset.size()]);
				IProject project = null;
				SubMonitor localmonitor = SubMonitor.convert(monitor, WizardMessages.JavadocConversionPage_scanning_projects_for_javadoc_tags, projects.length * 3);
				refactoring.resetRefactoring();
				boolean remove = removetags.getSelection();
				CompositeChange pchange = null;
				for (int i = 0; i < projects.length; i++) {
					project = (IProject) projects[i];
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
						createChanges(pchange, JavaCore.create(project), remove, localmonitor.newChild(1));
					} catch (CoreException e) {
						ApiUIPlugin.log(e);
					}
					if (pchange.getChildren().length > 0) {
						refactoring.addChange(pchange);
					}
					Util.updateMonitor(localmonitor, 1);
				}
			}
		};
		try {
			getContainer().run(false, false, op);
		} catch (InvocationTargetException e) {
			ApiUIPlugin.log(e);
		} catch (InterruptedException e) {
			ApiUIPlugin.log(e);
		}
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
		HashMap<IFile, Set<TextEdit>> map = new HashMap<IFile, Set<TextEdit>>();
		// XXX visit all CU's -> all doc nodes -> create add annotations
		RefactoringStatus status = collectAnnotationEdits(project, map, remove, monitor.newChild(1));
		if (status.isOK()) {
			IFile file = null;
			TextFileChange change = null;
			MultiTextEdit multiedit = null;
			Set<TextEdit> alledits = null;
			for (Entry<IFile, Set<TextEdit>> entry : map.entrySet()) {
				file = entry.getKey();
				monitor.setTaskName(NLS.bind(WizardMessages.JavadocConversionPage_collect_edits, new Object[] { file.getName() }));
				change = new TextFileChange(MessageFormat.format(WizardMessages.JavadocConversionPage_convert_javadoc_tags_in, new Object[] { file.getName() }), file);
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
			Util.updateMonitor(monitor, 1);
		}
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
				AnnotVisitor visitor = new AnnotVisitor(project, description, remove, monitor);
				description.accept(visitor, null);
				collector.putAll(visitor.changes);
			}
		}
		return status;
	}
}
