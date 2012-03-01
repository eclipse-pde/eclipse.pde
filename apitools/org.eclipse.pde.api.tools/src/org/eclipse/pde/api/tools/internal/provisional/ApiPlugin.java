/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.SessionManager;
import org.eclipse.pde.api.tools.internal.WorkspaceDeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.FileManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;

/**
 * API Tools core plug-in.
 * API tools can be run with or without an OSGi framework.
 * 
 * @since 1.0.0
 */
public class ApiPlugin extends Plugin implements ISaveParticipant, DebugOptionsListener {
	
	/**
	 * Constant representing the expected name for an execution environment description fragment
	 * Value is <code>org.eclipse.pde.api.tools.ee"
	 */
	private static final String EE_DESCRIPTION_PREFIX = "org.eclipse.pde.api.tools.ee"; //$NON-NLS-1$
	/**
	 * Constant representing the name of the javadoc tag extension point.
	 * Value is <code>apiJavadocTags</code>
	 */
	public static final String EXTENSION_JAVADOC_TAGS = "apiJavadocTags"; //$NON-NLS-1$
	/**
	 * The plug-in identifier of the PDE API tool support
	 * (value <code>"org.eclipse.pde.api.tools"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.pde.api.tools" ; //$NON-NLS-1$
	/**
	 * The API Tools nature id
	 * (value <code>"org.eclipse.pde.api.tools.apiAnalysisNature"</code>).
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".apiAnalysisNature" ; //$NON-NLS-1$
	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 120;
	/**
	 * Status code indicating an unexpected error
	 */
	public static final int ERROR = 121;
	
	/**
	 * Status code indicating a resolution error
	 */
	public static final int REPORT_RESOLUTION_ERRORS = 122;

	/**
	 * Status code indicating that a baseline is disposed
	 */
	public static final int REPORT_BASELINE_IS_DISPOSED = 123;

	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>0</code>
	 */
	public static final int SEVERITY_IGNORE = 0;
	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>1</code>
	 */
	public static final int SEVERITY_WARNING = 1;
	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>2</code>
	 */
	public static final int SEVERITY_ERROR = 2;
	
	/**
	 * Constant representing the preference value 'ignore'.
	 * Value is: <code>Ignore</code>
	 */
	public static final String VALUE_IGNORE = "Ignore"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'warning'.
	 * Value is: <code>Warning</code>
	 */
	public static final String VALUE_WARNING = "Warning"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'error'.
	 * Value is: <code>Error</code>
	 */
	public static final String VALUE_ERROR = "Error"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'disabled'.
	 * Value is: <code>Disabled</code>
	 */
	public static final String VALUE_DISABLED = "Disabled"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'enabled'.
	 * Value is: <code>Enabled</code>
	 */
	public static final String VALUE_ENABLED = "Enabled"; //$NON-NLS-1$
	/**
	 * The identifier for the API builder
	 * Value is: <code>"org.eclipse.pde.api.tools.apiAnalysisBuilder"</code>
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".apiAnalysisBuilder" ; //$NON-NLS-1$
	
	/**
	 * Preference ID for a knownEEFragments of EE fragments that were previously installed, the stored preference
	 * string must be a list of name followed by version separated by semicolons ';'.
	 * <p>
	 * ex: "org.eclipse.one;1.0.0;org.eclipse.two;2.0.0;"
	 * </p><p>
	 * Value is: <code>knownEEFragments</code>
	 * </p>
	 */
	public static final String KNOWN_EE_FRAGMENTS = "knownEEFragments"; //$NON-NLS-1$
	/**
	 * Singleton instance of the plugin
	 */
	private static ApiPlugin fgDefault = null;
	/**
	 * Singleton instance of the {@link JavadocTagManager}
	 */
	private static JavadocTagManager fgTagManager = null;
	/**
	 * Singleton instance of the {@link ISessionManager}
	 */
	private static ISessionManager fgSessionManager = null;
	/**
	 * This bundle's OSGi context
	 */
	private BundleContext fBundleContext = null;
	
	private static boolean DEBUG = false;
	
	/**
	 * Private debug options
	 */
	private static final String DEBUG_FLAG = PLUGIN_ID + "/debug"; //$NON-NLS-1$
	private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder" ; //$NON-NLS-1$
	private static final String DELTA_DEBUG = PLUGIN_ID + "/debug/delta" ; //$NON-NLS-1$
	private static final String SEARCH_DEBUG = PLUGIN_ID + "/debug/search"; //$NON-NLS-1$
	private static final String CLASSFILE_VISITOR_DEBUG = PLUGIN_ID + "/debug/classfilevisitor" ; //$NON-NLS-1$
	private static final String DESCRIPTOR_FRAMEWORK_DEBUG = PLUGIN_ID + "/debug/descriptor/framework" ; //$NON-NLS-1$
	private static final String TAG_SCANNER_DEBUG = PLUGIN_ID + "/debug/tagscanner" ; //$NON-NLS-1$
	private static final String PLUGIN_WORKSPACE_COMPONENT_DEBUG = PLUGIN_ID + "/debug/pluginworkspacecomponent"; //$NON-NLS-1$
	private static final String API_PROFILE_MANAGER_DEBUG = PLUGIN_ID + "/debug/profilemanager"; //$NON-NLS-1$
	private static final String API_FILTER_STORE_DEBUG = PLUGIN_ID + "/debug/apifilterstore"; //$NON-NLS-1$
	private static final String API_REFERENCE_ANALYZER_DEBUG = PLUGIN_ID + "/debug/refanalyzer"; //$NON-NLS-1$
	private static final String PROBLEM_DETECTOR_DEBUG = PLUGIN_ID + "/debug/problemdetector"; //$NON-NLS-1$
	private static final String REFERENCE_RESOLVER_DEBUG = PLUGIN_ID + "/debug/refresolver"; //$NON-NLS-1$
	private static final String API_DESCRIPTION = PLUGIN_ID + "/debug/apidescription"; //$NON-NLS-1$
	private static final String WORKSPACE_DELTA_PROCESSOR = PLUGIN_ID + "/debug/workspacedeltaprocessor"; //$NON-NLS-1$
	private static final String API_ANALYZER_DEBUG = PLUGIN_ID + "/debug/apianalyzer"; //$NON-NLS-1$
	private static final String USE_REPORT_CONVERTER_DEBUG = PLUGIN_ID + "/debug/usereportconverter"; //$NON-NLS-1$

	/**
	 * Constant used for controlling tracing in the report converter
	 */
	public static boolean DEBUG_USE_REPORT_CONVERTER = false;
	/**
	 * Constant used for controlling tracing in the search engine
	 */
	public static boolean DEBUG_SEARCH_ENGINE = false;
	/**
	 * Constant used for controlling tracing in the scanner
	 */
	public static boolean DEBUG_TAG_SCANNER = false;
	/**
	 * Constant used for controlling tracing in the API comparator
	 */
	public static boolean DEBUG_API_COMPARATOR = false;
	/**
	 * Constant used for controlling tracing in the plug-in workspace component
	 */
	public static boolean DEBUG_PROJECT_COMPONENT = false;
	/**
	 * Constant used for controlling tracing in the descriptor framework
	 */
	public static boolean DEBUG_ELEMENT_DESCRIPTOR_FRAMEWORK = false;
	/**
	 * Constant used for controlling tracing in the class file comparator
	 */
	public static boolean DEBUG_CLASSFILE_COMPARATOR = false;
	/**
	 * Constant used for controlling tracing in the search engine
	 */
	public static boolean DEBUG_REFERENCE_RESOLVER = false;
	/**
	 * Constant used for controlling tracing in the visitor
	 */
	public static boolean DEBUG_REFERENCE_EXTRACTOR = false;
	/**
	 * Constant used for controlling tracing in the search engine
	 */
	public static boolean DEBUG_REFERENCE_ANALYZER = false;
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	public static boolean DEBUG_API_ANALYZER = false;
	/**
	 * Constant used for controlling tracing in the problem detectors
	 */
	public static boolean DEBUG_PROBLEM_DETECTOR = false;
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	public static boolean DEBUG_WORKSPACE_DELTA_PROCESSOR = false;
	/**
	 * Constant used for controlling tracing in the plug-in workspace component
	 */
	public static boolean DEBUG_FILTER_STORE = false;
	/**
	 * Constant used for controlling tracing in the API descriptions
	 */
	public static boolean DEBUG_API_DESCRIPTION = false;
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	public static boolean DEBUG_BASELINE_MANAGER = false;
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	public static boolean DEBUG_BUILDER = false;
	
	public static String[] AllCompatibilityKeys = new String[] {
		IApiProblemTypes.API_COMPONENT_REMOVED_TYPE,
		IApiProblemTypes.API_COMPONENT_REMOVED_API_TYPE,
		IApiProblemTypes.API_COMPONENT_REMOVED_REEXPORTED_TYPE,
		IApiProblemTypes.API_COMPONENT_REMOVED_REEXPORTED_API_TYPE,
		IApiProblemTypes.ANNOTATION_REMOVED_FIELD,
		IApiProblemTypes.ANNOTATION_REMOVED_METHOD,
		IApiProblemTypes.ANNOTATION_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.ANNOTATION_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
		IApiProblemTypes.INTERFACE_ADDED_FIELD,
		IApiProblemTypes.INTERFACE_ADDED_METHOD,
		IApiProblemTypes.INTERFACE_ADDED_RESTRICTIONS,
		IApiProblemTypes.INTERFACE_ADDED_SUPER_INTERFACE_WITH_METHODS,
		IApiProblemTypes.INTERFACE_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.INTERFACE_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.INTERFACE_REMOVED_FIELD,
		IApiProblemTypes.INTERFACE_REMOVED_METHOD,
		IApiProblemTypes.INTERFACE_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.INTERFACE_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.ENUM_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.ENUM_REMOVED_FIELD,
		IApiProblemTypes.ENUM_REMOVED_ENUM_CONSTANT,
		IApiProblemTypes.ENUM_REMOVED_METHOD,
		IApiProblemTypes.ENUM_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.CLASS_ADDED_METHOD,
		IApiProblemTypes.CLASS_ADDED_RESTRICTIONS,
		IApiProblemTypes.CLASS_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		IApiProblemTypes.CLASS_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.CLASS_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.CLASS_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.CLASS_REMOVED_FIELD,
		IApiProblemTypes.CLASS_REMOVED_METHOD,
		IApiProblemTypes.CLASS_REMOVED_CONSTRUCTOR,
		IApiProblemTypes.CLASS_REMOVED_SUPERCLASS,
		IApiProblemTypes.CLASS_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.CLASS_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.FIELD_ADDED_VALUE,
		IApiProblemTypes.FIELD_CHANGED_TYPE,
		IApiProblemTypes.FIELD_CHANGED_VALUE,
		IApiProblemTypes.FIELD_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
		IApiProblemTypes.FIELD_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.FIELD_CHANGED_STATIC_TO_NON_STATIC,
		IApiProblemTypes.FIELD_CHANGED_NON_STATIC_TO_STATIC,
		IApiProblemTypes.FIELD_REMOVED_VALUE,
		IApiProblemTypes.FIELD_REMOVED_TYPE_ARGUMENT,
		IApiProblemTypes.METHOD_ADDED_RESTRICTIONS,
		IApiProblemTypes.METHOD_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.METHOD_CHANGED_VARARGS_TO_ARRAY,
		IApiProblemTypes.METHOD_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		IApiProblemTypes.METHOD_CHANGED_NON_STATIC_TO_STATIC,
		IApiProblemTypes.METHOD_CHANGED_STATIC_TO_NON_STATIC,
		IApiProblemTypes.METHOD_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
		IApiProblemTypes.METHOD_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.CONSTRUCTOR_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
		IApiProblemTypes.CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.TYPE_PARAMETER_ADDED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_CHANGED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_REMOVED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND,
		IApiProblemTypes.REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED,
//		IApiProblemTypes.REPORT_API_CHANGE_WHEN_MINOR_VERSION_INCREMENTED,
	};
	/**
	 * A set of listeners that want to participate in the saving life-cycle of the workbench
	 * via this plugin
	 */
	private HashSet savelisteners = new HashSet();
	
	/**
	 * This is used to log resolution errors only once per session
	 */
	private int logBits = 0;
	
	/**
	 * This is used to log resolution errors only once per session.
	 * This is used outside the workbench.
	 */
	private static int LogBits= 0;
	
	/**
	 * Standard delta processor for Java element changes
	 */
	private WorkspaceDeltaProcessor deltaProcessor = null;
	
	private static final int RESOLUTION_LOG_BIT = 1;
	private static final int BASELINE_DISPOSED_LOG_BIT = 2;

	/**
	 * Constructor
	 */
	public ApiPlugin() {
		super();
		fgDefault = this;
	}
	
	/**
	 * @return The singleton instance of the plugin
	 */
	public static ApiPlugin getDefault() {
		return fgDefault;
	}
	
	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 */
	public static void log(IStatus status) {
		ApiPlugin getDefault = getDefault();
		if (getDefault == null) {
			switch(status.getCode()) {
				case REPORT_RESOLUTION_ERRORS :
					if ((LogBits & RESOLUTION_LOG_BIT) == 0) {
						Throwable exception = status.getException();
						if (exception != null) {
							exception.printStackTrace();
						}
						LogBits |= RESOLUTION_LOG_BIT;
					}
					break;
				case REPORT_BASELINE_IS_DISPOSED :
					if ((LogBits & BASELINE_DISPOSED_LOG_BIT) == 0) {
						Throwable exception = status.getException();
						if (exception != null) {
							exception.printStackTrace();
						}
						LogBits |= BASELINE_DISPOSED_LOG_BIT;
					}
					break;
				default:
					Throwable exception = status.getException();
					if (exception != null) {
						exception.printStackTrace();
					}
			}
		} else {
			switch(status.getCode()) {
				case REPORT_RESOLUTION_ERRORS :
					if ((getDefault.logBits & RESOLUTION_LOG_BIT) == 0) {
						getDefault.getLog().log(status);
						getDefault.logBits |= RESOLUTION_LOG_BIT;
					}
					break;
				case REPORT_BASELINE_IS_DISPOSED :
					if ((getDefault.logBits & BASELINE_DISPOSED_LOG_BIT) == 0) {
						getDefault.getLog().log(status);
						getDefault.logBits |= BASELINE_DISPOSED_LOG_BIT;
					}
					break;
				default:
					getDefault.getLog().log(status);
			}
		}
	}

	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 */
	public static void log(Throwable t) {
		log(newErrorStatus("Error logged from API Tools Core: ", t)); //$NON-NLS-1$
	}
	
	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message the error message to log
	 */
	public static void logErrorMessage(String message) {
		// this message is intentionally not internationalized, as an exception may
		// be due to the resource bundle itself
		log(newErrorStatus("Internal message logged from API Tools Core: " + message, null)); //$NON-NLS-1$	
	}
	
	/**
	 * Returns a new error status for this plug-in with the given message
	 * @param message the message to be included in the status
	 * @param exception the exception to be included in the status or <code>null</code> if none
	 * @return a new error status
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, exception);
	}
	
	/**
	 * Returns whether the API tools bundle is running inside an OSGi framework.
	 * 
	 * @return whether the API tools bundle is running inside an OSGi framework
	 */
	public static boolean isRunningInFramework() {
		return fgDefault != null;
	}

	/**
	 * Returns the {@link IApiBaselineManager}, allowing clients to add/remove and search
	 * for {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline}s stored in the manager.
	 * 
	 * @return the singleton instance of the {@link IApiProfileManager}
	 */
	public IApiBaselineManager getApiBaselineManager() {
		return ApiBaselineManager.getManager();
	}
	
	/**
	 * @return The singleton instance of the {@link JavadocTagManager}
	 */
	public static JavadocTagManager getJavadocTagManager() {
		if(fgTagManager == null) {
			fgTagManager = new JavadocTagManager();
		}
		return fgTagManager;
	}
	
	/**
	 * Adds the given save participant to the listing of participants to 
	 * be notified when the workbench saving life-cycle occurs. If the specified
	 * participant is <code>null</code> no changes are made.
	 * @param participant
	 */
	public void addSaveParticipant(ISaveParticipant participant) {
		if(participant != null) {
			savelisteners.add(participant);
		}
	}
	
	/**
	 * Removes the given save participant from the current listing.
	 * If the specified participant is <code>null</code> no changes are made.
	 * @param participant
	 */
	public void removeSaveParticipant(ISaveParticipant participant) {
		if(participant != null) {
			savelisteners.remove(participant);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.doneSaving(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.prepareToSave(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.rollback(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.saving(context);
		}
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		if(node != null) {
			try {
				node.flush();
			} catch (BackingStoreException e) {
				log(e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		try {
			super.start(context);
			Hashtable props = new Hashtable(2);
			props.put(org.eclipse.osgi.service.debug.DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
			context.registerService(DebugOptionsListener.class.getName(), this, props);
		} finally {
			ResourcesPlugin.getWorkspace().addSaveParticipant(PLUGIN_ID, this);
			fBundleContext = context;
			deltaProcessor = new WorkspaceDeltaProcessor();
			JavaCore.addElementChangedListener(deltaProcessor, ElementChangedEvent.POST_CHANGE);
			ResourcesPlugin.getWorkspace().addResourceChangeListener(deltaProcessor, IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_BUILD);
			checkForEEDescriptionChanges();
		}
	}
	
	/**
	 * Checks if the current set of installed execution environment description fragments differs from the last
	 * time this workspace was started.  If so, a full api analysis build is run.
	 */
	private void checkForEEDescriptionChanges() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		if (node == null){
			return;
		}
		String knownFragmentsList = node.get(KNOWN_EE_FRAGMENTS, null);
		Bundle[] allFragments = Platform.getFragments(fBundleContext.getBundle());
		if (allFragments == null)
			allFragments= new Bundle[0];

		// No preference stored yet, set the preference for future startup
		if (knownFragmentsList == null){
			String list = getListOfEEFragments(allFragments);
			node.put(KNOWN_EE_FRAGMENTS, list);
			try {
				node.flush();
			} catch (BackingStoreException e) {
				log(e);
			}
			return;
		}
		
		// Break the list into a set we can search
		Set knownFragments = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(knownFragmentsList, ";"); //$NON-NLS-1$
		String name = null;
		while (tokenizer.hasMoreTokens()) {
			name = tokenizer.nextToken().trim();
			if (name.length() > 0 && tokenizer.hasMoreTokens()){
				knownFragments.add(new NameVersionDescriptor(name, tokenizer.nextToken()));
			}
		}
		
		// Figure out if we need to rebuild (fragments added or removed
		boolean mustRebuild = false; 
		for (int i = 0; i < allFragments.length; i++) {
			// We only care about 
			if (allFragments[i].getSymbolicName().indexOf(EE_DESCRIPTION_PREFIX) >= 0){
				NameVersionDescriptor current = new NameVersionDescriptor(allFragments[i].getSymbolicName(), allFragments[i].getVersion().toString());
				if (knownFragments.contains(current)){
					knownFragments.remove(current);
				} else {
					// New EE fragment installed
					mustRebuild = true;
					break;
				}
			}
		}
			
		if (knownFragments.size() > 0){
			// EE fragment removed
			mustRebuild = true;
		}
			
		// Run a full api analysis build and update the preference
		if (mustRebuild){
			IProject[] projects = Util.getApiProjects();
			if (projects != null){
				for (int i = 0; i < projects.length; i++) {
					try {
						projects[i].build(IncrementalProjectBuilder.FULL_BUILD, ApiPlugin.BUILDER_ID, null, null);
					} catch (CoreException e) {
						log(e.getStatus());
					}
				}
			}
			
			// Write out the preferences so we don't rebuild on next startup
			String list = getListOfEEFragments(allFragments);
			node.put(KNOWN_EE_FRAGMENTS, list);
			try {
				node.flush();
			} catch (BackingStoreException e) {
				log(e);
			}
		}
	}

	/**
	 * @return a list of fragments that consider this bundle their host and symbolic names contain {@link #EE_DESCRIPTION_PREFIX}
	 */
	private String getListOfEEFragments(Bundle[] allFragments){
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < allFragments.length; i++) {
			if (allFragments[i].getSymbolicName().indexOf(EE_DESCRIPTION_PREFIX) >= 0){
				result.append(allFragments[i].getSymbolicName());
				result.append(';');
				result.append(allFragments[i].getVersion().toString());
				result.append(';');
			}
		}
		return result.toString();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			ApiDescriptionManager.shutdown();
			ApiBaselineManager.getManager().stop();
			ResourcesPlugin.getWorkspace().removeSaveParticipant(PLUGIN_ID);
			FileManager.getManager().deleteFiles();
			fBundleContext = null;
			if(deltaProcessor != null) {
				JavaCore.removeElementChangedListener(deltaProcessor);
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(deltaProcessor);
			}
		}
		finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the severity for the specific key from the given {@link IProject}.
	 * If the project does not have project specific settings, the workspace preference
	 * is returned. If <code>null</code> is passed in as the project the workspace
	 * preferences are consulted.
	 * 
	 * @param prefkey the given preference key
	 * @param project the given project or <code>null</code>
	 * @return the severity level for the given pref key
	 */
	public int getSeverityLevel(String prefkey, IProject project) {
		IPreferencesService service = Platform.getPreferencesService();
		IScopeContext[] context = null;
		if(hasProjectSettings(project)) {
			context = new IScopeContext[] {new ProjectScope(project), InstanceScope.INSTANCE,DefaultScope.INSTANCE};
		}
		else {
			context = new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE};
		}
		String value = service.get(prefkey, null, getPreferences(context));
		if(VALUE_ERROR.equals(value)) {
			return SEVERITY_ERROR;
		}
		if(VALUE_WARNING.equals(value)) {
			return SEVERITY_WARNING;
		}
		return SEVERITY_IGNORE;
	}
	
	/**
	 * Returns the array of {@link IEclipsePreferences} nodes to look in to determine
	 * the value of a given preference. 
	 * This method will return <code>null</code> iff:
	 * <ul>
	 * <li>the given array of contexts are <code>null</code></li> 
	 * <li>if no nodes could be determined from the given contexts</li>
	 * </ul>
	 * @param context
	 * @return the array of {@link IEclipsePreferences} to look in or <code>null</code>.
	 * @since 1.1
	 */
	IEclipsePreferences[] getPreferences(IScopeContext[] context) {
		if(context != null) {
			ArrayList nodes = new ArrayList(context.length);
			IEclipsePreferences node = null;
			for (int i = 0; i < context.length; i++) {
				node = context[i].getNode(PLUGIN_ID);
				if(node != null) {
					nodes.add(node);
				}
			}
			if(nodes.size() > 0) {
				return (IEclipsePreferences[]) nodes.toArray(new IEclipsePreferences[nodes.size()]);
			}
		}
		return null;
	}
	
	/**
	 * Returns if the given project has project-specific settings.
	 * 
	 * @param project
	 * @return true if the project has specific settings, false otherwise
	 * @since 1.1
	 */
	boolean hasProjectSettings(IProject project) {
		if(project != null) {
			ProjectScope scope = new ProjectScope(project);
			IEclipsePreferences node = scope.getNode(PLUGIN_ID);
			try {
				return node != null && node.keys().length > 0;
			}
			catch(BackingStoreException bse) {
				log(bse);
			}
		}
		return false;
	}
	
	public ISessionManager getSessionManager() {
		if(fgSessionManager == null) {
			fgSessionManager = new SessionManager();
		}
		return fgSessionManager;
	}

	/**
	 * Returns the enable state for the specific key from the given {@link IProject}.
	 * If the project does not have project specific settings, the workspace preference
	 * is returned. If <code>null</code> is passed in as the project the workspace
	 * preferences are consulted.
	 * 
	 * @param prefkey the given preference key
	 * @param project the given project or <code>null</code>
	 * @return the enable state
	 */
	public boolean getEnableState(String prefkey, IProject project) {
		IPreferencesService service = Platform.getPreferencesService();
		IScopeContext[] context = null;
		if(hasProjectSettings(project)) {
			context = new IScopeContext[] {new ProjectScope(project), InstanceScope.INSTANCE, DefaultScope.INSTANCE};
		}
		else {
			context = new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE};
		}
		String value = service.get(prefkey, null, getPreferences(context));
		return VALUE_ENABLED.equals(value);
	}
	
	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 * 
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 */
	public Object acquireService(String serviceName) {
		ServiceReference reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptionsListener#optionsChanged(org.eclipse.osgi.service.debug.DebugOptions)
	 */
	public void optionsChanged(DebugOptions options) {
		DEBUG = options.getBooleanOption(DEBUG_FLAG, false);
		boolean option = options.getBooleanOption(DELTA_DEBUG, false);
		DEBUG_CLASSFILE_COMPARATOR = DEBUG && option;
		DEBUG_API_COMPARATOR = DEBUG_CLASSFILE_COMPARATOR;
		DEBUG_BUILDER = DEBUG && options.getBooleanOption(BUILDER_DEBUG, false);
		DEBUG_SEARCH_ENGINE = DEBUG && options.getBooleanOption(SEARCH_DEBUG, false);
		DEBUG_REFERENCE_EXTRACTOR = DEBUG && options.getBooleanOption(CLASSFILE_VISITOR_DEBUG, false);
		DEBUG_ELEMENT_DESCRIPTOR_FRAMEWORK = DEBUG && options.getBooleanOption(DESCRIPTOR_FRAMEWORK_DEBUG, false);
		DEBUG_TAG_SCANNER = DEBUG && options.getBooleanOption(TAG_SCANNER_DEBUG, false);
		DEBUG_PROJECT_COMPONENT = DEBUG && options.getBooleanOption(PLUGIN_WORKSPACE_COMPONENT_DEBUG, false);
		DEBUG_BASELINE_MANAGER = DEBUG && options.getBooleanOption(API_PROFILE_MANAGER_DEBUG, false);
		DEBUG_FILTER_STORE = DEBUG && options.getBooleanOption(API_FILTER_STORE_DEBUG, false);
		DEBUG_REFERENCE_ANALYZER = DEBUG && options.getBooleanOption(API_REFERENCE_ANALYZER_DEBUG, false);
		DEBUG_REFERENCE_RESOLVER = DEBUG && options.getBooleanOption(REFERENCE_RESOLVER_DEBUG, false);
		DEBUG_PROBLEM_DETECTOR = DEBUG && options.getBooleanOption(PROBLEM_DETECTOR_DEBUG, false);
		DEBUG_API_DESCRIPTION = DEBUG && options.getBooleanOption(API_DESCRIPTION, false);
		DEBUG_WORKSPACE_DELTA_PROCESSOR = DEBUG && options.getBooleanOption(WORKSPACE_DELTA_PROCESSOR, false);
		DEBUG_API_ANALYZER = DEBUG && options.getBooleanOption(API_ANALYZER_DEBUG, false);
		DEBUG_USE_REPORT_CONVERTER = DEBUG && options.getBooleanOption(USE_REPORT_CONVERTER_DEBUG, false);
	}
}
