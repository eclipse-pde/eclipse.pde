/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.IVersionRange;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.SinceTagVersion;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import com.ibm.icu.text.MessageFormat;

/**
 * Base implementation of the analyzer used in the {@link ApiAnalysisBuilder}
 * 
 * @since 1.0.0
 */
public class BaseApiAnalyzer implements IApiAnalyzer {
	private static final String QUALIFIER = "qualifier"; //$NON-NLS-1$
	/**
	 * @since 1.1
	 */
	static final String[] NO_TYPES = new String[0];
	private static class ReexportedBundleVersionInfo {
		String componentID;
		int kind;
		
		ReexportedBundleVersionInfo(String componentID, int kind) {
			this.componentID = componentID;
			this.kind = kind;
		}
	}
	
	/**
	 * The backing list of problems found so far
	 */
	private ArrayList fProblems = new ArrayList(25);
	
	/**
	 * List of pending deltas for which the @since tags should be checked
	 */
	private List fPendingDeltaInfos = new ArrayList(3);
		
	/**
	 * The current build state to use
	 */
	private BuildState fBuildState = null;
	/**
	 * The current filter store to use
	 */
	private IApiFilterStore fFilterStore = null;
	/**
	 * The associated {@link IJavaProject}, if there is one
	 */
	private IJavaProject fJavaProject = null;
	/**
	 * The current preferences to use when the platform is not running.
	 */
	private Properties fPreferences = null;
	
	/**
	 * Constructs an API analyzer
	 */
	public BaseApiAnalyzer() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer#analyzeComponent(..)
	 */
	public void analyzeComponent(
			final BuildState state,
			final IApiFilterStore filterStore,
			final Properties preferences,
			final IApiBaseline baseline,
			final IApiComponent component,
			final IBuildContext context,
			IProgressMonitor monitor) {
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_analyzing_api, 8);
		try {
			fJavaProject = getJavaProject(component);
			this.fFilterStore = filterStore;
			this.fPreferences = preferences;
			if(!ignoreUnusedProblemFilterCheck()) {
				((ApiFilterStore)component.getFilterStore()).recordFilterUsage();
			}
			ResolverError[] errors = component.getErrors();
			if (errors != null) {
				// check if all errors have a constraint
				StringBuffer buffer = null;
				for (int i = 0, max = errors.length; i < max; i++) {
					ResolverError error = errors[i];
					VersionConstraint constraint = error.getUnsatisfiedConstraint();
					if (constraint == null) continue;
					VersionRange versionRange = constraint.getVersionRange();
					String minimum = versionRange == null ? BuilderMessages.undefinedRange : versionRange.getMinimum().toString();
					String maximum = versionRange == null ? BuilderMessages.undefinedRange : versionRange.getMaximum().toString();
					if (buffer == null) {
						buffer = new StringBuffer();
					}
					if (i > 0) {
						buffer.append(',');
					}
					buffer.append(
						NLS.bind(
								BuilderMessages.reportUnsatisfiedConstraint,
								new String[] {
										constraint.getName(),
										minimum,
										maximum
								}
						));
				}
				if (buffer != null) {
					// API component has errors that should be reported
					createApiComponentResolutionProblem(component, String.valueOf(buffer));
					if (baseline == null) {
						checkDefaultBaselineSet();
					}
					return;
				}
			}
			IBuildContext bcontext = context;
			if(bcontext == null) {
				bcontext = new BuildContext();
			}
			boolean checkfilters = false;
			if(baseline != null) {
				IApiComponent reference = baseline.getApiComponent(component.getSymbolicName());
				this.fBuildState = state;
				if(fBuildState == null) {
					fBuildState = getBuildState();
				}
				//compatibility checks
				if(reference != null) {
					localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_comparing_api_profiles, new String[] {reference.getSymbolicName(), baseline.getName()}));
					if(bcontext.hasTypes()) {
						String[] changedtypes = bcontext.getStructurallyChangedTypes();
						checkCompatibility(changedtypes, reference, component, localMonitor);
					} else {
						// store re-exported bundle into the build state
						checkCompatibility(reference, component, localMonitor.newChild(1));
						Util.updateMonitor(localMonitor);
					}
					this.fBuildState.setReexportedComponents(Util.getReexportedComponents(component));
				} else {
					localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_comparing_api_profiles, new String[] {component.getSymbolicName(), baseline.getName()}));
					checkCompatibility(null, component, localMonitor.newChild(1));
					Util.updateMonitor(localMonitor);
				}
				//version checks
				checkApiComponentVersion(reference, component);
				Util.updateMonitor(localMonitor);
				checkfilters = true;
			}
			else {
				//check default baseline
				checkDefaultBaselineSet();
				Util.updateMonitor(localMonitor);
			}
			
			//check E description status
			checkEEDescriptions();
			
			//usage checks
			checkApiUsage(bcontext, component, localMonitor.newChild(1));
			Util.updateMonitor(localMonitor);
			//tag validation
			checkTagValidation(bcontext, component, localMonitor.newChild(1));
			Util.updateMonitor(localMonitor);
			if(checkfilters) {
				//check for unused filters only if the scans have been done
				checkUnusedProblemFilters(bcontext, component, localMonitor.newChild(1));
			}
			Util.updateMonitor(localMonitor);
			
			if (component instanceof ProjectComponent) {
				checkExternalDependencies(component, bcontext, null, localMonitor.newChild(1));
			}
		} catch(CoreException e) {
			ApiPlugin.log(e);
		}
		catch(OperationCanceledException oce) {
			//do nothing, but don't forward it
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=304315
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Trapped OperationCanceledException"); //$NON-NLS-1$
			}
		}
		finally {
			localMonitor.done();
		}
	}

	/**
	 * Checks if the setting to scan for invalid references is not set to be ignored AND there are no descriptions installed
	 * 
	 * @param component
	 * @param monitor
	 * @since 1.0.400
	 */
	void checkEEDescriptions() {
		if(ignoreEEDescriptionCheck()) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Ignoring check for API EE descriptions"); //$NON-NLS-1$
			}
			return;
		}
		if(ApiPlugin.DEBUG_API_ANALYZER) {
			System.out.println("Checking if there are any API EE descriptions installed if the preference is set to not be 'ignore'"); //$NON-NLS-1$
		}
		String[] ees = StubApiComponent.getInstalledMetadata();
		if(ees.length < 1) {
			IApiProblem problem = ApiProblemFactory.newApiUsageProblem(
					Path.EMPTY.toString(), 
					null, 
					new String[] {fJavaProject.getElementName()}, 
					new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID},
					new Object[] {new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
					-1, 
					-1, 
					-1, 
					IElementDescriptor.RESOURCE, 
					IApiProblem.MISSING_EE_DESCRIPTIONS);
			addProblem(problem);
		}
	}
	
	/**
	 * @return if the API EE description check should be ignored or not
	 */
	private boolean ignoreEEDescriptionCheck() {
		if(fJavaProject == null) {
			return true;
		}
		return ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, fJavaProject.getProject().getProject()) == ApiPlugin.SEVERITY_IGNORE;
	}
	
	/**
	 * Processes the API Use Scan report for the given API Component
	 * 
	 * @param apiComponent
	 * @param bcontext
	 * @param monitor
	 * @throws CoreException
	 */
	public void checkExternalDependencies(IApiComponent apiComponent, IBuildContext bcontext, Properties properties, IProgressMonitor monitor) throws CoreException {
		if (!isSeverityEnabled(properties)) {
			return;
		}
		String[] apiUseTypes = getApiUseTypes(bcontext);
		if (ApiPlugin.DEBUG_API_ANALYZER) {
			if(apiUseTypes.length < 1) {
				System.out.println("Checking use scan dependencies for: "+apiComponent.getSymbolicName()+" ("+apiComponent.getVersion()+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			else {
				System.out.println("Checking use scan dependencies for: " + Arrays.asList(apiUseTypes)); //$NON-NLS-1$
			}
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.checking_external_dependencies, 10);
		IReferenceDescriptor[] externalDependencies  = UseScanManager.getInstance().getExternalDependenciesFor(apiComponent, apiUseTypes, localmonitor.newChild(10));
		try {
			if (externalDependencies != null) {
				localmonitor.setWorkRemaining(externalDependencies.length);
				HashMap problems = new HashMap();
				for (int i = 0; i < externalDependencies.length; i++) {		
					Util.updateMonitor(localmonitor, 1);
					Reference externalReference = null;
					IApiTypeRoot type = null;
					IMemberDescriptor referencedMember = externalDependencies[i].getReferencedMember();
					IReferenceTypeDescriptor referenceMemberType = referencedMember.getEnclosingType();
					if (referenceMemberType != null) {
						type = apiComponent.findTypeRoot(referenceMemberType.getQualifiedName());
					}
					switch (referencedMember.getElementType()) {
						case IElementDescriptor.TYPE :
							referenceMemberType = (IReferenceTypeDescriptor) referencedMember;
							type = apiComponent.findTypeRoot(referenceMemberType.getQualifiedName());
							if (type != null) {
								externalReference = Reference.typeReference(type.getStructure(), referenceMemberType.getQualifiedName(), externalDependencies[i].getReferenceKind());
							}
							break;
						case IElementDescriptor.METHOD :
							if (type != null) {
								externalReference = Reference.methodReference(type.getStructure(), referenceMemberType.getQualifiedName(), referencedMember.getName(), ((IMethodDescriptor) referencedMember).getSignature(), externalDependencies[i].getReferenceKind());
							}
							break;
						case IElementDescriptor.FIELD :
							if (type != null) {
								externalReference = Reference.fieldReference(type.getStructure(), referenceMemberType.getQualifiedName(), referencedMember.getName(), externalDependencies[i].getReferenceKind());
							}
							break;
					}
					if (type == null) {
						createExternalDependenciesProblem(problems, externalDependencies[i], referenceMemberType.getQualifiedName(), referencedMember, externalDependencies[i].getReferencedMember().getElementType(), 0);
					} else {
						externalReference.resolve();
						if (externalReference.getResolvedReference() == null) {
							createExternalDependenciesProblem(problems, externalDependencies[i], referenceMemberType.getQualifiedName(), referencedMember, externalDependencies[i].getReferencedMember().getElementType(), 1);
						}
					}				
				}
				for (Iterator iterator = problems.values().iterator(); iterator.hasNext();) {
					IApiProblem apiProblem = (IApiProblem) iterator.next();
					addProblem(apiProblem);
				}
			}
		}
		finally {
			localmonitor.done();
		}
	}

	public boolean isSeverityEnabled(Properties properties) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		if (properties == null) {
			if (!isIgnore(node.get(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY, ApiPlugin.VALUE_IGNORE)))
				return true;
			if (!isIgnore(node.get(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY, ApiPlugin.VALUE_IGNORE)))
				return true;
			if (isIgnore(node.get(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY, ApiPlugin.VALUE_IGNORE)))
				return true;
			return false;
		} else {
			if (properties.isEmpty())
				return true; // preferences parameter not provided
			if (!isIgnore(properties.get(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY))) 
				return true;
			if (!isIgnore(properties.get(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY))) 
				return true;
			if (!isIgnore(properties.get(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY))) 
				return true;
			return false;
		}
	}

	private boolean isIgnore(Object value) {
		if (value != null && (value.toString().equalsIgnoreCase(ApiPlugin.VALUE_ERROR) || value.toString().equalsIgnoreCase(ApiPlugin.VALUE_WARNING))) {
			return false;
		}
		return true;
	}
	
	/**
	 * Creates an {@link IApiProblem} for the broken external dependency 
	 * 
	 * @param problems
	 * @param dependency
	 * @param referenceType
	 * @param referencedMember
	 * @param elementType
	 * @param flag
	 * @return
	 */
	protected IApiProblem createExternalDependenciesProblem(HashMap problems, IReferenceDescriptor dependency, String referenceTypeName, IMemberDescriptor referencedMember, int elementType, int flag) {	
		String resource = referenceTypeName;
		String primaryTypeName = referenceTypeName.replace('$', '.');		
		int charStart = -1, charEnd = -1, lineNumber = -1; 
		if (fJavaProject != null) {
			try {
				
				IType type = fJavaProject.findType(primaryTypeName);
				IResource res = Util.getResource(fJavaProject.getProject(), type);
				if(res == null) {
					return null;
				}
				if(!Util.isManifest(res.getProjectRelativePath())) {
					resource = res.getProjectRelativePath().toString();
				}
				else {
					resource = "."; //$NON-NLS-1$
				}
				if (type != null) {
					ISourceRange range = type.getNameRange();
					charStart = range.getOffset();
					charEnd = charStart + range.getLength();
					try {
						IDocument document = Util.getDocument(type.getCompilationUnit());
						lineNumber = document.getLineOfOffset(charStart);
					} catch (BadLocationException e) {
						// ignore
					}			
					catch (CoreException ce) {}
				}
			} catch (JavaModelException e) {}
		}
		String[] msgArgs = new String[] {referenceTypeName, referencedMember.getName(), dependency.getComponent().getId()};
		int kind = 0;
		switch (elementType) {
			case IElementDescriptor.TYPE :
				kind = IApiProblem.API_USE_SCAN_TYPE_PROBLEM;				
				break;
			case IElementDescriptor.METHOD :
				kind = IApiProblem.API_USE_SCAN_METHOD_PROBLEM;
				msgArgs[1] = BuilderMessages.BaseApiAnalyzer_Method + ' ' + msgArgs[1];
				if ((dependency.getReferenceKind() & IReference.REF_CONSTRUCTORMETHOD) > 0) {
					msgArgs[1] = BuilderMessages.BaseApiAnalyzer_Constructor + ' ' + msgArgs[1];
				}
				break;
			case IElementDescriptor.FIELD :
				kind = IApiProblem.API_USE_SCAN_FIELD_PROBLEM;
				break;
		}
		
		int dependencyNameIndex = 2;	// the comma separated list of dependent plugins 
		int problemId = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM, elementType, kind, flag);
		String problemKey = referenceTypeName +  problemId;
		IApiProblem similarProblem =  (IApiProblem) problems.get(problemKey);
		if (similarProblem != null) {
			String[] existingMsgArgs = similarProblem.getMessageArguments()[dependencyNameIndex].split(", "); //$NON-NLS-1$
			if (!Arrays.asList(existingMsgArgs).contains(msgArgs[dependencyNameIndex])) {
				msgArgs[dependencyNameIndex] = similarProblem.getMessageArguments()[dependencyNameIndex] + ',' + ' ' + msgArgs[dependencyNameIndex];
			} else {
				return similarProblem;
			}
		}
		IApiProblem problem = ApiProblemFactory.newApiUseScanProblem(
				resource, 
				primaryTypeName, 
				msgArgs, 
				new String[] {IApiMarkerConstants.API_USESCAN_TYPE}, 
				new String[] {primaryTypeName },
				lineNumber,
				charStart,
				charEnd,
				elementType, 
				kind, 
				flag);
		problems.put(problemKey, problem);
		return problem;		
	}
	
	/**
	 * Checks the compatibility of each type.
	 * 
	 * @param changedtypes type names, may have <code>null</code> entries
	 * @param reference API component in the reference baseline
	 * @param component API component being checked for compatibility
	 * @param localMonitor
	 * @throws CoreException
	 */
	private void checkCompatibility(String[] changedtypes, IApiComponent reference, IApiComponent component, SubMonitor localMonitor) throws CoreException {
		for(int i = 0; i < changedtypes.length; i++) {
			if(changedtypes[i] == null) {
				continue;
			}
			checkCompatibility(changedtypes[i], reference, component, localMonitor.newChild(1));
			Util.updateMonitor(localMonitor);
		}
	}
	/**
	 * Checks for unused API problem filters
	 * @param context the current build context
	 * @param reference
	 * @param monitor
	 */
	private void checkUnusedProblemFilters(final IBuildContext context, IApiComponent reference, IProgressMonitor monitor) {
		if(ignoreUnusedProblemFilterCheck()) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Ignoring unused problem filter check"); //$NON-NLS-1$
			}
			Util.updateMonitor(monitor, 1);
			return;
		}
		try {
			ApiFilterStore store = (ApiFilterStore)reference.getFilterStore();
			IProject project = fJavaProject.getProject();
			boolean autoremove = ApiPlugin.getDefault().getEnableState(IApiProblemTypes.AUTOMATICALLY_REMOVE_UNUSED_PROBLEM_FILTERS, project);
			ArrayList toremove = null;
			if(autoremove) {
				toremove = new ArrayList(8);
			}
			IApiProblemFilter[] filters = null;
			if(context.hasTypes()) {
				IResource resource = null;
				String[] types = getApiUseTypes(context);
				for (int i = 0; i < types.length; i++) {
					if(types[i] == null) {
						continue;
					}
					resource = Util.getResource(project, fJavaProject.findType(Signatures.getPrimaryTypeName(types[i])));
					if(resource != null) {
						filters = store.getUnusedFilters(resource, types[i], null);
						if(autoremove) {
							addToList(toremove, filters);
							continue;
						}
						createUnusedApiFilterProblems(filters);
					}
				}
				/*types = context.getStructuralDependentTypes();
				for (int i = 0; i < types.length; i++) {
					if(types[i] == null) {
						continue;
					}
					resource = Util.getResource(project, fJavaProject.findType(Signatures.getPrimaryTypeName(types[i])));
					if(resource != null) {
						filters = store.getUnusedFilters(
								resource, 
								types[i], 
								new int[] {IApiProblem.CATEGORY_COMPATIBILITY, IApiProblem.CATEGORY_SINCETAGS, IApiProblem.CATEGORY_VERSION});
						if(autoremove) {
							addToList(toremove, filters);
							continue;
						}
						createUnusedApiFilterProblems(filters);
					}
				}*/
				if(autoremove) {
					removeUnusedProblemFilters(store, toremove, monitor);
				}
			} else {
				filters = store.getUnusedFilters(null, null, null);
				if(autoremove) {
					addToList(toremove, filters);
					removeUnusedProblemFilters(store, toremove, monitor);
				}
				else {
					//full build, clean up all old markers
					createUnusedApiFilterProblems(filters);
				}
			}
		}
		catch(CoreException ce) {
			//ignore, just don't create problems
		}
		finally {
			Util.updateMonitor(monitor, 1);
		}
	}
	
	/**
	 * Adds all of the non-null elements from the given array to the given list
	 * @param list
	 * @param array
	 * @since 1.1
	 */
	void addToList(List list, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			if(array[i] != null) {
				list.add(array[i]);
			}
		}
	}
	
	/**
	 * Removes the given set of {@link IApiProblemFilter}s from the given {@link IApiFilterStore}
	 * using a workspace runnable to avoid resource notifications
	 * @param store the store to remove from
	 * @param filterlist list of filters to batch remove
	 * @param monitor 
	 * @throws CoreException
	 * @since 1.1
	 */
	void removeUnusedProblemFilters(final IApiFilterStore store, final List filterlist, final IProgressMonitor monitor) throws CoreException {
		if(filterlist.size() > 0) {
			IWorkspaceRunnable runner = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					store.removeFilters((IApiProblemFilter[]) filterlist.toArray(new IApiProblemFilter[filterlist.size()]));
				}
			};
			ResourcesPlugin.getWorkspace().run(runner, null, IWorkspace.AVOID_UPDATE, monitor);
		}
	}
	
	/**
	 * Creates a new unused {@link IApiProblemFilter} problem
	 * @param filters the filters to create the problems for
	 * @return a new {@link IApiProblem} for unused problem filters or <code>null</code>
	 */
	private void createUnusedApiFilterProblems(IApiProblemFilter[] filters) {
		if(fJavaProject == null) {
			return;
		}
		IApiProblemFilter filter = null;
		IApiProblem problem = null;
		for (int i = 0; i < filters.length; i++) {
			filter = filters[i];
			problem = filter.getUnderlyingProblem();
			if(problem == null) {
				return;
			}
			IResource resource = null;
			IType type = null;
			// retrieve line number, char start and char end
			int lineNumber = 0;
			int charStart = -1;
			int charEnd = 1;
			if (fJavaProject != null) {
				try {
					String typeName = problem.getTypeName();
					if (typeName != null) {
						type = fJavaProject.findType(typeName.replace('$', '.'));
					}
					IProject project = fJavaProject.getProject();
					resource = Util.getResource(project, type);
					if (resource == null) {
						return;
					}
					if (!Util.isManifest(resource.getProjectRelativePath()) && !type.isBinary()) {
						ISourceRange range = type.getNameRange();
						charStart = range.getOffset();
						charEnd = charStart + range.getLength();
						try {
							IDocument document = Util.getDocument(type.getCompilationUnit());
							lineNumber = document.getLineOfOffset(charStart);
						} catch (BadLocationException e) {
							// ignore
						}
					}
				} catch (JavaModelException e) {
					ApiPlugin.log(e);
				} catch(CoreException e) {
					ApiPlugin.log(e);
				}
			}
			String path = null;
			if (resource != null) {
				path = resource.getProjectRelativePath().toPortableString();
			}
			addProblem(ApiProblemFactory.newApiUsageProblem(path,
					problem.getTypeName(),
					new String[] {filter.getUnderlyingProblem().getMessage()}, //message args
					new String[] {IApiMarkerConstants.MARKER_ATTR_FILTER_HANDLE_ID, IApiMarkerConstants.API_MARKER_ATTR_ID},
					new Object[] {((ApiProblemFilter)filter).getHandle(), new Integer(IApiMarkerConstants.UNUSED_PROBLEM_FILTER_MARKER_ID)},
					lineNumber,
					charStart,
					charEnd,
					problem.getElementKind(),
					IApiProblem.UNUSED_PROBLEM_FILTERS));
		}
	}
	
	/**
	 * Check the version changes of re-exported bundles to make sure that the given component
	 * version is modified accordingly.
	 * 
	 * @param reference the given reference API component
	 * @param component the given component
	 */
	private ReexportedBundleVersionInfo checkBundleVersionsOfReexportedBundles(
			IApiComponent reference, IApiComponent component) throws CoreException {
		IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
		int length = requiredComponents.length;
		ReexportedBundleVersionInfo info = null;
		if (length != 0) {
			loop: for (int i = 0; i < length; i++) {
				IRequiredComponentDescription description = requiredComponents[i];
				if (description.isExported()) {
					// get the corresponding IRequiredComponentDescription for the component from the reference baseline
					String id = description.getId();
					IRequiredComponentDescription[] requiredComponents2 = reference.getRequiredComponents();
					// get the corresponding exported bundle
					IRequiredComponentDescription referenceDescription = null;
					int length2 = requiredComponents2.length;
					loop2: for (int j = 0; j < length2; j++) {
						IRequiredComponentDescription description2 = requiredComponents2[j];
						if (description2.getId().equals(id)) {
							if (description2.isExported()) {
								referenceDescription = description2;
								break loop2;
							}
						}
					}
					if (referenceDescription == null) {
						continue loop;
					}
					IVersionRange versionRange = description.getVersionRange();
					IVersionRange versionRange2 = referenceDescription.getVersionRange();
					
					Version currentLowerBound = new Version(versionRange.getMinimumVersion());
					Version referenceLowerBound = new Version(versionRange2.getMinimumVersion());
					
					int currentLowerMajorVersion = currentLowerBound.getMajor();
					int referenceLowerMajorVersion = referenceLowerBound.getMajor();
					int currentLowerMinorVersion = currentLowerBound.getMinor();
					int referenceLowerMinorVersion = referenceLowerBound.getMinor();
					
					if (currentLowerMajorVersion < referenceLowerMajorVersion
							|| currentLowerMinorVersion < referenceLowerMinorVersion) {
						return new ReexportedBundleVersionInfo(id , IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE);
					}
					
					if (currentLowerMajorVersion > referenceLowerMajorVersion) {
						return new ReexportedBundleVersionInfo(id , IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE);
					}
					if (currentLowerMinorVersion > referenceLowerMinorVersion) {
						info = new ReexportedBundleVersionInfo(id , IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE);
					}
				}
			}
		}
		return info;
	}

	/**
	 * Creates and AST for the given {@link ITypeRoot} at the given offset
	 * @param root
	 * @param offset
	 * @return
	 */
	private CompilationUnit createAST(ITypeRoot root, int offset) {
		if(fJavaProject == null) {
			return null;
		}
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setFocalPosition(offset);
		parser.setResolveBindings(false);
		parser.setSource(root);
		Map options = fJavaProject.getOptions(true);
		options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(options);
		return (CompilationUnit) parser.createAST(new NullProgressMonitor());
	}
	
	/**
	 * @return the build state to use.
	 */
	private BuildState getBuildState() {
		IProject project = null;
		if (fJavaProject != null) {
			project = fJavaProject.getProject();
		}
		if(project == null) {
			return new BuildState();
		}
		try {
			BuildState state = BuildState.getLastBuiltState(project);
			if(state != null) {
				return state;
			}
		} 
		catch (CoreException e) {}
		return new BuildState();
	}
	
	/**
	 * Returns an {@link IApiTypeContainer} given the component and type names context
	 * @param component
	 * @param types
	 * @return a new {@link IApiTypeContainer} for the component and type names context
	 */
	private IApiTypeContainer getSearchScope(final IApiComponent component, final String[] typenames) {
		if(typenames == null) {
			return component;
		}
		if(typenames.length == 0) {
			return component;
		}
		else {
			return Factory.newTypeScope(component, getScopedElements(typenames));
		}
	}
	
	/**
	 * Returns a listing of {@link IReferenceTypeDescriptor}s given the listing of type names
	 * @param typenames
	 * @return
	 */
	private IReferenceTypeDescriptor[] getScopedElements(final String[] typenames) {
		ArrayList types = new ArrayList(typenames.length);
		for(int i = 0; i < typenames.length; i++) {
			if(typenames[i] == null) {
				continue;
			}
			types.add(Util.getType(typenames[i]));
		}
		return (IReferenceTypeDescriptor[]) types.toArray(new IReferenceTypeDescriptor[types.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer#getProblems()
	 */
	public IApiProblem[] getProblems() {
		if(fProblems == null) {
			return new IApiProblem[0];
		}
		return (IApiProblem[]) fProblems.toArray(new IApiProblem[fProblems.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer#dispose()
	 */
	public void dispose() {
		if(fProblems != null) {
			fProblems.clear();
			fProblems = null;
		}
		if(fPendingDeltaInfos != null) {
			fPendingDeltaInfos.clear();
			fPendingDeltaInfos = null;
		}
		if(fBuildState != null) {
			fBuildState = null;
		}
	}
	
	/**
	 * @return if the API usage scan should be ignored
	 */
	private boolean ignoreApiUsageScan() {
		if (fJavaProject == null) {
			// do the API use scan for binary bundles in non-OSGi mode
			return false;
		}
		IProject project = fJavaProject.getProject();
		boolean ignore = true;
		ApiPlugin plugin = ApiPlugin.getDefault();
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.ILLEGAL_EXTEND, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.ILLEGAL_IMPLEMENT, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.ILLEGAL_INSTANTIATE, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.ILLEGAL_REFERENCE, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.ILLEGAL_OVERRIDE, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_EXTEND, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_FIELD_DECL, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_IMPLEMENT, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_METHOD_PARAM, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, project) == ApiPlugin.SEVERITY_IGNORE;
		return ignore;
	}
	/**
	 * @return if the API usage scan should be ignored
	 */
	private boolean reportApiBreakageWhenMajorVersionIncremented() {
		if (fJavaProject == null) {
			// we ignore it for non-OSGi case
			return false;
		}
		return ApiPlugin.getDefault().getEnableState(IApiProblemTypes.REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED, fJavaProject.getProject().getProject());
	}
	
	/**
	 * @return if the default API baseline check should be ignored or not
	 */
	private boolean ignoreDefaultBaselineCheck() {
		if(fJavaProject == null) {
			return true;
		}
		return ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, fJavaProject.getProject().getProject()) == ApiPlugin.SEVERITY_IGNORE;
	}
	/**
	 * Whether to ignore since tag checks. If <code>null</code> is passed in we are asking if all since tag checks should be ignored,
	 * if a pref is specified we only want to know if that kind should be ignored
	 * @param pref
	 * @return
	 */
	private boolean ignoreSinceTagCheck(String pref) {
		if (fJavaProject == null) {
			return true;
		}
		IProject project = fJavaProject.getProject();
		ApiPlugin plugin = ApiPlugin.getDefault();
		if(pref == null) {
			boolean ignore = plugin.getSeverityLevel(IApiProblemTypes.MALFORMED_SINCE_TAG, project) == ApiPlugin.SEVERITY_IGNORE;
			ignore &= plugin.getSeverityLevel(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, project) == ApiPlugin.SEVERITY_IGNORE;
			ignore &= plugin.getSeverityLevel(IApiProblemTypes.MISSING_SINCE_TAG, project) == ApiPlugin.SEVERITY_IGNORE;
			return ignore;
		}
		else {
			return plugin.getSeverityLevel(pref, project) == ApiPlugin.SEVERITY_IGNORE;
		}
	}
	
	/**
	 * @return if the component version checks should be ignored or not
	 */
	private boolean ignoreComponentVersionCheck() {
		if (fJavaProject == null) {
			// still do version checks for non-OSGi case
			return false;
		}
		return ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, fJavaProject.getProject().getProject()) == ApiPlugin.SEVERITY_IGNORE;
	}
	private boolean ignoreMinorVersionCheckWithoutApiChange() {
		if (fJavaProject == null) {
			// we ignore it for non-OSGi case
			return true;
		}
		return !ApiPlugin.getDefault().getEnableState(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, fJavaProject.getProject().getProject());
	}
	private boolean ignoreMajorVersionCheckWithoutBreakingChange() {
		if (fJavaProject == null) {
			// we ignore it for non-OSGi case
			return true;
		}
		return !ApiPlugin.getDefault().getEnableState(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, fJavaProject.getProject().getProject());
	}
	/**
	 * @return if the invalid tag check should be ignored
	 */
	private boolean ignoreInvalidTagCheck() {
		if(fJavaProject == null) {
			return true;
		}
		return ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.INVALID_JAVADOC_TAG, fJavaProject.getProject()) == ApiPlugin.SEVERITY_IGNORE;
	}
	/**
	 * @return if the unused problem filter check should be ignored or not
	 */
	private boolean ignoreUnusedProblemFilterCheck() {
		if(fJavaProject == null) {
			return true;
		}
		return ApiPlugin.getDefault().getSeverityLevel(IApiProblemTypes.UNUSED_PROBLEM_FILTERS, fJavaProject.getProject()) == ApiPlugin.SEVERITY_IGNORE;
	}
	/**
	 * Checks the validation of tags for the given {@link IApiComponent}
	 * @param context
	 * @param component
	 * @param monitor
	 */
	private void checkTagValidation(final IBuildContext context, final IApiComponent component, IProgressMonitor monitor) {
		if(ignoreInvalidTagCheck()) {
			return;
		}
		SubMonitor localMonitor = null;
		try {
			
			localMonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_validating_javadoc_tags, 1 + component.getApiTypeContainers().length);
			if(context.hasTypes()) {
				String[] typenames = context.getStructurallyChangedTypes();
				for(int i = 0; i < typenames.length; i++) {
					if(typenames[i] == null) {
						continue;
					}
					localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_scanning_0, typenames[i]));
					processType(typenames[i]);
					Util.updateMonitor(localMonitor);
				}
			}
			else {
				try {
					IPackageFragmentRoot[] roots = fJavaProject.getPackageFragmentRoots();
					for(int i = 0; i < roots.length; i++) {
						if(roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
							localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_scanning_0, roots[i].getPath().toOSString()));
							scanSource(roots[i], localMonitor.newChild(1));
							Util.updateMonitor(localMonitor);
						}
					}
				}
				catch(JavaModelException jme) {
					ApiPlugin.log(jme);
				}
			}
			Util.updateMonitor(localMonitor);
		} catch(CoreException e) {
			ApiPlugin.log(e);
		}
		finally {
			if(localMonitor != null) {
				localMonitor.done();
			}
		}
	}
	
	/**
	 * Recursively finds all source in the given project and scans it for invalid tags 
	 * @param element
	 * @param monitor
	 * @throws JavaModelException
	 */
	private void scanSource(IJavaElement element, IProgressMonitor monitor) throws JavaModelException {
		try {
			switch(element.getElementType()) {
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT: {
					IParent parent = (IParent) element;
					IJavaElement[] children = parent.getChildren();
					for (int i = 0; i < children.length; i++) {
						scanSource(children[i], monitor);
						Util.updateMonitor(monitor, 0);
					}
					break;
				}
				case IJavaElement.COMPILATION_UNIT: {
					ICompilationUnit unit = (ICompilationUnit) element;
					processType(unit);
					Util.updateMonitor(monitor, 0);
					break;
				}
			}
		}
		finally {
			if(monitor != null) {
				Util.updateMonitor(monitor);
				monitor.done();
			}
		}
	}
	
	/**
	 * Processes the given type name for invalid Javadoc tags
	 * @param typename
	 */
	private void processType(String typename) {
		try {
			IType type = fJavaProject.findType(typename);
			if(type != null && !type.isMember()) {
				// member types are processed while processing the compilation unit
				ICompilationUnit cunit = type.getCompilationUnit();
				if(cunit != null) {
					processType(cunit);
				}
			}
		} 
		catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Processes the given {@link ICompilationUnit} for invalid tags
	 * @param cunit
	 */
	private void processType(ICompilationUnit cunit) {
		TagValidator tv = new TagValidator(cunit);
		CompilationUnit comp = createAST(cunit, 0);
		if(comp == null) {
			return;
		}
		comp.accept(tv);
		IApiProblem[] tagProblems = tv.getTagProblems();
		for (int i = 0; i < tagProblems.length; i++) {
			addProblem(tagProblems[i]);
		}
	}
	/**
	 * Checks for illegal API usage in the specified component, creating problem
	 * markers as required.
	 * 
	 * @param context the current build context
	 * @param component component being built
	 * @param monitor progress monitor
	 */
	private void checkApiUsage(final IBuildContext context, final IApiComponent component, IProgressMonitor monitor) throws CoreException {
		if(ignoreApiUsageScan()) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Ignoring API usage scan"); //$NON-NLS-1$
			}
			return;
		} 
		IApiTypeContainer scope = null;
		if(context.hasTypes()) {
			String[] typenames = getApiUseTypes(context);
			if(typenames.length < 1) {
				monitor.done();
				return;
			}
			scope = getSearchScope(component, typenames);
		} else {
			scope = getSearchScope(component, null); // entire component
		}
		SubMonitor localMonitor = SubMonitor.convert(monitor, MessageFormat.format(BuilderMessages.checking_api_usage, new String[] {component.getSymbolicName()}), 2);
		ReferenceAnalyzer analyzer = new ReferenceAnalyzer();
		try {
			long start = System.currentTimeMillis();
			IApiProblem[] illegal = analyzer.analyze(component, scope, localMonitor.newChild(2));
			Util.updateMonitor(localMonitor);
			long end = System.currentTimeMillis();
			if (ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("API usage scan: " + (end- start) + " ms\t" + illegal.length + " problems"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}		
			for (int i = 0; i < illegal.length; i++) {
				addProblem(illegal[i]);
			}
			Util.updateMonitor(localMonitor);
		} 
		catch (CoreException ce) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				ApiPlugin.log(ce);
			}
		}
		finally {
			if(monitor != null) {
				monitor.done();
			}
		}
	}
	
	/**
	 * Returns the collection of type names to be built
	 * @param context
	 * @return the complete listing of type names to build or an empty array, never <code>null</code>
	 * @since 1.1
	 */
	String[] getApiUseTypes(IBuildContext context) {
		if(context.hasTypes()) {
			String[] deptypes = null;
			int size = 0;
			if (context.hasDescriptionDependents()) {
				// only check dependents if there were description changes
				deptypes = context.getDescriptionDependentTypes();
				size += deptypes.length;
			}
			String[] structtypes = context.getStructurallyChangedTypes();
			HashSet typenames = new HashSet(size + structtypes.length); // TODO: better sizing
			if (deptypes != null) {
				for (int i = 0; i < deptypes.length; i++) {
					if(deptypes[i] == null) {
						continue;
					}
					typenames.add(deptypes[i]);
				}
			}
			for (int i = 0; i < structtypes.length; i++) {
				if(structtypes[i] == null) {
					continue;
				}
				typenames.add(structtypes[i]);
			}
			return (String[]) typenames.toArray(new String[typenames.size()]);
		}
		return NO_TYPES;
	}
	
	/**
	 * Compares the given type between the two API components
	 * @param typeName the type to check in each component
	 * @param reference 
	 * @param component
	 * @param monitor
	 */
	private void checkCompatibility(final String typeName, final IApiComponent reference, final IApiComponent component, IProgressMonitor monitor) throws CoreException {
		String id = component.getSymbolicName();
		if (ApiPlugin.DEBUG_API_ANALYZER) {
			System.out.println("comparing components ["+reference.getSymbolicName()+"] and ["+id+"] for type ["+typeName+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		IApiTypeRoot classFile = null;
		try {
			if (Util.ORG_ECLIPSE_SWT.equals(id)) {
				classFile = component.findTypeRoot(typeName);
			} else {
				classFile = component.findTypeRoot(typeName, id);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_checking_compat, 4);
		try {
			IDelta delta = null;
			IApiComponent provider = null;
			boolean reexported = false;
			if (classFile == null) {
				String packageName = Signatures.getPackageName(typeName);
				// check if the type is provided by a required component (it could have been moved/re-exported)
				IApiComponent[] providers = component.getBaseline().resolvePackage(component, packageName);
				int index = 0;
				while (classFile == null && index < providers.length) {
					IApiComponent p = providers[index];
					if (!p.equals(component)) {
						String id2 = p.getSymbolicName();
						if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
							classFile = p.findTypeRoot(typeName);
						} else {
							classFile = p.findTypeRoot(typeName, id2);
						}
						if (classFile != null) {
							IRequiredComponentDescription[] components = component.getRequiredComponents();
							for (int i = 0; i < components.length; i++) {
								IRequiredComponentDescription description = components[i];
								if (description.getId().equals(p.getSymbolicName()) && description.isExported()) {
									reexported = true;
									break;
								}
							}
							provider = p;
						}
					}
					index++;
				}
			} else {
				provider = component;
			}
			Util.updateMonitor(localmonitor, 1);
			if (classFile == null) {
				// this indicates a removed type
				// we should try to get the class file from the reference
				IApiTypeRoot referenceClassFile = null;
				try {
					referenceClassFile = reference.findTypeRoot(typeName);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
				if (referenceClassFile != null) {
					try {
						IApiType type = referenceClassFile.getStructure();
						if(type == null) {
							return;
						}
						final IApiDescription referenceApiDescription = reference.getApiDescription();
						IApiAnnotations elementDescription = referenceApiDescription.resolveAnnotations(type.getHandle());
						int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
						if (!type.isMemberType() && !type.isAnonymous() && !type.isLocal()) {
							int visibility = VisibilityModifiers.ALL_VISIBILITIES;
							// we skip nested types (member, local and anonymous)
							if (elementDescription != null) {
								restrictions = elementDescription.getRestrictions();
								visibility = elementDescription.getVisibility();
							}
							// if the visibility is API, we only consider public and protected types
							if (Util.isDefault(type.getModifiers())
										|| Flags.isPrivate(type.getModifiers())) {
								return;
							}
							if (VisibilityModifiers.isAPI(visibility)) {
								String deltaComponentID = Util.getDeltaComponentVersionsId(reference);
								delta = new Delta(
										deltaComponentID,
										IDelta.API_COMPONENT_ELEMENT_TYPE,
										IDelta.REMOVED,
										IDelta.TYPE,
										restrictions,
										RestrictionModifiers.NO_RESTRICTIONS,
										type.getModifiers(),
										0,
										typeName,
										typeName,
										new String[] { typeName, Util.getComponentVersionsId(reference)});
							}
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
				Util.updateMonitor(localmonitor, 1);
			} else {
				fBuildState.cleanup(typeName);
				long time = System.currentTimeMillis();
				try {
					IApiComponent exporter = null;
					if (reexported) {
						exporter = component;
					}
					delta = ApiComparator.compare(classFile, reference, provider, exporter, reference.getBaseline(), provider.getBaseline(), VisibilityModifiers.API, localmonitor.newChild(1));
				} catch(OperationCanceledException oce) {
					//do nothing, but don't forward it
					//https://bugs.eclipse.org/bugs/show_bug.cgi?id=304315
					if(ApiPlugin.DEBUG_API_ANALYZER) {
						System.out.println("Trapped OperationCanceledException"); //$NON-NLS-1$
					}
				} catch(Exception e) {
					ApiPlugin.log(e);
				} finally {
					if (ApiPlugin.DEBUG_API_ANALYZER) {
						System.out.println("Time spent for " + typeName + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					fPendingDeltaInfos.clear();
				}
			}
			if (delta == null) {
				return;
			}
			if (delta != ApiComparator.NO_DELTA) {
				List allDeltas = Util.collectAllDeltas(delta);
				localmonitor.subTask(BuilderMessages.BaseApiAnalyzer_processing_deltas);
				for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
					processDelta((IDelta) iterator.next(), reference, component);
				}
				Util.updateMonitor(localmonitor, 1);
				if (!fPendingDeltaInfos.isEmpty()) {
					localmonitor.subTask(BuilderMessages.BaseApiAnalyzer_checking_since_tags);
					for (Iterator iterator = fPendingDeltaInfos.iterator(); iterator.hasNext();) {
						checkSinceTags((Delta) iterator.next(), component);
					}
				}
				Util.updateMonitor(localmonitor, 1);
			}
			else {
				Util.updateMonitor(localmonitor, 2);
			}
		}
		finally {
			localmonitor.done();
		}
	}
	/**
	 * Compares the two given components and generates an {@link IDelta}
	 * 
	 * @param jproject
	 * @param reference
	 * @param component
	 * @param monitor
	 */
	private void checkCompatibility(final IApiComponent reference, final IApiComponent component, IProgressMonitor monitor) throws CoreException {
		long time = System.currentTimeMillis();
		SubMonitor localmonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_checking_compat, 3);
		try {
			IDelta delta = null;
			if (reference == null) {
				delta =
					new Delta(
						null,
						IDelta.API_BASELINE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.API_COMPONENT,
						null,
						component.getSymbolicName(),
						component.getSymbolicName());
				Util.updateMonitor(localmonitor, 5);
			} else {
				try {
					delta = ApiComparator.compare(reference, component, VisibilityModifiers.API, localmonitor.newChild(1));
				} finally {
					if (ApiPlugin.DEBUG_API_ANALYZER) {
						System.out.println("Time spent for " + component.getSymbolicName() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					fPendingDeltaInfos.clear();
				}
			}
			if (delta == null) {
				return;
			}
			if (delta != ApiComparator.NO_DELTA) {
				List allDeltas = Util.collectAllDeltas(delta);
				if (allDeltas.size() != 0) {
					localmonitor.subTask(BuilderMessages.BaseApiAnalyzer_processing_deltas);
					for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
						processDelta((IDelta) iterator.next(), reference, component);
					}
					Util.updateMonitor(localmonitor, 1);
					localmonitor.subTask(BuilderMessages.BaseApiAnalyzer_checking_since_tags);
					if (!fPendingDeltaInfos.isEmpty()) {
						for (Iterator iterator = fPendingDeltaInfos.iterator(); iterator.hasNext();) {
							checkSinceTags((Delta) iterator.next(), component);
						}
					}
					Util.updateMonitor(localmonitor, 1);
				}
			}
			else {
				Util.updateMonitor(localmonitor, 2);
			}
		}
		finally {
			localmonitor.done();
		}
	}
	
	/**
	 * Processes delta to determine if it needs an @since tag. If it does and one
	 * is not present or the version of the tag is incorrect, a marker is created
	 * @param jproject
	 * @param delta
	 * @param component
	 */
	private void checkSinceTags(final Delta delta, final IApiComponent component) throws CoreException {
		if(ignoreSinceTagCheck(null)) {
			return;
		}
		IMember member = Util.getIMember(delta, fJavaProject);
		if (member == null || member.isBinary()) {
			return;
		}
		ICompilationUnit cunit = member.getCompilationUnit();
		if (cunit == null) {
			return;
		}
		try {
			if (! cunit.isConsistent()) {
				cunit.makeConsistent(null);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		IApiProblem problem = null;
		ISourceRange nameRange = null;
		try {
			nameRange = member.getNameRange();
		} catch (JavaModelException e) {
			ApiPlugin.log(e);
			return;
		}
		if (nameRange == null) {
			return;
		}
		try {
			int offset = nameRange.getOffset();
			CompilationUnit comp = createAST(cunit, offset);
			if(comp == null) {
				return;
			}
			SinceTagChecker visitor = new SinceTagChecker(offset);
			comp.accept(visitor);
			// we must retrieve the component version from the delta component id
			String componentVersionId = delta.getComponentVersionId();
			String componentVersionString = null;
			if (componentVersionId == null) {
				componentVersionString = component.getVersion();
			} else {
				componentVersionString = extractVersion(componentVersionId);
			}
			try {
				if (visitor.hasNoComment() || visitor.isMissing()) {
					if(ignoreSinceTagCheck(IApiProblemTypes.MISSING_SINCE_TAG)) {
						if(ApiPlugin.DEBUG_API_ANALYZER) {
							System.out.println("Ignoring missing since tag problem"); //$NON-NLS-1$
						}
						return;
					}
					StringBuffer buffer = new StringBuffer();
					Version componentVersion = new Version(componentVersionString);
					buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
					problem = createSinceTagProblem(IApiProblem.SINCE_TAG_MISSING, new String[] { Util.getDeltaArgumentString(delta) }, delta, member, String.valueOf(buffer));
				} else if (visitor.hasJavadocComment()) {
					// we don't want to flag block comment
					String sinceVersion = visitor.getSinceVersion();
					if (sinceVersion != null) {
						SinceTagVersion tagVersion = new SinceTagVersion(sinceVersion);
						String postfixString = tagVersion.postfixString();
						if (tagVersion.getVersion() == null || Util.getFragmentNumber(tagVersion.getVersionString()) > 2) {
							if(ignoreSinceTagCheck(IApiProblemTypes.MALFORMED_SINCE_TAG)) {
								if(ApiPlugin.DEBUG_API_ANALYZER) {
									System.out.println("Ignoring malformed since tag problem"); //$NON-NLS-1$
								}
								return;
							}
							StringBuffer buffer = new StringBuffer();
							if (tagVersion.prefixString() != null) {
								buffer.append(tagVersion.prefixString());
							}
							Version componentVersion = new Version(componentVersionString);
							buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
							if (postfixString != null) {
								buffer.append(postfixString);
							}
							problem = createSinceTagProblem(IApiProblem.SINCE_TAG_MALFORMED, new String[] {sinceVersion, Util.getDeltaArgumentString(delta)}, delta, member, String.valueOf(buffer));
						} else {
							if(ignoreSinceTagCheck(IApiProblemTypes.INVALID_SINCE_TAG_VERSION)) {
								if(ApiPlugin.DEBUG_API_ANALYZER) {
									System.out.println("Ignoring invalid tag version problem"); //$NON-NLS-1$
								}
								return;
							}
							StringBuffer accurateVersionBuffer = new StringBuffer();
							Version componentVersion = new Version(componentVersionString);
							accurateVersionBuffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
							String accurateVersion = String.valueOf(accurateVersionBuffer);
							if (Util.isDifferentVersion(sinceVersion, accurateVersion)) {
								// report invalid version number
								StringBuffer buffer = new StringBuffer();
								if (tagVersion.prefixString() != null) {
									buffer.append(tagVersion.prefixString());
								}
								Version version = new Version(accurateVersion);
								buffer.append(version.getMajor()).append('.').append(version.getMinor());
								if (postfixString != null) {
									buffer.append(postfixString);
								}
								String accurateSinceTagValue = String.valueOf(buffer);
								problem = createSinceTagProblem(IApiProblem.SINCE_TAG_INVALID, new String[] {sinceVersion, accurateSinceTagValue, Util.getDeltaArgumentString(delta)}, delta, member, accurateSinceTagValue);
							}
						}
					}
				}
			} catch (IllegalArgumentException e) {
				ApiPlugin.log(e);
			}
		} catch (RuntimeException e) {
			ApiPlugin.log(e);
		}
		if(problem != null) {
			addProblem(problem);
		}
	}
	
	private String extractVersion(String componentVersionId) {
		// extract the version from the delta component id. It is located between parenthesis
		int indexOfOpen = componentVersionId.lastIndexOf('(');
		return componentVersionId.substring(indexOfOpen + 1, componentVersionId.length() - 1);
	}

	/**
	 * Creates a marker to denote a problem with the since tag (existence or correctness) for a member
	 * and returns it, or <code>null</code>
	 * @param kind
	 * @param messageargs
	 * @param compilationUnit
	 * @param member
	 * @param version
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createSinceTagProblem(int kind, final String[] messageargs, final Delta info, final IMember member, final String version) {
		try {
			// create a marker on the member for missing @since tag
			IType declaringType = null;
			if (member.getElementType() == IJavaElement.TYPE) {
				declaringType = (IType) member;
			} else {
				declaringType = member.getDeclaringType();
			}
			IResource resource = Util.getResource(this.fJavaProject.getProject(), declaringType);
			if (resource == null) {
				return null;
			}
			int lineNumber = 1;
			int charStart = 0;
			int charEnd = 1;
			String qtn = null;
			if (member instanceof IType) {
				qtn = ((IType)member).getFullyQualifiedName();
			} else {
				qtn = declaringType.getFullyQualifiedName();
			}
			String[] messageArguments = null;
			if (!Util.isManifest(resource.getProjectRelativePath())) {
				messageArguments = messageargs;
				ICompilationUnit unit = member.getCompilationUnit();
				ISourceRange range = member.getNameRange();
				charStart = range.getOffset();
				charEnd = charStart + range.getLength();
				try {
					// unit cannot be null
					IDocument document = Util.getDocument(unit);
					lineNumber = document.getLineOfOffset(charStart);
				} catch (BadLocationException e) {
					ApiPlugin.log(e);
				}
			} else {
				// update the last entry in the message arguments 
				if (!(member instanceof IType)) {
					// insert the declaring type
					int length = messageargs.length;
					messageArguments = new String[length];
					System.arraycopy(messageargs, 0, messageArguments, 0, length);
					StringBuffer buffer = new StringBuffer();
					buffer.append(qtn).append('.').append(messageargs[length - 1]);
					messageArguments[length - 1] = String.valueOf(buffer);
				} else {
					messageArguments = messageargs;
				}
			}
			return ApiProblemFactory.newApiSinceTagProblem(resource.getProjectRelativePath().toPortableString(),
					qtn,
					messageArguments,
					new String[] {IApiMarkerConstants.MARKER_ATTR_VERSION, IApiMarkerConstants.API_MARKER_ATTR_ID, IApiMarkerConstants.MARKER_ATTR_HANDLE_ID},
					new Object[] {version, new Integer(IApiMarkerConstants.SINCE_TAG_MARKER_ID), member.getHandleIdentifier()},
					lineNumber,
					charStart,
					charEnd,
					info.getElementType(),
					kind);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	
	/**
	 * Creates an {@link IApiProblem} for the given compatibility delta 
	 * @param delta
	 * @param jproject
	 * @param reference
	 * @param component
	 * @return a new compatibility problem or <code>null</code>
	 */
	private IApiProblem createCompatibilityProblem(final IDelta delta, final IApiComponent reference, final IApiComponent component) {
		try {
			Version referenceVersion = new Version(reference.getVersion());
			Version componentVersion = new Version(component.getVersion());
			if ((referenceVersion.getMajor() < componentVersion.getMajor())
					&& !reportApiBreakageWhenMajorVersionIncremented()) {
				// API breakage are ok in this case and we don't want them to be reported
				fBuildState.addBreakingChange(delta);
				return null;
			}
			IResource resource = null;
			IType type = null;
			// retrieve line number, char start and char end
			int lineNumber = 0;
			int charStart = -1;
			int charEnd = 1;
			IMember member = null;
			if (fJavaProject != null) {
				try {
					type = fJavaProject.findType(delta.getTypeName().replace('$', '.'));
				} catch (JavaModelException e) {
					ApiPlugin.log(e);
				}
				IProject project = fJavaProject.getProject();
				resource = Util.getResource(project, type);
				if (resource == null) {
					return null;
				}
				if (!Util.isManifest(resource.getProjectRelativePath())) {
					member = Util.getIMember(delta, fJavaProject);
				}
				if (member != null && !member.isBinary() && member.exists()) {
					ISourceRange range = member.getNameRange();
					charStart = range.getOffset();
					charEnd = charStart + range.getLength();
					try {
						IDocument document = Util.getDocument(member.getCompilationUnit());
						lineNumber = document.getLineOfOffset(charStart);
					} catch (BadLocationException e) {
						// ignore
					}
				}
			}
			String path = null;
			if (resource != null) {
				path = resource.getProjectRelativePath().toPortableString();
			}
			IApiProblem apiProblem = ApiProblemFactory.newApiProblem(path,
					delta.getTypeName(),
					delta.getArguments(),
					new String[] {
						IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
						IApiMarkerConstants.API_MARKER_ATTR_ID
					},
					new Object[] {
						member == null ? null : member.getHandleIdentifier(),
						new Integer(IApiMarkerConstants.COMPATIBILITY_MARKER_ID),
					},
					lineNumber,
					charStart,
					charEnd,
					IApiProblem.CATEGORY_COMPATIBILITY,
					delta.getElementType(),
					delta.getKind(),
					delta.getFlags());
			return apiProblem;
			
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	/**
	 * Creates an {@link IApiProblem} for the given api component
	 * @param component
	 * @return a new api component resolution problem or <code>null</code>
	 */
	private void createApiComponentResolutionProblem(final IApiComponent component, final String message) throws CoreException {
		IApiProblem problem = ApiProblemFactory.newApiComponentResolutionProblem(
				Path.EMPTY.toString(),
				new String[] {component.getSymbolicName(), message },
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID},
				new Object[] {new Integer(IApiMarkerConstants.API_COMPONENT_RESOLUTION_MARKER_ID)},
				IElementDescriptor.RESOURCE,
				IApiProblem.API_COMPONENT_RESOLUTION);
		addProblem(problem);
	}
	/**
	 * Processes a delta to know if we need to check for since tag or version numbering problems
	 * @param jproject
	 * @param delta
	 * @param reference
	 * @param component
	 */
	private void processDelta(final IDelta delta, final IApiComponent reference, final IApiComponent component) {
		int flags = delta.getFlags();
		int kind = delta.getKind();
		int modifiers = delta.getNewModifiers();
		if (DeltaProcessor.isCompatible(delta)) {
			if (!RestrictionModifiers.isReferenceRestriction(delta.getCurrentRestrictions())) {
				if (Util.isVisible(modifiers)) {
					if (Flags.isProtected(modifiers)) {
						String typeName = delta.getTypeName();
						if (typeName != null) {
							IApiTypeRoot typeRoot = null;
							IApiType type = null;
							try {
								String id = component.getSymbolicName();
								if (Util.ORG_ECLIPSE_SWT.equals(id)) {
									typeRoot = component.findTypeRoot(typeName);
								} else {
									typeRoot = component.findTypeRoot(typeName, id);
								}
								if (typeRoot == null) {
									String packageName = Signatures.getPackageName(typeName);
									// check if the type is provided by a required component (it could have been moved/re-exported)
									IApiComponent[] providers = component.getBaseline().resolvePackage(component, packageName);
									int index = 0;
									while (typeRoot == null && index < providers.length) {
										IApiComponent p = providers[index];
										if (!p.equals(component)) {
											String id2 = p.getSymbolicName();
											if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
												typeRoot = p.findTypeRoot(typeName);
											} else {
												typeRoot = p.findTypeRoot(typeName, id2);
											}
										}
										index++;
									}
								}
								if (typeRoot == null) {
									return;
								}
								type = typeRoot.getStructure();
							} catch (CoreException e) {
								// ignore
							}
							if (type == null || Flags.isFinal(type.getModifiers())) {
								// no @since tag to report for new protected methods inside a final class
								return;
							}
						}
					}
					// if protected, we only want to check @since tags if the enclosing class can be subclassed
					switch(kind) {
						case IDelta.ADDED :
							// if public, we always want to check @since tags
							switch(flags) {
								case IDelta.TYPE_MEMBER :
								case IDelta.METHOD :
								case IDelta.CONSTRUCTOR :
								case IDelta.ENUM_CONSTANT :
								case IDelta.METHOD_WITH_DEFAULT_VALUE :
								case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
								case IDelta.FIELD :
								case IDelta.TYPE :
									if (ApiPlugin.DEBUG_API_ANALYZER) {
										String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
										System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
									}
									this.fBuildState.addCompatibleChange(delta);
									fPendingDeltaInfos.add(delta);
									break;
							}
							break;
						case IDelta.CHANGED :
							if (flags == IDelta.INCREASE_ACCESS) {
								if (ApiPlugin.DEBUG_API_ANALYZER) {
									String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
									System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
								}
								this.fBuildState.addCompatibleChange(delta);
								fPendingDeltaInfos.add(delta);
							}
					}
				}
			}
		} else {
			switch(kind) {
				case IDelta.ADDED :
					// if public, we always want to check @since tags
					switch(flags) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
							// ensure that there is a @since tag for the corresponding member
							if (Util.isVisible(modifiers)) {
								if (ApiPlugin.DEBUG_API_ANALYZER) {
									String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
									System.err.println(deltaDetails + " is not compatible"); //$NON-NLS-1$
								}
								fPendingDeltaInfos.add(delta);
							}
					}
					break;
			}
			IApiProblem problem = createCompatibilityProblem(delta, reference, component);
			if(addProblem(problem)) {
				fBuildState.addBreakingChange(delta);
			}
		}
	}
	/**
	 * Checks the version number of the API component and creates a problem markers as needed
	 * @param reference
	 * @param component
	 */
	private void checkApiComponentVersion(final IApiComponent reference, final IApiComponent component) throws CoreException {
		if(ignoreComponentVersionCheck() || reference == null || component == null) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Ignoring component version check"); //$NON-NLS-1$
			}
			return;
		}
		IApiProblem problem = null;
		String refversionval = reference.getVersion();
		String compversionval = component.getVersion();
		Version refversion = new Version(refversionval);
		Version compversion = new Version(compversionval);
		Version newversion = null;
		if (ApiPlugin.DEBUG_API_ANALYZER) {
			System.out.println("reference version of " + reference.getSymbolicName() + " : " + refversion); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("component version of " + component.getSymbolicName() + " : " + compversion); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IDelta[] breakingChanges = fBuildState.getBreakingChanges();
		if (breakingChanges.length != 0) {
			// make sure that the major version has been incremented
			if (compversion.getMajor() <= refversion.getMajor()) {
				newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier() != null ? QUALIFIER : null);
				problem = createVersionProblem(
						IApiProblem.MAJOR_VERSION_CHANGE,
						new String[] {
							compversionval,
							refversionval
						},
						String.valueOf(newversion),
						collectDetails(breakingChanges));
			}
		} else {
			IDelta[] compatibleChanges = fBuildState.getCompatibleChanges();
			if (compatibleChanges.length != 0) {
				// only new API have been added
				if (compversion.getMajor() != refversion.getMajor()) {
					if (!ignoreMajorVersionCheckWithoutBreakingChange()) {
						// major version should be identical
						newversion = new Version(refversion.getMajor(), refversion.getMinor() + 1, 0, compversion.getQualifier() != null ? QUALIFIER : null);
						problem = createVersionProblem(
								IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE,
								new String[] {
									compversionval,
									refversionval
								},
								String.valueOf(newversion),
								collectDetails(compatibleChanges));
					}
				} else if (compversion.getMinor() <= refversion.getMinor()) {
					// the minor version should be incremented
					newversion = new Version(compversion.getMajor(), compversion.getMinor() + 1, 0, compversion.getQualifier() != null ? QUALIFIER : null);
					problem = createVersionProblem(
							IApiProblem.MINOR_VERSION_CHANGE,
							new String[] {
								compversionval,
								refversionval
							},
							String.valueOf(newversion),
							collectDetails(compatibleChanges));
				}
			} else if (compversion.getMajor() != refversion.getMajor()) {
				if (!ignoreMajorVersionCheckWithoutBreakingChange()) {
					// major version should be identical
					newversion = new Version(refversion.getMajor(), refversion.getMinor(), refversion.getMicro(), refversion.getQualifier() != null ? QUALIFIER : null);
					problem = createVersionProblem(
							IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE,
							new String[] {
								compversionval,
								refversionval
							},
							String.valueOf(newversion),
							Util.EMPTY_STRING);
				}
			} else if (compversion.getMinor() > refversion.getMinor()) {
				// the minor version should not be incremented
				if (!ignoreMinorVersionCheckWithoutApiChange()) {
					newversion = new Version(refversion.getMajor(), refversion.getMinor(), refversion.getMicro(), refversion.getQualifier() != null ? QUALIFIER : null);
					problem = createVersionProblem(
							IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API,
							new String[] {
								compversionval,
								refversionval
							},
							String.valueOf(newversion),
							Util.EMPTY_STRING);
				}
			}
			// analyze version of required components
			ReexportedBundleVersionInfo info = null;
			if (problem != null) {
				switch (problem.getKind()) {
					case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE :
						// check if there is a version change required due to re-exported bundles
						info = checkBundleVersionsOfReexportedBundles(reference, component);
						if (info != null) {
							switch(info.kind) {
								case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
									/* we don't do anything since the major version is already incremented
									 * we cancel the previous issue. No need to report that the major version
									 * should not be incremented */
									problem = null;
									break;
								case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE :
									// we should reset the major version and increment only the minor version
									newversion = new Version(refversion.getMajor(), refversion.getMinor() + 1, 0, compversion.getQualifier() != null ? QUALIFIER : null);
									problem = createVersionProblem(
											IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE,
											new String[] {
												compversionval,
												info.componentID,
											},
											String.valueOf(newversion),
											Util.EMPTY_STRING);
							}
						}
						break;
					case IApiProblem.MINOR_VERSION_CHANGE :
						// check if there is a version change required due to re-exported bundles
						info = checkBundleVersionsOfReexportedBundles(reference, component);
						if (info != null) {
							switch(info.kind) {
								case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
									// we keep this problem
									newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier() != null ? QUALIFIER : null);
									problem = createVersionProblem(
											info.kind,
											new String[] {
												compversionval,
												info.componentID,
											},
											String.valueOf(newversion),
											Util.EMPTY_STRING);
									break;
								case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE :
									// we don't do anything since we should already increment the minor version
							}
						}
						break;
					case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API :
						// check if there is a version change required due to re-exported bundles
						info = checkBundleVersionsOfReexportedBundles(reference, component);
						if (info != null) {
							switch(info.kind) {
								case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
									// we return this one
									newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier() != null ? QUALIFIER : null);
									problem = createVersionProblem(
											info.kind,
											new String[] {
												compversionval,
												info.componentID,
											},
											String.valueOf(newversion),
											Util.EMPTY_STRING);
									break;
								case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE :
									// we don't do anything since we already incremented the minor version
									// we get rid of the previous problem
									problem = null;
							}
						}
				}
			} else {
				info = checkBundleVersionsOfReexportedBundles(reference, component);
				if (info != null) {
					switch(info.kind) {
						case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
							// major version change
							if (compversion.getMajor() <= refversion.getMajor()) {
								newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier() != null ? QUALIFIER : null);
								problem = createVersionProblem(
										info.kind,
										new String[] {
											compversionval,
											info.componentID,
										},
										String.valueOf(newversion),
										Util.EMPTY_STRING);
							}
							break;
						case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE :
							// minor version change
							if (compversion.getMinor() <= refversion.getMinor()) {
								newversion = new Version(compversion.getMajor(), compversion.getMinor() + 1, 0, compversion.getQualifier());
								problem = createVersionProblem(
									info.kind,
									new String[] {
											compversionval,
											info.componentID,
									},
									String.valueOf(newversion),
									Util.EMPTY_STRING);
							}
					}
				}
			}
		}
		if(problem != null) {
			addProblem(problem);
		}
	}
	
	/**
	 * Collects details from the given delta listing for version problems
	 * @param deltas
	 * @return a {@link String} of the details why the version number should be changed
	 */
	private String collectDetails(final IDelta[] deltas) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		//TODO contrived default for https://bugs.eclipse.org/bugs/show_bug.cgi?id=251313
		int max = Math.min(20, deltas.length);
		for (int i = 0; i < max; i++) {
			printWriter.print("- "); //$NON-NLS-1$
			printWriter.println(deltas[i].getMessage());
			if(i == max-1 && max < deltas.length) {
				printWriter.println(NLS.bind(BuilderMessages.BaseApiAnalyzer_more_version_problems, new Integer(deltas.length - max)));
			}
		}
		printWriter.flush();
		printWriter.close();
		return String.valueOf(writer.getBuffer());
	}
	
	/**
	 * Creates a marker on a manifest file for a version numbering problem and returns it
	 * or <code>null</code> 
	 * @param kind
	 * @param messageargs
	 * @param version
	 * @param description the description of details
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createVersionProblem(int kind, final String[] messageargs, String version, String description) {
		IResource manifestFile = null;
		String path = JarFile.MANIFEST_NAME;
		if (fJavaProject != null) {
			manifestFile = Util.getManifestFile(fJavaProject.getProject());
		}
		// this error should be located on the manifest.mf file
		// first of all we check how many api breakage marker are there
		int lineNumber = -1;
		int charStart = 0;
		int charEnd = 1;
		char[] contents = null;
		if (manifestFile!= null && manifestFile.getType() == IResource.FILE) {
			path = manifestFile.getProjectRelativePath().toPortableString();
			IFile file = (IFile) manifestFile;
			InputStream inputStream = null;
			LineNumberReader reader = null;
			try {
				inputStream = file.getContents(true);
				contents = Util.getInputStreamAsCharArray(inputStream, -1, IApiCoreConstants.UTF_8);
				reader = new LineNumberReader(new BufferedReader(new StringReader(new String(contents))));
				int lineCounter = 0;
				String line = null;
				loop: while ((line = reader.readLine()) != null) {
					lineCounter++;
					if (line.startsWith(Constants.BUNDLE_VERSION)) {
						lineNumber = lineCounter;
						break loop;
					}
				}
			} catch (CoreException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					// ignore
				}
			}
		}
		if (lineNumber != -1 && contents != null) {
			// initialize char start, char end
			int index = CharOperation.indexOf(Constants.BUNDLE_VERSION.toCharArray(), contents, true);
			loop: for (int i = index + Constants.BUNDLE_VERSION.length() + 1, max = contents.length; i < max; i++) {
				char currentCharacter = contents[i];
				if (CharOperation.isWhitespace(currentCharacter)) {
					continue;
				}
				charStart = i;
				break loop;
			}
			loop: for (int i = charStart + 1, max = contents.length; i < max; i++) {
				switch(contents[i]) {
					case '\r' :
					case '\n' :
						charEnd = i;
						break loop;
				}
			}
		} else {
			lineNumber = 1;
		}
		return ApiProblemFactory.newApiVersionNumberProblem(path,
				null,
				messageargs, 
				new String[] {
					IApiMarkerConstants.MARKER_ATTR_VERSION,
					IApiMarkerConstants.API_MARKER_ATTR_ID,
					IApiMarkerConstants.VERSION_NUMBERING_ATTR_DESCRIPTION,
				}, 
				new Object[] {
					version,
					new Integer(IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID),
					description
				}, 
				lineNumber, 
				charStart, 
				charEnd, 
				IElementDescriptor.RESOURCE, 
				kind);
	}
	
	/**
	 * Checks to see if there is a default API baseline set in the workspace,
	 * if not create a marker
	 */
	private void checkDefaultBaselineSet() {
		if(ignoreDefaultBaselineCheck()) {
			if(ApiPlugin.DEBUG_API_ANALYZER) {
				System.out.println("Ignoring check for default API baseline"); //$NON-NLS-1$
			}
			return;
		}
		if(ApiPlugin.DEBUG_API_ANALYZER) {
			System.out.println("Checking if the default api baseline is set"); //$NON-NLS-1$
		}
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem(
				Path.EMPTY.toString(),
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID},
				new Object[] {new Integer(IApiMarkerConstants.DEFAULT_API_BASELINE_MARKER_ID)},
				IElementDescriptor.RESOURCE,
				IApiProblem.API_BASELINE_MISSING);
		addProblem(problem);
	}
	
	/**
	 * Returns the Java project associated with the given API component, or <code>null</code>
	 * if none.
	 * 
	 *@param component API component
	 * @return Java project or <code>null</code>
	 */
	private IJavaProject getJavaProject(IApiComponent component) {
		if (component instanceof ProjectComponent) {
			ProjectComponent pp = (ProjectComponent) component;
			return pp.getJavaProject();
		}
		return null;
	}
	
	/**
	 * Adds the problem to the list of problems iff it is not <code>null</code> and not filtered
	 * @param problem
	 * @return
	 */
	private boolean addProblem(IApiProblem problem) {
		if (problem == null || isProblemFiltered(problem)) {
			return false;
		}
		return fProblems.add(problem);
	}
	/**
	 * Returns if the given {@link IApiProblem} should be filtered from having a problem marker created for it
	 * 
	 * @param problem the problem that may or may not be filtered
	 * @return true if the {@link IApiProblem} should not have a marker created, false otherwise
	 */
	private boolean isProblemFiltered(IApiProblem problem) {
		if (fJavaProject == null) {
			if (this.fFilterStore != null) {
				boolean filtered = this.fFilterStore.isFiltered(problem);
				if (filtered) {
					return true;
				}
			}
			if (this.fPreferences != null) {
				String key = ApiProblemFactory.getProblemSeverityId(problem);
				if (key != null) {
					String value = this.fPreferences.getProperty(key, null);
					return ApiPlugin.VALUE_IGNORE.equals(value);
				}
			}
			return false;
		}

		IProject project = fJavaProject.getProject();
		// first the severity is checked
		if (ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(problem), project) == ApiPlugin.SEVERITY_IGNORE) {
			return true;
		}

		IApiBaselineManager manager = ApiBaselineManager.getManager();
		IApiBaseline baseline = manager.getWorkspaceBaseline();
		if(baseline == null) {
			return false;
		}
		IApiComponent component = baseline.getApiComponent(project);
		if(component != null) {
			try {
				IApiFilterStore filterStore = component.getFilterStore();
				if (filterStore != null) {
					return filterStore.isFiltered(problem);
				}
			}
			catch(CoreException e) {}
		}
		return false;
	}
}
