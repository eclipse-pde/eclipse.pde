/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.pde.ds.internal.annotations.messages"; //$NON-NLS-1$

	public static String AnnotationProcessor_duplicateActivateMethod;

	public static String AnnotationProcessor_duplicateDeactivateMethod;

	public static String AnnotationProcessor_duplicateLifeCycleMethodParameterType;

	public static String AnnotationProcessor_duplicateModifiedMethod;

	public static String AnnotationProcessor_duplicateReferenceName;

	public static String AnnotationProcessor_duplicateServiceDeclaration;

	public static String AnnotationProcessor_inconsistentComponentPropertyType;

	public static String AnnotationProcessor_invalidReference_invalidBindMethodParameters;

	public static String AnnotationProcessor_invalidReference_invalidBindMethodReturnType;

	public static String AnnotationProcessor_invalidCompImplClass_abstract;

	public static String AnnotationProcessor_invalidCompImplClass_annotation;

	public static String AnnotationProcessor_invalidCompImplClass_enumeration;

	public static String AnnotationProcessor_invalidCompImplClass_interface;

	public static String AnnotationProcessor_invalidCompImplClass_noDefaultConstructor;

	public static String AnnotationProcessor_invalidCompImplClass_notPublic;

	public static String AnnotationProcessor_invalidCompImplClass_notTopLevel;

	public static String AnnotationProcessor_invalidComponentConfigurationPid;

	public static String AnnotationProcessor_invalidComponentDescriptorNamespace;

	public static String AnnotationProcessor_invalidComponentFactoryName;

	public static String AnnotationProcessor_invalidComponentImplementationClass;

	public static String AnnotationProcessor_invalidComponentName;

	public static String AnnotationProcessor_invalidComponentProperty_nameRequired;

	public static String AnnotationProcessor_invalidComponentProperty_valueRequired;

	public static String AnnotationProcessor_invalidComponentPropertyFile;

	public static String AnnotationProcessor_invalidComponentPropertyType;

	public static String AnnotationProcessor_invalidComponentPropertyValue;

	public static String AnnotationProcessor_invalidComponentService;

	public static String AnnotationProcessor_invalidLifecycleMethod_static;

	public static String AnnotationProcessor_invalidLifeCycleMethodParameterType;

	public static String AnnotationProcessor_invalidLifeCycleMethodReturnType;

	public static String AnnotationProcessor_invalidReference_bindMethodNameMismatch;

	public static String AnnotationProcessor_invalidReference_bindMethodNoArgs;

	public static String AnnotationProcessor_invalidReference_bindMethodNotFound;

	public static String AnnotationProcessor_invalidReference_fieldCardinalityPolicyCollectionType;

	public static String AnnotationProcessor_invalidReference_fieldCollection_fieldOption;

	public static String AnnotationProcessor_invalidReference_fieldFinal_fieldOption;

	public static String AnnotationProcessor_invalidReference_fieldNameMismatch;

	public static String AnnotationProcessor_invalidReference_fieldNotFound;

	public static String AnnotationProcessor_invalidReference_fieldOptionNotApplicable;

	public static String AnnotationProcessor_invalidReference_fieldPolicyCardinality_fieldOption;

	public static String AnnotationProcessor_invalidReference_fieldTypeCardinalityMismatch;

	public static String AnnotationProcessor_invalidReference_fieldUnknownServiceType;

	public static String AnnotationProcessor_invalidReference_incompatibleFieldType;

	public static String AnnotationProcessor_invalidReference_incompatibleServiceType;

	public static String AnnotationProcessor_invalidReference_invalidBindMethodArg;

	public static String AnnotationProcessor_invalidReference_missingRequiredParam;

	public static String AnnotationProcessor_invalidReference_staticBindMethod;

	public static String AnnotationProcessor_invalidReference_staticField;

	public static String AnnotationProcessor_invalidReference_serviceType;

	public static String AnnotationProcessor_invalidReference_serviceUnknown;

	public static String AnnotationProcessor_invalidReference_unbindMethod;

	public static String AnnotationProcessor_invalidReference_updatedMethod;

	public static String AnnotationProcessor_invalidReference_noImplicitUnbind;

	public static String AnnotationProcessor_stringOrEmpty;

	public static String AnnotationProcessor_unknownServiceTypeLabel;

	public static String AnnotationVisitor_invalidComponentConfigurationPid_duplicate;

	public static String AnnotationVisitor_invalidFactoryComponent_immediate;

	public static String AnnotationVisitor_invalidDelayedComponent_noServices;

	public static String AnnotationVisitor_invalidScope_factoryImmediate;

	public static String AnnotationVisitor_invalidScope_noServices;

	public static String AnnotationVisitor_invalidServiceFactory_factoryImmediate;

	public static String AnnotationVisitor_invalidServiceFactory_ignored;

	public static String AnnotationVisitor_invalidServiceFactory_noServices;

	public static String BuildPathMarkerResolutionGenerator_additionalBundleResolution_description;

	public static String BuildPathMarkerResolutionGenerator_additionalBundleResolution_label;

	public static String BuildPathMarkerResolutionGenerator_extraLibraryResolution_description;

	public static String BuildPathMarkerResolutionGenerator_extraLibraryResolution_label;

	public static String BuildPathMarkerResolutionGenerator_packageImportResolution_description;

	public static String BuildPathMarkerResolutionGenerator_packageImportResolution_label;

	public static String ComponentMoveParticipant_name;

	public static String ComponentRefactoringHelper_checkConditionsTaskLabel;

	public static String ComponentRefactoringHelper_createChangeTaskLabel;

	public static String ComponentRefactoringHelper_topLevelChangeLabel;

	public static String ComponentRenameParticipant_name;

	public static String DSAnnotationCompilationParticipant_buildpathProblemMarker_location;

	public static String DSAnnotationCompilationParticipant_buildpathProblemMarker_message;

	public static String DSAnnotationPreferenceListener_jobName;

	public static String DSAnnotationPreferenceListener_taskName;

	public static String DSAnnotationPropertyPage_classpathCheckbox_text;

	public static String DSAnnotationPropertyPage_enableCheckbox_text;

	public static String DSAnnotationPropertyPage_errorLevelError;

	public static String DSAnnotationPropertyPage_errorLevelLabel_text;

	public static String DSAnnotationPropertyPage_errorLevelIgnore;

	public static String DSAnnotationPropertyPage_errorLevelWarning;

	public static String DSAnnotationPropertyPage_errorMessage_path;

	public static String DSAnnotationPropertyPage_missingUnbindMethodLevelLabel_text;

	public static String DSAnnotationPropertyPage_pathLabel_text;

	public static String DSAnnotationPropertyPage_projectCheckbox_text;

	public static String DSAnnotationPropertyPage_workspaceLink_text;

	public static String DSAnnotationPropertyPage_enableBAPLGenerationLabel_text;

	public static String DSAnnotationPropertyPage_specVersionLabel_text;

	public static String ProjectClasspathPreferenceChangeListener_jobName;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
