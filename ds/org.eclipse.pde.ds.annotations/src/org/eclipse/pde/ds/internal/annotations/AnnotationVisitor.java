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

import static java.util.Map.entry;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
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
import org.eclipse.pde.internal.ds.core.IDSBundleProperties;
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
import org.eclipse.pde.internal.ds.core.IDSSingleProperty;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.BundleContext;

@SuppressWarnings("restriction")
public class AnnotationVisitor extends ASTVisitor {

	private static final String MAP_TYPE = Map.class.getName();

	private static final String BUNDLE_CONTEXT = BundleContext.class.getName();

	private static final String DEFAULT_ACTIVATE_METHOD_NAME = "activate";

	private static final String COMPONENT_CONTEXT = "org.osgi.service.component.ComponentContext"; //$NON-NLS-1$

	private static final String COMPONENT_ANNOTATION = DSAnnotationCompilationParticipant.COMPONENT_ANNOTATION;

	private static final String ACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Activate"; //$NON-NLS-1$

	private static final String MODIFIED_ANNOTATION = "org.osgi.service.component.annotations.Modified"; //$NON-NLS-1$

	private static final String DEACTIVATE_ANNOTATION = "org.osgi.service.component.annotations.Deactivate"; //$NON-NLS-1$

	private static final String REFERENCE_ANNOTATION = "org.osgi.service.component.annotations.Reference"; //$NON-NLS-1$

	private static final String COMPONENT_PROPERTY_TYPE_ANNOTATION = "org.osgi.service.component.annotations.ComponentPropertyType";

	private static final String DESIGNATE_ANNOTATION = "org.osgi.service.metatype.annotations.Designate"; //$NON-NLS-1$

	private static final Pattern PID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*"); //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_CONFIGURATION_PID = "configuration-pid"; //$NON-NLS-1$

	private static final String ATTRIBUTE_COMPONENT_REFERENCE = "reference"; //$NON-NLS-1$

	private static final String ATTRIBUTE_SERVICE_SCOPE = "scope"; //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_DEFAULT = DSEnums.getServiceScope("DEFAULT"); //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_SINGLETON = DSEnums.getServiceScope("SINGLETON"); //$NON-NLS-1$

	private static final String VALUE_SERVICE_SCOPE_BUNDLE = DSEnums.getServiceScope("BUNDLE"); //$NON-NLS-1$

	private static final Set<String> PROPERTY_TYPES = Set.of( //
			IDSConstants.VALUE_PROPERTY_TYPE_STRING, //
			IDSConstants.VALUE_PROPERTY_TYPE_LONG, //
			IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE, //
			IDSConstants.VALUE_PROPERTY_TYPE_FLOAT, //
			IDSConstants.VALUE_PROPERTY_TYPE_INTEGER, //
			IDSConstants.VALUE_PROPERTY_TYPE_BYTE, //
			IDSConstants.VALUE_PROPERTY_TYPE_CHAR, //
			IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN, //
			IDSConstants.VALUE_PROPERTY_TYPE_SHORT);

	private static final Map<String, String> PRIMITIVE_TYPE_MAP = Map.ofEntries( //
			entry(Long.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_LONG), //
			entry(Double.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE), //
			entry(Float.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_FLOAT), //
			entry(Integer.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_INTEGER), //
			entry(Byte.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BYTE), //
			entry(Character.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_CHAR), //
			entry(Boolean.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN), //
			entry(Short.class.getName(), IDSConstants.VALUE_PROPERTY_TYPE_SHORT), //
			entry(Long.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_LONG), //
			entry(Double.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE), //
			entry(Float.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_FLOAT), //
			entry(Integer.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_INTEGER), //
			entry(Byte.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BYTE), //
			entry(Character.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_CHAR), //
			entry(Boolean.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN), //
			entry(Short.TYPE.getName(), IDSConstants.VALUE_PROPERTY_TYPE_SHORT));

	private static final Comparator<IDSReference> REF_NAME_COMPARATOR = Comparator
			.comparing(IDSReference::getReferenceName);

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
			boolean hasInjectableConstructor = false;
			if ((isInterface = type.isInterface())
					|| (isAbstract = Modifier.isAbstract(type.getModifiers()))
					|| (isNested = (!type.isPackageMemberTypeDeclaration() && !isNestedPublicStatic(type)))
					|| (noDefaultConstructor = !(hasDefaultConstructor(type)
							|| (hasInjectableConstructor = hasInjectableConstructor(type, problemReporter))))) {
				// interfaces, abstract types, non-static/non-public nested types, or types with no default constructor cannot be components
				if (!errorLevel.isIgnore()) {
					if (isInterface) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_interface, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isAbstract) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_abstract, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (isNested) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_notTopLevel, type.getName().getIdentifier()), type.getName().getIdentifier());
					} else if (noDefaultConstructor) {
						if (specVersion.isEqualOrHigherThan(DSAnnotationVersion.V1_4)) {
							problemReporter.reportProblem(annotation, null,
									NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_compatibleConstructor,
											type.getName().getIdentifier()),
									type.getName().getIdentifier());
						} else {
							if (hasInjectableConstructor) {
								// TODO we should add an error marker that offers a quickfix to upgrade the spec
								// version to 1.4
							}
							problemReporter.reportProblem(annotation, null,
									NLS.bind(Messages.AnnotationProcessor_invalidCompImplClass_noDefaultConstructor,
											type.getName().getIdentifier()),
									type.getName().getIdentifier());
						}
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
			if (!(item instanceof Annotation annotation)) {
				continue;
			}

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
		IPath path = IPath.fromOSString(state.getPath()).append(name).addFileExtension("xml"); //$NON-NLS-1$

		String dsKey = path.toPortableString();
		dsKeys.put(implClass, dsKey);

		IProject project = typeBinding.getJavaElement().getJavaProject().getProject();
		IFile file = PDEProject.getBundleRelativeFile(project, path);
		IPath filePath = file.getFullPath();

		processor.verifyOutputLocation(file);

		// handle file move/rename
		String oldPath = state.getModelFile(implClass);
		if (oldPath != null && !oldPath.equals(dsKey) && !file.exists()) {
			IFile oldFile = PDEProject.getBundleRelativeFile(project, IPath.fromPortableString(oldPath));
			if (oldFile.exists()) {
				try {
					oldFile.move(file.getFullPath(), true, true, null);
				} catch (CoreException e) {
					Activator.log(Status.warning(String.format("Unable to move model file from '%s' to '%s'.", oldPath, file.getFullPath()), e)); //$NON-NLS-1$
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
						if (debug.isDebugging()) {
							debug.trace("Interrupted while waiting for edits to complete on display thread.", e); //$NON-NLS-1$
						}
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
		} catch (MalformedTreeException | BadLocationException e) {
			throw new CoreException(Status.error("Error applying changes to component model.", e)); //$NON-NLS-1$
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

		String[] properties = collectProperties("property", params);
		String[] factoryProperties = collectProperties("factoryProperty", params);

		String[] propertyFiles = collectPropertiesFiles("properties", typeBinding, annotation, params);
		String[] factoryPropertyFiles = collectPropertiesFiles("factoryProperties", typeBinding, annotation, params);

		String configPolicy = null;
		if ((value = params.get("configurationPolicy")) instanceof IVariableBinding) { //$NON-NLS-1$
			IVariableBinding configPolicyBinding = (IVariableBinding) value;
			configPolicy = DSEnums.getConfigurationPolicy(configPolicyBinding.getName());
		} else if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
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

		// The following changes where made between 1.0 (Compendium 4.1) and 1.1
		// (compendium 4.2) we must check these
		// if we really want to support such old DS versions, currently we just use 1.0
		// as lowest version because the OSGi TCKs would fail otherwise:
		// (1) Definition of the Service-Component header now uses the definition of a
		// header from the module layer. It also allows a wildcards to be used in the
		// last component of the path of a header entry.
		// FIXME if manifest uses a wildcard we can assume that 1.1 is minimum
		// (2) SCR must follow the recommendations of Property Propagation on page
		// 86 and not propagate properties whose names start with ’.’ to service
		// properties.
		// XXX this is probably something we can't assert here
		// (3) The component description now allows for a configuration policy to
		// control whether component configurations are activated when
		// Configuration object are present or not.
		// FIXME if configuration policy is used 1.1 is the minimum
		// (4) The component description now allows the names of the activate and
		// deactivate methods to be specified. The signatures of the activate and
		// deactivate methods are also modified.
		// FIXME a custom name of the methods or the "modified signatures) need to
		// trigger minimum of 1.1
		// (5) The signatures of the bind and unbind methods are modified.
		// FIXME check if a "new" signature is used
		// (6) The definition of accessible methods for activate, deactivate, bind and
		// unbind methods is expanded to include any method accessible from the
		// component implementation class. This allows private and package
		// private method declared in the component implementation class to be
		// used.
		// FIXME if non public methods are used for bind/unbind we must require DS 1.1
		// (7) The additional signatures and additional accessibility for the activate,
		// deactivate, bind and unbind methods can cause problems for compo-
		// nents written to version 1.0 of this specification. The behavior in this
		// specification only applies to component descriptions using the v1.1.0
		// namespace.
		// This is something not controlled by the generator and don't needs further
		// actions
		// (8) The XML schema and namespace have been updated to v1.1.0. It now
		// supports extensibility for new attributes and elements. The name
		// attribute of the component element is now optional and the default
		// value of this attribute is the value of the class attribute of the nested
		// implementation element. The name attribute of the reference element
		// is now optional and the default value of this attribute is the value of the
		// interface attribute of the reference element. The Char type for the
		// property element has been renamed Character to match the Java type
		// name. The attributes configuration-policy, activate, deactivate and
		// modified have been added to the component element.
		// FIXME usage of 'Character' type in properties require 1.1
		// (9) When logging error messages, SCR must use a Log Service obtained
		// using the component’s bundle context so that the resulting Log Entry is
		// associated with the component’s bundle.
		// This do not affect the generator
		// (10) Clarified that target properties are component properties that can be
		// set
		// wherever component properties can be set, including configurations.
		// This do not affect the generator
		// (11) A component configuration can now avoid being deactivated when a
		// Configuration changes by specifying the modified attribute.
		// FIXME a modified method should require SCR 1.1

		String configPid = null;
		if ((value = params.get("configurationPid")) instanceof String) { //$NON-NLS-1$
			configPid = (String) value;
			validateComponentConfigPID(annotation, configPid, -1);
			requiredVersion = DSAnnotationVersion.V1_2;
		} else if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion) && value instanceof Object[] configPidElems) {
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
		if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)
				&& (value = params.get("scope")) instanceof IVariableBinding) { //$NON-NLS-1$
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

		if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion) && serviceFactory != null && serviceScope != null
				&& !serviceScope.equals(VALUE_SERVICE_SCOPE_DEFAULT)) {
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

		HashMap<String, IDSReference> refMap = buildReferenceMap(refElements);

		if (annotation.isNormalAnnotation() && DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
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
					requiredVersion = requiredVersion.max(referenceProcessor.processReference(reference, typeBinding,
							referenceAnnotation, referenceAnnotationBinding, annotationParams, referenceNames));
				}
			}
		}
		List<ComponentActivationAnnotation> activations = new ArrayList<>();
		if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
			for (FieldDeclaration field : type.getFields()) {
				for (Object modifier : field.modifiers()) {
					if (!(modifier instanceof Annotation fieldAnnotation)) {
						continue;
					}

					IAnnotationBinding fieldAnnotationBinding = fieldAnnotation.resolveAnnotationBinding();
					if (fieldAnnotationBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for annotation: %s", fieldAnnotation)); //$NON-NLS-1$
						}

						continue;
					}

					String annotationName = fieldAnnotationBinding.getAnnotationType().getQualifiedName();
					if (REFERENCE_ANNOTATION.equals(annotationName)) {
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
								for (IMemberValuePairBinding pair : fieldAnnotationBinding
										.getDeclaredMemberValuePairs()) {
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

							ReferenceProcessor referenceProcessor = new ReferenceProcessor(this, specVersion,
									requiredVersion, errorLevel, state.getMissingUnbindMethodLevel(), problemReporter);
							DSAnnotationVersion impliedVersion = referenceProcessor.processReference(reference, field,
									field.getModifiers(), fieldBinding,
									fieldAnnotation, fieldAnnotationBinding, annotationParams, referenceNames);
							requiredVersion = impliedVersion.max(requiredVersion);
						}
					} else if (ACTIVATE_ANNOTATION.equals(annotationName)) {
						for (Object fragmentElement : field.fragments()) {
							VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentElement;
							IVariableBinding fieldBinding = fragment.resolveBinding();
							if (fieldBinding == null) {
								if (debug.isDebugging()) {
									debug.trace(String.format("Unable to resolve binding for field: %s", fragment)); //$NON-NLS-1$
								}
								continue;
							}
							if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
								String fieldName = fieldBinding.getName();
								ITypeBinding binding = field.getType().resolveBinding();
								// Check if activation object and add to fields...
								if (isActivationObject(binding)) {
									if (Modifier.isStatic(field.getModifiers())) {
										problemReporter.reportProblem(fieldAnnotation, null,
												Messages.AnnotationProcessor_invalidActivate_staticField);
									} else {
										activations.add(new ComponentActivationAnnotation(fieldName, fieldAnnotation,
												null, binding));
									}
								} else {
									problemReporter.reportProblem(fieldAnnotation, null,
											Messages.AnnotationProcessor_invalidActivateField, fieldName);
								}
								requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
							} else {
								problemReporter.reportProblem(fieldAnnotation, null,
										Messages.AnnotationProcessor_invalidActivate);
							}
						}
					}
				}
			}
		}

		String deactivate = null;
		boolean lookedForDeactivateMethod = false;
		IMethodBinding deactivateMethod = null;
		Annotation deactivateAnnotation = null;
		String modified = null;
		IMethodBinding modifiedMethod = null;
		Annotation modifiedAnnotation = null;



		for (MethodDeclaration method : type.getMethods()) {
			for (Object modifier : method.modifiers()) {
				if (!(modifier instanceof Annotation methodAnnotation)) {
					continue;
				}

				IAnnotationBinding methodAnnotationBinding = methodAnnotation.resolveAnnotationBinding();
				if (methodAnnotationBinding == null) {
					if (debug.isDebugging()) {
						debug.trace(String.format("Unable to resolve binding for annotation: %s", methodAnnotation)); //$NON-NLS-1$
					}

					continue;
				}

				String annotationName = methodAnnotationBinding.getAnnotationType().getQualifiedName();

				if (ACTIVATE_ANNOTATION.equals(annotationName)) {
					ComponentActivationAnnotation activation;
					String activate = method.getName().getIdentifier();
					if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
						activation = new ComponentActivationAnnotation(activate, methodAnnotation, method,
								method.resolveBinding());
					} else {
						// prior to 1.3 only an 'activate' method is allowed
						activation = new ComponentActivationAnnotation(activate, methodAnnotation, null,
								findLifeCycleMethod(typeBinding, DEFAULT_ACTIVATE_METHOD_NAME));
					}
					activations.add(activation);
					if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion) && method.isConstructor()) {
						// will validate later...
					} else {
						validateLifeCycleMethod(methodAnnotation, DEFAULT_ACTIVATE_METHOD_NAME, method); // $NON-NLS-1$
					}
					continue;
				}

				if (DEACTIVATE_ANNOTATION.equals(annotationName)) {
					if (deactivate == null) {
						deactivate = method.getName().getIdentifier();
						if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
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
						if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
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

		if (activations.isEmpty()) {
			// lets see if we can find one...
			IMethodBinding binding = findLifeCycleMethod(typeBinding, DEFAULT_ACTIVATE_METHOD_NAME);
			if (binding != null) {
				ComponentActivationAnnotation activation = new ComponentActivationAnnotation(
						DEFAULT_ACTIVATE_METHOD_NAME, null, null, binding);
				activations.add(activation);
			}
		}
		ComponentActivationAnnotation activateMethod = validateOnlyOne( activations.stream().filter(ca -> ca.isMethod()).toList());
		ComponentActivationAnnotation activateConstructor = validateOnlyOne(
				activations.stream().filter(ca -> ca.isConstructor()).toList());
		// The fields are processed in lexicographical order, using String.compareTo, of
		// the field names
		List<ComponentActivationAnnotation> activateFields = activations.stream().filter(ca -> ca.isType())
				.sorted(Comparator.comparing(ComponentActivationAnnotation::activate)).toList();
		if (activateMethod == null || DEFAULT_ACTIVATE_METHOD_NAME.equals(activateMethod.activate())) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATE, null);
		} else {
			component.setActivateMethod(activateMethod.activate());
		}
		if (activateConstructor == null || activateConstructor.parameterCount() == 0) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_INIT, null);
		} else {
			component.setXMLAttribute(IDSConstants.ATTRIBUTE_COMPONENT_INIT,
					Integer.toString(activateConstructor.parameterCount()));
		}
		if (activateFields.isEmpty()) {
			removeAttribute(component, IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATION_FIELDS, null);
		} else {
			component.setXMLAttribute(IDSConstants.ATTRIBUTE_COMPONENT_ACTIVATION_FIELDS, activateFields.stream()
					.map(ComponentActivationAnnotation::activate).collect(Collectors.joining(" ")));
		}

		if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion) && activateConstructor != null) {
			MethodDeclaration method = activateConstructor.method();
			@SuppressWarnings("unchecked")
			List<SingleVariableDeclaration> parameters = method.parameters();
			for (int i = 0; i < parameters.size(); i++) {
				SingleVariableDeclaration parameter = parameters.get(i);
				IVariableBinding variableBinding = parameter.resolveBinding();
				Optional<Annotation> referenceAnnotation = annotations(parameter.modifiers())
						.filter(a -> isReferenceAnnotation(a.resolveAnnotationBinding())).findFirst();
				if (referenceAnnotation.isEmpty()) {
					if (isActivationObject(variableBinding.getType())) {
						// That is okay!
						continue;
					} else {
						// the spec requires @Reference annotation on non activation objects!
						problemReporter.reportProblem(activateConstructor.annotation(), null,
								Messages.AnnotationProcessor_invalidConstructorArgument, parameter.getName().toString(),
								Integer.toString(i));
					}
				} else {
					Annotation constructorParameterAnnotation = referenceAnnotation.get();
					IAnnotationBinding constructorParameterAnnotationBinding = constructorParameterAnnotation
							.resolveAnnotationBinding();
					if (constructorParameterAnnotationBinding == null) {
						if (debug.isDebugging()) {
							debug.trace(String.format("Unable to resolve binding for parameter: %s", parameter)); //$NON-NLS-1$
						}
						continue;
					}
					Map<String, Object> annotationParams = new HashMap<>();
					for (IMemberValuePairBinding pair : constructorParameterAnnotationBinding
							.getDeclaredMemberValuePairs()) {
						annotationParams.put(pair.getName(), pair.getValue());
					}
					String referenceName = (String) annotationParams.get("name"); //$NON-NLS-1$
					if (referenceName == null) {
						referenceName = variableBinding.getName();
					}
					IDSReference reference = refMap.remove(referenceName);
					if (reference == null) {
						reference = createReference(dsFactory);
					}
					references.add(reference);
					ReferenceProcessor referenceProcessor = new ReferenceProcessor(this, specVersion, requiredVersion,
							errorLevel, state.getMissingUnbindMethodLevel(), problemReporter);
					referenceProcessor.processReference(reference, parameter, parameter.getModifiers(), variableBinding,
							constructorParameterAnnotation, constructorParameterAnnotationBinding, annotationParams,
							referenceNames);
					reference.setXMLAttribute("parameter", Integer.toString(i));
					removeAttribute(reference, ReferenceProcessor.ATTRIBUTE_REFERENCE_FIELD, null);
					requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
				}
			}
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
		// see 112.8.3 Ordering of Generated Component Properties ...
		if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
			// collect component property types from activate, modified, and deactivate methods
			HashSet<ITypeBinding> cptClosure = new HashSet<>();

			// 1. Properties defined through component property types used as the type of an
			// activation object.
			if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
				if (activateConstructor != null) {
					// 1 a) The component property types used as parameters to the constructor.
					requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
					collectProperties(activateConstructor.binding(), dsFactory, newPropMap, cptClosure);
				}
				// 1 b) The component property types used as activation fields.
				for (ComponentActivationAnnotation activateField : activateFields) {
					requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
					collectProperties(activateField.binding(), dsFactory, newPropMap, cptClosure);
				}
			}
			// 1 c) The component property types used as parameters to the activate method.
			if (activateMethod != null) {
				collectProperties(activateMethod.binding(), dsFactory, newPropMap, cptClosure);
			}
			// 1 d) The component property types used as parameters to the modified method.
			if (modifiedMethod != null) {
				collectProperties(modifiedMethod, dsFactory, newPropMap, cptClosure);
			}
			// 1 e) The component property types used as parameters to the deactivate method
			if (deactivateMethod == null && !lookedForDeactivateMethod) {
				deactivateMethod = findLifeCycleMethod(typeBinding, "deactivate"); //$NON-NLS-1$
			}
			if (deactivateMethod != null) {
				collectProperties(deactivateMethod, dsFactory, newPropMap, cptClosure);
			}
			if (!cptClosure.isEmpty()) {
				requiredVersion = DSAnnotationVersion.V1_3.max(requiredVersion);
			}
			// 2) Properties defined through component property types annotating the
			// component implementation class.
			if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
				List<Annotation> propertyTypeAnnotations = annotations(type.modifiers())
						.filter(a -> isComponentPropertyType(a.resolveTypeBinding())).toList();
				for (Annotation propertyType : propertyTypeAnnotations) {
					requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
					collectComponentPropertyTypes(dsFactory, newPropMap, propertyType);
				}
			}
		}
		// 3) property element of the Component annotation.
		updateProperties(model, type, annotation, value, properties, component, dsFactory::createProperty,
				component.getPropertyElements(), newPropMap);
		updateProperties(model, type, annotation, value, factoryProperties, component, dsFactory::createFactoryProperty,
				component.getFactoryPropertyElements(), new LinkedHashMap<>());
		// 4) properties element of the Component annotation.
		updatePropertyFiles(propertyFiles, component, dsFactory::createProperties, component.getPropertiesElements());
		updatePropertyFiles(factoryPropertyFiles, component, dsFactory::createFactoryProperties,
				component.getFactoryPropertiesElements());
		if (factoryPropertyFiles.length > 0 || factoryProperties.length > 0) {
			requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
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

		if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
			if (activateConstructor != null) {
				requiredVersion = DSAnnotationVersion.V1_4.max(requiredVersion);
			}
			// TODO using logger component also requires 1.4!
		}

		String xmlns = requiredVersion.getNamespace();
		if ((value = params.get("xmlns")) instanceof String) { //$NON-NLS-1$
			xmlns = (String) value;
			validateComponentXMLNS(annotation, xmlns, requiredVersion);
		}
		component.setNamespace(xmlns);
	}

	private void collectComponentPropertyTypes(IDSDocumentFactory dsFactory,
			LinkedHashMap<String, IDSProperty> newPropMap, Annotation propertyType) {
		ITypeBinding propertyTypeBinding = propertyType.resolveTypeBinding();
		String simpleName = propertyTypeBinding.getName();

		String prefix = getPrefix(propertyTypeBinding);
		IMethodBinding[] methods = propertyTypeBinding.getDeclaredMethods();
		Map<String, IDSProperty> map = Arrays.stream(methods)
				.map(methodBinding -> createProperty(methodBinding, prefix, dsFactory))
				.filter(Objects::nonNull).collect(Collectors.toMap(IDSProperty::getName,
						Function.identity(), (a, b) -> a, LinkedHashMap::new));
		if (propertyType instanceof NormalAnnotation normal) {
			@SuppressWarnings("unchecked")
			List<MemberValuePair> values = normal.values();
			for (MemberValuePair pair : values) {
				String propName = pair.getName().getFullyQualifiedName();
				String key = NameGenerator.createPropertyName(propName, prefix, specVersion);
				setPropertyValue(map.get(key), pair.getValue());
			}
		}
		if (propertyType instanceof MarkerAnnotation && map.isEmpty()) {
			IDSProperty property = dsFactory.createProperty();
			property.setPropertyName(NameGenerator.createClassPropertyName(simpleName, prefix));
			property.setPropertyType(IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN);
			property.setPropertyValue(String.valueOf(Boolean.TRUE));
			newPropMap.remove(property.getName()); // force re-insert (append)
			newPropMap.put(property.getName(), property);
			return;
		}
		if (propertyType instanceof SingleMemberAnnotation single && map.size() == 1) {
			IDSProperty property = dsFactory.createProperty();
			property.setPropertyName(NameGenerator.createClassPropertyName(simpleName, prefix));
			Expression expression = single.getValue();
			property.setPropertyType(getPropertyType(expression.resolveTypeBinding()));
			setPropertyValue(property, expression);
			newPropMap.remove(property.getName()); // force re-insert (append)
			newPropMap.put(property.getName(), property);
			return;
		}
		for (IDSProperty prop : map.values()) {
			newPropMap.remove(prop.getName()); // force re-insert (append)
			if (prop.getPropertyValue() != null || prop.getPropertyElemBody() != null) {
				// only insert if it has a value...
				newPropMap.put(prop.getName(), prop);
			}
		}
	}

	private void setPropertyValue(IDSProperty property, Expression expression) {
		if (expression instanceof QualifiedName name) {
			Object constantExpressionValue = name.resolveConstantExpressionValue();
			if (constantExpressionValue == null) {
				removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
			} else {
				property.setPropertyValue(constantExpressionValue.toString());
			}
		} else if (expression instanceof StringLiteral string) {
			property.setPropertyValue(string.getLiteralValue());
		} else if (expression instanceof ArrayInitializer array) {
			removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
			@SuppressWarnings("unchecked")
			List<Expression> expressions = array.expressions();
			if (!expressions.isEmpty()) {
				String body = expressions.stream().map(arrayExpression -> {
					if (arrayExpression instanceof StringLiteral string) {
						return string.getLiteralValue();
					} else if (arrayExpression instanceof TypeLiteral type) {
						return type.getType().resolveBinding().getQualifiedName();
					} else if (expression instanceof QualifiedName name) {
						return String.valueOf(name.resolveConstantExpressionValue());
					}
					return expression.toString();
				}).collect(Collectors.joining(TextUtil.getDefaultLineDelimiter()));
				if (expressions.size() == 1) {
					property.setPropertyValue(body);
				} else {
					property.setPropertyElemBody(body);
				}
			}
		} else if (expression instanceof TypeLiteral type) {
			property.setPropertyValue(type.getType().resolveBinding().getQualifiedName());
		} else {
			property.setPropertyValue(expression.toString());
		}
	}

	private String getPrefix(ITypeBinding typeBinding) {
		if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
			IVariableBinding[] fields = typeBinding.getDeclaredFields();
			for (IVariableBinding binding : fields) {
				String name = binding.getName();
				if ("PREFIX_".equals(name)) {
					if (binding.getConstantValue() instanceof String prefix) {
						return prefix;
					}
				}
			}
		}
		return null;
	}

	private <T extends IDSSingleProperty> void updateProperties(IDSModel model, TypeDeclaration type,
			Annotation annotation, Object value,
			String[] properties, IDSComponent component, Supplier<T> factory, T[] propElements,
			LinkedHashMap<String, T> newPropMap) {
		if (newPropMap.isEmpty() && properties.length == 0) {
			removeChildren(component, Arrays.asList(propElements));
		} else {
			// build up new property elements
			LinkedHashMap<String, T> map = new LinkedHashMap<>(properties.length);
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
				if (propertyValue != null && IDSConstants.VALUE_PROPERTY_TYPE_CHAR.equals(propertyType)) {
					// according to the spec a char must be encoded as its unicode point: For
					// Character types, the conversion must be handled by Integer.valueOf method, a
					// Character is always represented by its Unicode value.
					if (propertyValue.length() == 0 || propertyValue.length() > 1) {
						problemReporter.reportProblem(annotation, "property", i, //$NON-NLS-1$
								NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyValue, type, value),
								String.valueOf(value));
					} else {
						char c = propertyValue.charAt(0);
						propertyValue = Integer.toString(c);
					}
				}

				T property = map.get(propertyName);
				if (property == null) {
					// create a new property
					property = factory.get();
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
						property.setPropertyElemBody(content + TextUtil.getDefaultLineDelimiter() + pair[1]);
					}
				}
			}

			// reconcile against existing property elements
			HashMap<String, T> propMap = new HashMap<>(propElements.length);
			for (T propElement : propElements) {
				T put = propMap.put(propElement.getPropertyName(), propElement);
				if (put != null) {
					// duplicate entry
					removeChildren(component, List.of(put));
				}
			}

			newPropMap.keySet().removeAll(map.keySet()); // force re-insert (append)
			newPropMap.putAll(map);

			ArrayList<T> propList = new ArrayList<>(newPropMap.values());
			for (ListIterator<T> i = propList.listIterator(); i.hasNext();) {
				T newProperty = i.next();
				T property = propMap.remove(newProperty.getPropertyName());
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
	}

	private String[] collectProperties(String key, Map<String, ?> params) {
		Object value = params.get(key);
		String[] properties;
		if (value instanceof Object[] elements) { // $NON-NLS-1$
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
		return properties;
	}

	private <T extends IDSBundleProperties> void updatePropertyFiles(String[] propertyFiles, IDSComponent component,
			Supplier<T> factory, T[] propFileElements) {
		if (propertyFiles.length == 0) {
			removeChildren(component, Arrays.asList(propFileElements));
		} else {
			HashMap<String, T> propFileMap = new HashMap<>(propFileElements.length);
			for (T propFileElement : propFileElements) {
				T put = propFileMap.put(propFileElement.getEntry(), propFileElement);
				if (put != null) {
					// duplicate entry!
					removeChildren(component, List.of(put));
				}
			}

			ArrayList<T> propFileList = new ArrayList<>(propertyFiles.length);
			for (String propertyFile : propertyFiles) {
				T propertiesElement = propFileMap.remove(propertyFile);
				if (propertiesElement == null) {
					propertiesElement = factory.get();
					propertiesElement.setInTheModel(false); // note: workaround for PDE bug
					propertiesElement.setEntry(propertyFile);
				}
				propFileList.add(propertiesElement);
			}

			int firstPos;
			if (propFileElements.length == 0) {
				// insert first properties element after last property or (if none) first child of component
				IDSProperty[] propElements = component.getPropertyElements();
				firstPos = propElements.length == 0 ? 0 : component.indexOf(propElements[propElements.length - 1]) + 1;
			} else {
				firstPos = component.indexOf(propFileElements[0]);
			}

			removeChildren(component, propFileMap.values());

			addOrMoveChildren(component, propFileList, firstPos);
		}
	}

	private String[] collectPropertiesFiles(String key, ITypeBinding typeBinding, Annotation annotation,
			Map<String, ?> params) {
		Object value = params.get(key);
		String[] propertyFiles;
		if (value instanceof Object[] elements) { // $NON-NLS-1$
			ArrayList<String> list = new ArrayList<>(elements.length);
			for (Object element : elements) {
				if (element instanceof String) {
					list.add((String) element);
				}
			}

			propertyFiles = list.toArray(new String[list.size()]);
			validateComponentPropertyFiles(key, annotation,
					((IType) typeBinding.getJavaElement()).getJavaProject().getProject(), propertyFiles);
		} else {
			propertyFiles = new String[0];
		}
		return propertyFiles;
	}

	private HashMap<String, IDSReference> buildReferenceMap(IDSReference[] refElements) {
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
		return refMap;
	}

	private ComponentActivationAnnotation validateOnlyOne(List<ComponentActivationAnnotation> list) {
		if (list.isEmpty()) {
			return null;
		}
		if (list.size() == 1 || errorLevel.isIgnore()) {
			return list.get(0);
		}
		for (ComponentActivationAnnotation a : list) {
			problemReporter.reportProblem(a.annotation(), null, Messages.AnnotationProcessor_duplicateActivateMethod,
					a.activate());
		}
		return null;
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
		} catch (IOException | ClassNotFoundException e) {
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
					buf.append(TextUtil.getDefaultLineDelimiter());
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

	private DSAnnotationVersion collectProperties(IBinding binding, IDSDocumentFactory factory,
			Map<String, IDSProperty> properties,
			Collection<ITypeBinding> visited) {
		DSAnnotationVersion version = DSAnnotationVersion.V1_3;
		ITypeBinding[] parameterTypes;
		if (binding instanceof IMethodBinding method) {
			parameterTypes = method.getParameterTypes();
		} else if (binding instanceof ITypeBinding type) {
			parameterTypes = new ITypeBinding[] { type };
		} else {
			// unsupported binding...
			return version;
		}
		for (ITypeBinding paramTypeBinding : parameterTypes) {
			if (!paramTypeBinding.isAnnotation() || !visited.add(paramTypeBinding)) {
				continue;
			}
			String prefix = getPrefix(paramTypeBinding);
			if (prefix != null) {
				version = DSAnnotationVersion.V1_4.max(version);
			}
			IMethodBinding[] declaredMethods = paramTypeBinding.getDeclaredMethods();
			if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
				if (declaredMethods.length == 0) {
					// a marker annotation! Actually the spec says it is not useful to have these on
					// methods but the TCK do so...
					// See https://github.com/osgi/osgi/issues/640
					version = DSAnnotationVersion.V1_4.max(version);
					IDSProperty property = factory.createProperty();
					property.setPropertyName(NameGenerator.createClassPropertyName(paramTypeBinding.getName(), prefix));
					property.setPropertyType(IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN);
					property.setPropertyValue(String.valueOf(Boolean.TRUE));
					properties.remove(property.getName()); // force re-insert (append)
					properties.put(property.getName(), property);
					continue;
				}
				if (declaredMethods.length == 1 && "value".equals(declaredMethods[0].getName())) {
					// a single member annotation
					version = DSAnnotationVersion.V1_4.max(version);
					IDSProperty property = createProperty(declaredMethods[0], prefix, factory);
					property.setPropertyName(NameGenerator.createClassPropertyName(paramTypeBinding.getName(), prefix));
					properties.remove(property.getName()); // force re-insert (append)
					properties.put(property.getName(), property);
					continue;
				}
			}
			for (IMethodBinding methodBinding : declaredMethods) {
				IDSProperty property = createProperty(methodBinding, prefix, factory);
				if (property != null
						&& (property.getPropertyElemBody() != null || property.getPropertyValue() != null)) {
					properties.remove(property.getName()); // force re-insert (append)
					properties.put(property.getName(), property);
				}
			}
		}
		return version;
	}

	private IDSProperty createProperty(IMethodBinding methodBinding, String prefix, IDSDocumentFactory factory) {
		if (!methodBinding.isAnnotationMember()) {
			return null;
		}
		ITypeBinding returnType = methodBinding.getReturnType();
		if (returnType.isArray() ? returnType.getElementType().isAnnotation() : returnType.isAnnotation()) {
			// TODO per spec we should report error, but we may have no annotation to report
			// it on!
			return null;
		}
		Object value = methodBinding.getDefaultValue();
		String propertyName = NameGenerator.createPropertyName(methodBinding.getName(), prefix, specVersion);
		String propertyType = getPropertyType(returnType);
		IDSProperty property = factory.createProperty();
		property.setPropertyName(propertyName);
		property.setPropertyType(propertyType);
		if (value == null) {
			removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
		} else {
			if (returnType.isArray()) {
				Object[] objects = (Object[]) value;
				if (objects.length > 0) {
					StringBuilder body = new StringBuilder();
					for (Object item : objects) {
						String itemValue = getPropertyValue(item);
						if (itemValue == null || (itemValue = itemValue.trim()).isEmpty()) {
							continue;
						}

						if (body.length() > 0) {
							body.append(TextUtil.getDefaultLineDelimiter());
						}

						body.append(itemValue);
					}
					property.setPropertyElemBody(body.toString());
				}
				removeAttribute(property, IDSConstants.ATTRIBUTE_PROPERTY_VALUE, null);
			} else {
				property.setPropertyValue(getPropertyValue(value));
			}
		}
		return property;
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
		if (value instanceof Character character) {
			// according to the spec a char must be encoded as its unicode point: For
			// Character types, the conversion must be handled by Integer.valueOf method, a
			// Character is always represented by its Unicode value.
			return Integer.toString(character.charValue());
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

		if (type == null || PROPERTY_TYPES.contains(type)) {
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

	private void validateComponentPropertyFiles(String key, Annotation annotation, IProject project, String[] files) {
		if (errorLevel.isIgnore()) {
			return;
		}

		for (int i = 0; i < files.length; ++i) {
			String file = files[i];
			IFile wsFile = PDEProject.getBundleRelativeFile(project, IPath.fromOSString(file));
			if (!wsFile.exists()) {
				problemReporter.reportProblem(annotation, key, i,
						NLS.bind(Messages.AnnotationProcessor_invalidComponentPropertyFile, file, key), file); // $NON-NLS-1$
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
		if (methodBinding.isConstructor()) {
			problemReporter.reportProblem(annotation, methodName,
					Messages.AnnotationProcessor_invalidLifecycleMethod_noMethod);
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

			if (paramTypeBinding.isAnnotation() && DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)) {
				if (!annotationParams.add(paramTypeBinding)) {
					isDuplicate = true;
				}
			} else if (MAP_TYPE.equals(paramTypeName)) {
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
			} else if (BUNDLE_CONTEXT.equals(paramTypeName)) {
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
						if (DSAnnotationVersion.V1_3.isEqualOrHigherThan(specVersion)
								&& annotationParams.add(paramTypeBinding)) {
							// component property type (multiple arguments allowed)
							continue;
						}

						isInvalid = true;
						break;
					}

					ITypeBinding paramTypeErasure = paramTypeBinding.getErasure();
					String paramTypeName = paramTypeErasure.isMember() ? paramTypeErasure.getBinaryName() : paramTypeErasure.getQualifiedName();

					if (MAP_TYPE.equals(paramTypeName)) {
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
					} else if (BUNDLE_CONTEXT.equals(paramTypeName)) {
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

	/**
	 * An injectable constructor is one annotated with <code>@Activate</code>
	 *
	 * @param type
	 * @param problemReporter2
	 * @return
	 */
	private static boolean hasInjectableConstructor(TypeDeclaration type, ProblemReporter problemReporter) {
		for (MethodDeclaration method : type.getMethods()) {
			if (method.isConstructor()
					&& annotations(method.modifiers()).map(Annotation::resolveAnnotationBinding)
							.anyMatch(AnnotationVisitor::isActivateAnnotation)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasDefaultConstructor(TypeDeclaration type) {
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

	private static Stream<Annotation> annotations(List<?> modifiers) {
		return modifiers.stream().filter(Annotation.class::isInstance).map(Annotation.class::cast);
	}

	private static boolean isActivateAnnotation(IAnnotationBinding binding) {
		return binding != null && ACTIVATE_ANNOTATION.equals(binding.getAnnotationType().getQualifiedName());
	}

	private static boolean isReferenceAnnotation(IAnnotationBinding binding) {
		return binding != null && REFERENCE_ANNOTATION.equals(binding.getAnnotationType().getQualifiedName());
	}

	private static boolean isComponentPropertyType(IAnnotationBinding binding) {
		return binding != null
				&& COMPONENT_PROPERTY_TYPE_ANNOTATION.equals(binding.getAnnotationType().getQualifiedName());
	}

	/**
	 * Check if the given {@link ITypeBinding} is an <a href=
	 * "https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-activation.objects">Activation
	 * Object</a>
	 *
	 * @param param the binding to check
	 * @return <code>true</code> if this is an Activation Object, <code>false</code>
	 *         otherwise
	 */
	private static boolean isActivationObject(ITypeBinding param) {
		String binaryName = param.getErasure().getBinaryName();
		if (COMPONENT_CONTEXT.equals(binaryName) || BUNDLE_CONTEXT.equals(binaryName) || MAP_TYPE.equals(binaryName)) {
			return true;
		}
		return param.isAnnotation();
	}

	/**
	 * Check if the given {@link ITypeBinding} is a <a href=
	 * "https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.property.types">Component
	 * Property Type</a>
	 *
	 * @param param the binding to check
	 * @return <code>true</code> if this is a Component Property Type
	 *         <code>false</code> otherwise
	 */
	private static boolean isComponentPropertyType(ITypeBinding param) {
		if (param != null) {
			IAnnotationBinding[] annotations = param.getAnnotations();
			for (IAnnotationBinding annotationAnnotation : annotations) {
				if (isComponentPropertyType(annotationAnnotation)) {
					return true;
				}
			}
		}
		return false;
	}
}