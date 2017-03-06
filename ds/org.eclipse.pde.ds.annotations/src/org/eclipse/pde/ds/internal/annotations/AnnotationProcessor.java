/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
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
import java.util.LinkedList;
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
import org.eclipse.jdt.core.dom.FieldDeclaration;
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
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
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

	static String getCompilationUnitKey(ICompilationUnit source) {
		IJavaElement parent = source.getParent();
		if (parent == null)
			return source.getElementName();
		else
			return String.format("%s/%s", parent.getElementName().replace('.', '/'), source.getElementName()); //$NON-NLS-1$
	}

	@Override
	public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
		// determine CU key
		String cuKey = getCompilationUnitKey(source);

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

	private static final String COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext"; //$NON-NLS-1$

	private static final String COMPONENT_SERVICE_OBJECTS = "org.osgi.service.component.ComponentServiceObjects"; //$NON-NLS-1$

	private static final String COMPONENT_ANNOTATION = DSAnnotationCompilationParticipant.COMPONENT_ANNOTATION;

	private static final String ACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Activate"; //$NON-NLS-1$

	private static final String MODIFIED_ANNOTATION = "org.osgi.service.component.annotations.Modified"; //$NON-NLS-1$

	private static final String DEACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Deactivate"; //$NON-NLS-1$

	private static final String REFERENCE_ANNOTATION = "org.osgi.service.component.annotations.Reference"; //$NON-NLS-1$

	private static final Pattern PID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*"); //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_CONFIGURATION_PID = "configuration-pid"; //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_REFERENCE = "reference"; //$NON-NLS-1$

	private static final String ATTRIBUTE_SERVICE_SCOPE = "scope"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_POLICY_OPTION = "policy-option"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_UPDATED = "updated"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_SCOPE = "scope"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_FIELD = "field"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_FIELD_OPTION = "field-option"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE = "field-collection-type"; //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_DEFAULT = DSEnums.getServiceScope("DEFAULT"); //$NON-NLS-1$

	private static final String VALUE_REFERENCE_FIELD_OPTION_REPLACE = DSEnums.getFieldOption("REPLACE"); //$NON-NLS-1$

	private static final String VALUE_REFERENCE_FIELD_OPTION_UPDATE = DSEnums.getFieldOption("UPDATE"); //$NON-NLS-1$

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

	private static final Map<String, String> PRIMITIVE_TYPE_MAP;

	static {
		HashMap<String, String> map = new HashMap<>(16);
		map.put(Long.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_LONG);
		map.put(Double.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE);
		map.put(Float.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_FLOAT);
		map.put(Integer.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_INTEGER);
		map.put(Byte.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BYTE);
		map.put(Character.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_CHAR);
		map.put(Boolean.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN);
		map.put(Short.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_SHORT);
		map.put(Long.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_LONG);
		map.put(Double.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE);
		map.put(Float.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_FLOAT);
		map.put(Integer.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_INTEGER);
		map.put(Byte.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BYTE);
		map.put(Character.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_CHAR);
		map.put(Boolean.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN);
		map.put(Short.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_SHORT);
		PRIMITIVE_TYPE_MAP = Collections.unmodifiableMap(map);
	}

	private static final Comparator<IDSReference> REF_NAME_COMPARATOR = new Comparator<IDSReference>() {

		@Override
		public int compare(IDSReference o1, IDSReference o2) {
			return o1.getReferenceName().compareTo(o2.getReferenceName());
		}
	};

	private static final Debug debug = AnnotationProcessor.debug;

	private final AnnotationProcessor processor;

	private final ProjectState state;

	private final DSAnnotationVersion specVersion;

	private final ValidationErrorLevel errorLevel;

	private final ValidationErrorLevel missingUnbindMethodLevel;

	private final Map<String, String> dsKeys;

	private final Set<DSAnnotationProblem> problems;

	public AnnotationVisitor(AnnotationProcessor processor, ProjectState state, Map<String, String> dsKeys, Set<DSAnnotationProblem> problems) {
		this.processor = processor;
		this.state = state;
		this.specVersion = state.getSpecVersion();
		this.errorLevel = state.getErrorLevel();
		this.missingUnbindMethodLevel = state.getMissingUnbindMethodLevel();
		this.dsKeys = dsKeys;
		this.problems = problems;
	}

	@Override
	public boolean visit(TypeDeclaration type) {
		if (!Modifier.isPublic(type.getModifiers())) {
			// non-public types cannot be (or have nested) components
			if (errorLevel.isIgnore()) {
				return false;
			}

			Annotation annotation = findComponentAnnotation(type);
			if (annotation != null) {
				reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notPublic, type.getName().getIdentifier()), type.getName().getIdentifier());
			}

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
					if (isInterface) {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_interface, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isAbstract) {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_abstract, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isNested) {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notTopLevel, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (noDefaultConstructor) {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_noDefaultConstructor, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidComponentImplementationClass, type.getName().getIdentifier()), type.getName().getIdentifier());
					}
				}
			} else {
				ITypeBinding typeBinding = type.resolveBinding();
				if (typeBinding == null) {
					if (debug.isDebugging()) {
						debug.trace(String.format("Unable to resolve binding for type: %s", type)); //$NON-NLS-1$
					}
				} else {
					IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
					if (annotationBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for annotation: %s", annotation)); //$NON-NLS-1$
						}
					} else {
						try {
							processComponent(type, typeBinding, annotation, annotationBinding);
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
		if (annotation != null) {
			reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_enumeration, node.getName().getIdentifier()), node.getName().getIdentifier());
		}

		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		Annotation annotation = findComponentAnnotation(node);
		if (annotation != null) {
			reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_annotation, node.getName().getIdentifier()), node.getName().getIdentifier());
		}

		return true;
	}

	private Annotation findComponentAnnotation(AbstractTypeDeclaration type) {
		for (Object item : type.modifiers()) {
			if (!(item instanceof Annotation)) {
				continue;
			}

			Annotation annotation = (Annotation) item;
			IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
			if (annotationBinding == null) {
				if (debug.isDebugging()) {
					debug.trace(String.format("Unable to resolve binding for annotation: %s", annotation)); //$NON-NLS-1$
				}

				continue;
			}

			if (COMPONENT_ANNOTATION.equals(annotationBinding.getAnnotationType().getQualifiedName())) {
				return annotation;
			}
		}

		return null;
	}

	private boolean isNestedPublicStatic(AbstractTypeDeclaration type) {
		if (Modifier.isStatic(type.getModifiers())) {
			ASTNode parent = type.getParent();
			if (parent != null && (parent.getNodeType() == ASTNode.TYPE_DECLARATION || parent.getNodeType() == ASTNode.ANNOTATION_TYPE_DECLARATION)) {
				AbstractTypeDeclaration parentType = (AbstractTypeDeclaration) parent;
				if (Modifier.isPublic(parentType.getModifiers())) {
					return parentType.isPackageMemberTypeDeclaration() || isNestedPublicStatic(parentType);
				}
			}
		}

		return false;
	}

	private boolean hasDefaultConstructor(TypeDeclaration type) {
		boolean hasConstructor = false;
		for (MethodDeclaration method : type.getMethods()) {
			if (method.isConstructor()) {
				hasConstructor = true;
				if (Modifier.isPublic(method.getModifiers()) && method.parameters().isEmpty()) {
					return true;
				}
			}
		}

		return !hasConstructor;
	}

	private void processComponent(TypeDeclaration type, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding) throws CoreException {
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
			validateComponentName(annotation, name);
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
		if (buffer.isDirty()) {
			buffer.commit(null, true);
		}

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
				if (!changed) {
					return new TextEdit[0];
				}

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
			processComponent(dsModel, type, typeBinding, annotation, annotationBinding, params, name, implClass);

			TextEdit[] edits = dsModel.getLastTextChangeListener().getTextOperations();
			if (edits.length > 0) {
				if (debug.isDebugging()) {
					debug.trace(String.format("Saving model: %s", file.getFullPath())); //$NON-NLS-1$
				}

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

					if (ex[0] != null) {
						throw ex[0];
					}
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

	private void processComponent(IDSModel model, TypeDeclaration type, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, ?> params, String name, String implClass) {
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
						reportProblem(annotation, "service", i, Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
						Integer pos = serviceDuplicates.put(serviceName, null);
						if (pos != null) {
							reportProblem(annotation, "service", pos.intValue(), Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
						}
					} else {
						serviceDuplicates.put(serviceName, i);
					}
				}

				services.add(serviceName);
				validateComponentService(annotation, typeBinding, serviceType, i);
			}
		} else {
			ITypeBinding[] serviceTypes = typeBinding.getInterfaces();
			services = new ArrayList<>(serviceTypes.length);
			for (ITypeBinding serviceType : serviceTypes) {
				services.add(serviceType.getBinaryName());
			}
		}

		String factory = null;
		if ((value = params.get("factory")) instanceof String) { //$NON-NLS-1$
			factory = (String) value;
			validateComponentFactory(annotation, factory);
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
			for (Object element : elements) {
				if (element instanceof String) {
					list.add((String) element);
				}
			}

			properties = list.toArray(new String[list.size()]);
		} else {
			properties = new String[0];
		}

		String[] propertyFiles;
		if ((value = params.get("properties")) instanceof Object[]) { //$NON-NLS-1$
			Object[] elements = (Object[]) value;
			ArrayList<String> list = new ArrayList<>(elements.length);
			for (Object element : elements) {
				if (element instanceof String) {
					list.add((String) element);
				}
			}

			propertyFiles = list.toArray(new String[list.size()]);
			validateComponentPropertyFiles(annotation, ((IType) typeBinding.getJavaElement()).getJavaProject().getProject(), propertyFiles);
		} else {
			propertyFiles = new String[0];
		}

		String configPolicy = null;
		if ((value = params.get("configurationPolicy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding configPolicyBinding = (IVariableBinding) value;
			configPolicy = DSEnums.getConfigurationPolicy(configPolicyBinding.getName());
		}

		DSAnnotationVersion requiredVersion = DSAnnotationVersion.V1_1;

		String configPid = null;
		if ((value = params.get("configurationPid")) instanceof String) { //$NON-NLS-1$
			configPid = (String) value;
			validateComponentConfigPID(annotation, configPid, -1);
			requiredVersion = DSAnnotationVersion.V1_2;
		} else if (specVersion == DSAnnotationVersion.V1_3 && value instanceof Object[]) {
			// TODO validate empty array and duplicate PIDs!
			LinkedHashSet<String> configPids = new LinkedHashSet<>(1);
			int i = 0;
			for (Object configPidElem : ((Object[]) value)) {
				String configPidStr = String.valueOf(configPidElem);
				if ("$".equals(configPidStr)) { //$NON-NLS-1$
					configPids.add(name);
				} else {
					configPids.add(configPidStr);
					validateComponentConfigPID(annotation, configPidStr, i);
				}

				i++;
			}

			requiredVersion = i > 1 ?  DSAnnotationVersion.V1_3 : DSAnnotationVersion.V1_2;

			StringBuilder configPidBuf = new StringBuilder();
			for (String configPidElem : configPids) {
				if (configPidBuf.length() > 0) {
					configPidBuf.append(' ');
				}

				configPidBuf.append(configPidElem);
			}

			configPid = configPidBuf.toString();
		}

		String serviceScope = null;
		if (specVersion == DSAnnotationVersion.V1_3 && (value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding serviceScopeBinding = (IVariableBinding) value;
			serviceScope = DSEnums.getServiceScope(serviceScopeBinding.getName());
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

		if (configPid == null) {
			removeAttribute(component, ATTRIBUTE_COMPONENT_CONFIGURATION_PID, null);
		} else {
			component.setXMLAttribute(ATTRIBUTE_COMPONENT_CONFIGURATION_PID, configPid);
		}

		IDSDocumentFactory dsFactory = model.getFactory();

		IDSService service = component.getService();
		if (services.isEmpty()) {
			if (service != null) {
				component.removeService(service);
			}
		} else {
			if (service == null) {
				service = dsFactory.createService();

				// insert service element after last property or properties element
				int firstPos = Math.max(0, indexOfLastPropertyOrProperties(component));
				component.addChildNode(service, firstPos, true);
			}

			if (serviceScope == null || serviceScope.equals(VALUE_SERVICE_SCOPE_DEFAULT)) {
				removeAttribute(service, "scope", null); //$NON-NLS-1$
			} else {
				service.setXMLAttribute(ATTRIBUTE_SERVICE_SCOPE, serviceScope);
				requiredVersion = DSAnnotationVersion.V1_3;
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

		ArrayList<IDSReference> references = new ArrayList<>();
		HashMap<String, Annotation> referenceNames = new HashMap<>();
		IDSReference[] refElements = component.getReferences();

		HashMap<String, IDSReference> refMap = new HashMap<>(refElements.length);
		for (IDSReference refElement : refElements) {
			String referenceName = refElement.getReferenceName();
			if (referenceName == null) {
				referenceName = refElement.getXMLAttributeValue(ATTRIBUTE_REFERENCE_FIELD);
				if (referenceName == null) {
					referenceName = refElement.getReferenceBind();
					if (referenceName == null) {
						referenceName = refElement.getReferenceInterface();
					}
				}
			}

			refMap.put(referenceName, refElement);
		}

		if (annotation.isNormalAnnotation() && specVersion == DSAnnotationVersion.V1_3) {
			for (Object annotationValue : ((NormalAnnotation) annotation).values()) {
				MemberValuePair annotationMemberValuePair = (MemberValuePair) annotationValue;
				if (!ATTRIBUTE_COMPONENT_REFERENCE.equals(annotationMemberValuePair.getName().getIdentifier())) {
					continue;
				}

				ArrayList<Annotation> annotations = new ArrayList<>();

				Expression memberValue = annotationMemberValuePair.getValue();
				if (memberValue instanceof Annotation) {
					annotations.add((Annotation) memberValue);
				} else if (memberValue instanceof ArrayInitializer) {
					for (Object memberValueElement : ((ArrayInitializer) memberValue).expressions()) {
						if (memberValueElement instanceof Annotation) {
							annotations.add((Annotation) memberValueElement);
						}
					}
				}

				for (Annotation referenceAnnotation : annotations) {
					IAnnotationBinding referenceAnnotationBinding = referenceAnnotation.resolveAnnotationBinding();
					if (referenceAnnotationBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for annotation: %s", referenceAnnotation)); //$NON-NLS-1$
						}

						continue;
					}

					String annotationName = referenceAnnotationBinding.getAnnotationType().getQualifiedName();
					if (!REFERENCE_ANNOTATION.equals(annotationName)) {
						continue;
					}

					HashMap<String, Object> annotationParams = new HashMap<>();
					for (IMemberValuePairBinding pair : referenceAnnotationBinding.getDeclaredMemberValuePairs()) {
						annotationParams.put(pair.getName(), pair.getValue());
					}

					String referenceName = (String) annotationParams.get(IDSConstants.ATTRIBUTE_REFERENCE_NAME);

					IDSReference reference = refMap.remove(referenceName);
					if (reference == null) {
						reference = createReference(dsFactory);
					}

					references.add(reference);

					processReference(reference, typeBinding, referenceAnnotation, referenceAnnotationBinding, annotationParams, referenceNames);
					requiredVersion = DSAnnotationVersion.V1_3;
				}
			}
		}

		if (specVersion == DSAnnotationVersion.V1_3) {
			for (FieldDeclaration field : type.getFields()) {
				for (Object modifier : field.modifiers()) {
					if (!(modifier instanceof Annotation)) {
						continue;
					}

					Annotation fieldAnnotation = (Annotation) modifier;
					IAnnotationBinding fieldAnnotationBinding = fieldAnnotation.resolveAnnotationBinding();
					if (fieldAnnotationBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for annotation: %s", fieldAnnotation)); //$NON-NLS-1$
						}

						continue;
					}

					String annotationName = fieldAnnotationBinding.getAnnotationType().getQualifiedName();
					if (!REFERENCE_ANNOTATION.equals(annotationName)) {
						continue;
					}

					HashMap<String, Object> annotationParams = null;
					// TODO do we really care about all fragments??
					for (Object fragmentElement : field.fragments()) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentElement;
						IVariableBinding fieldBinding = fragment.resolveBinding();
						if (fieldBinding == null) {
							if (debug.isDebugging()) {
								debug.trace(String.format("Unable to resolve binding for field: %s", fragment)); //$NON-NLS-1$
							}

							continue;
						}

						if (annotationParams == null) {
							annotationParams = new HashMap<>();
							for (IMemberValuePairBinding pair : fieldAnnotationBinding.getDeclaredMemberValuePairs()) {
								annotationParams.put(pair.getName(), pair.getValue());
							}
						}

						String referenceName = (String) annotationParams.get("name"); //$NON-NLS-1$
						if (referenceName == null) {
							referenceName = fieldBinding.getName();
						}

						IDSReference reference = refMap.remove(referenceName);
						if (reference == null) {
							reference = createReference(dsFactory);
						}

						references.add(reference);

						processReference(reference, field, fieldBinding, fieldAnnotation, fieldAnnotationBinding, annotationParams, referenceNames);
						requiredVersion = DSAnnotationVersion.V1_3;
					}
				}
			}
		}

		String activate = null;
		boolean lookedForActivateMethod = false;
		IMethodBinding activateMethod = null;
		Annotation activateAnnotation = null;
		String deactivate = null;
		boolean lookedForDeactivateMethod = false;
		IMethodBinding deactivateMethod = null;
		Annotation deactivateAnnotation = null;
		String modified = null;
		IMethodBinding modifiedMethod = null;
		Annotation modifiedAnnotation = null;

		for (MethodDeclaration method : type.getMethods()) {
			for (Object modifier : method.modifiers()) {
				if (!(modifier instanceof Annotation)) {
					continue;
				}

				Annotation methodAnnotation = (Annotation) modifier;
				IAnnotationBinding methodAnnotationBinding = methodAnnotation.resolveAnnotationBinding();
				if (methodAnnotationBinding == null) {
					if (debug.isDebugging()) {
						debug.trace(String.format("Unable to resolve binding for annotation: %s", methodAnnotation)); //$NON-NLS-1$
					}

					continue;
				}

				String annotationName = methodAnnotationBinding.getAnnotationType().getQualifiedName();

				if (ACTIVATE_ANNOTATION.equals(annotationName)) {
					if (activate == null) {
						activate = method.getName().getIdentifier();
						if (specVersion == DSAnnotationVersion.V1_3) {
							activateMethod = method.resolveBinding();
						}

						activateAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "activate", method); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateActivateMethod, method.getName().getIdentifier());
						if (activateAnnotation != null) {
							reportProblem(activateAnnotation, null, Messages.AnnotationProcessor_duplicateActivateMethod, activate);
							activateAnnotation = null;
						}
					}

					continue;
				}

				if (DEACTIVATE_ANNOTATION.equals(annotationName)) {
					if (deactivate == null) {
						deactivate = method.getName().getIdentifier();
						if (specVersion == DSAnnotationVersion.V1_3) {
							deactivateMethod = method.resolveBinding();
						}

						deactivateAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "deactivate", method); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateDeactivateMethod, method.getName().getIdentifier());
						if (deactivateAnnotation != null) {
							reportProblem(deactivateAnnotation, null, Messages.AnnotationProcessor_duplicateDeactivateMethod, deactivate);
							deactivateAnnotation = null;
						}
					}

					continue;
				}

				if (MODIFIED_ANNOTATION.equals(annotationName)) {
					if (modified == null) {
						modified = method.getName().getIdentifier();
						if (specVersion == DSAnnotationVersion.V1_3) {
							modifiedMethod = method.resolveBinding();
						}

						modifiedAnnotation = methodAnnotation;
						validateLifeCycleMethod(methodAnnotation, "modified", method); //$NON-NLS-1$
					} else if (!errorLevel.isIgnore()) {
						reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateModifiedMethod, method.getName().getIdentifier());
						if (modifiedAnnotation != null) {
							reportProblem(modifiedAnnotation, null, Messages.AnnotationProcessor_duplicateModifiedMethod, modified);
							modifiedAnnotation = null;
						}
					}

					continue;
				}

				if (REFERENCE_ANNOTATION.equals(annotationName)) {
					IMethodBinding methodBinding = method.resolveBinding();
					if (methodBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for method: %s", method)); //$NON-NLS-1$
						}
					} else {
						HashMap<String, Object> annotationParams = new HashMap<>();
						for (IMemberValuePairBinding pair : methodAnnotationBinding.getDeclaredMemberValuePairs()) {
							annotationParams.put(pair.getName(), pair.getValue());
						}

						String referenceName = getReferenceName(methodBinding.getName(), annotationParams);

						IDSReference reference = refMap.remove(referenceName);
						if (reference == null) {
							reference = createReference(dsFactory);
						}

						references.add(reference);

						requiredVersion = requiredVersion.max(processReference(reference, method, methodBinding, methodAnnotation, methodAnnotationBinding, annotationParams, referenceNames));
					}

					continue;
				}
			}
		}

		if (activate == null) {
			// only remove activate="activate" if method not found
			if (!"activate".equals(component.getActivateMethod()) //$NON-NLS-1$
					|| ((lookedForActivateMethod = true)
							&& (activateMethod = findLifeCycleMethod(typeBinding, "activate")) == null)) { //$NON-NLS-1$
				removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATE, null);
			}
		} else {
			component.setActivateMethod(activate);
		}

		if (deactivate == null) {
			// only remove deactivate="deactivate" if method not found
			if (!"deactivate".equals(component.getDeactivateMethod()) //$NON-NLS-1$
					|| ((lookedForDeactivateMethod = true)
							&& (deactivateMethod = findLifeCycleMethod(typeBinding, "deactivate")) == null)) { //$NON-NLS-1$
				removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_DEACTIVATE, null);
			}
		} else {
			component.setDeactivateMethod(deactivate);
		}

		if (modified == null) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_MODIFIED, null);
		} else {
			component.setModifiedeMethod(modified);
		}

		ArrayList<IDSProperty> propList = new ArrayList<>();

		if (specVersion == DSAnnotationVersion.V1_3) {
			// collect component property types from activate, modified, and deactivate methods
			if (activateMethod == null && !lookedForActivateMethod) {
				activateMethod = findLifeCycleMethod(typeBinding, "activate"); //$NON-NLS-1$
			}

			if (deactivateMethod == null && !lookedForDeactivateMethod) {
				deactivateMethod = findLifeCycleMethod(typeBinding, "deactivate"); //$NON-NLS-1$
			}

			HashSet<ITypeBinding> cptClosure = new HashSet<>();

			if (activateMethod != null) {
				collectProperties(activateMethod, dsFactory, propList, cptClosure);
			}

			if (modifiedMethod != null) {
				collectProperties(modifiedMethod, dsFactory, propList, cptClosure);
			}

			if (deactivateMethod != null) {
				collectProperties(deactivateMethod, dsFactory, propList, cptClosure);
			}
		}

		IDSProperty[] propElements = component.getPropertyElements();
		if (propList.isEmpty() && properties.length == 0) {
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
					if (propertyType == null) {
						removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_TYPE, null);	 // just remove the attribute completely so we can detect changes when reconciling
					} else {
						property.setPropertyType(propertyType);
					}

					property.setPropertyValue(propertyValue);
					validateComponentProperty(annotation, propertyName, propertyType, propertyValue, i);
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
						if (!actual.equals(expected)) {
							reportProblem(annotation, "property", i, NLS.bind(Messages.AnnotationProcessor_inconsistentComponentPropertyType, actual, expected), actual); //$NON-NLS-1$
						} else {
							validateComponentProperty(annotation, propertyName, propertyType, propertyValue, i);
						}
					}

					if (propertyValue != null) {
						property.setPropertyElemBody(content + "\n" + pair[1]); //$NON-NLS-1$
					}
				}
			}

			// reconcile against existing property elements
			HashMap<String, List<IDSProperty>> propMap = new HashMap<>(propElements.length);
			for (IDSProperty propElement : propElements) {
				List<IDSProperty> duplicates = propMap.get(propElement.getPropertyName());
				if (duplicates == null) {
					duplicates = new LinkedList<>();
					propMap.put(propElement.getPropertyName(), duplicates);
				}

				duplicates.add(propElement);
			}

			propList.addAll(map.values());
			for (ListIterator<IDSProperty> i = propList.listIterator(); i.hasNext();) {
				IDSProperty newProperty = i.next();
				List<IDSProperty> propertyElemList = propMap.get(newProperty.getPropertyName());
				if (propertyElemList == null) {
					continue;
				}

				IDSProperty property = propertyElemList.remove(0);
				if (propertyElemList.isEmpty()) {
					propMap.remove(newProperty.getPropertyName());
				}

				i.set(property);

				String newPropertyType = newProperty.getPropertyType();
				if (newPropertyType != null || !IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(property.getPropertyType())) {
					property.setPropertyType(newPropertyType);
				}

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

			ArrayList<IDSProperty> leftovers = new ArrayList<>();
			for (List<IDSProperty> propertyElementList : propMap.values()) {
				leftovers.addAll(propertyElementList);
			}

			removeChildren(component, leftovers);

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

		String xmlns = requiredVersion.getNamespace();
		if ((value = params.get("xmlns")) instanceof String) { //$NON-NLS-1$
			xmlns = (String) value;
			validateComponentXMLNS(annotation, xmlns, requiredVersion);
		}

		component.setNamespace(xmlns);
	}

	private IDSReference createReference(IDSDocumentFactory dsFactory) {
		IDSReference reference = dsFactory.createReference();
		// reset unnecessary defaults set by PDE
		removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY, null);
		removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_POLICY, null);
		return reference;
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
			if (value != null && value.equals(defaultValue)) {
				return;
			}

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
		if (currentIndex == -1) {
			return;
		}

		int newIndex = newRelativeIndex + currentIndex;
		if (newIndex < 0 || newIndex >= obj.getChildCount()) {
			return;
		}

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
		if (propElements.length > 0) {
			pos = component.indexOf(propElements[propElements.length - 1]) + 1;
		}

		if (propFileElements.length > 0) {
			int lastPos = component.indexOf(propFileElements[propFileElements.length - 1]) + 1;
			if (lastPos > pos) {
				pos = lastPos;
			}
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
				if (trimmed.length() == 0) {
					continue;
				}

				if (buf.length() > 0) {
					buf.append('\n');
				}

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

	private void collectProperties(IMethodBinding method, IDSDocumentFactory factory, Collection<IDSProperty> properties, Collection<ITypeBinding> visited) {
		for (ITypeBinding paramTypeBinding : method.getParameterTypes()) {
			if (!paramTypeBinding.isAnnotation() || !visited.add(paramTypeBinding)) {
				continue;
			}

			for (IMethodBinding methodBinding : paramTypeBinding.getDeclaredMethods()) {
				if (!methodBinding.isAnnotationMember()) {
					continue;
				}

				Object value = methodBinding.getDefaultValue();
				if (value == null || (value instanceof Object[] && ((Object[]) value).length == 0)) {
					continue;
				}

				ITypeBinding returnType = methodBinding.getReturnType();
				if (returnType.isArray() ? returnType.getElementType().isAnnotation() : returnType.isAnnotation()) {
					// TODO per spec we should report error, but we may have no annotation to report it on!
					continue;
				}

				IDSProperty property = factory.createProperty();
				property.setPropertyName(createPropertyName(methodBinding.getName()));
				property.setPropertyType(getPropertyType(returnType));

				if (returnType.isArray()) {
					StringBuilder body = new StringBuilder();
					for (Object item : ((Object[]) value)) {
						String itemValue = getPropertyValue(item);
						if (itemValue == null || (itemValue = itemValue.trim()).isEmpty()) {
							continue;
						}

						if (body.length() > 0) {
							body.append('\n');
						}

						body.append(itemValue);
					}

					property.setPropertyElemBody(body.toString());
				} else {
					property.setPropertyValue(getPropertyValue(value));
				}

				properties.add(property);
			}
		}
	}

	private String createPropertyName(String name) {
		StringBuilder buf = new StringBuilder(name.length());
		char[] chars = name.toCharArray();
		for (int i = 0, n = chars.length; i < n; ++i) {
			if (chars[i] == '$') {
				if (i == n - 1 || chars[i + 1] != '$') {
					continue;
				}

				i++;
			} else if (chars[i] == '_') {
				if (i == n - 1 || chars[i + 1] != '_') {
					chars[i] = '.';
				} else {
					i++;
				}
			}

			buf.append(chars[i]);
		}

		return buf.toString();
	}

	private String getPropertyType(ITypeBinding type) {
		if (type.isArray()) {
			return getPropertyType(type.getElementType());
		}

		if (type.isPrimitive()) {
			String name = type.getQualifiedName();
			String result = PRIMITIVE_TYPE_MAP.get(name);
			if (result != null) {
				return result;
			}
		}

		return IDSConstants.VALUE_PROPERTY_TYPE_STRING;
	}

	private String getPropertyValue(Object value) {
		// enum
		if (value instanceof IVariableBinding) {
			return ((IVariableBinding) value).getName();
		}

		// class
		if (value instanceof ITypeBinding) {
			return ((ITypeBinding) value).getQualifiedName();
		}

		// everything else
		return String.valueOf(value);
	}

	private void validateComponentName(Annotation annotation, String name) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(name).matches()) {
			reportProblem(annotation, "name", NLS.bind(Messages.AnnotationProcessor_invalidComponentName, name), name); //$NON-NLS-1$
		}
	}

	private void validateComponentService(Annotation annotation, ITypeBinding componentType, ITypeBinding serviceType, int index) {
		if (!errorLevel.isIgnore() && !componentType.isAssignmentCompatible(serviceType)) {
			reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidComponentService, serviceType.getName()), serviceType.getName()); //$NON-NLS-1$
		}
	}

	private void validateComponentFactory(Annotation annotation, String factory) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(factory).matches()) {
			reportProblem(annotation, "factory", NLS.bind(Messages.AnnotationProcessor_invalidComponentFactoryName, factory), factory); //$NON-NLS-1$
		}
	}

	private void validateComponentProperty(Annotation annotation, String name, String type, String value, int index) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (PROPERTY_TYPES.contains(type)) {
			if (name == null || name.trim().length() == 0) {
				reportProblem(annotation, "property", index, Messages.AnnotationProcessor_invalidComponentProperty_nameRequired, name); //$NON-NLS-1$
			}

			if (value == null) {
				reportProblem(annotation, "property", index, Messages.AnnotationProcessor_invalidComponentProperty_valueRequired, name); //$NON-NLS-1$
			} else {
				try {
					if (IDSConstants.VALUE_PROPERTY_TYPE_LONG.equals(type)) {
						Long.valueOf(value);
					} else if (IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE.equals(type)) {
						Double.valueOf(value);
					} else if (IDSConstants.VALUE_PROPERTY_TYPE_FLOAT.equals(type)) {
						Float.valueOf(value);
					} else if (IDSConstants.VALUE_PROPERTY_TYPE_INTEGER.equals(type) || IDSConstants.VALUE_PROPERTY_TYPE_CHAR.equals(type)) {
						Integer.valueOf(value);
					} else if (IDSConstants.VALUE_PROPERTY_TYPE_BYTE.equals(type)) {
						Byte.valueOf(value);
					} else if (IDSConstants.VALUE_PROPERTY_TYPE_SHORT.equals(type)) {
						Short.valueOf(value);
					}
				} catch (NumberFormatException e) {
					reportProblem(annotation, "property", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyValue, type, value), String.valueOf(value)); //$NON-NLS-1$
				}
			}
		} else {
			reportProblem(annotation, "property", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyType, type), String.valueOf(type)); //$NON-NLS-1$
		}
	}

	private void validateComponentPropertyFiles(Annotation annotation, IProject project, String[] files) {
		if (errorLevel.isIgnore()) {
			return;
		}

		for (int i = 0; i < files.length; ++i) {
			String file = files[i];
			IFile wsFile = PDEProject.getBundleRelativeFile(project, new Path(file));
			if (!wsFile.exists()) {
				reportProblem(annotation, "properties", i, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyFile, file), file); //$NON-NLS-1$
			}
		}
	}

	private void validateComponentXMLNS(Annotation annotation, String xmlns, DSAnnotationVersion requiredVersion) {
		if (errorLevel.isIgnore()) {
			return;
		}

		DSAnnotationVersion specifiedVersion = DSAnnotationVersion.fromNamespace(xmlns);
		if (requiredVersion.compareTo(specifiedVersion) > 0) {
			reportProblem(annotation, "xmlns", NLS.bind(Messages.AnnotationProcessor_invalidComponentDescriptorNamespace, xmlns), xmlns); //$NON-NLS-1$
		}
	}

	private void validateComponentConfigPID(Annotation annotation, String configPid, int index) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(configPid).matches()) {
			reportProblem(annotation, "configurationPid", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentConfigurationPid, configPid), configPid); //$NON-NLS-1$
		}
	}

	private void validateLifeCycleMethod(Annotation annotation, String methodName, MethodDeclaration method) {
		if (errorLevel.isIgnore()) {
			return;
		}

		IMethodBinding methodBinding = method.resolveBinding();
		if (methodBinding == null) {
			if (debug.isDebugging()) {
				debug.trace(String.format("Unable to resolve binding for method: %s", method)); //$NON-NLS-1$
			}

			return;
		}

		if (Modifier.isStatic(methodBinding.getModifiers())) {
			reportProblem(annotation, methodName, Messages.AnnotationProcessor_invalidLifecycleMethod_static);
		}

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName)) {
			reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodReturnType, methodName, returnTypeName), returnTypeName);
		}

		ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();

		if (paramTypeBindings.length == 0) {
			// no-arg method
			return;
		}

		// every argument must be either Map, Annotation (component property type), ComponentContext, or BundleContext
		boolean hasMap = false;
		boolean hasCompCtx = false;
		boolean hasBundleCtx = false;
		boolean hasInt = false;
		HashSet<ITypeBinding> annotationParams = new HashSet<>(1);

		for (ITypeBinding paramTypeBinding : paramTypeBindings) {
			ITypeBinding paramTypeErasure = paramTypeBinding.getErasure();
			String paramTypeName = paramTypeErasure.isMember() ? paramTypeErasure.getBinaryName() : paramTypeErasure.getQualifiedName();
			boolean isDuplicate = false;

			if (paramTypeBinding.isAnnotation() && specVersion == DSAnnotationVersion.V1_3) {
				if (!annotationParams.add(paramTypeBinding)) {
					isDuplicate = true;
				}
			} else if (Map.class.getName().equals(paramTypeName)) {
				if (hasMap) {
					isDuplicate = true;
				} else {
					hasMap = true;
				}
			} else if (COMPONENT_CONTEXT.equals(paramTypeName)) {
				if (hasCompCtx) {
					isDuplicate = true;
				} else {
					hasCompCtx = true;
				}
			} else if (BundleContext.class.getName().equals(paramTypeName)) {
				if (hasBundleCtx) {
					isDuplicate = true;
				} else {
					hasBundleCtx = true;
				}
			} else if ("deactivate".equals(methodName) //$NON-NLS-1$
					&& (Integer.class.getName().equals(paramTypeName) || Integer.TYPE.getName().equals(paramTypeName))) {
				if (hasInt) {
					isDuplicate = true;
				} else {
					hasInt = true;
				}
			} else {
				reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
			}

			if (isDuplicate) {
				reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_duplicateLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
			}
		}
	}

	private IMethodBinding findLifeCycleMethod(ITypeBinding componentClass, String methodName) {
		for (IMethodBinding methodBinding : componentClass.getDeclaredMethods()) {
			if (methodName.equals(methodBinding.getName())
					&& Void.TYPE.getName().equals(methodBinding.getReturnType().getName())) {
				ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();

				// every argument must be either Map, Annotation (component property type), ComponentContext, or BundleContext
				boolean hasMap = false;
				boolean hasCompCtx = false;
				boolean hasBundleCtx = false;
				boolean hasInt = false;
				boolean isInvalid = false;
				HashSet<ITypeBinding> annotationParams = new HashSet<>(1);
				for (ITypeBinding paramTypeBinding : paramTypeBindings) {
					if (paramTypeBinding.isAnnotation()) {
						if (specVersion == DSAnnotationVersion.V1_3 && annotationParams.add(paramTypeBinding)) {
							// component property type (multiple arguments allowed)
							continue;
						}

						isInvalid = true;
						break;
					}

					ITypeBinding paramTypeErasure = paramTypeBinding.getErasure();
					String paramTypeName = paramTypeErasure.isMember() ? paramTypeErasure.getBinaryName() : paramTypeErasure.getQualifiedName();

					if (Map.class.getName().equals(paramTypeName)) {
						if (hasMap) {
							isInvalid = true;
						} else {
							hasMap = true;
						}
					} else if (COMPONENT_CONTEXT.equals(paramTypeName)) {
						if (hasCompCtx) {
							isInvalid = true;
						} else {
							hasCompCtx = true;
						}
					} else if (BundleContext.class.getName().equals(paramTypeName)) {
						if (hasBundleCtx) {
							isInvalid = true;
						} else {
							hasBundleCtx = true;
						}
					} else if ("deactivate".equals(methodName) //$NON-NLS-1$
							&& (Integer.class.getName().equals(paramTypeName) || Integer.TYPE.getName().equals(paramTypeName))) {
						if (hasInt) {
							isInvalid = true;
						} else {
							hasInt = true;
						}
					} else {
						isInvalid = true;
					}

					if (isInvalid) {
						break;
					}
				}

				if (!isInvalid) {
					return methodBinding;
				}
			}
		}

		return null;
	}

	private DSAnnotationVersion processReference(IDSReference reference, MethodDeclaration method, IMethodBinding methodBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
		ITypeBinding[] argTypes = methodBinding.getParameterTypes();

		ITypeBinding serviceType;
		Object value;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
			if (!errorLevel.isIgnore() && argTypes.length > 0) {
				if (specVersion == DSAnnotationVersion.V1_3) {
					for (ITypeBinding argType : argTypes) {
						if (!isValidArgumentForService(argType, serviceType)) {
							reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_serviceType, argType.getName(), serviceType.getName()), argType.getName(), serviceType.getName()); //$NON-NLS-1$
							break;
						}
					}
				} else {
					String erasure = argTypes[0].getErasure().getBinaryName();
					ITypeBinding[] typeArgs;
					if (!(ServiceReference.class.getName().equals(erasure)
							&& ((typeArgs = argTypes[0].getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
							&& !serviceType.isAssignmentCompatible(argTypes[0])) {
						reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_serviceType, argTypes[0].getName(), serviceType.getName()), argTypes[0].getName(), serviceType.getName()); //$NON-NLS-1$
					}
				}
			}
		} else if (argTypes.length > 0) {
			if (specVersion == DSAnnotationVersion.V1_3) {
				serviceType = null;
				for (ITypeBinding argType : argTypes) {
					String erasure = argType.getErasure().getBinaryName();
					if (ServiceReference.class.getName().equals(erasure) || COMPONENT_SERVICE_OBJECTS.equals(erasure)) {
						ITypeBinding[] typeArgs = argType.getTypeArguments();
						if (typeArgs.length > 0) {
							serviceType = typeArgs[0];
							break;
						}

						continue;
					}

					if (Map.class.equals(erasure)) {
						continue;
					}

					serviceType = argType.isPrimitive() ? getObjectType(method.getAST(), argType) : argType;
					break;
				}
			} else {
				String erasure = argTypes[0].getErasure().getBinaryName();
				if (ServiceReference.class.getName().equals(erasure)) {
					ITypeBinding[] typeArgs = argTypes[0].getTypeArguments();
					if (typeArgs.length > 0) {
						serviceType = typeArgs[0];
					} else {
						serviceType = null;
					}
				} else {
					serviceType = argTypes[0].isPrimitive() ? getObjectType(method.getAST(), argTypes[0]) : argTypes[0];
				}
			}
		} else {
			serviceType = null;
		}

		if (serviceType == null) {
			reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_serviceUnknown);

			serviceType = method.getAST().resolveWellKnownType(Object.class.getName());
		}

		validateReferenceBindMethod(annotation, serviceType, methodBinding);

		String service = serviceType == null ? null : serviceType.getBinaryName();

		String methodName = methodBinding.getName();
		String name = getReferenceName(methodName, params);

		validateReferenceName(name, annotation, names);

		String cardinality = null;
		if ((value = params.get("cardinality")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding cardinalityBinding = (IVariableBinding) value;
			cardinality = DSEnums.getReferenceCardinality(cardinalityBinding.getName());
		}

		String policy = null;
		if ((value = params.get("policy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyBinding = (IVariableBinding) value;
			policy = DSEnums.getReferencePolicy(policyBinding.getName());
		}

		String target = null;
		if ((value = params.get("target")) instanceof String) { //$NON-NLS-1$
			target = (String) value;
			validateReferenceTarget(annotation, target);
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
					if (unbindMethod == null) {
						reportProblem(annotation, "unbind", NLS.bind(Messages.AnnotationProcessor_invalidReference_unbindMethod, unbind), unbind); //$NON-NLS-1$
					}
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
					reportProblem(annotation, null, missingUnbindMethodLevel, NLS.bind(Messages.AnnotationProcessor_invalidReference_noImplicitUnbind, unbindCandidate), unbindCandidate);
				}
			} else {
				unbind = unbindMethod.getName();
			}
		}

		String policyOption = null;
		if ((value = params.get("policyOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyOptionBinding = (IVariableBinding) value;
			policyOption = DSEnums.getReferencePolicyOption(policyOptionBinding.getName());
		}

		String updated;
		if ((value = params.get("updated")) instanceof String) { //$NON-NLS-1$
			String updatedValue = (String) value;
			if ("-".equals(updatedValue)) { //$NON-NLS-1$
				updated = null;
			} else {
				updated = updatedValue;
				if (!errorLevel.isIgnore()) {
					IMethodBinding updatedMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, updated, true);
					if (updatedMethod == null) {
						reportProblem(annotation, "updated", NLS.bind(Messages.AnnotationProcessor_invalidReference_updatedMethod, updated), updated); //$NON-NLS-1$
					}
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
			if (updatedMethod == null) {
				updated = null;
			} else {
				updated = updatedMethod.getName();
			}
		}

		String referenceScope = null;
		if (specVersion == DSAnnotationVersion.V1_3) {
			if ((value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
				IVariableBinding referenceScopeBinding = (IVariableBinding) value;
				referenceScope = DSEnums.getReferenceScope(referenceScopeBinding.getName());
			}

			processReferenceFieldParams(reference, methodBinding.getDeclaringClass(), annotation, params, serviceType, cardinality, policy);

			if (!errorLevel.isIgnore()) {
				String bind;
				if ((value = params.get("bind")) instanceof String) { //$NON-NLS-1$
					bind = (String) value;
					if (!methodName.equals(bind)) {
						reportProblem(annotation, "bind", Messages.AnnotationProcessor_invalidReference_bindMethodNameMismatch, bind); //$NON-NLS-1$
					}
				}
			}
		} else {
			updateFieldAttributes(reference, null, null, null);
		}

		updateAttributes(reference, name, service, cardinality, policy, target, policyOption, referenceScope);
		updateMethodAttributes(reference, methodName, updated, unbind);

		DSAnnotationVersion requiredVersion;
		if (reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_SCOPE) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD_OPTION) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE) != null) {
			requiredVersion = DSAnnotationVersion.V1_3;
		} else if (reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_UPDATED) != null) {
			requiredVersion = DSAnnotationVersion.V1_2;
		} else {
			requiredVersion = DSAnnotationVersion.V1_1;
		}

		return requiredVersion;
	}

	private boolean isValidArgumentForService(ITypeBinding argType, ITypeBinding serviceType) {
		String erasure = argType.getErasure().getBinaryName();
		ITypeBinding[] typeArgs;
		return ((ServiceReference.class.getName().equals(erasure) || COMPONENT_SERVICE_OBJECTS.equals(erasure))
				&& ((typeArgs = argType.getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
				|| serviceType.isAssignmentCompatible(argType)
				|| Map.class.getName().equals(erasure);
	}

	private String getReferenceName(String methodName, Map<String, Object> params) {
		Object value;
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

		return name;
	}

	private void validateReferenceName(String name, Annotation annotation, Map<String, Annotation> names) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (names.containsKey(name)) {
			reportProblem(annotation, "name", NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
			Annotation duplicate = names.put(name, null);
			if (duplicate != null) {
				reportProblem(duplicate, "name", NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
			}
		} else {
			names.put(name, annotation);
		}
	}

	private void processReferenceFieldParams(IDSReference reference, ITypeBinding typeBinding, Annotation annotation, Map<String, ?> params, ITypeBinding serviceType, String cardinality, String policy) {
		String field = null;
		IVariableBinding fieldBinding = null;
		FieldCollectionTypeDescriptor collectionType = null;
		Object value;
		if ((value = params.get("field")) instanceof String) { //$NON-NLS-1$
			field = (String) value;
			if (!errorLevel.isIgnore()) {
				fieldBinding = findReferenceField(field, typeBinding);
				if (fieldBinding == null) {
					reportProblem(annotation, "field", NLS.bind(Messages.AnnotationProcessor_invalidReference_fieldNotFound, field), field); //$NON-NLS-1$
				} else if (serviceType != null) {
					ITypeBinding targetType = fieldBinding.getType();
					if (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
							|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)) {
						if (collectionType == null) {
							collectionType = determineCollectionType(annotation.getAST(), targetType);
						}

						targetType = collectionType.getElementType();
					}

					if (targetType != null) {
						if (!isValidFieldForService(targetType, serviceType)) {
							reportProblem(annotation, "field", NLS.bind(Messages.AnnotationProcessor_invalidReference_incompatibleFieldType, targetType.getName(), serviceType.getName()), targetType.getName(), serviceType.getName()); //$NON-NLS-1$
						}
					}
				}
			}
		}

		String fieldOption = null;
		if ((value = params.get("fieldOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding fieldOptionBinding = (IVariableBinding) value;
			fieldOption = DSEnums.getFieldOption(fieldOptionBinding.getName());
			if (!errorLevel.isIgnore()) {
				if (field == null) {
					reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldOptionNotApplicable); //$NON-NLS-1$
				} else if (VALUE_REFERENCE_FIELD_OPTION_REPLACE.equals(fieldOption)) {
					if (fieldBinding == null) {
						fieldBinding = findReferenceField(field, typeBinding);
					}

					if (fieldBinding != null && Modifier.isFinal(fieldBinding.getModifiers())) {
						reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldFinal_fieldOption, fieldOption); //$NON-NLS-1$
					}
				} else if (VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					if (!(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)
							&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
									|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)))) {
						reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldPolicyCardinality_fieldOption, fieldOption); //$NON-NLS-1$
					}
				}
			}
		}

		String fieldCollectionType = null;
		if (field != null && (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
				|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality))) {
			if (fieldBinding == null) {
				fieldBinding = findReferenceField(field, typeBinding);
			}

			if (fieldBinding != null) {
				if (collectionType == null) {
					collectionType = determineCollectionType(annotation.getAST(), fieldBinding.getType());
				}

				if (collectionType.getElementType() != null) {
					fieldCollectionType = getFieldCollectionType(collectionType);
				}
			}
		}

		updateFieldAttributes(reference, field, fieldOption, fieldCollectionType);
	}

	private void updateAttributes(
			IDSReference reference,
			String name,
			String service,
			String cardinality,
			String policy,
			String target,
			String policyOption,
			String scope) {
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

		if (policyOption == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_POLICY_OPTION, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION, policyOption);
		}

		if (scope == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_SCOPE, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_SCOPE, scope);
		}
	}

	private void updateMethodAttributes(
			IDSReference reference,
			String bind,
			String updated,
			String unbind) {
		if (bind == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_BIND, null);
		} else {
			reference.setReferenceBind(bind);
		}

		if (updated == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_UPDATED, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_UPDATED, updated);
		}

		if (unbind == null) {
			removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_UNBIND, null);
		} else {
			reference.setReferenceUnbind(unbind);
		}
	}

	private void updateFieldAttributes(
			IDSReference reference,
			String field,
			String fieldOption,
			String fieldCollectionType) {
		if (field == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD, field);
		}

		if (fieldOption == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD_OPTION, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD_OPTION, fieldOption);
		}

		if (fieldCollectionType == null) {
			removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE, fieldCollectionType);
		}
	}

	private void processReference(IDSReference reference, FieldDeclaration field, IVariableBinding fieldBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
		String cardinality = null;
		Object value;
		if ((value = params.get("cardinality")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding cardinalityBinding = (IVariableBinding) value;
			cardinality = DSEnums.getReferenceCardinality(cardinalityBinding.getName());
		}

		ITypeBinding fieldType = fieldBinding.getType();

		FieldCollectionTypeDescriptor collectionType = null;
		if (cardinality == null) {
			collectionType = determineCollectionType(field.getAST(), fieldType);
			if (collectionType.getElementType() == null) {
				cardinality = IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE;
			} else {
				cardinality = IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N;
			}
		} else {
			if (!errorLevel.isIgnore()
					&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
							|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality))) {
				collectionType = determineCollectionType(field.getAST(), fieldType);
				if (collectionType.getElementType() == null) {
					reportProblem(annotation, "cardinality", Messages.AnnotationProcessor_invalidReference_fieldTypeCardinalityMismatch, cardinality); //$NON-NLS-1$
				}
			}
		}

		ITypeBinding serviceType;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
			if (!errorLevel.isIgnore()) {
				ITypeBinding targetType = fieldType;
				if (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
						|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)) {
					if (collectionType == null) {
						collectionType = determineCollectionType(field.getAST(), fieldType);
					}

					targetType = collectionType.getElementType();
				}

				if (targetType != null) {
					if (!isValidFieldForService(targetType, serviceType)) {
						reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_incompatibleServiceType, targetType.getName(), serviceType.getName()), targetType.getName(), serviceType.getName()); //$NON-NLS-1$
					}
				}
			}
		} else {
			ITypeBinding targetType = fieldType;
			if (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
					|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)) {
				if (collectionType == null) {
					collectionType = determineCollectionType(field.getAST(), targetType);
				}

				targetType = collectionType.getElementType();
			}

			serviceType = targetType == null ? null : getFieldServiceType(field.getAST(), targetType);
		}

		if (serviceType == null) {
			reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_fieldUnknownServiceType);

			serviceType = field.getAST().resolveWellKnownType(Object.class.getName());
		}

		validateReferenceField(annotation, fieldBinding);

		String service = serviceType == null ? null : serviceType.getBinaryName();

		String fieldCollectionType = null;
		if (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
				|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)) {
			if (collectionType == null) {
				collectionType = determineCollectionType(field.getAST(), fieldType);
			}

			if (collectionType.getElementType() != null) {
				fieldCollectionType = getFieldCollectionType(collectionType);
			}
		}

		String fieldName = fieldBinding.getName();
		String name;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			name = (String) value;
		} else {
			name = fieldName;
		}

		validateReferenceName(name, annotation, names);

		if (!errorLevel.isIgnore()) {
			String fieldVal;
			if ((value = params.get("field")) instanceof String) { //$NON-NLS-1$
				fieldVal = (String) value;
				if (!fieldName.equals(fieldVal)) {
					reportProblem(annotation, "field", Messages.AnnotationProcessor_invalidReference_fieldNameMismatch, fieldVal); //$NON-NLS-1$
				}
			}
		}

		String policy = null;
		if ((value = params.get("policy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyBinding = (IVariableBinding) value;
			policy = DSEnums.getReferencePolicy(policyBinding.getName());
		} else if (Modifier.isVolatile(field.getModifiers())) {
			policy = IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC;
		}

		String target = null;
		if ((value = params.get("target")) instanceof String) { //$NON-NLS-1$
			target = (String) value;
			validateReferenceTarget(annotation, target);
		}

		String policyOption = null;
		if ((value = params.get("policyOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyOptionBinding = (IVariableBinding) value;
			policyOption = DSEnums.getReferencePolicyOption(policyOptionBinding.getName());
		}

		String referenceScope = null;
		if ((value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding referenceScopeBinding = (IVariableBinding) value;
			referenceScope = DSEnums.getReferenceScope(referenceScopeBinding.getName());
		}

		String fieldOption = null;
		if ((value = params.get("fieldOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding fieldOptionBinding = (IVariableBinding) value;
			fieldOption = DSEnums.getFieldOption(fieldOptionBinding.getName());
			if (!errorLevel.isIgnore()) {
				if (VALUE_REFERENCE_FIELD_OPTION_REPLACE.equals(fieldOption)) {
					if (Modifier.isFinal(field.getModifiers())) {
						reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldFinal_fieldOption, fieldOption); //$NON-NLS-1$
					}
				} else if (VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					if (!(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)
							&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
									|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)))) {
						reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldPolicyCardinality_fieldOption, fieldOption); //$NON-NLS-1$
					}
				}
			}
		} else {
			if (IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)
					&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
							|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality))
					&& Modifier.isFinal(field.getModifiers())) {
				fieldOption = VALUE_REFERENCE_FIELD_OPTION_UPDATE;
			}
		}

		if (!errorLevel.isIgnore()) {
			if (collectionType != null && collectionType.getElementType() != null && !collectionType.isExact()) {
				if (!IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)) {
					reportProblem(annotation, policy == null ? null : "policy", Messages.AnnotationProcessor_invalidReference_fieldCardinalityPolicyCollectionType); //$NON-NLS-1$
				}

				if (!VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					reportProblem(annotation, fieldOption == null ? null : "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldCollection_fieldOption); //$NON-NLS-1$
				}

				// TODO validate that field is initialized in constructor!
			}
		}

		processReferenceMethodParams(reference, fieldBinding.getDeclaringClass(), annotation, params, serviceType);

		updateAttributes(reference, name, service, cardinality, policy, target, policyOption, referenceScope);
		updateFieldAttributes(reference, fieldName, fieldOption, fieldCollectionType);
	}

	private FieldCollectionTypeDescriptor determineCollectionType(AST ast, ITypeBinding type) {
		HashSet<ITypeBinding> visited = new HashSet<>();
		LinkedList<ITypeBinding> types = new LinkedList<>();
		boolean exact = true;
		do {
			if (!visited.add(type)) {
				continue;
			}

			String erasure = type.getErasure().getBinaryName();
			if (Collection.class.getName().equals(erasure) || List.class.getName().equals(erasure)) {
				ITypeBinding[] typeArgs = type.getTypeArguments();
				if (typeArgs.length > 0) {
					return new FieldCollectionTypeDescriptor(typeArgs[0], exact);
				}

				return new FieldCollectionTypeDescriptor(ast.resolveWellKnownType(Object.class.getName()), exact);
			}

			exact = false;

			ITypeBinding superType = type.getSuperclass();
			if (superType != null && !superType.isEqualTo(ast.resolveWellKnownType(Object.class.getName()))) {
				types.add(superType);
			}

			types.addAll(Arrays.asList(type.getInterfaces()));
		} while ((type = types.poll()) != null);

		return new FieldCollectionTypeDescriptor(null, false);
	}

	private boolean isValidFieldForService(ITypeBinding fieldType, ITypeBinding serviceType) {
		String erasure = fieldType.getErasure().getBinaryName();
		ITypeBinding[] typeArgs;
		return ((ServiceReference.class.getName().equals(erasure) || COMPONENT_SERVICE_OBJECTS.equals(erasure))
				&& ((typeArgs = fieldType.getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
				|| Map.class.getName().equals(erasure)
				|| (Map.Entry.class.getName().equals(erasure)
						&& ((typeArgs = fieldType.getTypeArguments()).length < 2 || (Map.class.getName().equals(typeArgs[0].getErasure().getBinaryName()) && serviceType.isAssignmentCompatible(typeArgs[1]))))
				|| serviceType.isAssignmentCompatible(fieldType);
	}

	private ITypeBinding getFieldServiceType(AST ast, ITypeBinding type) {
		ITypeBinding serviceType;
		String erasure = type.getErasure().getBinaryName();
		if (ServiceReference.class.getName().equals(erasure) || COMPONENT_SERVICE_OBJECTS.equals(erasure)) {
			ITypeBinding[] typeArgs = type.getTypeArguments();
			if (typeArgs.length > 0) {
				serviceType = typeArgs[0];
			} else {
				serviceType = null;
			}
		} else if (Map.Entry.class.getName().equals(erasure)) {
			ITypeBinding[] typeArgs = type.getTypeArguments();
			if (typeArgs.length >= 2 && Map.class.getName().equals(typeArgs[0].getErasure().getBinaryName())) {
				serviceType = typeArgs[1];
			} else {
				serviceType = null;
			}
		} else if (Map.class.getName().equals(erasure)) {
			serviceType = null;
		} else {
			serviceType = type.isPrimitive() ? getObjectType(ast, type) : type;
		}

		return serviceType;
	}

	private ITypeBinding getObjectType(AST ast, ITypeBinding primitive) {
		if (Boolean.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Boolean.class.getName());
		}

		if (Byte.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Byte.class.getName());
		}

		if (Character.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Character.class.getName());
		}

		if (Double.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Double.class.getName());
		}

		if (Float.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Float.class.getName());
		}

		if (Integer.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Integer.class.getName());
		}

		if (Long.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Long.class.getName());
		}

		if (Short.TYPE.getName().equals(primitive.getName())) {
			return ast.resolveWellKnownType(Short.class.getName());
		}

		return null;
	}

	private String getFieldCollectionType(FieldCollectionTypeDescriptor collectionType) {
		String fieldCollectionType = null;

		String erasure = collectionType.getElementType().getErasure().getBinaryName();
		if (ServiceReference.class.getName().equals(erasure)) {
			fieldCollectionType = "reference"; //$NON-NLS-1$
		} else if (COMPONENT_SERVICE_OBJECTS.equals(erasure)) {
			fieldCollectionType = "serviceobjects"; //$NON-NLS-1$
		} else if (Map.class.getName().equals(erasure)) {
			fieldCollectionType = "properties"; //$NON-NLS-1$
		} else if (Map.Entry.class.equals(erasure)) {
			fieldCollectionType = "tuple"; //$NON-NLS-1$
		}

		return fieldCollectionType;
	}

	private void processReferenceMethodParams(IDSReference reference, ITypeBinding typeBinding, Annotation annotation, Map<String, ?> params, ITypeBinding serviceType) {
		String bind = null;
		Object value;
		if ((value = params.get("bind")) instanceof String) { //$NON-NLS-1$
			bind = (String) value;
			if (!errorLevel.isIgnore()) {
				IMethodBinding bindMethod = findReferenceMethod(typeBinding, serviceType, bind, true);
				if (bindMethod == null) {
					reportProblem(annotation, "bind", NLS.bind(Messages.AnnotationProcessor_invalidReference_bindMethodNotFound, bind), bind); //$NON-NLS-1$
				}
			}
		}

		String unbind = null;
		if ((value = params.get("unbind")) instanceof String) { //$NON-NLS-1$
			unbind = (String) value;
			if (!errorLevel.isIgnore()) {
				IMethodBinding unbindMethod = findReferenceMethod(typeBinding, serviceType, unbind, true);
				if (unbindMethod == null) {
					reportProblem(annotation, "unbind", NLS.bind(Messages.AnnotationProcessor_invalidReference_unbindMethod, unbind), unbind); //$NON-NLS-1$
				}
			}
		}

		String updated = null;
		if ((value = params.get("updated")) instanceof String) { //$NON-NLS-1$
			updated = (String) value;
			if (!errorLevel.isIgnore()) {
				IMethodBinding updatedMethod = findReferenceMethod(typeBinding, serviceType, updated, true);
				if (updatedMethod == null) {
					reportProblem(annotation, "updated", NLS.bind(Messages.AnnotationProcessor_invalidReference_updatedMethod, updated), updated); //$NON-NLS-1$
				}
			}
		}

		updateMethodAttributes(reference, bind, updated, unbind);
	}

	private void validateReferenceField(Annotation annotation, IVariableBinding fieldBinding) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_staticField);
		}
	}

	private void processReference(IDSReference reference, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
		ITypeBinding serviceType;
		Object value;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
		} else {
			// service must be explicitly specified; default to Object
			serviceType = annotation.getAST().resolveWellKnownType(Object.class.getName());

			if (!errorLevel.isIgnore()) {
				reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_missingRequiredParam, "service")); //$NON-NLS-1$
			}
		}

		String service = serviceType == null ? null : serviceType.getBinaryName();

		String name = null;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			name = (String) value;
			validateReferenceName(name, annotation, names);
		} else {
			if (!errorLevel.isIgnore()) {
				reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_missingRequiredParam, "name")); //$NON-NLS-1$
			}
		}

		String cardinality = null;
		if ((value = params.get("cardinality")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding cardinalityBinding = (IVariableBinding) value;
			cardinality = DSEnums.getReferenceCardinality(cardinalityBinding.getName());
		}

		String policy = null;
		if ((value = params.get("policy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyBinding = (IVariableBinding) value;
			policy = DSEnums.getReferencePolicy(policyBinding.getName());
		}

		String target = null;
		if ((value = params.get("target")) instanceof String) { //$NON-NLS-1$
			target = (String) value;
			validateReferenceTarget(annotation, target);
		}

		String policyOption = null;
		if ((value = params.get("policyOption")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding policyOptionBinding = (IVariableBinding) value;
			policyOption = DSEnums.getReferencePolicyOption(policyOptionBinding.getName());
		}

		String referenceScope = null;
		if ((value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding referenceScopeBinding = (IVariableBinding) value;
			referenceScope = DSEnums.getReferenceScope(referenceScopeBinding.getName());
		}

		processReferenceMethodParams(reference, typeBinding, annotation, params, serviceType);
		processReferenceFieldParams(reference, typeBinding, annotation, params, serviceType, cardinality, policy);

		updateAttributes(reference, name, service, cardinality, policy, target, policyOption, referenceScope);
	}

	private void validateReferenceBindMethod(Annotation annotation, ITypeBinding serviceType, IMethodBinding methodBinding) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (Modifier.isStatic(methodBinding.getModifiers())) {
			reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_staticBindMethod);
		}

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName)) {
			reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodReturnType, returnTypeName), returnTypeName);
		}

		ITypeBinding[] paramTypeBindings = methodBinding.getParameterTypes();
		if (specVersion == DSAnnotationVersion.V1_3) {
			if (paramTypeBindings.length == 0) {
				reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_bindMethodNoArgs, serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()));
			} else if (serviceType != null) {
				for (ITypeBinding paramTypeBinding : paramTypeBindings) {
					String erasure = paramTypeBinding.getErasure().getBinaryName();
					if (!ServiceReference.class.getName().equals(erasure)
							&& !COMPONENT_SERVICE_OBJECTS.equals(erasure)
							&& !(serviceType == null || serviceType.isAssignmentCompatible(paramTypeBinding))
							&& !Map.class.getName().equals(erasure)) {
						reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodArg, paramTypeBinding.getName(), serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()), paramTypeBinding.getName());
					}
				}
			}
		} else {
			if (!(paramTypeBindings.length == 1
					&& (ServiceReference.class.getName().equals(paramTypeBindings[0].getErasure().getBinaryName())
							|| serviceType == null
							|| serviceType.isAssignmentCompatible(paramTypeBindings[0])))
					&& !(paramTypeBindings.length == 2
					&& (serviceType == null || serviceType.isAssignmentCompatible(paramTypeBindings[0]))
					&& Map.class.getName().equals(paramTypeBindings[1].getErasure().getBinaryName()))) {
				String[] params = new String[paramTypeBindings.length];
				StringBuilder buf = new StringBuilder(64);
				buf.append('(');
				for (int i = 0; i < params.length; ++i) {
					params[i] = paramTypeBindings[i].getName();
					if (buf.length() > 1) {
						buf.append(", "); //$NON-NLS-1$
					}

					buf.append(params[i]);
				}

				buf.append(')');
				reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodParameters, buf, serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()), params);
			}
		}
	}

	private void validateReferenceTarget(Annotation annotation, String target) {
		if (errorLevel.isIgnore()) {
			return;
		}

		try {
			FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			String msg = e.getMessage();
			String suffix = ": " + e.getFilter(); //$NON-NLS-1$
			if (msg.endsWith(suffix)) {
				msg = msg.substring(0, msg.length() - suffix.length());
			}

			reportProblem(annotation, "target", msg, target); //$NON-NLS-1$
		}
	}

	private IMethodBinding findReferenceMethod(ITypeBinding componentClass, ITypeBinding serviceType, String name, boolean recurse) {
		ITypeBinding testedClass = componentClass;

		IMethodBinding candidate = null;
		int priority = 0;
		do {
			for (IMethodBinding declaredMethod : testedClass.getDeclaredMethods()) {
				if (name.equals(declaredMethod.getName())
						&& !Modifier.isStatic(declaredMethod.getModifiers())
						&& Void.TYPE.getName().equals(declaredMethod.getReturnType().getName())
						&& (testedClass == componentClass
						|| Modifier.isPublic(declaredMethod.getModifiers())
						|| Modifier.isProtected(declaredMethod.getModifiers())
						|| (!Modifier.isPrivate(declaredMethod.getModifiers())
								&& testedClass.getPackage().isEqualTo(componentClass.getPackage())))) {
					ITypeBinding[] paramTypes = declaredMethod.getParameterTypes();

					if (specVersion == DSAnnotationVersion.V1_3) {
						for (int i = 0; i < paramTypes.length; ++i) {
							ITypeBinding paramType = paramTypes[i];
							int priorityOffset = i == 0 ? 10 : 0;
							String erasure = paramType.getErasure().getBinaryName();
							if (ServiceReference.class.getName().equals(erasure)) {
								if (paramTypes.length == 1) {
									// we have the winner
									return declaredMethod;
								}

								if (priority < 5) {
									priority = 5;
								}
							} else if (priority < priorityOffset + 4 && COMPONENT_SERVICE_OBJECTS.equals(erasure)) {
								priority = priorityOffset + 4;
							} else if (priority < priorityOffset + 3 && serviceType != null && serviceType.isEqualTo(paramType)) {
								priority = priorityOffset + 3;
							} else if (priority < priorityOffset + 2 && serviceType != null && serviceType.isAssignmentCompatible(paramType)) {
								priority = priorityOffset + 2;
							} else if (priority < priorityOffset + 1 && Map.class.getName().equals(erasure)) {
								priority = priorityOffset + 1;
							} else {
								continue;
							}

							candidate = declaredMethod;
						}
					} else {
						if (paramTypes.length == 1) {
							String erasure = paramTypes[0].getErasure().getBinaryName();
							if (ServiceReference.class.getName().equals(erasure)) {
								// we have the winner
								return declaredMethod;
							}

							if (priority < 3 && serviceType != null && serviceType.isEqualTo(paramTypes[0])) {
								priority = 3;
							} else if (priority < 2 && serviceType != null && serviceType.isAssignmentCompatible(paramTypes[0])) {
								priority = 2;
							} else {
								continue;
							}

							// we have a (better) candidate
							candidate = declaredMethod;
						} else if (paramTypes.length == 2) {
							if (priority < 1
									&& serviceType != null && serviceType.isEqualTo(paramTypes[0])
									&& Map.class.getName().equals(paramTypes[1].getErasure().getBinaryName())) {
								priority = 1;
							} else if (candidate != null
									|| !(serviceType != null && serviceType.isAssignmentCompatible(paramTypes[0]))
									|| !Map.class.getName().equals(paramTypes[1].getErasure().getBinaryName())) {
								continue;
							}

							// we have a candidate
							candidate = declaredMethod;
						}
					}
				}
			}
		} while (recurse && (testedClass = testedClass.getSuperclass()) != null);

		return candidate;
	}

	private IVariableBinding findReferenceField(String name, ITypeBinding componentClass) {
		ITypeBinding testedClass = componentClass;

		do {
			for (IVariableBinding declaredField : testedClass.getDeclaredFields()) {
				if (name.equals(declaredField.getName())
						&& !Modifier.isStatic(declaredField.getModifiers())
						&& (testedClass == componentClass
						|| Modifier.isPublic(declaredField.getModifiers())
						|| Modifier.isProtected(declaredField.getModifiers())
						|| (!Modifier.isPrivate(declaredField.getModifiers())
								&& testedClass.getPackage().isEqualTo(componentClass.getPackage())))) {
					return declaredField;
				}
			}
		} while ((testedClass = testedClass.getSuperclass()) != null);

		return null;
	}

	private void reportProblem(Annotation annotation, String member, String message, String... args) {
		reportProblem(annotation, member, -1, message, args);
	}

	private void reportProblem(Annotation annotation, String member, ValidationErrorLevel errorLevel, String message, String... args) {
		reportProblem(annotation, member, -1, errorLevel, message, args);
	}

	private void reportProblem(Annotation annotation, String member, int valueIndex, String message, String... args) {
		reportProblem(annotation, member, valueIndex, errorLevel, message, args);
	}

	private void reportProblem(Annotation annotation, String member, int valueIndex, ValidationErrorLevel errorLevel, String message, String... args) {
		reportProblem(annotation, member, valueIndex, false, errorLevel, message, args);
	}

	private void reportProblem(Annotation annotation, String member, int valueIndex, boolean fullPair, ValidationErrorLevel errorLevel, String message, String... args) {
		if (errorLevel.isIgnore()) {
			return;
		}

		ASTNode element = annotation;
		if (annotation.isNormalAnnotation() && member != null) {
			NormalAnnotation na = (NormalAnnotation) annotation;
			for (Object value : na.values()) {
				MemberValuePair pair = (MemberValuePair) value;
				if (member.equals(pair.getName().getIdentifier())) {
					element = fullPair ? pair : pair.getValue();
					break;
				}
			}
		} else if (annotation.isSingleMemberAnnotation()) {
			SingleMemberAnnotation sma = (SingleMemberAnnotation) annotation;
			element = sma.getValue();
		}

		int start = element.getStartPosition();
		int length = element.getLength();

		if (valueIndex >= 0 && element instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) element;
			if (valueIndex < ai.expressions().size()) {
				Expression expression = (Expression) ai.expressions().get(valueIndex);
				start = expression.getStartPosition();
				length = expression.getLength();
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