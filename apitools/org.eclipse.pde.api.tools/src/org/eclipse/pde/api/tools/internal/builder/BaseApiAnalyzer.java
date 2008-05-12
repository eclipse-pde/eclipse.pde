/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
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
	private static class ReexportedBundleVersionInfo {
		String componentID;
		int kind;
		
		ReexportedBundleVersionInfo(String componentID, int kind) {
			this.componentID = componentID;
			this.kind = kind;
		}
	}

	/**
	 * Visitor for validating Javadoc tags in {@link IClassFile}s 
	 */
	class ClassFileVisitor extends ClassFileContainerVisitor {
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.internal.provisional.IClassFile)
		 */
		public void visit(String packageName, IClassFile classFile) {
			processType(classFile.getTypeName());
		}
	}
	
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	private static boolean DEBUG = Util.DEBUG;
	
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
	 * Method used for initializing tracing in the API tool builder
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * Constructs an API analyzer
	 */
	public BaseApiAnalyzer() {
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiAnalyzer#analyzeComponent(org.eclipse.pde.api.tools.internal.builder.BuildState, org.eclipse.pde.api.tools.internal.provisional.IApiProfile, org.eclipse.pde.api.tools.internal.provisional.IApiComponent, java.lang.String[], java.lang.String[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void analyzeComponent(
			final BuildState state,
			final IApiFilterStore filterStore,
			final IApiProfile baseline,
			final IApiComponent component,
			final String[] typenames,
			final String[] changedtypes,
			IProgressMonitor monitor) {
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_analyzing_api, 3 + (typenames == null ? 0 : typenames.length));
		try {
			fJavaProject = getJavaProject(component);
			if(baseline == null) {
				//check default baseline
				checkDefaultBaselineSet();
				updateMonitor(localMonitor, 3);
				return;
			}
			IApiComponent reference = baseline.getApiComponent(component.getId());
			this.fBuildState = state;
			if(fBuildState == null) {
				fBuildState = getBuildState();
			}
			if (filterStore != null) {
				this.fFilterStore = filterStore;
			}
			//compatibility checks
			if(reference != null) {
				localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_comparing_api_profiles, reference.getId()));
				if(changedtypes != null) {
					for(int i = 0; i < changedtypes.length; i++) {
						checkCompatibility(changedtypes[i], reference, component);
						updateMonitor(localMonitor);
					}
				} else {
					checkCompatibility(reference, component);
					updateMonitor(localMonitor);
				}
			} else {
				localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_comparing_api_profiles, component.getId()));
				checkCompatibility(reference, component);
				updateMonitor(localMonitor);
			}
			//usage checks
			checkApiUsage(component, typenames, localMonitor);
			updateMonitor(localMonitor);
			//version checks
			checkApiComponentVersion(reference, component);
			updateMonitor(localMonitor);
			//tag validation
			checkTagValidation(changedtypes, component, localMonitor);
			updateMonitor(localMonitor);
		}
		finally {
			localMonitor.done();
		}
	}

	/**
	 * Check the version changes of reexported bundles to make sure that the given component
	 * version is modified accordingly.
	 * 
	 * @param reference the given reference api profile
	 * @param component the given component
	 */
	private ReexportedBundleVersionInfo checkBundleVersionsOfReexportedBundles(
			IApiComponent reference, IApiComponent component) {
		IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
		int length = requiredComponents.length;
		ReexportedBundleVersionInfo info = null;
		if (length != 0) {
			IApiProfile profile = component.getProfile();
			IApiProfile baseline = reference.getProfile();
			loop: for (int i = 0; i < length; i++) {
				IRequiredComponentDescription description = requiredComponents[i];
				if (description.isExported()) {
					String id = description.getId();
					IApiComponent baselineRequiredApiComponent = baseline.getApiComponent(id);
					IApiComponent currentRequiredApiComponent = profile.getApiComponent(id);
					Version baselineRequiredVersion = new Version(baselineRequiredApiComponent.getVersion());
					Version currentRequiredVersion = new Version(currentRequiredApiComponent.getVersion());
					if (DEBUG) {
						System.out.println("reexported component " + id); //$NON-NLS-1$
						System.out.println("\t- version in baseline profile : " + baselineRequiredVersion); //$NON-NLS-1$
						System.out.println("\t- version in current profile : " + currentRequiredVersion); //$NON-NLS-1$
					}
					if (baselineRequiredVersion.getMajor() != currentRequiredVersion.getMajor()) {
						// major version was changed so the current plugin should also update its major version
						return new ReexportedBundleVersionInfo(id , IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE);
					} else if (baselineRequiredVersion.getMinor() != currentRequiredVersion.getMinor()) {
						// minor version was changed so the current plugin should also update its minor version
						if (info != null)  {
							// already found a reexported minor version change. No need to create a new one
							continue loop;
						}
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
		ASTParser parser = ASTParser.newParser(AST.JLS3);
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
			BuildState state = ApiAnalysisBuilder.getLastBuiltState(project);
			if(state != null) {
				return state;
			}
		} 
		catch (CoreException e) {}
		return new BuildState();
	}
	
	/**
	 * Returns an {@link IApiSearchScope} given the component and type names context
	 * @param component
	 * @param types
	 * @return a new {@link IApiSearchScope} for the component and type names context
	 */
	private IApiSearchScope getSearchScope(final IApiComponent component, final String[] typenames) {
		if(typenames == null) {
			return Factory.newScope(new IApiComponent[]{component});
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
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_EXTEND, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_FIELD_DECL, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_IMPLEMENT, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_METHOD_PARAM, project) == ApiPlugin.SEVERITY_IGNORE;
		ignore &= plugin.getSeverityLevel(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, project) == ApiPlugin.SEVERITY_IGNORE;
		return ignore;
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
	 * Checks the validation of tags for the given {@link IApiComponent}
	 * @param typenames
	 * @param component
	 * @param monitor
	 */
	private void checkTagValidation(String[] typenames, IApiComponent component, IProgressMonitor monitor) {
		if(ignoreInvalidTagCheck()) {
			return;
		}
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.BaseApiAnalyzer_validating_javadoc_tags, 1 + (typenames == null ? component.getClassFileContainers().length : typenames.length));
		try {
			if(typenames == null) {
				IClassFileContainer[] containers = component.getClassFileContainers();
				ClassFileVisitor visitor = new ClassFileVisitor();
				for(int i = 0; i < containers.length; i++) {
					try {
						localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_scanning_0, containers[i].getOrigin()));
						containers[i].accept(visitor);
						updateMonitor(localMonitor);
					}
					catch(CoreException ce) {}
				}
			}
			else {
				for(int i = 0; i < typenames.length; i++) {
					localMonitor.subTask(NLS.bind(BuilderMessages.BaseApiAnalyzer_scanning_0, typenames[i]));
					processType(typenames[i]);
					updateMonitor(localMonitor);
				}
			}
			updateMonitor(localMonitor);
		}
		finally {
			localMonitor.done();
		}
	}
	
	/**
	 * Processes the given type name for invalid Javadoc tags
	 * @param typename
	 */
	private void processType(String typename) {
		try {
			IMember type = fJavaProject.findType(typename);
			if(type != null) {
				ICompilationUnit cunit = type.getCompilationUnit();
				if(cunit != null) {
					TagValidator tv = new TagValidator(cunit);
					CompilationUnit comp = createAST(cunit, 0);
					if(comp == null) {
						return;
					}
					comp.accept(tv);
					IApiProblem[] tagProblems = tv.getTagProblems();
					for (int i = 0; i < tagProblems.length; i++) {
						IApiProblem apiProblem = tagProblems[i];
						addProblem(apiProblem);
					}
				}
			}
		} 
		catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks for illegal API usage in the specified component, creating problem
	 * markers as required.
	 * 
	 * @param profile profile being analyzed
	 * @param component component being built
	 * @param typenames
	 * @param monitor progress monitor
	 */
	private void checkApiUsage(final IApiComponent component, final String[] typenames, IProgressMonitor monitor) {
		if(ignoreApiUsageScan()) {
			if(DEBUG) {
				System.out.println("Ignoring API usage scan"); //$NON-NLS-1$
			}
			return;
		} 
		IApiSearchScope scope = getSearchScope(component, typenames);
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, MessageFormat.format(BuilderMessages.checking_api_usage, new String[] {component.getId()}), 2);
		ApiUseAnalyzer analyzer = new ApiUseAnalyzer();
		try {
			long start = System.currentTimeMillis();
			IApiProblem[] illegal = analyzer.findIllegalApiUse(component, scope, monitor);
			updateMonitor(localMonitor);
			long end = System.currentTimeMillis();
			if (DEBUG) {
				System.out.println("API usage scan: " + (end- start) + " ms\t" + illegal.length + " problems"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}		
			for (int i = 0; i < illegal.length; i++) {
				addProblem(illegal[i]);
			}
			updateMonitor(localMonitor);
		} 
		catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
		finally {
			localMonitor.done();
		}
	}
	
	/**
	 * Compares the given type between the two API components
	 * @param typeName the type to check in each component
	 * @param reference 
	 * @param component
	 */
	private void checkCompatibility(final String typeName, final IApiComponent reference, final IApiComponent component) {
		if (DEBUG) {
			System.out.println("comparing profiles ["+reference.getId()+"] and ["+component.getId()+"] for type ["+typeName+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		IClassFile classFile = null;
		try {
			classFile = component.findClassFile(typeName);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		if (classFile == null) {
			if (DEBUG) {
				System.err.println("Could not retrieve class file for " + typeName + " in " + component.getId()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return;
		}
		fBuildState.cleanup(typeName);
		IDelta delta = null;
		long time = System.currentTimeMillis();
		try {
			delta = ApiComparator.compare(classFile, reference, component, reference.getProfile(), component.getProfile(), VisibilityModifiers.API);
		} catch(Exception e) {
			ApiPlugin.log(e);
		} finally {
			if (DEBUG) {
				System.out.println("Time spent for " + typeName + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			fPendingDeltaInfos.clear();
		}
		if (delta == null) {
			return;
		}
		if (delta != ApiComparator.NO_DELTA) {
			List allDeltas = Util.collectAllDeltas(delta);
			for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
				processDelta((IDelta) iterator.next(), reference, component);
			}
			if (!fPendingDeltaInfos.isEmpty()) {
				for (Iterator iterator = fPendingDeltaInfos.iterator(); iterator.hasNext();) {
					checkSinceTags((Delta) iterator.next(), component);
				}
			}
		}
	}
	/**
	 * Compares the two given profiles and generates an {@link IDelta}
	 * 
	 * @param jproject
	 * @param reference
	 * @param component
	 */
	private void checkCompatibility(final IApiComponent reference, final IApiComponent component) {
		long time = System.currentTimeMillis();
		IDelta delta = null;
		if (reference == null) {
			delta =
				new Delta(
					null,
					IDelta.API_PROFILE_ELEMENT_TYPE,
					IDelta.ADDED,
					IDelta.API_COMPONENT,
					null,
					component.getId(),
					component.getId());
		} else {
			try {
				delta = ApiComparator.compare(reference, component, VisibilityModifiers.API);
			} catch(Exception e) {
				ApiPlugin.log(e);
			} finally {
				if (DEBUG) {
					System.out.println("Time spent for " + component.getId() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
				for (Iterator iterator = allDeltas.iterator(); iterator.hasNext();) {
					processDelta((IDelta) iterator.next(), reference, component);
				}
				if (!fPendingDeltaInfos.isEmpty()) {
					for (Iterator iterator = fPendingDeltaInfos.iterator(); iterator.hasNext();) {
						checkSinceTags((Delta) iterator.next(), component);
					}
				}
			}
		}
	}
	
	/**
	 * Processes delta to determine if it needs an @since tag. If it does and one
	 * is not present or the version of the tag is incorrect, a marker is created
	 * @param jproject
	 * @param delta
	 * @param component
	 */
	private void checkSinceTags(final Delta delta, final IApiComponent component) {
		if(ignoreSinceTagCheck(null)) {
			return;
		}
		IMember member = Util.getIMember(delta, fJavaProject);
		if (member == null) {
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
			String componentVersionString = component.getVersion();
			try {
				if (visitor.hasNoComment() || visitor.isMissing()) {
					if(ignoreSinceTagCheck(IApiProblemTypes.MISSING_SINCE_TAG)) {
						if(DEBUG) {
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
						if (tagVersion.getVersion() == null || Util.getFragmentNumber(tagVersion.getVersionString()) > 2) {
							if(ignoreSinceTagCheck(IApiProblemTypes.MALFORMED_SINCE_TAG)) {
								if(DEBUG) {
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
							if (tagVersion.postfixString() != null) {
								buffer.append(tagVersion.postfixString());
							}
							problem = createSinceTagProblem(IApiProblem.SINCE_TAG_MALFORMED, new String[] {sinceVersion}, delta, member, String.valueOf(buffer));
						} else {
							if(ignoreSinceTagCheck(IApiProblemTypes.INVALID_SINCE_TAG_VERSION)) {
								if(DEBUG) {
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
								if (tagVersion.postfixString() != null) {
									buffer.append(tagVersion.postfixString());
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
			IResource resource = null;
			ICompilationUnit unit = null;
			try {
				unit = member.getCompilationUnit();
				if (unit != null) {
					resource = unit.getCorrespondingResource();
				}
			} catch (JavaModelException e) {
				ApiPlugin.log(e);
			}
			if (resource == null) {
				return null;
			}
			int lineNumber = 1;
			int charStart = 0;
			int charEnd = 1;
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
			String qtn = null;
			if (member instanceof IType) {
				qtn = ((IType)member).getFullyQualifiedName();
			} else {
				qtn = member.getDeclaringType().getFullyQualifiedName();
			}
			return ApiProblemFactory.newApiSinceTagProblem(resource.getProjectRelativePath().toPortableString(),
					qtn,
					messageargs,
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
			if (referenceVersion.getMajor() < componentVersion.getMajor()) {
				// API breakage are ok in this case
				fBuildState.addBreakingChange(delta);
				return null;
			}
			IResource resource = null;
			IType type = null;
			// retrieve line number, char start and char end
			int lineNumber = 1;
			int charStart = -1;
			int charEnd = 1;
			IMember member = null;
			if (fJavaProject != null) {
				try {
					type = fJavaProject.findType(delta.getTypeName().replace('$', '.'));
				} catch (JavaModelException e) {
					ApiPlugin.log(e);
				}
				if (type == null) {
					IResource manifestFile = Util.getManifestFile(fJavaProject.getProject());
					if (manifestFile == null) {
						// Cannot retrieve the manifest.mf file
						return null;
					}
					resource = manifestFile;
				} else {
					ICompilationUnit unit = type.getCompilationUnit();
					if (unit != null) {
						resource = unit.getCorrespondingResource();
						if (resource == null) {
							return null;
						}
					} else {
						IResource manifestFile = Util.getManifestFile(fJavaProject.getProject());
						if (manifestFile == null) {
							// Cannot retrieve the manifest.mf file
							return null;
						}
						resource = manifestFile;
					}
				}
				member = Util.getIMember(delta, fJavaProject);
				if (member != null) {
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
	 * Processes a delta to know if we need to check for since tag or version numbering problems
	 * @param jproject
	 * @param delta
	 * @param reference
	 * @param component
	 */
	private void processDelta(final IDelta delta, final IApiComponent reference, final IApiComponent component) {
		int flags = delta.getFlags();
		int kind = delta.getKind();
		if (DeltaProcessor.isCompatible(delta)) {
			if (isNewAPI(delta)) {
				this.fBuildState.addCompatibleChange(delta);
			}
			if (kind == IDelta.ADDED) {
				int modifiers = delta.getModifiers();
				if (Util.isPublic(modifiers)) {
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
							if (DEBUG) {
								String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
								System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
							}
							fPendingDeltaInfos.add(delta);
							break;
					}
				} else if (Util.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
					// if protected, we only want to check @since tags if the enclosing class can be subclassed
					switch(flags) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.FIELD :
						case IDelta.TYPE :
							if (DEBUG) {
								String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
								System.out.println(deltaDetails + " is compatible"); //$NON-NLS-1$
							}
							fPendingDeltaInfos.add(delta);
							break;
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
							if (Util.isVisible(delta)) {
								if (DEBUG) {
									String deltaDetails = "Delta : " + Util.getDetail(delta); //$NON-NLS-1$
									System.err.println(deltaDetails + " is not compatible"); //$NON-NLS-1$
								}
								fPendingDeltaInfos.add(delta);
							}
					}
					break;
				case IDelta.CHANGED :
					if (flags == IDelta.RESTRICTIONS) {
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=228424
						return;
					}
			}
			IApiProblem problem = createCompatibilityProblem(delta, reference, component);
			if(addProblem(problem)) {
				fBuildState.addBreakingChange(delta);
			}
		}
	}
	
	/**
	 * This is called when the delta is a compatible delta. So 
	 * @param delta
	 * @return
	 */
	private boolean isNewAPI(IDelta delta) {
		switch(delta.getKind()) {
			case IDelta.ADDED :
				switch(delta.getFlags()) {
					case IDelta.EXECUTION_ENVIRONMENT :
					case IDelta.OVERRIDEN_METHOD :
					case IDelta.CLINIT :
						return false;
					case IDelta.TYPE_MEMBER :
					case IDelta.METHOD :
					case IDelta.CONSTRUCTOR :
					case IDelta.ENUM_CONSTANT :
					case IDelta.METHOD_WITH_DEFAULT_VALUE :
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
					case IDelta.FIELD :
					case IDelta.TYPE :
					case IDelta.TYPE_PARAMETERS :
					case IDelta.TYPE_ARGUMENTS :
					case IDelta.SUPERCLASS :
					case IDelta.CHECKED_EXCEPTION :
					case IDelta.UNCHECKED_EXCEPTION :
						return Util.isVisible(delta);
				}
				break;
			case IDelta.CHANGED :
				switch(delta.getFlags()) {
					case IDelta.NON_NATIVE_TO_NATIVE :
					case IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED :
					case IDelta.NON_TRANSIENT_TO_TRANSIENT :
					case IDelta.TRANSIENT_TO_NON_TRANSIENT :
					case IDelta.NATIVE_TO_NON_NATIVE :
					case IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED :
					case IDelta.NON_VOLATILE_TO_VOLATILE :
					case IDelta.VOLATILE_TO_NON_VOLATILE :
						return false;
				}
				switch(delta.getElementType()) {
					case IDelta.FIELD_ELEMENT_TYPE :
					case IDelta.METHOD_ELEMENT_TYPE :
					case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
						return Util.isVisible(delta);
					default:
						switch(delta.getFlags()) {
							case IDelta.EXPANDED_SUPERCLASS_SET :
							case IDelta.EXPANDED_SUPERINTERFACES_SET :
							case IDelta.INCREASE_ACCESS :
								return Util.isVisible(delta);
							default :
								return false;
						}
				}
			default :
				// IDelta.REMOVED
				switch(delta.getFlags()) {
					case IDelta.EXECUTION_ENVIRONMENT :
					case IDelta.CLINIT :
						return false;
					case IDelta.METHOD_MOVED_UP :
					case IDelta.FIELD_MOVED_UP :
					case IDelta.CHECKED_EXCEPTION :
					case IDelta.UNCHECKED_EXCEPTION :
						return Util.isVisible(delta);
				}
		}
		return false;
	}
	/**
	 * Checks the version number of the API component and creates a problem markers as needed
	 * @param reference
	 * @param component
	 */
	private void checkApiComponentVersion(final IApiComponent reference, final IApiComponent component) {
		if(ignoreComponentVersionCheck() || reference == null || component == null) {
			if(DEBUG) {
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
		if (DEBUG) {
			System.out.println("reference version of " + reference.getId() + " : " + refversion); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("component version of " + component.getId() + " : " + compversion); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IDelta[] breakingChanges = fBuildState.getBreakingChanges();
		if (breakingChanges.length != 0) {
			// make sure that the major version has been incremented
			if (compversion.getMajor() <= refversion.getMajor()) {
				newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier());
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
					// major version should be identical
					newversion = new Version(refversion.getMajor(), refversion.getMinor() + 1, 0, compversion.getQualifier());
					problem = createVersionProblem(
							IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE,
							new String[] {
								compversionval,
								refversionval
							},
							String.valueOf(newversion),
							collectDetails(compatibleChanges));
				} else if (compversion.getMinor() <= refversion.getMinor()) {
					// the minor version should be incremented
					newversion = new Version(compversion.getMajor(), compversion.getMinor() + 1, 0, compversion.getQualifier());
					problem = createVersionProblem(
							IApiProblem.MINOR_VERSION_CHANGE, 
							new String[] {
								compversionval,
								refversionval
							},
							String.valueOf(newversion),
							collectDetails(compatibleChanges));
				}
			} else if (compversion.getMinor() > refversion.getMinor()) {
				// the minor version should not be incremented
				problem = createVersionProblem(
						IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API, 
						new String[] {
							compversionval,
							refversionval
						},
						String.valueOf(refversion),
						Util.EMPTY_STRING);
			}
			// analyse version of required components
			ReexportedBundleVersionInfo info = null;
			if (problem != null) {
				switch (problem.getKind()) {
					case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE :
						// check if there is a version change required due to reexported bundles
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
									newversion = new Version(refversion.getMajor(), refversion.getMinor() + 1, 0, compversion.getQualifier());
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
						// check if there is a version change required due to reexported bundles
						info = checkBundleVersionsOfReexportedBundles(reference, component);
						if (info != null) {
							switch(info.kind) {
								case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
									// we keep this problem
									newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier());
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
						// check if there is a version change required due to reexported bundles
						info = checkBundleVersionsOfReexportedBundles(reference, component);
						if (info != null) {
							switch(info.kind) {
								case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE :
									// we return this one
									newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier());
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
								newversion = new Version(compversion.getMajor() + 1, 0, 0, compversion.getQualifier());
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
	 * @return
	 */
	private String collectDetails(final IDelta[] deltas) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		for (int i = 0, max = deltas.length; i < max ; i++) {
			printWriter.print("- "); //$NON-NLS-1$
			printWriter.println(deltas[i].getMessage());
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
				IElementDescriptor.T_RESOURCE, 
				kind);
	}
	
	/**
	 * Checks to see if there is a default API profile set in the workspace,
	 * if not create a marker
	 */
	private void checkDefaultBaselineSet() {
		if(ignoreDefaultBaselineCheck()) {
			if(DEBUG) {
				System.out.println("Ignoring check for default API baseline"); //$NON-NLS-1$
			}
			return;
		}
		if(DEBUG) {
			System.out.println("Checking if the default api baseline is set"); //$NON-NLS-1$
		}
		IApiProblem problem = ApiProblemFactory.newApiProfileProblem(
				Path.EMPTY.toPortableString(),
				null,
				null,
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID},
				new Object[] {new Integer(IApiMarkerConstants.DEFAULT_API_PROFILE_MARKER_ID)},
				-1,
				-1,
				-1,
				IElementDescriptor.T_RESOURCE,
				IApiProblem.API_PROFILE_MISSING);
		addProblem(problem);
	}
	
	/**
	 * Updates the work done on the monitor by 1 tick and polls to see if the monitor has been cancelled
	 * @param monitor
	 * @throws OperationCanceledException if the monitor has been cancelled
	 */
	private void updateMonitor(IProgressMonitor monitor) throws OperationCanceledException {
		updateMonitor(monitor, 1);
	}
	
	/**
	 * Updates the work done on the monitor by 1 tick and polls to see if the monitor has been cancelled
	 * @param monitor
	 * @param work
	 * @throws OperationCanceledException if the monitor has been cancelled
	 */
	private void updateMonitor(IProgressMonitor monitor, int work) throws OperationCanceledException {
		if(monitor != null) {
			monitor.worked(work);
			monitor.setTaskName(""); //$NON-NLS-1$
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}
	
	/**
	 * Returns the Java project associated with the given API component, or <code>null</code>
	 * if none.
	 * 
	 *@param component API component
	 * @return Java project or <code>null</code>
	 */
	private IJavaProject getJavaProject(IApiComponent component) {
		if (component instanceof PluginProjectApiComponent) {
			PluginProjectApiComponent pp = (PluginProjectApiComponent) component;
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
				return this.fFilterStore.isFiltered(problem);
			}
			return false;
		}

		IProject project = fJavaProject.getProject();
		// first the severity is checked
		if (ApiPlugin.getDefault().getSeverityLevel(ApiProblemFactory.getProblemSeverityId(problem), project) == ApiPlugin.SEVERITY_IGNORE) {
			return true;
		}

		IApiProfileManager manager = ApiProfileManager.getManager();
		IApiProfile profile = manager.getWorkspaceProfile();
		if(profile == null) {
			return false;
		}
		IApiComponent component = profile.getApiComponent(project.getName());
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
