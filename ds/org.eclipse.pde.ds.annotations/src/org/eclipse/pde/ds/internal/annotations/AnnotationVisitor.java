/*******************************************************************************
 * Copyright (c) 2017, 2018 Ecliptical Software Inc. and others.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
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

@SuppressWarnings("restriction")
public class AnnotationVisitor extends ASTVisitor {

	private static final String COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext"; //$NON-NLS-1$

	private static final String COMPONENT_ANNOTATION = DSAnnotationCompilationParticipant.COMPONENT_ANNOTATION;

	private static final String ACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Activate"; //$NON-NLS-1$

	private static final String MODIFIED_ANNOTATION = "org.osgi.service.component.annotations.Modified"; //$NON-NLS-1$

	private static final String DEACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Deactivate"; //$NON-NLS-1$

	private static final String REFERENCE_ANNOTATION = "org.osgi.service.component.annotations.Reference"; //$NON-NLS-1$

	private static final String DESIGNATE_ANNOTATION = "org.osgi.service.metatype.annotations.Designate"; //$NON-NLS-1$

	private static final Pattern PID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*"); //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_CONFIGURATION_PID = "configuration-pid"; //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_REFERENCE = "reference"; //$NON-NLS-1$

	private static final String ATTRIBUTE_SERVICE_SCOPE = "scope"; //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_DEFAULT = DSEnums.getServiceScope("DEFAULT"); //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_SINGLETON = DSEnums.getServiceScope("SINGLETON"); //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_BUNDLE = DSEnums.getServiceScope("BUNDLE"); //$NON-NLS-1$

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

	private static final Comparator<IDSReference> REF_NAME_COMPARATOR = (o1, o2) -> o1.getReferenceName()
			.compareTo(o2.getReferenceName());

	private static final Debug debug = AnnotationProcessor.debug;

	private final AnnotationProcessor processor;

	private final ProjectState state;

	private final DSAnnotationVersion specVersion;

	private final ValidationErrorLevel errorLevel;

	private final Map<String, String> dsKeys;

	private final ProblemReporter problemReporter;

	public AnnotationVisitor(AnnotationProcessor processor, ProjectState state, Map<String, String> dsKeys, Set<DSAnnotationProblem> problems) {
		this.processor = processor;
		this.state = state;
		this.specVersion = state.getSpecVersion();
		this.errorLevel = state.getErrorLevel();
		this.dsKeys = dsKeys;
		problemReporter = new ProblemReporter(state.getErrorLevel(), problems);
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
				problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notPublic, type.getName().getIdentifier()), type.getName().getIdentifier());
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
				if (!errorLevel.isIgnore()) {
					if (isInterface) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_interface, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isAbstract) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_abstract, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isNested) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notTopLevel, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (noDefaultConstructor) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_noDefaultConstructor, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidComponentImplementationClass, type.getName().getIdentifier()), type.getName().getIdentifier());
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
			problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_enumeration, node.getName().getIdentifier()), node.getName().getIdentifier());
		}

		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		Annotation annotation = findComponentAnnotation(node);
		if (annotation != null) {
			problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_annotation, node.getName().getIdentifier()), node.getName().getIdentifier());
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
		dsModel.setCharset(StandardCharsets.UTF_8); // $NON-NLS-1$
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
					bufferManager.execute(() -> {
						try {
							performEdit(doc, edit);
						} catch (CoreException e) {
							ex[0] = e;
						}

						latch.countDown();
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
				if (services.add(serviceName)) {
					if (serviceDuplicates != null) {
						serviceDuplicates.put(serviceName, i);
					}
				} else {
					if (serviceDuplicates != null) {
						problemReporter.reportProblem(annotation, "service", i, Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
						Integer pos = serviceDuplicates.put(serviceName, null);
						if (pos != null) {
							problemReporter.reportProblem(annotation, "service", pos.intValue(), Messages.AnnotationProcessor_duplicateServiceDeclaration, serviceName); //$NON-NLS-1$
						}
					}
				}

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
			if (!errorLevel.isIgnore() && Boolean.TRUE.equals(serviceFactory) && services.isEmpty()) {
				problemReporter.reportProblem(annotation, "servicefactory", Messages.AnnotationVisitor_invalidServiceFactory_noServices); //$NON-NLS-1$
			}
		}

		Boolean enabled = null;
		if ((value = params.get("enabled")) instanceof Boolean) { //$NON-NLS-1$
			enabled = (Boolean) value;
		}

		Boolean immediate = null;
		if ((value = params.get("immediate")) instanceof Boolean) { //$NON-NLS-1$
			immediate = (Boolean) value;
			if (!errorLevel.isIgnore()) {
				if (factory != null && Boolean.TRUE.equals(immediate)) {
					problemReporter.reportProblem(annotation, "immediate", Messages.AnnotationVisitor_invalidFactoryComponent_immediate); //$NON-NLS-1$
				}

				if (services.isEmpty() && Boolean.FALSE.equals(immediate)) {
					problemReporter.reportProblem(annotation, "immediate", Messages.AnnotationVisitor_invalidDelayedComponent_noServices); //$NON-NLS-1$
				}
			}
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
		} else if (specVersion == DSAnnotationVersion.V1_3) {
			for (IAnnotationBinding typeAnnotation : typeBinding.getAnnotations()) {
				if (!DESIGNATE_ANNOTATION.equals(typeAnnotation.getAnnotationType().getQualifiedName())) {
					continue;
				}

				for (IMemberValuePairBinding memberValuePair : typeAnnotation.getDeclaredMemberValuePairs()) {
					if (!"factory".equals(memberValuePair.getName())) { //$NON-NLS-1$
						continue;
					}

					if (Boolean.TRUE.equals(memberValuePair.getValue())) {
						configPolicy = IDSConstants.VALUE_CONFIGURATION_POLICY_REQUIRE;
					}

					break;
				}

				break;
			}
		}

		DSAnnotationVersion requiredVersion = DSAnnotationVersion.V1_1;

		String configPid = null;
		if ((value = params.get("configurationPid")) instanceof String) { //$NON-NLS-1$
			configPid = (String) value;
			validateComponentConfigPID(annotation, configPid, -1);
			requiredVersion = DSAnnotationVersion.V1_2;
		} else if (specVersion == DSAnnotationVersion.V1_3 && value instanceof Object[]) {
			Object[] configPidElems = (Object[]) value;
			if (configPidElems.length > 0) {
				LinkedHashSet<String> configPids = new LinkedHashSet<>(configPidElems.length);
				HashMap<String, Integer> pidDuplicates = errorLevel.isIgnore() ? null : new HashMap<>(configPidElems.length);
				int i = 0;
				for (Object configPidElem : configPidElems) {
					String configPidStr = String.valueOf(configPidElem);
					if ("$".equals(configPidStr)) { //$NON-NLS-1$
						configPidStr = name;
					} else {
						validateComponentConfigPID(annotation, configPidStr, i);
					}

					if (configPids.add(configPidStr)) {
						if (pidDuplicates != null) {
							pidDuplicates.put(configPidStr, i);
						}
					} else {
						if (pidDuplicates != null) {
							problemReporter.reportProblem(annotation, "configurationPid", i, Messages.AnnotationVisitor_invalidComponentConfigurationPid_duplicate); //$NON-NLS-1$
							Integer pos = pidDuplicates.put(configPidStr, null);
							if (pos != null) {
								problemReporter.reportProblem(annotation, "configurationPid", pos.intValue(), Messages.AnnotationVisitor_invalidComponentConfigurationPid_duplicate); //$NON-NLS-1$
							}
						}
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
		}

		String serviceScope = null;
		if (specVersion == DSAnnotationVersion.V1_3 && (value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding serviceScopeBinding = (IVariableBinding) value;
			serviceScope = DSEnums.getServiceScope(serviceScopeBinding.getName());
			if (!errorLevel.isIgnore()) {
				if (services.isEmpty()) {
					problemReporter.reportProblem(annotation, "scope", Messages.AnnotationVisitor_invalidScope_noServices); //$NON-NLS-1$
				} else if ((factory != null || Boolean.TRUE.equals(immediate)) && !serviceScope.equals(VALUE_SERVICE_SCOPE_SINGLETON)) {
					problemReporter.reportProblem(annotation, "scope", Messages.AnnotationVisitor_invalidScope_factoryImmediate); //$NON-NLS-1$
				}
			}
		}

		if (specVersion == DSAnnotationVersion.V1_3 && serviceFactory != null && serviceScope != null && !serviceScope.equals(VALUE_SERVICE_SCOPE_DEFAULT)) {
			// ignore servicefactory if scope specified and not <<DEFAULT>>
			if (!errorLevel.isIgnore() && !serviceFactory.equals(VALUE_SERVICE_SCOPE_BUNDLE.equals(serviceScope))) {
				problemReporter.reportProblem(annotation, "servicefactory", -1, true, errorLevel, Messages.AnnotationVisitor_invalidServiceFactory_ignored); //$NON-NLS-1$
			}

			serviceFactory = null;
		}

		if (!errorLevel.isIgnore() && serviceFactory != null && !serviceFactory.equals(Boolean.FALSE) && !services.isEmpty()) {
			if (factory != null || Boolean.TRUE.equals(immediate)) {
				problemReporter.reportProblem(annotation, "servicefactory", Messages.AnnotationVisitor_invalidServiceFactory_factoryImmediate); //$NON-NLS-1$
			}
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
				String referenceBind = refElement.getXMLAttributeValue(ReferenceProcessor.ATTRIBUTE_REFERENCE_FIELD);
				if (referenceBind != null) {
					referenceName = ReferenceProcessor.getReferenceName(referenceBind);
				}

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

					ReferenceProcessor referenceProcessor = new ReferenceProcessor(this, specVersion, requiredVersion, errorLevel, state.getMissingUnbindMethodLevel(), problemReporter);
					requiredVersion = requiredVersion.max(referenceProcessor.processReference(reference, typeBinding, referenceAnnotation, referenceAnnotationBinding, annotationParams, referenceNames));
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

						ReferenceProcessor referenceProcessor = new ReferenceProcessor(this, specVersion, requiredVersion, errorLevel, state.getMissingUnbindMethodLevel(), problemReporter);
						referenceProcessor.processReference(reference, field, fieldBinding, fieldAnnotation, fieldAnnotationBinding, annotationParams, referenceNames);
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
						problemReporter.reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateActivateMethod, method.getName().getIdentifier());
						if (activateAnnotation != null) {
							problemReporter.reportProblem(activateAnnotation, null, Messages.AnnotationProcessor_duplicateActivateMethod, activate);
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
						problemReporter.reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateDeactivateMethod, method.getName().getIdentifier());
						if (deactivateAnnotation != null) {
							problemReporter.reportProblem(deactivateAnnotation, null, Messages.AnnotationProcessor_duplicateDeactivateMethod, deactivate);
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
						problemReporter.reportProblem(methodAnnotation, null, Messages.AnnotationProcessor_duplicateModifiedMethod, method.getName().getIdentifier());
						if (modifiedAnnotation != null) {
							problemReporter.reportProblem(modifiedAnnotation, null, Messages.AnnotationProcessor_duplicateModifiedMethod, modified);
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

						ReferenceProcessor referenceProcessor = new ReferenceProcessor(this, specVersion, requiredVersion, errorLevel, state.getMissingUnbindMethodLevel(), problemReporter);
						String referenceName = referenceProcessor.getReferenceName(methodBinding.getName(), annotationParams);

						IDSReference reference = refMap.remove(referenceName);
						if (reference == null) {
							reference = createReference(dsFactory);
						}

						references.add(reference);

						requiredVersion = requiredVersion.max(referenceProcessor.processReference(reference, method, methodBinding, methodAnnotation, methodAnnotationBinding, annotationParams, referenceNames));
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

		LinkedHashMap<String, IDSProperty> newPropMap = new LinkedHashMap<>();

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
				collectProperties(activateMethod, dsFactory, newPropMap, cptClosure);
			}

			if (modifiedMethod != null) {
				collectProperties(modifiedMethod, dsFactory, newPropMap, cptClosure);
			}

			if (deactivateMethod != null) {
				collectProperties(deactivateMethod, dsFactory, newPropMap, cptClosure);
			}

			if (!cptClosure.isEmpty()) {
				requiredVersion = DSAnnotationVersion.V1_3;
			}
		}

		IDSProperty[] propElements = component.getPropertyElements();
		if (newPropMap.isEmpty() && properties.length == 0) {
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
						removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
					}

					if (!errorLevel.isIgnore()) {
						String expected = property.getPropertyType() == null || property.getPropertyType().length() == 0 || IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(property.getPropertyType()) ? Messages.AnnotationProcessor_stringOrEmpty : property.getPropertyType();
						String actual = propertyType == null || IDSConstants.VALUE_PROPERTY_TYPE_STRING.equals(propertyType) ? Messages.AnnotationProcessor_stringOrEmpty : propertyType;
						if (!actual.equals(expected)) {
							problemReporter.reportProblem(annotation, "property", i, NLS.bind(Messages.AnnotationProcessor_inconsistentComponentPropertyType, actual, expected), actual); //$NON-NLS-1$
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
			HashMap<String, IDSProperty> propMap = new HashMap<>(propElements.length);
			for (IDSProperty propElement : propElements) {
				propMap.put(propElement.getPropertyName(), propElement);
			}

			newPropMap.keySet().removeAll(map.keySet()); // force re-insert (append)
			newPropMap.putAll(map);

			ArrayList<IDSProperty> propList = new ArrayList<>(newPropMap.values());
			for (ListIterator<IDSProperty> i = propList.listIterator(); i.hasNext();) {
				IDSProperty newProperty = i.next();
				IDSProperty property = propMap.remove(newProperty.getPropertyName());
				if (property == null) {
					continue;
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

	void removeAttribute(IDSObject obj, String name, String defaultValue) {
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
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			// Serialize
			try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
				out.writeObject(node);
				out.flush();
			}
			byte[] bytes = bout.toByteArray();
			// Deserialize
			try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
					ObjectInputStream in = new ObjectInputStream(bin)) {
				clone = (IDocumentElementNode) in.readObject();
			}
			// Reconnect
			clone.reconnect(obj, obj.getSharedModel());
		} catch (IOException e) {
			if (debug.isDebugging()) {
				debug.trace("Error cloning element.", e); //$NON-NLS-1$
			}
		} catch (ClassNotFoundException e) {
			if (debug.isDebugging()) {
				debug.trace("Error cloning element.", e); //$NON-NLS-1$
			}
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
		try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
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
			if (debug.isDebugging()) {
				debug.trace("Error reading property element body.", e); //$NON-NLS-1$
			}
		}

		return buf.toString();
	}

	private void collectProperties(IMethodBinding method, IDSDocumentFactory factory, Map<String, IDSProperty> properties, Collection<ITypeBinding> visited) {
		for (ITypeBinding paramTypeBinding : method.getParameterTypes()) {
			if (!paramTypeBinding.isAnnotation() || !visited.add(paramTypeBinding)) {
				continue;
			}

			for (IMethodBinding methodBinding : paramTypeBinding.getDeclaredMethods()) {
				if (!methodBinding.isAnnotationMember()) {
					continue;
				}

				Object value = methodBinding.getDefaultValue();
				if (value == null) {
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

					removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
					property.setPropertyElemBody(body.toString());
				} else {
					property.setPropertyValue(getPropertyValue(value));
				}

				properties.remove(property.getName()); // force re-insert (append)
				properties.put(property.getName(), property);
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
			problemReporter.reportProblem(annotation, "name", NLS.bind(Messages.AnnotationProcessor_invalidComponentName, name), name); //$NON-NLS-1$
		}
	}

	private void validateComponentService(Annotation annotation, ITypeBinding componentType, ITypeBinding serviceType, int index) {
		if (!errorLevel.isIgnore() && !componentType.isAssignmentCompatible(serviceType)) {
			problemReporter.reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidComponentService, serviceType.getName()), serviceType.getName()); //$NON-NLS-1$
		}
	}

	private void validateComponentFactory(Annotation annotation, String factory) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(factory).matches()) {
			problemReporter.reportProblem(annotation, "factory", NLS.bind(Messages.AnnotationProcessor_invalidComponentFactoryName, factory), factory); //$NON-NLS-1$
		}
	}

	private void validateComponentProperty(Annotation annotation, String name, String type, String value, int index) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (PROPERTY_TYPES.contains(type)) {
			if (name == null || name.trim().length() == 0) {
				problemReporter.reportProblem(annotation, "property", index, Messages.AnnotationProcessor_invalidComponentProperty_nameRequired, name); //$NON-NLS-1$
			}

			if (value == null) {
				problemReporter.reportProblem(annotation, "property", index, Messages.AnnotationProcessor_invalidComponentProperty_valueRequired, name); //$NON-NLS-1$
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
					problemReporter.reportProblem(annotation, "property", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyValue, type, value), String.valueOf(value)); //$NON-NLS-1$
				}
			}
		} else {
			problemReporter.reportProblem(annotation, "property", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyType, type), String.valueOf(type)); //$NON-NLS-1$
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
				problemReporter.reportProblem(annotation, "properties", i, NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyFile, file), file); //$NON-NLS-1$
			}
		}
	}

	private void validateComponentXMLNS(Annotation annotation, String xmlns, DSAnnotationVersion requiredVersion) {
		if (errorLevel.isIgnore()) {
			return;
		}

		DSAnnotationVersion specifiedVersion = DSAnnotationVersion.fromNamespace(xmlns);
		if (specifiedVersion == null || requiredVersion.compareTo(specifiedVersion) > 0) {
			problemReporter.reportProblem(annotation, "xmlns", NLS.bind(Messages.AnnotationProcessor_invalidComponentDescriptorNamespace, xmlns), xmlns); //$NON-NLS-1$
		}
	}

	private void validateComponentConfigPID(Annotation annotation, String configPid, int index) {
		if (!errorLevel.isIgnore() && !PID_PATTERN.matcher(configPid).matches()) {
			problemReporter.reportProblem(annotation, "configurationPid", index, NLS.bind(Messages.AnnotationProcessor_invalidComponentConfigurationPid, configPid), configPid); //$NON-NLS-1$
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
			problemReporter.reportProblem(annotation, methodName, Messages.AnnotationProcessor_invalidLifecycleMethod_static);
		}

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName)) {
			problemReporter.reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodReturnType, methodName, returnTypeName), returnTypeName);
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
				problemReporter.reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_invalidLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
			}

			if (isDuplicate) {
				problemReporter.reportProblem(annotation, methodName, NLS.bind(Messages.AnnotationProcessor_duplicateLifeCycleMethodParameterType, methodName, paramTypeName), paramTypeName);
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
}