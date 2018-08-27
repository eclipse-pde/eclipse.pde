/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class ReferenceProcessor {

	private static final String COMPONENT_SERVICE_OBJECTS = "org.osgi.service.component.ComponentServiceObjects"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_POLICY_OPTION = "policy-option"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_UPDATED = "updated"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_SCOPE = "scope"; //$NON-NLS-1$

	static final String ATTRIBUTE_REFERENCE_FIELD = "field"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_FIELD_OPTION = "field-option"; //$NON-NLS-1$

	private static final String ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE = "field-collection-type"; //$NON-NLS-1$

	private static final String VALUE_REFERENCE_FIELD_OPTION_REPLACE = DSEnums.getFieldOption("REPLACE"); //$NON-NLS-1$

	private static final String VALUE_REFERENCE_FIELD_OPTION_UPDATE = DSEnums.getFieldOption("UPDATE"); //$NON-NLS-1$

	private final AnnotationVisitor visitor;

	private final DSAnnotationVersion specVersion;

	private final DSAnnotationVersion requiredVersion;

	private final ValidationErrorLevel errorLevel;

	private final ValidationErrorLevel missingUnbindMethodLevel;

	private final ProblemReporter problemReporter;

	public ReferenceProcessor(AnnotationVisitor visitor, DSAnnotationVersion specVersion, DSAnnotationVersion requiredVersion, ValidationErrorLevel errorLevel, ValidationErrorLevel missingUnbindMethodLevel, ProblemReporter problemReporter) {
		this.visitor = visitor;
		this.specVersion = specVersion;
		this.requiredVersion = requiredVersion;
		this.errorLevel = errorLevel;
		this.missingUnbindMethodLevel = missingUnbindMethodLevel;
		this.problemReporter = problemReporter;
	}

	public DSAnnotationVersion processReference(IDSReference reference, MethodDeclaration method, IMethodBinding methodBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
		ITypeBinding[] argTypes = methodBinding.getParameterTypes();

		ITypeBinding serviceType;
		Object value;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
			if (!errorLevel.isIgnore() && argTypes.length > 0) {
				if (specVersion == DSAnnotationVersion.V1_3) {
					for (ITypeBinding argType : argTypes) {
						if (!isValidArgumentForService(argType, serviceType)) {
							problemReporter.reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_serviceType, argType.getName(), serviceType.getName()), argType.getName(), serviceType.getName()); //$NON-NLS-1$
							break;
						}
					}
				} else {
					String erasure = argTypes[0].getErasure().getBinaryName();
					ITypeBinding[] typeArgs;
					if (!(ServiceReference.class.getName().equals(erasure)
							&& ((typeArgs = argTypes[0].getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
							&& !serviceType.isAssignmentCompatible(argTypes[0])) {
						problemReporter.reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_serviceType, argTypes[0].getName(), serviceType.getName()), argTypes[0].getName(), serviceType.getName()); //$NON-NLS-1$
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

					if (Map.class.getName().equals(erasure)) {
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
			problemReporter.reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_serviceUnknown);

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
		IMethodBinding unbindMethod = null;
		if ((value = params.get("unbind")) instanceof String) { //$NON-NLS-1$
			String unbindValue = (String) value;
			if ("-".equals(unbindValue)) { //$NON-NLS-1$
				unbind = null;
			} else {
				unbind = unbindValue;
				if (!errorLevel.isIgnore()) {
					unbindMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, unbind, true);
					if (unbindMethod == null) {
						problemReporter.reportProblem(annotation, "unbind", NLS.bind(Messages.AnnotationProcessor_invalidReference_unbindMethod, unbind), unbind); //$NON-NLS-1$
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

			unbindMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, unbindCandidate, false);
			if (unbindMethod == null) {
				unbind = null;
				if (IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)) {
					problemReporter.reportProblem(annotation, null, missingUnbindMethodLevel, NLS.bind(Messages.AnnotationProcessor_invalidReference_noImplicitUnbind, unbindCandidate), unbindCandidate);
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
		IMethodBinding updatedMethod = null;
		if ((value = params.get("updated")) instanceof String) { //$NON-NLS-1$
			String updatedValue = (String) value;
			if ("-".equals(updatedValue)) { //$NON-NLS-1$
				updated = null;
			} else {
				updated = updatedValue;
				if (!errorLevel.isIgnore()) {
					updatedMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, updated, true);
					if (updatedMethod == null) {
						problemReporter.reportProblem(annotation, "updated", NLS.bind(Messages.AnnotationProcessor_invalidReference_updatedMethod, updated), updated); //$NON-NLS-1$
					}
				}
			}
		} else {
			String updatedCandidate = ATTRIBUTE_REFERENCE_UPDATED + getReferenceName(methodName);
			updatedMethod = findReferenceMethod(methodBinding.getDeclaringClass(), serviceType, updatedCandidate, false);
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
						problemReporter.reportProblem(annotation, "bind", Messages.AnnotationProcessor_invalidReference_bindMethodNameMismatch, bind); //$NON-NLS-1$
					}
				}
			}
		} else {
			updateFieldAttributes(reference, null, null, null);
		}

		updateAttributes(reference, name, service, cardinality, policy, target, policyOption, referenceScope);
		updateMethodAttributes(reference, methodName, updated, unbind);

		return determineRequiredVersion(reference, methodBinding.getDeclaringClass(), serviceType, new MethodParams(methodName, methodBinding, updated, updatedMethod, unbind, unbindMethod));
	}

	private boolean isValidArgumentForService(ITypeBinding argType, ITypeBinding serviceType) {
		String erasure = argType.getErasure().getBinaryName();
		ITypeBinding[] typeArgs;
		return ((ServiceReference.class.getName().equals(erasure) || COMPONENT_SERVICE_OBJECTS.equals(erasure))
				&& ((typeArgs = argType.getTypeArguments()).length == 0 || serviceType.isAssignmentCompatible(typeArgs[0])))
				|| serviceType.isAssignmentCompatible(argType)
				|| Map.class.getName().equals(erasure);
	}

	public String getReferenceName(String methodName, Map<String, Object> params) {
		Object value;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			return (String) value;
		}

		return getReferenceName(methodName);
	}

	static String getReferenceName(String bindMethodName) {
		if (bindMethodName.startsWith("bind")) { //$NON-NLS-1$
			return bindMethodName.substring("bind".length()); //$NON-NLS-1$
		}

		if (bindMethodName.startsWith("set")) { //$NON-NLS-1$
			return bindMethodName.substring("set".length()); //$NON-NLS-1$
		}

		if (bindMethodName.startsWith("add")) { //$NON-NLS-1$
			return bindMethodName.substring("add".length()); //$NON-NLS-1$
		}

		return bindMethodName;
	}

	private void validateReferenceName(String name, Annotation annotation, Map<String, Annotation> names) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (names.containsKey(name)) {
			problemReporter.reportProblem(annotation, "name", NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
			Annotation duplicate = names.put(name, null);
			if (duplicate != null) {
				problemReporter.reportProblem(duplicate, "name", NLS.bind(Messages.AnnotationProcessor_duplicateReferenceName, name), name); //$NON-NLS-1$
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
					problemReporter.reportProblem(annotation, "field", NLS.bind(Messages.AnnotationProcessor_invalidReference_fieldNotFound, field), field); //$NON-NLS-1$
				} else if (serviceType != null) {
					ITypeBinding targetType = fieldBinding.getType();
					if (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
							|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)) {
						collectionType = determineCollectionType(annotation.getAST(), targetType);
						targetType = collectionType.getElementType();
					}

					if (targetType != null) {
						if (!isValidFieldForService(targetType, serviceType)) {
							problemReporter.reportProblem(annotation, "field", NLS.bind(Messages.AnnotationProcessor_invalidReference_incompatibleFieldType, targetType.getName(), serviceType.getName()), targetType.getName(), serviceType.getName()); //$NON-NLS-1$
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
					problemReporter.reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldOptionNotApplicable); //$NON-NLS-1$
				} else if (VALUE_REFERENCE_FIELD_OPTION_REPLACE.equals(fieldOption)) {
					if (fieldBinding == null) {
						fieldBinding = findReferenceField(field, typeBinding);
					}

					if (fieldBinding != null && Modifier.isFinal(fieldBinding.getModifiers())) {
						problemReporter.reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldFinal_fieldOption, fieldOption); //$NON-NLS-1$
					}
				} else if (VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					if (!(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)
							&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
									|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)))) {
						problemReporter.reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldPolicyCardinality_fieldOption, fieldOption); //$NON-NLS-1$
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
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_NAME, null);
		} else {
			reference.setReferenceName(name);
		}

		if (service == null) {
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_INTERFACE, null);
		} else {
			reference.setReferenceInterface(service);
		}

		if (cardinality == null) {
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_CARDINALITY, IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE);
		} else {
			reference.setReferenceCardinality(cardinality);
		}

		if (policy == null) {
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_POLICY, IDSConstants.VALUE_REFERENCE_POLICY_STATIC);
		} else {
			reference.setReferencePolicy(policy);
		}

		if (target == null) {
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_TARGET, null);
		} else {
			reference.setReferenceTarget(target);
		}

		if (policyOption == null) {
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_POLICY_OPTION, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION, policyOption);
		}

		if (scope == null) {
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_SCOPE, null);
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
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_BIND, null);
		} else {
			reference.setReferenceBind(bind);
		}

		if (updated == null) {
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_UPDATED, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_UPDATED, updated);
		}

		if (unbind == null) {
			visitor.removeAttribute(reference, IDSConstants.ATTRIBUTE_REFERENCE_UNBIND, null);
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
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD, field);
		}

		if (fieldOption == null) {
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD_OPTION, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD_OPTION, fieldOption);
		}

		if (fieldCollectionType == null) {
			visitor.removeAttribute(reference, ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE, null);
		} else {
			reference.setXMLAttribute(ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE, fieldCollectionType);
		}
	}

	public void processReference(IDSReference reference, FieldDeclaration field, IVariableBinding fieldBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
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
					problemReporter.reportProblem(annotation, "cardinality", Messages.AnnotationProcessor_invalidReference_fieldTypeCardinalityMismatch, cardinality); //$NON-NLS-1$
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
						problemReporter.reportProblem(annotation, "service", NLS.bind(Messages.AnnotationProcessor_invalidReference_incompatibleServiceType, targetType.getName(), serviceType.getName()), targetType.getName(), serviceType.getName()); //$NON-NLS-1$
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
			problemReporter.reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_fieldUnknownServiceType);

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
					problemReporter.reportProblem(annotation, "field", Messages.AnnotationProcessor_invalidReference_fieldNameMismatch, fieldVal); //$NON-NLS-1$
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
						problemReporter.reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldFinal_fieldOption, fieldOption); //$NON-NLS-1$
					}
				} else if (VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					if (!(IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC.equals(policy)
							&& (IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N.equals(cardinality)
									|| IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N.equals(cardinality)))) {
						problemReporter.reportProblem(annotation, "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldPolicyCardinality_fieldOption, fieldOption); //$NON-NLS-1$
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
					problemReporter.reportProblem(annotation, policy == null ? null : "policy", Messages.AnnotationProcessor_invalidReference_fieldCardinalityPolicyCollectionType); //$NON-NLS-1$
				}

				if (!VALUE_REFERENCE_FIELD_OPTION_UPDATE.equals(fieldOption)) {
					problemReporter.reportProblem(annotation, fieldOption == null ? null : "fieldOption", Messages.AnnotationProcessor_invalidReference_fieldCollection_fieldOption); //$NON-NLS-1$
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
		} else if (Map.Entry.class.getName().equals(erasure)) {
			fieldCollectionType = "tuple"; //$NON-NLS-1$
		}

		return fieldCollectionType;
	}

	private MethodParams processReferenceMethodParams(IDSReference reference, ITypeBinding typeBinding, Annotation annotation, Map<String, ?> params, ITypeBinding serviceType) {
		String bind = null;
		IMethodBinding bindMethod = null;
		Object value;
		if ((value = params.get("bind")) instanceof String) { //$NON-NLS-1$
			bind = (String) value;
			if (!errorLevel.isIgnore()) {
				bindMethod = findReferenceMethod(typeBinding, serviceType, bind, true);
				if (bindMethod == null) {
					problemReporter.reportProblem(annotation, "bind", NLS.bind(Messages.AnnotationProcessor_invalidReference_bindMethodNotFound, bind), bind); //$NON-NLS-1$
				}
			}
		}

		String unbind = null;
		IMethodBinding unbindMethod = null;
		if ((value = params.get("unbind")) instanceof String) { //$NON-NLS-1$
			unbind = (String) value;
			if (!errorLevel.isIgnore()) {
				unbindMethod = findReferenceMethod(typeBinding, serviceType, unbind, true);
				if (unbindMethod == null) {
					problemReporter.reportProblem(annotation, "unbind", NLS.bind(Messages.AnnotationProcessor_invalidReference_unbindMethod, unbind), unbind); //$NON-NLS-1$
				}
			}
		}

		String updated = null;
		IMethodBinding updatedMethod = null;
		if ((value = params.get("updated")) instanceof String) { //$NON-NLS-1$
			updated = (String) value;
			if (!errorLevel.isIgnore()) {
				updatedMethod = findReferenceMethod(typeBinding, serviceType, updated, true);
				if (updatedMethod == null) {
					problemReporter.reportProblem(annotation, "updated", NLS.bind(Messages.AnnotationProcessor_invalidReference_updatedMethod, updated), updated); //$NON-NLS-1$
				}
			}
		}

		updateMethodAttributes(reference, bind, updated, unbind);

		return new MethodParams(bind, bindMethod, updated, updatedMethod, unbind, unbindMethod);
	}

	private void validateReferenceField(Annotation annotation, IVariableBinding fieldBinding) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			problemReporter.reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_staticField);
		}
	}

	public DSAnnotationVersion processReference(IDSReference reference, ITypeBinding typeBinding, Annotation annotation, IAnnotationBinding annotationBinding, Map<String, Object> params, Map<String, Annotation> names) {
		ITypeBinding serviceType;
		Object value;
		if ((value = params.get("service")) instanceof ITypeBinding) { //$NON-NLS-1$
			serviceType = (ITypeBinding) value;
		} else {
			// service must be explicitly specified; default to Object
			serviceType = annotation.getAST().resolveWellKnownType(Object.class.getName());

			if (!errorLevel.isIgnore()) {
				problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_missingRequiredParam, "service")); //$NON-NLS-1$
			}
		}

		String service = serviceType == null ? null : serviceType.getBinaryName();

		String name = null;
		if ((value = params.get("name")) instanceof String) { //$NON-NLS-1$
			name = (String) value;
			validateReferenceName(name, annotation, names);
		} else {
			if (!errorLevel.isIgnore()) {
				problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_missingRequiredParam, "name")); //$NON-NLS-1$
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

		MethodParams methodParams = processReferenceMethodParams(reference, typeBinding, annotation, params, serviceType);
		processReferenceFieldParams(reference, typeBinding, annotation, params, serviceType, cardinality, policy);

		updateAttributes(reference, name, service, cardinality, policy, target, policyOption, referenceScope);

		return determineRequiredVersion(reference, typeBinding, serviceType, methodParams);
	}

	private void validateReferenceBindMethod(Annotation annotation, ITypeBinding serviceType, IMethodBinding methodBinding) {
		if (errorLevel.isIgnore()) {
			return;
		}

		if (Modifier.isStatic(methodBinding.getModifiers())) {
			problemReporter.reportProblem(annotation, null, Messages.AnnotationProcessor_invalidReference_staticBindMethod);
		}

		String returnTypeName = methodBinding.getReturnType().getName();
		if (!Void.TYPE.getName().equals(returnTypeName)) {
			problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodReturnType, returnTypeName), returnTypeName);
		}

		ITypeBinding[] argTypes = methodBinding.getParameterTypes();
		if (specVersion == DSAnnotationVersion.V1_3) {
			if (argTypes.length == 0) {
				problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_bindMethodNoArgs, serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()));
			} else if (serviceType != null) {
				for (ITypeBinding argType : argTypes) {
					String erasure = argType.getErasure().getBinaryName();
					if (!ServiceReference.class.getName().equals(erasure)
							&& !COMPONENT_SERVICE_OBJECTS.equals(erasure)
							&& !(serviceType == null || serviceType.isAssignmentCompatible(argType))
							&& !Map.class.getName().equals(erasure)) {
						problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodArg, argType.getName(), serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()), argType.getName());
					}
				}
			}
		} else {
			if (!isLegacySignature(methodBinding, serviceType)) {
				String[] args = new String[argTypes.length];
				StringBuilder buf = new StringBuilder(64);
				buf.append('(');
				for (int i = 0; i < args.length; ++i) {
					args[i] = argTypes[i].getName();
					if (buf.length() > 1) {
						buf.append(", "); //$NON-NLS-1$
					}

					buf.append(args[i]);
				}

				buf.append(')');
				problemReporter.reportProblem(annotation, null, NLS.bind(Messages.AnnotationProcessor_invalidReference_invalidBindMethodParameters, buf, serviceType == null ? Messages.AnnotationProcessor_unknownServiceTypeLabel : serviceType.getName()), args);
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

			problemReporter.reportProblem(annotation, "target", msg, target); //$NON-NLS-1$
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

	private DSAnnotationVersion determineRequiredVersion(
			IDSReference reference,
			ITypeBinding implType,
			ITypeBinding serviceType,
			MethodParams methodParams) {
		if (requiredVersion == DSAnnotationVersion.V1_3) {
			return DSAnnotationVersion.V1_3;
		}

		DSAnnotationVersion requiredVersion;
		if (specVersion == DSAnnotationVersion.V1_3 &&
				(reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_SCOPE) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD_OPTION) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_FIELD_COLLECTION_TYPE) != null)) {
			requiredVersion = DSAnnotationVersion.V1_3;
		} else if (reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_POLICY_OPTION) != null
				|| reference.getDocumentAttribute(ATTRIBUTE_REFERENCE_UPDATED) != null) {
			requiredVersion = DSAnnotationVersion.V1_2;
		} else {
			requiredVersion = DSAnnotationVersion.V1_1;
		}

		if (specVersion == DSAnnotationVersion.V1_3 && requiredVersion != DSAnnotationVersion.V1_3) {
			// check if any one of the event methods *don't* have legacy-compatible signature
			String bind = methodParams.getBind();
			IMethodBinding bindMethod = methodParams.getBindMethod();
			String updated = methodParams.getUpdated();
			IMethodBinding updatedMethod = methodParams.getUpdatedMethod();
			String unbind = methodParams.getUnbind();
			IMethodBinding unbindMethod = methodParams.getUnbindMethod();

			if (bind != null) {
				if (bindMethod == null) {
					bindMethod = findReferenceMethod(implType, serviceType, bind, true);
				}

				if (bindMethod != null && !isLegacySignature(bindMethod, serviceType)) {
					requiredVersion = DSAnnotationVersion.V1_3;
				}
			}

			if (requiredVersion != DSAnnotationVersion.V1_3 && unbind != null) {
				if (unbindMethod == null) {
					unbindMethod = findReferenceMethod(implType, serviceType, unbind, true);
				}

				if (unbindMethod != null && !isLegacySignature(bindMethod, serviceType)) {
					requiredVersion = DSAnnotationVersion.V1_3;
				}
			}

			if (requiredVersion != DSAnnotationVersion.V1_3 && updated != null) {
				if (updatedMethod == null) {
					updatedMethod = findReferenceMethod(implType, serviceType, updated, true);
				}

				if (updatedMethod != null && !isLegacySignature(updatedMethod, serviceType)) {
					requiredVersion = DSAnnotationVersion.V1_3;
				}
			}
		}

		return requiredVersion;
	}

	private boolean isLegacySignature(IMethodBinding methodBinding, ITypeBinding serviceType) {
		ITypeBinding[] argTypes = methodBinding.getParameterTypes();
		return argTypes.length == 1
				&& (ServiceReference.class.getName().equals(argTypes[0].getErasure().getBinaryName())
						|| serviceType == null
						|| serviceType.isAssignmentCompatible(argTypes[0]))
				|| (argTypes.length == 2
				&& (serviceType == null || serviceType.isAssignmentCompatible(argTypes[0]))
				&& Map.class.getName().equals(argTypes[1].getErasure().getBinaryName()));
	}

	private static class MethodParams {

		private final String bind;

		private final IMethodBinding bindMethod;

		private final String updated;

		private final IMethodBinding updatedMethod;

		private final String unbind;

		private final IMethodBinding unbindMethod;

		public MethodParams(String bind, IMethodBinding bindMethod, String updated, IMethodBinding updatedMethod,
				String unbind, IMethodBinding unbindMethod) {
			super();
			this.bind = bind;
			this.bindMethod = bindMethod;
			this.updated = updated;
			this.updatedMethod = updatedMethod;
			this.unbind = unbind;
			this.unbindMethod = unbindMethod;
		}

		public String getBind() {
			return bind;
		}

		public IMethodBinding getBindMethod() {
			return bindMethod;
		}

		public String getUpdated() {
			return updated;
		}

		public IMethodBinding getUpdatedMethod() {
			return updatedMethod;
		}

		public String getUnbind() {
			return unbind;
		}

		public IMethodBinding getUnbindMethod() {
			return unbindMethod;
		}

	}
}
