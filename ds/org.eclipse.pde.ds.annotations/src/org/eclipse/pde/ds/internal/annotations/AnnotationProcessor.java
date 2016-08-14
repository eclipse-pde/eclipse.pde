/*******************************************************************************
 * Copyright (c) 2012, 2016 Ecliptical Software Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@SuppressWarnings("restriction")
public class AnnotationProcessor extends ASTRequestor {

	private static final String DS_BUILDER = "org.eclipse.pde.ds.core.builder"; //$NON-NLS-1$

	static final Debug debug = Debug.getDebug("ds-annotation-builder/processor"); //$NON-NLS-1$

	private final ProjectContext context;

	private final Map<ICompilationUnit, BuildContext> fileMap;

	private boolean hasBuilder;

	public AnnotationProcessor(ProjectContext context, Map<ICompilationUnit, BuildContext> fileMap) {
		this.context = context;
		this.fileMap = fileMap;
	}

	@Override
	public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
		// determine CU key
		String cuKey;
		IJavaElement parent = source.getParent();
		if (parent == null)
			cuKey = source.getElementName();
		else
			cuKey = String.format("%s/%s", parent.getElementName().replace('.',  '/'), source.getElementName()); //$NON-NLS-1$

		context.getUnprocessed().remove(cuKey);

		ProjectState state = context.getState();
		HashMap<String, String> dsKeys = new HashMap<>();
		HashSet<DSAnnotationProblem> problems = new HashSet<>();

		ast.accept(new AnnotationVisitor(this, state, dsKeys, problems));

		// track abandoned files (may be garbage)
		Collection<String> oldDSKeys = state.updateMappings(cuKey, dsKeys);
		if (oldDSKeys != null) {
			oldDSKeys.removeAll(dsKeys.values());
			context.getAbandoned().addAll(oldDSKeys);
		}

		if (!problems.isEmpty()) {
			char[] filename = source.getResource().getFullPath().toString().toCharArray();
			for (DSAnnotationProblem problem : problems) {
				problem.setOriginatingFileName(filename);
				if (problem.getSourceStart() >= 0)
					problem.setSourceLineNumber(ast.getLineNumber(problem.getSourceStart()));
			}

			BuildContext buildContext = fileMap.get(source);
			if (buildContext != null)
				buildContext.recordNewProblems(problems.toArray(new CategorizedProblem[problems.size()]));
		}
	}

	private void ensureDSProject(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();

		for (ICommand command : commands) {
			if (DS_BUILDER.equals(command.getBuilderName()))
				return;
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = description.newCommand();
		command.setBuilderName(DS_BUILDER);
		newCommands[newCommands.length - 1] = command;
		description.setBuildSpec(newCommands);
		project.setDescription(description, null);
	}

	private void ensureExists(IFolder folder) throws CoreException {
		if (folder.exists())
			return;

		IContainer parent = folder.getParent();
		if (parent != null && parent.getType() == IResource.FOLDER)
			ensureExists((IFolder) parent);

		folder.create(true, true, null);
	}

	void verifyOutputLocation(IFile file) throws CoreException {
		if (hasBuilder)
			return;

		hasBuilder = true;
		IProject project = file.getProject();

		IPath parentPath = file.getParent().getProjectRelativePath();
		if (!parentPath.isEmpty()) {
			IFolder folder = project.getFolder(parentPath);
			ensureExists(folder);
		}

		try {
			ensureDSProject(project);
		} catch (CoreException e) {
			Activator.log(e);
		}
	}
}

@SuppressWarnings("restriction")
class AnnotationVisitor extends ASTVisitor {

	private static final String COMPONENT_ANNOTATION = DSAnnotationCompilationParticipant.COMPONENT_ANNOTATION;

	private static final String ACTIVATE_ANNOTATION = Activate.class.getName();

	private static final String MODIFIED_ANNOTATION = Modified.class.getName();

	private static final String DEACTIVATE_ANNOTATION = Deactivate.class.getName();

	private static final String REFERENCE_ANNOTATION = Reference.class.getName();

	private static final Pattern PID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*"); //$NON-NLS-1$

	private static final String NAMESPACE_1_1 = IDSConstants.NAMESPACE;

	private static final String NAMESPACE_1_2 = "http://www.osgi.org/xmlns/scr/v1.2.0"; //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_CONFIGURATION_PID = "configuration-pid"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_POLICY_OPTION = "policy-option"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_UPDATED = "updated"; //$NON-NLS-1$

	private static final String VALUE_REFERENCE_POLICY_OPTION_RELUCTANT = "reluctant"; //$NON-NLS-1$

	private static final Set<String> PROPERTY_TYPES = Collections.unmodifiableSet(
			new HashSet<>(
					Arrays.asList(
							null,
							IDSConstants.VALUE_PROPERTY_TYPE_STRING,
							IDSConstants.VALUE_PROPERTY_TYPE_LONG,
							IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE,
							IDSConstants.VALUE_PROPERTY_TYPE_FLOAT,
							IDSConstants.VALUE_PROPERTY_TYPE_INTEGER,
							IDSConstants.VALUE_PROPERTY_TYPE_BYTE,
							IDSConstants.VALUE_PROPERTY_TYPE_CHAR,
							IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN,
							IDSConstants.VALUE_PROPERTY_TYPE_SHORT)));

	private static final Comparator<IDSReference> REF_NAME_COMPARATOR = new Comparator<IDSReference>() {

		@Override
		public int compare(IDSReference o1, IDSReference o2) {
			return o1.getReferenceName().compareTo(o2.getReferenceName());
		}
	};

	private static final Debug debug = AnnotationProcessor.debug;

	private final AnnotationProcessor processor;

	private final ProjectState state;

	private final ValidationErrorLevel errorLevel;

	private final ValidationErrorLevel missingUnbindMethodLevel;

	private final Map<String, String> dsKeys;

	private final Set<DSAnnotationProblem> problems;

	public AnnotationVisitor(AnnotationProcessor processor, ProjectState state, Map<String, String> dsKeys, Set<DSAnnotationProblem> problems) {
		this.processor = processor;
		this.state = state;
		this.errorLevel = state.getErrorLevel();
		this.missingUnbindMethodLevel = state.getMissingUnbindMethodLevel();
		this.dsKeys = dsKeys;
		this.problems = problems;
	}

	@Override
	public boolean visit(TypeDeclaration type) {
		if (!Modifier.isPublic(type.getModifiers())) {
			// non-public types cannot be (or have nested) components
			if (errorLevel.isIgnore())
				return false;

			Annotation annotation = findComponentAnnotation(type);
			if (annotation != null)
				reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notPublic, type.getName().getIdentifier()), type.getName().getIdentifier());

			return true;
		}

		Annotation annotation = findComponentAnnotation(type);
		if (annotation != null) {
			boolean isInterface = false;
			boolean isAbstract = false;
			boolean isNested = false;
			boolean noDefaultConstructor = false;
			if ((isInterface = type.isInterface())
					|| (isAbstract = Modifier.isAbstract(type.getModifiers()))
					|| (isNested = (!type.isPackageMemberTypeDeclaration() && !isNestedPublicStatic(type)))
					|| (noDefaultConstructor = !hasDefaultConstructor(type))) {
				// interfaces, abstract types, non-static/non-public nested types, or types with no default constructor cannot be components
				if (errorLevel != ValidationErrorLevel.ignore) {
					if (isInterface)
						reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_interface, type.getName().getIdentifier()), type.getName().getIdentifier());
					else if (isAbstract)
						reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_abstract, type.getName().getIdentifier()), type.getName().getIdentifier());
					else if (isNested)
						reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notTopLevel, type.getName().getIdentifier()), type.getName().getIdentifier());
					else if (noDefaultConstructor)
						reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_noDefaultConstructor, type.getName().getIdentifier()), type.getName().getIdentifier());
					else
						reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentImplementationClass, type.getName().getIdentifier()), type.getName().getIdentifier());
				}
			} else {
				ITypeBinding typeBinding = type.resolveBinding();
				if (typeBinding == null) {
					if (debug.isDebugging())
						debug.trace(String.format("Unable to resolve binding for type: %s", type)); //$NON-NLS-1$
				} else {
					IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
					if (annotationBinding == null) {
						if (debug.isDebugging())
							debug.trace(String.format("Unable to resolve binding for annotation: %s", annotation)); //$NON-NLS-1$
					} else {
						try {
							processComponent(type, typeBinding, annotation, annotationBinding, problems);
						} catch (CoreException e) {
							Activator.log(e);
						}
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		Annotation annotation = findComponentAnnotation(node);
		if (annotation != null)
			reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_enumeration, node.getName().getIdentifier()), node.getName().getIdentifier());

		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		Annotation annotation = findComponentAnnotation(node);
		if (annotation != null)
			reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_annotation, node.getName().getIdentifier()), node.getName().getIdentifier());

		return true;
	}

	private Annotation findComponentAnnotation(AbstractTypeDeclaration type) {
		for (Object item : type.modifiers()) {
			if (!(item instanceof Annotation))
				continue;

			Annotation annotation = (Annotation) item;
			IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
			if (annotationBinding == null) {
				if (debug.isDebugging())
					debug.trace(String.format("Unable to resolve binding for annotation: %s", annotation)); //$NON-NLS-1$

				continue;
			}

			if (COMPONENT_ANNOTATION.equals(annotationBinding.getAnnotationType().getQualifiedName()))
				return annotation;
		}

		return null;
	}

	private boolean isNestedPublicStatic(AbstractTypeDeclaration type) {
		if (Modifier.isStatic(type.getModifiers())) {
			ASTNode parent = type.getParent();
			if (parent != null && (parent.getNodeType() == ASTNode.TYPE_DECLARATION || parent.getNodeType() == ASTNode.ANNOTATION_TYPE_DECLARATION)) {
				AbstractTypeDeclaration parentType = (AbstractTypeDeclaration) parent;
				if (Modifier.isPublic(parentType.getModifiers()))
					return parentType.isPackageMemberTypeDeclaration() || isNestedPublicStatic(parentType);
			}
		}

		return false;
	}

	private boolean hasDefaultConstructor(TypeDeclaration type) {
		boolean hasConstructor = false;
		for (MethodDeclaration method : type.getMethods()) {
			if (method.isConstructor()) {
				hasConstructor = true;
				if (Modifier.isPublic(method.getModifiers()) && method.parameters().isEmpty())
					return true;
			}
		}

		return !hasConstructor;
	}

	private void processComponent(TypeDeclaration type, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding, Collection<DSAnnotationProblem> problems) throws CoreException {
		// determine component name
		HashMap<String, Object> params = new HashMap<>();
		for (IMemberValuePairBinding pair : annotationBinding.getDeclaredMemberValuePairs()) {
			params.put(pair.getName(), pair.getValue());
		}

		String implClass = typeBinding.getBinaryName();

		String name = implClass;
		Object value;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			name = (String) value;
			validateComponentName(annotation, name, problems);
		}

		// set up document to edit
		IPath path = new Path(state.getPath()).append(name).addFileExtension("xml"); //$NON-NLS-1$

		String dsKey = path.toPortableString();
		dsKeys.put(implClass, dsKey);

		IProject project = typeBinding.getJavaElement().getJavaProject().getProject();
		IFile file = PDEProject.getBundleRelativeFile(project, path);
		IPath filePath = file.getFullPath();

		processor.verifyOutputLocation(file);

		// handle file move/rename
		String oldPath = state.getModelFile(implClass);
		if (oldPath != null && !oldPath.equals(dsKey) && !file.exists()) {
			IFile oldFile = PDEProject.getBundleRelativeFile(project, Path.fromPortableString(oldPath));
			if (oldFile.exists()) {
				try {
					oldFile.move(file.getFullPath(), true, true, null);
				} catch (CoreException e) {
					Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, String.format("Unable to move model file from '%s' to '%s'.", oldPath, file.getFullPath()), e)); //$NON-NLS-1$
				}
			}
		}

		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		bufferManager.connect(filePath, LocationKind.IFILE, null);
		ITextFileBuffer buffer = bufferManager.getTextFileBuffer(filePath, LocationKind.IFILE);
		if (buffer.isDirty())
			buffer.commit(null, true);

		IDocument document = buffer.getDocument();

		final DSModel dsModel = new DSModel(document, true);
		dsModel.setUnderlyingResource(file);
		dsModel.setCharset("UTF-8"); //$NON-NLS-1$
		dsModel.load();

		// note: we can't use XMLTextChangeListener because it generates overlapping edits!
		// thus we replace the entire content with one edit (if changed)
		final IDocument fDoc = document;
		dsModel.addModelChangedListener(new IModelTextChangeListener() {

			private final IDocument document = fDoc;

			private boolean changed;

			@Override
			public void modelChanged(IModelChangedEvent event) {
				changed = true;
			}

			@Override
			public TextEdit[] getTextOperations() {
				if (!changed)
					return new TextEdit[0];

				String text = dsModel.getContents();
				ReplaceEdit edit = new ReplaceEdit(0, document.getLength(), text);
				return new TextEdit[] { edit };
			}

			@Override
			public String getReadableName(TextEdit edit) {
				return null;
			}
		});

		try {
			processComponent(dsModel, type, typeBinding, annotation, annotationBinding, params, name, implClass, problems);

			TextEdit[] edits = dsModel.getLastTextChangeListener().getTextOperations();
			if (edits.length > 0) {
				if (debug.isDebugging())
					debug.trace(String.format("Saving model: %s", file.getFullPath())); //$NON-NLS-1$

				final MultiTextEdit edit = new MultiTextEdit();
				edit.addChildren(edits);

				if (buffer.isSynchronizationContextRequested()) {
					final IDocument doc = document;
					final CoreException[] ex = new CoreException[1];
					final CountDownLatch latch = new CountDownLatch(1);
					bufferManager.execute(new Runnable() {
						@Override
						public void run() {
							try {
								performEdit(doc, edit);
							} catch (CoreException e) {
								ex[0] = e;
							}

							latch.countDown();
						}
					});

					try {
						latch.await();
					} catch (InterruptedException e) {
						if (debug.isDebugging())
							debug.trace("Interrupted while waiting for edits to complete on display thread.", e); //$NON-NLS-1$
					}

					if (ex[0] != null)
						throw ex[0];
				} else {
					performEdit(document, edit);
				}

				buffer.commit(null, true);
			}
		} finally {
			dsModel.dispose();
			bufferManager.disconnect(buffer.getLocation(), LocationKind.IFILE, null);
		}
	}

	private void performEdit(IDocument document, TextEdit edit) throws CoreException {
		DocumentRewriteSession session = null;
		try {
			if (document instanceof IDocumentExtension4) {
				session = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
			}

			LinkedModeModel.closeAllModels(document);
			edit.apply(document);
		} catch (MalformedTreeException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error applying changes to component model.", e)); //$NON-NLS-1$
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error applying changes to component model.", e)); //$NON-NLS-1$
		} finally {
			if (session != null) {
				((IDocumentExtension4) document).stopRewriteSession(session);
			}
		}
	}

	private void processComponent(IDSModel model, TypeDeclaration type, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, ?> params, String name, String implClass, Collection<DSAnnotationProblem> problems) {
		Object value;
		Collection<String> services;
		if ((value = params.get("service")) instanceof Object[]) { //$NON-NLS-1$
			Object[] elements = (Object[]) value;
			services = new LinkedHashSet<>(elements.length);
			Map<String, Integer> serviceDuplicates = errorLevel.isIgnore() ? null : new HashMap<>();
			for (int i = 0; i < elements.length; ++i) {
				ITypeBinding serviceType = (ITypeBinding) elements[i];
				String serviceName = serviceType.getBinaryName();
				if (!errorLevel.isIgnore()) {
					if (serviceDuplicates.containsKey(serviceName)) {
						reportProblem(annotation, "service", i, problems, Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
						Integer pos = serviceDuplicates.put(serviceName, null);
						if (pos != null)
							reportProblem(annotation, "service", pos.intValue(), problems, Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
					} else {
						serviceDuplicates.put(serviceName, i);
					}
				}

				services.add(serviceName);
				validateComponentService(annotation, typeBinding, serviceType, i, problems);
			}
		} else {
			ITypeBinding[] serviceTypes = typeBinding.getInterfaces();
			services = new ArrayList<>(serviceTypes.length);
			for (int i = 0; i < serviceTypes.length; ++i) {
				services.add(serviceTypes[i].getBinaryName());
			}
		}

		String factory = null;
		if ((value = params.get("factory")) instanceof String) { //$NON-NLS-1$
			factory = (String) value;
			validateComponentFactory(annotation, factory, problems);
		}

		Boolean serviceFactory = null;
		if ((value = params.get("servicefactory")) instanceof Boolean) { //$NON-NLS-1$
			serviceFactory = (Boolean) value;
		}

		Boolean enabled = null;
		if ((value = params.get("enabled")) instanceof Boolean) { //$NON-NLS-1$
			enabled = (Boolean) value;
		}

		Boolean immediate = null;
		if ((value = params.get("immediate")) instanceof Boolean) { //$NON-NLS-1$
			immediate = (Boolean) value;
		}

		String[] properties;
		if ((value = params.get("property")) instanceof Object[]) { //$NON-NLS-1$
			Object[] elements = (Object[]) value;
			ArrayList<String> list = new ArrayList<>(elements.length);
			for (int i = 0; i < elements.length; ++i) {
				if (elements[i] instanceof String)
					list.add((String) elements[i]);
			}

			properties = list.toArray(new String[list.size()]);
		} else {
			properties = new String[0];
		}

		String[] propertyFiles;
		if ((value = params.get("properties")) instanceof Object[]) { //$NON-NLS-1$
			Object[] elements = (Object[]) value;
			ArrayList<String> list = new ArrayList<>(elements.length);
			for (int i = 0; i < elements.length; ++i) {
				if (elements[i] instanceof String)
					list.add((String) elements[i]);
			}

			propertyFiles = list.toArray(new String[list.size()]);
			validateComponentPropertyFiles(annotation, ((IType) typeBinding.getJavaElement()).getJavaProject().getProject(), propertyFiles, problems);
		} else {
			propertyFiles = new String[0];
		}

		String configPolicy = null;
		if ((value = params.get("configurationPolicy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding configPolicyBinding = (IVariableBinding) value;
			ConfigurationPolicy configPolicyLiteral = ConfigurationPolicy.valueOf(configPolicyBinding.getName());
			if (configPolicyLiteral != null)
				configPolicy = configPolicyLiteral.toString();
		}

		String configPid = null;
		if ((value = params.get("configurationPid")) instanceof String) { //$NON-NLS-1$
			configPid = (String) value;
			validateComponentConfigPID(annotation, configPid, problems);
		}

		IDSComponent component = model.getDSComponent();

		if (enabled == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_ENABLED, IDSConstants.VALUE_TRUE);
		} else {
			component.setEnabled(enabled.booleanValue());
		}

		if (name == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_NAME, null);
		} else {
			component.setAttributeName(name);
		}

		if (factory == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_FACTORY, null);
		} else {
			component.setFactory(factory);
		}

		if (immediate == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_IMMEDIATE, null);
		} else {
			component.setImmediate(immediate.booleanValue());
		}

		if (configPolicy == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_CONFIGURATION_POLICY, IDSConstants.VALUE_CONFIGURATION_POLICY_OPTIONAL);
		} else {
			component.setConfigurationPolicy(configPolicy);
		}

		IDSDocumentFactory dsFactory = model.getFactory();

		IDSProperty[] propElements = component.getPropertyElements();
		if (properties.length == 0) {
			removeChildren(component, Arrays.asList(propElements));
		} else {
			// build up new property elements
			LinkedHashMap<String, IDSProperty> map = new LinkedHashMap<>(properties.length);
			for (int i = 0; i < properties.length; ++i) {
				String propertyStr = properties[i];
				String[] pair = propertyStr.split("=", 2); //$NON-NLS-1$
				int colon = pair[0].indexOf(':');
				String propertyName, propertyType;
				if (colon == -1) {
					propertyName = pair[0];
					propertyType = null;
				} else {
					propertyName = pair[0].substring(0, colon);
					propertyType = pair[0].substring(colon + 1);
				}

				String propertyValue = pair.length > 1 ? pair[1].trim() : null;

				IDSProperty property = map.get(propertyName);
				if (property == null) {
					// create a new property
					property = dsFactory.createProperty();
					map.put(propertyName, property);
					property.setPropertyName(propertyName);
					if (propertyType == null)
						removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_TYPE, null);	 // just remove the attribute completely so we can detect changes when reconciling
					else
						property.setPropertyType(propertyType);

					property.setPropertyValue(propertyValue);
					validateComponentProperty(annotation, propertyName, propertyType, propertyValue, i, problems);
				} else {
					// property is multi-valued
					String content = property.getPropertyElemBody();
					if (content == null) {
						content = property.getPropertyValue();
						property.setPropertyElemBody(content);
						property.setPropertyValue(null);
					}

					if (!errorLevel.isIgnore()) {
						String expected = property.getPropertyType() == null || property.getPropertyType().length() == 0 || IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(property.getPropertyType()) ? Messages.AnnotationProcessor_stringOrEmpty : property.getPropertyType();
						String actual = propertyType == null || IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(propertyType) ? Messages.AnnotationProcessor_stringOrEmpty : propertyType;
						if (!actual.equals(expected))
							reportProblem(annotation, "property", i, problems, NLS.bind(Messages.AnnotationProcessor_inconsistentComponentPropertyType, actual, expected), actual); //$NON-NLS-1$
						else
							validateComponentProperty(annotation, propertyName, propertyType, propertyValue, i, problems);
					}

					if (propertyValue != null)
						property.setPropertyElemBody(content + "\n" + pair[1]); //$NON-NLS-1$
				}
			}

			// reconcile against existing property elements
			HashMap<String, IDSProperty> propMap = new HashMap<>(propElements.length);
			for (IDSProperty propElement : propElements) {
				propMap.put(propElement.getPropertyName(), propElement);
			}

			ArrayList<IDSProperty> propList = new ArrayList<>(map.values());
			for (ListIterator<IDSProperty> i = propList.listIterator(); i.hasNext();) {
				IDSProperty newProperty = i.next();
				IDSProperty property = propMap.remove(newProperty.getPropertyName());
				if (property == null)
					continue;

				i.set(property);

				String newPropertyType = newProperty.getPropertyType();
				if (newPropertyType != null || !IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(property.getPropertyType()))
					property.setPropertyType(newPropertyType);

				String newContent = newProperty.getPropertyElemBody();
				if (newContent == null) {
					property.setPropertyValue(newProperty.getPropertyValue());
					IDocumentTextNode textNode = property.getTextNode();
					if (textNode != null) {
						property.removeTextNode();
						if (property.isInTheModel() && property.isEditable()) {
							model.fireModelChanged(new ModelChangedEvent(model, IModelChangedEvent.REMOVE, new Object[] { textNode }, null));
						}
					}
				} else {
					removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
					String content = property.getPropertyElemBody();
					if (content == null || !newContent.equals(normalizePropertyElemBody(content))) {
						property.setPropertyElemBody(newContent);
					}
				}
			}

			int firstPos = propElements.length == 0
					? 0	// insert first property element as first child of component
							: component.indexOf(propElements[0]);
			removeChildren(component, propMap.values());

			addOrMoveChildren(component, propList, firstPos);
		}

		IDSProperties[] propFileElements = component.getPropertiesElements();
		if (propertyFiles.length == 0) {
			removeChildren(component, Arrays.asList(propFileElements));
		} else {
			HashMap<String, IDSProperties> propFileMap = new HashMap<>(propFileElements.length);
			for (IDSProperties propFileElement : propFileElements) {
				propFileMap.put(propFileElement.getEntry(), propFileElement);
			}

			ArrayList<IDSProperties> propFileList = new ArrayList<>(propertyFiles.length);
			for (String propertyFile : propertyFiles) {
				IDSProperties propertiesElement = propFileMap.remove(propertyFile);
				if (propertiesElement == null) {
					propertiesElement = dsFactory.createProperties();
					propertiesElement.setInTheModel(false); // note: workaround for PDE bug
					propertiesElement.setEntry(propertyFile);
				}

				propFileList.add(propertiesElement);
			}

			int firstPos;
			if (propFileElements.length == 0) {
				// insert first properties element after last property or (if none) first child of component
				propElements = component.getPropertyElements();
				firstPos = propElements.length == 0 ? 0 : component.indexOf(propElements[propElements.length - 1]) + 1;
			} else {
				firstPos = component.indexOf(propFileElements[0]);
			}

			removeChildren(component, propFileMap.values());

			addOrMoveChildren(component, propFileList, firstPos);
		}

		IDSService service = component.getService();
		if (services.isEmpty()) {
			if (service != null)
				component.removeService(service);
		} else {
			if (service == null) {
				service = dsFactory.createService();

				// insert service element after last property or properties element
				int firstPos = Math.max(0, indexOfLastPropertyOrProperties(component));
				component.addChildNode(service, firstPos, true);
			}

			IDSProvide[] provides = service.getProvidedServices();
			HashMap<String, IDSProvide> provideMap = new HashMap<>(provides.length);
			for (IDSProvide provide : provides) {
				provideMap.put(provide.getInterface(), provide);
			}

			ArrayList<IDSProvide> provideList = new ArrayList<>(services.size());
			for (String serviceName : services) {
				IDSProvide provide = provideMap.remove(serviceName);
				if (provide == null) {
					provide = dsFactory.createProvide();
					provide.setInterface(serviceName);
				}

				provideList.add(provide);
			}

			int firstPos = provides.length == 0 ? -1 : service.indexOf(provides[0]);
			removeChildren(service, (provideMap.values()));

			addOrMoveChildren(service, provideList, firstPos);

			if (serviceFactory == null) {
				removeAttribute(service, IDSConstants.ATTRIBUTE_SERVICE_FACTORY, IDSConstants.VALUE_FALSE);
			} else {
				service.setServiceFactory(serviceFactory.booleanValue());
			}
		}

		boolean requiresV12 = false;
		String activate = null;
		Annotation activateAnnotation = null;
		String deactivate = null;
		Annotation deactivateAnnotation = null;
		String modified = null;
		Annotation modifiedAnnotation = null;

		ArrayList<IDSReference> references = new ArrayList<>();
		HashMap<String, Annotation> referenceNames = new HashMap<>();
		IDSReference[] refElements = component.getReferences();

		HashMap<String, IDSReference> refMap = new HashMap<>(refElements.length);
		for (IDSReference refElement : refElements) {
			refMap.put(refElement.getReferenceBind(), refElement);
		}

		for (MethodDeclaration method : type.getMethods()) {
			for (Object modifier : method.modifiers()) {
				if (!(modifier instanceof Annotation))
					continue;

				Annotation methodAnnotation = (Annotation) modifier;
				IAnnotationBinding methodAnnotationBinding = methodAnnotation.resolveAnnotationBinding();
				if (methodAnnotationBinding == null) {
					if (debug.isDebugging())
						debug.trace(String.format("Unable to resolve binding for annotation: %s", methodAnnotation)); //$NON-NLS-1$

					continue;
				}

				String annotationName = methodAnnotationBinding.getAnnotationType().getQualifiedName();

				if (ACTIVATE_ANNOTATION.equals(annotationName)) {
					if (activate == null) {
						activate = method.getName().getIdentifier();
						activateAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "activate", method, problems); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, problems, Messages.AnnotationProcessor_duplicateActivateMethod, method.getName().getIdentifier());
						if (activateAnnotation != null) {
							reportProblem(activateAnnotation, null, problems, Messages.AnnotationProcessor_duplicateActivateMethod, activate);
							activateAnnotation = null;
						}
					}

					continue;
				}

				if (DEACTIVATE_ANNOTATION.equals(annotationName)) {
					if (deactivate == null) {
						deactivate = method.getName().getIdentifier();
						deactivateAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "deactivate", method, problems); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, problems, Messages.AnnotationProcessor_duplicateDeactivateMethod, method.getName().getIdentifier());
						if (deactivateAnnotation != null) {
							reportProblem(deactivateAnnotation, null, problems, Messages.AnnotationProcessor_duplicateDeactivateMethod, deactivate);
							deactivateAnnotation = null;
						}
					}

					continue;
				}

				if (MODIFIED_ANNOTATION.equals(annotationName)) {
					if (modified == null) {
						modified = method.getName().getIdentifier();
						modifiedAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "modified", method, problems); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, problems, Messages.AnnotationProcessor_duplicateModifiedMethod, method.getName().getIdentifier());
						if (modifiedAnnotation != null) {
							reportProblem(modifiedAnnotation, null, problems, Messages.AnnotationProcessor_duplicateModifiedMethod, modified);
							modifiedAnnotation = null;
						}
					}

					continue;
				}

				if (REFERENCE_ANNOTATION.equals(annotationName)) {
					IMethodBinding methodBinding = method.resolveBinding();
					if (methodBinding == null) {
						if (debug.isDebugging())
							debug.trace(String.format("Unable to resolve binding for method: %s", method)); //$NON-NLS-1$
					} else {
						requiresV12 |= processReference(method, methodBinding, methodAnnotation, methodAnnotationBinding, refMap, dsFactory, references, referenceNames, problems);
					}

					continue;
				}
			}
		}

		if (activate == null) {
			// only remove activate="activate" if method not found
			if (!"activate".equals(component.getActivateMethod()) //$NON-NLS-1$
					|| !hasLifeCycleMethod(typeBinding, "activate")) //$NON-NLS-1$
				removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATE, null); //$NON-NLS-1$
		} else {
			component.setActivateMethod(activate);
		}

		if (deactivate == null) {
			// only remove deactivate="deactivate" if method not found
			if (!"deactivate".equals(component.getDeactivateMethod()) //$NON-NLS-1$
					|| !hasLifeCycleMethod(typeBinding, "deactivate")) //$NON-NLS-1$
				removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_DEACTIVATE, null); //$NON-NLS-1$
		} else {
			component.setDeactivateMethod(deactivate);
		}

		if (modified == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_MODIFIED, null);
		} else {
			component.setModifiedeMethod(modified);
		}

		if (configPid == null) {
			removeAttribute(component, ATTRIBUTE_COMPONENT_CONFIGURATION_PID, null);
		} else {
			component.setXMLAttribute(ATTRIBUTE_COMPONENT_CONFIGURATION_PID, configPid);
			requiresV12 = true;
		}

		if (references.isEmpty()) {
			removeChildren(component, Arrays.asList(refElements));
		} else {
			// references must be declared in ascending lexicographical order of their names
			Collections.sort(references, REF_NAME_COMPARATOR);

			int firstPos;
			if (refElements.length == 0) {
				// insert first reference element after service element, or (if not present) last property or properties
				service = component.getService();
				if (service == null) {
					firstPos = Math.max(0, indexOfLastPropertyOrProperties(component));
				} else {
					firstPos = component.indexOf(service) + 1;
				}
			} else {
				firstPos = component.indexOf(refElements[0]);
			}

			removeChildren(component, refMap.values());

			addOrMoveChildren(component, references, firstPos);
		}

		IDSImplementation impl = component.getImplementation();
		if (impl == null) {
			impl = dsFactory.createImplementation();
			component.setImplementation(impl);
		}

		impl.setClassName(implClass);

		String xmlns = NAMESPACE_1_1;
		if ((value = params.get("xmlns")) instanceof String) { //$NON-NLS-1$
			xmlns = (String) value;
			validateComponentXMLNS(annotation, xmlns, requiresV12, problems);
		} else if (requiresV12) {
			xmlns = NAMESPACE_1_2;
		}

		component.setNamespace(xmlns);
	}

	private void removeChildren(IDSObject parent, Collection<? extends IDocumentElementNode> children) {
		for (IDocumentElementNode child : children) {
			parent.removeChildNode(child, true);
		}
	}

	private void removeAttribute(IDSObject obj, String name, String defaultValue) {
		IDocumentAttributeNode attrNode = obj.getDocumentAttribute(name);
		if (attrNode != null) {
			// only remove if value is not default
			String value = attrNode.getAttributeValue();
			if (value != null && value.equals(defaultValue))
				return;

			obj.removeDocumentAttribute(attrNode);
			if (obj.isInTheModel() && obj.isEditable()) {
				obj.getModel().fireModelChanged(new ModelChangedEvent(obj.getModel(), ModelChangedEvent.REMOVE, new Object[] { attrNode }, null));
			}
		}
	}

	private void addOrMoveChildren(IDSObject parent, List<? extends IDSObject> children, int firstPos) {
		for (int i = 0, n = children.size(); i < n; ++i) {
			IDSObject child = children.get(i);
			if (child.isInTheModel()) {
				int pos = parent.indexOf(child);
				if (i == 0) {
					if (firstPos < pos) {
						// move to first place
						moveChildNode(parent, child, firstPos - pos, true);
					}
				} else {
					int prevPos = parent.indexOf(children.get(i - 1));
					if (prevPos > pos) {
						// move to previous sibling's position
						moveChildNode(parent, child, prevPos - pos, true);
					}
				}
			} else {
				if (i == 0) {
					if (firstPos == -1) {
						parent.addChildNode(child, true);
					} else {
						// insert into first place
						parent.addChildNode(child, firstPos, true);
					}
				} else {
					// insert after preceding sibling
					parent.addChildNode(child, parent.indexOf(children.get(i - 1)) + 1, true);
				}
			}
		}
	}

	private void moveChildNode(IDocumentObject obj, IDocumentElementNode node, int newRelativeIndex, boolean fireEvent) {
		if (newRelativeIndex == 1 || newRelativeIndex == -1) {
			obj.moveChildNode(node, newRelativeIndex, fireEvent);
			return;
		}

		// workaround for PDE's busted DocumentObject.clone() method
		int currentIndex = obj.indexOf(node);
		if (currentIndex == -1)
			return;

		int newIndex = newRelativeIndex + currentIndex;
		if (newIndex < 0 || newIndex >= obj.getChildCount())
			return;

		obj.removeChildNode(node, fireEvent);
		IDocumentElementNode clone = clone(obj, node);
		obj.addChildNode(clone, newIndex, fireEvent);
	}

	private IDocumentElementNode clone(IDocumentObject obj, IDocumentElementNode node) {
		// note: same exact impl as DocumentObject.clone()
		// but here the deserialized object will actually resolve successfully
		// because our classloader (with DSPropery visible) will be on top of the stack
		// yay for Java serialization, *sigh*
		IDocumentElementNode clone = null;
		try {
			// Serialize
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bout);
			out.writeObject(node);
			out.flush();
			out.close();
			byte[] bytes = bout.toByteArray();
			// Deserialize
			ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
			ObjectInputStream in = new ObjectInputStream(bin);
			clone = (IDocumentElementNode) in.readObject();
			in.close();
			// Reconnect
			clone.reconnect(obj, obj.getSharedModel());
		} catch (IOException e) {
			if (debug.isDebugging())
				debug.trace("Error cloning element.", e); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			if (debug.isDebugging())
				debug.trace("Error cloning element.", e); //$NON-NLS-1$
		}

		return clone;
	}

	private int indexOfLastPropertyOrProperties(IDSComponent component) {
		int pos = -1;
		IDSProperty[] propElements = component.getPropertyElements();
		IDSProperties[] propFileElements = component.getPropertiesElements();
		if (propElements.length > 0)
			pos = component.indexOf(propElements[propElements.length - 1]) + 1;

		if (propFileElements.length > 0) {
			int lastPos = component.indexOf(propFileElements[propFileElements.length - 1]) + 1;
			if (lastPos > pos)
				pos = lastPos;
		}

		return pos;
	}

	private String normalizePropertyElemBody(String content) {
		StringBuilder buf = new StringBuilder(content.length());
		BufferedReader reader = new BufferedReader(new StringReader(content));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.length() == 0)
					continue;

				if (buf.length() > 0)
					buf.append('\n');

				buf.append(trimmed);
			}
		} catch (IOException e) {
			if (debug.isDebugging())
				debug.trace("Error reading property element body.", e); //$NON-NLS-1$
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}

		return buf.toString();
	}

	private void validateComponentName(Annotation annotation, String name, Collection<DSAnnotationProblem> problems) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(name).matches())
			reportProblem(annotation, "name", problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentName, name), name); //$NON-NLS-1$
	}

	private void validateComponentService(Annotation annotation, ITypeBinding componentType, ITypeBinding serviceType, int index, Collection<DSAnnotationProblem> problems) {
		if (!errorLevel.isIgnore() && !componentType.isAssignmentCompatible(serviceType))
			reportProblem(annotation, "service", problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentService, serviceType.getName()), serviceType.getName()); //$NON-NLS-1$
	}

	private void validateComponentFactory(Annotation annotation, String factory, Collection<DSAnnotationProblem> problems) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(factory).matches())
			reportProblem(annotation, "factory", problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentFactoryName, factory), factory); //$NON-NLS-1$
	}

	private void validateComponentProperty(Annotation annotation, String name, String type, String value, int index, Collection<DSAnnotationProblem> problems) {
		if (errorLevel.isIgnore())
			return;

		if (PROPERTY_TYPES.contains(type)) {
			if (name == null || name.trim().length() == 0)
				reportProblem(annotation, "property", index, problems, Messages.AnnotationProcessor_invalidComponentProperty_nameRequired, name); //$NON-NLS-1$

			if (value == null) {
				reportProblem(annotation, "property", index, problems, Messages.AnnotationProcessor_invalidComponentProperty_valueRequired, name); //$NON-NLS-1$
			} else {
				try {
					if (IDSConstants.VALUE_PROPERTY_TYPE_LONG.equals(type))
						Long.valueOf(value);
					else if (IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE.equals(type))
						Double.valueOf(value);
					else if (IDSConstants.VALUE_PROPERTY_TYPE_FLOAT.equals(type))
						Float.valueOf(value);
					else if (IDSConstants.VALUE_PROPERTY_TYPE_INTEGER.equals(type) || IDSConstants.VALUE_PROPERTY_TYPE_CHAR.equals(type))
						Integer.valueOf(value);
					else if (IDSConstants.VALUE_PROPERTY_TYPE_BYTE.equals(type))
						Byte.valueOf(value);
					else if (IDSConstants.VALUE_PROPERTY_TYPE_SHORT.equals(type))
						Short.valueOf(value);
				} catch (NumberFormatException e) {
					reportProblem(annotation, "property", index, problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyValue, type, value), String.valueOf(value)); //$NON-NLS-1$
				}
			}
		} else {
			reportProblem(annotation, "property", index, problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyType, type), String.valueOf(type)); //$NON-NLS-1$
		}
	}

	private void validateComponentPropertyFiles(Annotation annotation, IProject project, String[] files, Collection<DSAnnotationProblem> problems) {
		if (errorLevel.isIgnore())
			return;

		for (int i = 0; i < files.length; ++i) {
			String file = files[i];
			IFile wsFile = PDEProject.getBundleRelativeFile(project, new Path(file));
			if (!wsFile.exists())
				reportProblem(annotation, "properties", i, problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyFile, file), file); //$NON-NLS-1$
		}
	}

	private void validateComponentXMLNS(Annotation annotation, String xmlns, boolean requiresV12, Collection<DSAnnotationProblem> problems) {
		if (!errorLevel.isIgnore() && (requiresV12 || !NAMESPACE_1_1.equals(xmlns)) && !NAMESPACE_1_2.equals(xmlns))
			reportProblem(annotation, "xmlns", problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentDescriptorNamespace, xmlns), xmlns); //$NON-NLS-1$
	}

	private void validateComponentConfigPID(Annotation annotation, String configPid, Collection<DSAnnotationProblem> problems) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(configPid).matches())
			reportProblem(annotation, "configurationPid", problems, NLS.bind(Messages.AnnotationProcessor_invalidComponentConfigurationPid, configPid), configPid); //$NON-NLS-1$
	}

	private void validateLifeCycleMethod(Annotation annotation, String methodName, MethodDeclaration method, Collection<DSAnnotationProblem> problems) {
		if (errorLevel.isIgnore())
			return;

		IMethodBinding methodBinding = method.resolveBinding();
		if (methodBinding == null) {
			if (debug.isDebugging())
				debug.trace(String.format("Unable to resolve binding for method: %s", method)); //$NON-NLS-1$

			return;
		}

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName))
			reportProblem(annotation, methodName, problems, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodReturnType, methodName, returnTypeName), returnTypeName);

		ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();

		if (paramTypeBindings.length == 0)
			// no-arg method
			return;

		// every argument must be either Map, ComponentContext, or BundleContext
		boolean hasMap = false;
		boolean hasCompCtx = false;
		boolean hasBundleCtx = false;
		boolean hasInt = false;
		for (ITypeBinding paramTypeBinding : paramTypeBindings) {
			String paramTypeName = paramTypeBinding.getErasure().getQualifiedName();
			boolean isDuplicate = false;

			if (Map.class.getName().equals(paramTypeName)) {
				if (hasMap)
					isDuplicate = true;
				else
					hasMap = true;
			} else if (ComponentContext.class.getName().equals(paramTypeName)) {
				if (hasCompCtx)
					isDuplicate = true;
				else
					hasCompCtx = true;
			} else if (BundleContext.class.getName().equals(paramTypeName)) {
				if (hasBundleCtx)
					isDuplicate = true;
				else
					hasBundleCtx = true;
			} else if ("deactivate".equals(methodName) //$NON-NLS-1$
					&& (Integer.class.getName().equals(paramTypeName) || Integer.TYPE.getName().equals(paramTypeName))) {
				if (hasInt)
					isDuplicate = true;
				else
					hasInt = true;
			} else {
				reportProblem(annotation, methodName, problems, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
			}

			if (isDuplicate)
				reportProblem(annotation, methodName, problems, NLS.bind(Messages.AnnotationProcessor_duplicateLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
		}
	}

	private boolean hasLifeCycleMethod(ITypeBinding componentClass, String methodName) {
		for (IMethodBinding methodBinding : componentClass.getDeclaredMethods()) {
			if (methodName.equals(methodBinding.getName())
					&& Void.TYPE.getName().equals(methodBinding.getReturnType().getName())) {
				ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();

				// every argument must be either Map, ComponentContext, or BundleContext
				boolean hasMap = false;
				boolean hasCompCtx = false;
				boolean hasBundleCtx = false;
				boolean hasInt = false;
				boolean isInvalid = false;
				for (ITypeBinding paramTypeBinding : paramTypeBindings) {
					String paramTypeName = paramTypeBinding.getErasure().getQualifiedName();

					if (Map.class.getName().equals(paramTypeName)) {
						if (hasMap)
							isInvalid = true;
						else
							hasMap = true;
					} else if (ComponentContext.class.getName().equals(paramTypeName)) {
						if (hasCompCtx)
							isInvalid = true;
						else
							hasCompCtx = true;
					} else if (BundleContext.class.getName().equals(paramTypeName)) {
						if (hasBundleCtx)
							isInvalid = true;
						else
							hasBundleCtx = true;
					} else if ("deactivate".equals(methodName) //$NON-NLS-1$
							&& (Integer.class.getName().equals(paramTypeName) || Integer.TYPE.getName().equals(paramTypeName))) {
						if (hasInt)
							isInvalid = true;
						else
							hasInt = true;
					} else {
						isInvalid = true;
					}

					if (isInvalid)
						break;
				}

				if (!isInvalid)
					return true;
			}
		}

		return false;
	}

	private boolean processReference(MethodDeclaration method, IMethodBinding methodBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, IDSReference> refMap, IDSDocumentFactory factory, Collection<IDSReference> collector, Map<String, Annotation> names, Collection<DSAnnotationProblem> problems) {
		HashMap<String, Object> params = new HashMap<>();
		for (IMemberValuePairBinding pair : annotationBinding.getDeclaredMemberValuePairs()) {
			params.put(pair.getName(), pair.getValue());
		}

		ITypeBinding[] argTypes = methodBinding.getParameterTypes();

		ITypeBinding serviceType;
		Object value;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
			if (!errorLevel.isIgnore() && argTypes.length > 0) {
				ITypeBinding[] typeArgs;
				if (!(ServiceReference.class.getName().equals(argTypes[0].getErasure().getQualifiedName())
						&& ((typeArgs = argTypes[0].getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
						&& !serviceType.isAssignmentCompatible(argTypes[0]))
					reportProblem(annotation, "service", problems, NLS.bind(Messages.AnnotationProcessor_invalidReferenceService, argTypes[0].getName(), serviceType.getName()), serviceType.getName()); //$NON-NLS-1$
			}
		} else if (argTypes.length > 0) {
			if (ServiceReference.class.getName().equals(argTypes[0].getErasure().getQualifiedName())) {
				ITypeBinding[] typeArgs = argTypes[0].getTypeArguments();
				if (typeArgs.length > 0)
					serviceType = typeArgs[0];
				else
					serviceType = null;
			} else {
				serviceType = argTypes[0].isPrimitive() ? getObjectType(method.getAST(), argTypes[0]) : argTypes[0];
			}
		} else {
			serviceType = null;
		}

		if (serviceType == null) {
			reportProblem(annotation, null, problems, Messages.AnnotationProcessor_invalidReferenceServiceUnknown);

			serviceType = method.getAST().resolveWellKnownType(Object.class.getName());
		}

		validateReferenceBindMethod(annotation, serviceType, methodBinding, problems);

		String service = serviceType == null ? null : serviceType.getBinaryName();

		String methodName = methodBinding.getName();
		String name;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			name = (String) value;
		} else if (methodName.startsWith("bind")) { //$NON-NLS-1$
			name = methodName.substring("bind".length()); //$NON-NLS-1$
		} else if (methodName.startsWith("set")) { //$NON-NLS-1$
			name = methodName.substring("set".length()); //$NON-NLS-1$
		} else if (methodName.startsWith("add")) { //$NON-NLS-1$
			name = methodName.substring("add".length()); //$NON-NLS-1$
		} else {
			name = methodName;
		}

		if (!errorLevel.isIgnore()) {
			if (names.containsKey(name)) {
				reportProblem(annotation, "name", problems, NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
				Annotation duplicate = names.put(name, null);
				if (duplicate != null)
					reportProblem(duplicate, "name", problems, NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
			} else {
				names.put(name, annotation);
			}
		}

		String cardinality = null;
		if ((value = params.get("cardinality")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding cardinalityBinding = (IVariableBinding) value;
			ReferenceCardinality cardinalityLiteral = ReferenceCardinality.valueOf(cardinalityBinding.getName());
			if (cardinalityLiteral != null)
				cardinality = cardinalityLiteral.toString();
		}

		String policy = null;
		if ((value = params.get("policy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyBinding = (IVariableBinding) value;
			ReferencePolicy policyLiteral = ReferencePolicy.valueOf(policyBinding.getName());
			if (policyLiteral != null)
				policy = policyLiteral.toString();
		}

		String target = null;
		if ((value = params.get("target")) instanceof String) { //$NON-NLS-1$
			target = (String) value;
			validateReferenceTarget(annotation, target, problems);
		}

		String unbind;
		if ((value = params.get("unbind")) instanceof String) { //$NON-NLS-1$
			String unbindValue = (String) value;
			if ("-".equals(unbindValue)) { //$NON-NLS-1$
				unbind = null;
			} else {
				unbind = unbindValue;
				if (!errorLevel.isIgnore()) {
					IMethodBinding unbindMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, unbind, true);
					if (unbindMethod == null)
						reportProblem(annotation, "unbind", problems, NLS.bind(Messages.AnnotationProcessor_invalidReferenceUnbind, unbind), unbind); //$NON-NLS-1$
				}
			}
		} else {
			String unbindCandidate;
			if (methodName.startsWith("add")) { //$NON-NLS-1$
				unbindCandidate = "remove" + methodName.substring("add".length()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				unbindCandidate = "un" + methodName; //$NON-NLS-1$
			}

			IMethodBinding unbindMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, unbindCandidate, false);
			if (unbindMethod == null) {
				unbind = null;
				if (IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)) {
					reportProblem(annotation, null, missingUnbindMethodLevel, problems, NLS.bind(Messages.AnnotationProcessor_noImplicitReferenceUnbind, unbindCandidate), unbindCandidate);
				}
			} else {
				unbind = unbindMethod.getName();
			}
		}

		String policyOption = null;
		if ((value = params.get("policyOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyOptionBinding = (IVariableBinding) value;
			ReferencePolicyOption policyOptionLiteral = ReferencePolicyOption.valueOf(policyOptionBinding.getName());
			if (policyOptionLiteral != null) {
				policyOption = policyOptionLiteral.toString();
			}
		}

		String updated;
		if ((value = params.get(ATTRIBUTE_REFERENCE_UPDATED)) instanceof String) {
			String updatedValue = (String) value;
			if ("-".equals(updatedValue)) { //$NON-NLS-1$
				updated = null;
			} else {
				updated = updatedValue;
				if (!errorLevel.isIgnore()) {
					IMethodBinding updatedMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, updated, true);
					if (updatedMethod == null)
						reportProblem(annotation, ATTRIBUTE_REFERENCE_UPDATED, problems, NLS.bind(Messages.AnnotationProcessor_invalidReferenceUpdated, updated), updated);
				}
			}
		} else {
			String updatedCandidate;
			if (methodName.startsWith("bind")) { //$NON-NLS-1$
				updatedCandidate = ATTRIBUTE_REFERENCE_UPDATED + methodName.substring("bind".length()); //$NON-NLS-1$
			} else if (methodName.startsWith("set")) { //$NON-NLS-1$
				updatedCandidate = ATTRIBUTE_REFERENCE_UPDATED + methodName.substring("set".length()); //$NON-NLS-1$
			} else if (methodName.startsWith("add")) { //$NON-NLS-1$
				updatedCandidate = ATTRIBUTE_REFERENCE_UPDATED + methodName.substring("add".length()); //$NON-NLS-1$
			} else {
				updatedCandidate = ATTRIBUTE_REFERENCE_UPDATED + methodName;
			}

			IMethodBinding updatedMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, updatedCandidate, false);
			if (updatedMethod == null)
				updated = null;
			else
				updated = updatedMethod.getName();
		}

		IDSReference reference = refMap.remove(methodName);
		if (reference == null) {
			reference = factory.createReference();
		}

		collector.add(reference);

		reference.setReferenceBind(methodName);

		if (name == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_NAME, null);
		} else {
			reference.setReferenceName(name);
		}

		if (service == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE, null);
		} else {
			reference.setReferenceInterface(service);
		}

		if (cardinality == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY, IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE);
		} else {
			reference.setReferenceCardinality(cardinality);
		}

		if (policy == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_POLICY, IDSConstants.VALUE_REFERENCE_POLICY_STATIC);
		} else {
			reference.setReferencePolicy(policy);
		}

		if (target == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_TARGET, null);
		} else {
			reference.setReferenceTarget(target);
		}

		if (unbind == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_UNBIND, null);
		} else {
			reference.setReferenceUnbind(unbind);
		}

		if (policyOption == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_POLICY_OPTION, VALUE_REFERENCE_POLICY_OPTION_RELUCTANT);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION, policyOption);
		}

		if (updated == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_UPDATED, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_UPDATED, updated);
		}

		return reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_UPDATED) != null;
	}

	private ITypeBinding getObjectType(AST ast, ITypeBinding primitive) {
		if (Boolean.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Boolean.class.getName());

		if (Byte.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Byte.class.getName());

		if (Character.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Character.class.getName());

		if (Double.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Double.class.getName());

		if (Float.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Float.class.getName());

		if (Integer.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Integer.class.getName());

		if (Long.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Long.class.getName());

		if (Short.TYPE.getName().equals(primitive.getName()))
			return ast.resolveWellKnownType(Short.class.getName());

		return null;
	}

	private void validateReferenceBindMethod(Annotation annotation, ITypeBinding serviceType, IMethodBinding methodBinding, Collection<DSAnnotationProblem> problems) {
		if (errorLevel.isIgnore())
			return;

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName))
			reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidBindMethodReturnType, returnTypeName), returnTypeName);

		ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();
		if (!(paramTypeBindings.length == 1 && (ServiceReference.class.getName().equals(paramTypeBindings[0].getErasure().getQualifiedName()) || serviceType == null || serviceType.isAssignmentCompatible(paramTypeBindings[0])))
				&& !(paramTypeBindings.length == 2 && (serviceType == null || serviceType.isAssignmentCompatible(paramTypeBindings[0])) && Map.class.getName().equals(paramTypeBindings[1].getErasure().getQualifiedName()))) {
			String[] params = new String[paramTypeBindings.length];
			StringBuilder buf = new StringBuilder(64);
			buf.append('(');
			for (int i = 0; i < params.length; ++i) {
				params[i] = paramTypeBindings[i].getName();
				if (buf.length() > 1)
					buf.append(", "); //$NON-NLS-1$

				buf.append(params[i]);
			}

			buf.append(')');
			reportProblem(annotation, null, problems, NLS.bind(Messages.AnnotationProcessor_invalidBindMethodParameters, buf, serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()), params);
		}
	}

	private void validateReferenceTarget(Annotation annotation, String target, Collection<DSAnnotationProblem> problems) {
		if (errorLevel.isIgnore())
			return;

		try {
			FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			String msg = e.getMessage();
			String suffix = ": " + e.getFilter(); //$NON-NLS-1$
			if (msg.endsWith(suffix))
				msg = msg.substring(0, msg.length() - suffix.length());

			reportProblem(annotation, "target", problems, msg, target); //$NON-NLS-1$
		}
	}

	private IMethodBinding findReferenceMethod(ITypeBinding componentClass, ITypeBinding serviceType, String name, boolean recurse) {
		ITypeBinding testedClass = componentClass;

		IMethodBinding candidate = null;
		int priority = 0;
		// priority:
		// 0: <assignment-compatible-type>, Map
		// 1: <exact-type>, Map
		// 2: <assignment-compatible-type>
		// 3: <exact-type>
		do {
			for (IMethodBinding declaredMethod : testedClass.getDeclaredMethods()) {
				if (name.equals(declaredMethod.getName())
						&& Void.TYPE.getName().equals(declaredMethod.getReturnType().getName())
						&& (testedClass == componentClass
						|| Modifier.isPublic(declaredMethod.getModifiers())
						|| Modifier.isProtected(declaredMethod.getModifiers())
						|| (!Modifier.isPrivate(declaredMethod.getModifiers())
								&& testedClass.getPackage().isEqualTo(componentClass.getPackage())))) {
					ITypeBinding[] paramTypes = declaredMethod.getParameterTypes();
					if (paramTypes.length == 1) {
						if (ServiceReference.class.getName().equals(paramTypes[0].getErasure().getQualifiedName()))
							// we have the winner
							return declaredMethod;

						if (priority < 3 && serviceType != null && serviceType.isEqualTo(paramTypes[0]))
							priority = 3;
						else if (priority < 2 && serviceType != null && serviceType.isAssignmentCompatible(paramTypes[0]))
							priority = 2;
						else
							continue;

						// we have a (better) candidate
						candidate = declaredMethod;
					} else if (paramTypes.length == 2) {
						if (priority < 1
								&& serviceType != null && serviceType.isEqualTo(paramTypes[0])
								&& Map.class.getName().equals(paramTypes[1].getErasure().getQualifiedName()))
							priority = 1;
						else if (candidate != null
								|| !(serviceType != null && serviceType.isAssignmentCompatible(paramTypes[0]))
								|| !Map.class.getName().equals(paramTypes[1].getErasure().getQualifiedName()))
							continue;

						// we have a candidate
						candidate = declaredMethod;
					}
				}
			}
		} while (recurse && (testedClass = testedClass.getSuperclass()) != null);

		return candidate;
	}

	private void reportProblem(Annotation annotation, String member, Collection<DSAnnotationProblem> problems, String message, String... args) {
		reportProblem(annotation, member, -1, problems, message, args);
	}

	private void reportProblem(Annotation annotation, String member, ValidationErrorLevel errorLevel, Collection<DSAnnotationProblem> problems, String message, String... args) {
		reportProblem(annotation, member, -1, errorLevel, problems, message, args);
	}

	private void reportProblem(Annotation annotation, String member, int valueIndex, Collection<DSAnnotationProblem> problems, String message, String... args) {
		reportProblem(annotation, member, valueIndex, errorLevel, problems, message, args);
	}

	private void reportProblem(Annotation annotation, String member, int valueIndex, ValidationErrorLevel errorLevel, Collection<DSAnnotationProblem> problems, String message, String... args) {
		if (errorLevel.isIgnore())
			return;

		Expression memberValue = annotation;
		if (annotation.isNormalAnnotation() && member != null) {
			NormalAnnotation na = (NormalAnnotation) annotation;
			for (Object value : na.values()) {
				MemberValuePair pair = (MemberValuePair) value;
				if (member.equals(pair.getName().getIdentifier())) {
					memberValue = pair.getValue();
					break;
				}
			}
		} else if (annotation.isSingleMemberAnnotation()) {
			SingleMemberAnnotation sma = (SingleMemberAnnotation) annotation;
			memberValue = sma.getValue();
		}

		int start = memberValue.getStartPosition();
		int length = memberValue.getLength();

		if (valueIndex >= 0 && memberValue instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) memberValue;
			if (valueIndex < ai.expressions().size()) {
				Expression element = (Expression) ai.expressions().get(valueIndex);
				start = element.getStartPosition();
				length = element.getLength();
			}
		}

		if (start >= 0) {
			DSAnnotationProblem problem = new DSAnnotationProblem(errorLevel.isError(), message, args);
			problem.setSourceStart(start);
			problem.setSourceEnd(start + length - 1);
			problems.add(problem);
		}
	}
}