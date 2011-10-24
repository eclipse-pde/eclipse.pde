/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.ApiDescriptionModifier;
import org.eclipse.pde.api.tools.internal.search.ConsumerReportConvertor;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.search.UseMetadata;
import org.eclipse.pde.api.tools.internal.search.UseReportConverter;
import org.eclipse.pde.api.tools.internal.search.UseSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.XmlSearchReporter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;

/**
 * Job that performs a API use scan.
 */
public class ApiUseScanJob extends Job {
	
	/**
	 * Associated launch configuration
	 */
	private ILaunchConfiguration configuration = null;

	/**
	 * List of components that were not searched
	 */
	Set notsearched = null;
	
	/**
	 * @param name
	 */
	public ApiUseScanJob(ILaunchConfiguration configuration) {
		super(MessageFormat.format(Messages.ApiUseScanJob_api_use_report, new String[]{configuration.getName()}));
		this.configuration = configuration;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		// Build API baseline
		SubMonitor localmonitor = SubMonitor.convert(monitor);
		try {
			IPath rootpath = null;
			String xmlPath = this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATH, (String)null);
			if (xmlPath == null) {
				abort(Messages.ApiUseScanJob_missing_xml_loc);
			}
			rootpath = new Path(xmlPath); 
			int kind = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_KIND, 0);
			if(kind != ApiUseLaunchDelegate.KIND_HTML_ONLY) {
				localmonitor.setTaskName(Messages.ApiUseScanJob_preparing_for_scan);
				localmonitor.setWorkRemaining((isSpecified(ApiUseLaunchDelegate.CREATE_HTML) ? 14 : 13));
				// create baseline
				IApiBaseline baseline = createApiBaseline(kind, localmonitor.newChild(1));
				Set ids = new HashSet();
				TreeSet scope = new TreeSet(Util.componentsorter);	
				getContext(baseline, ids, scope, localmonitor.newChild(2));
				int kinds = 0;
				if (isSpecified(ApiUseLaunchDelegate.MOD_API_REFERENCES)) {
					kinds |= IApiSearchRequestor.INCLUDE_API;
				}
				if (isSpecified(ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES)) {
					kinds |= IApiSearchRequestor.INCLUDE_INTERNAL;
				}
				if(isSpecified(ApiUseLaunchDelegate.MOD_ILLEGAL_USE)) {
					kinds |= IApiSearchRequestor.INCLUDE_ILLEGAL_USE;
				}
				UseSearchRequestor requestor = new UseSearchRequestor(ids, (IApiElement[]) scope.toArray(new IApiElement[scope.size()]), kinds);
				List jars = this.configuration.getAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, (List)null);
				String[] sjars = getStrings(jars);
				requestor.setJarPatterns(sjars);
				List api = this.configuration.getAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, (List)null);
				String[] sapi = getStrings(api);
				List internal = this.configuration.getAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, (List)null);
				String[] sinternal = getStrings(internal);
				if (sapi != null || sinternal != null) {
					// modify API descriptions
					IApiComponent[] components = baseline.getApiComponents();
					ApiDescriptionModifier visitor = new ApiDescriptionModifier(sinternal, sapi);
					for (int i = 0; i < components.length; i++) {
						IApiComponent component = components[i];
						if (!component.isSystemComponent() && !component.isSourceComponent()) {
							visitor.setApiDescription(component.getApiDescription());
							component.getApiDescription().accept(visitor, null);
						}
					}
				}
				xmlPath = rootpath.append("xml").toOSString(); //$NON-NLS-1$
				if(isSpecified(ApiUseLaunchDelegate.CLEAN_XML)) {
					localmonitor.setTaskName(Messages.ApiUseScanJob_cleaning_xml_loc);
					scrubReportLocation(new File(xmlPath), localmonitor.newChild(1));
				}
				UseMetadata data = new UseMetadata(
						kinds, 
						this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, (String)null), 
						this.configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, (String)null), 
						baseline.getLocation(), 
						xmlPath, 
						sapi, 
						sinternal, 
						sjars,
						DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()),
						this.configuration.getAttribute(ApiUseLaunchDelegate.DESCRIPTION, (String)null));
				IApiSearchReporter reporter = new XmlSearchReporter(
						xmlPath, 
						false);
				try {
					ApiSearchEngine engine = new ApiSearchEngine();
					engine.search(baseline, requestor, reporter, localmonitor.newChild(6));
				}
				finally {
					reporter.reportNotSearched((IApiElement[]) ApiUseScanJob.this.notsearched.toArray(new IApiElement[ApiUseScanJob.this.notsearched.size()]));
					reporter.reportMetadata(data);
					reporter.reportCounts();
					// Dispose the baseline if it's not managed (it's temporary)
					ApiBaselineManager apiManager = ApiBaselineManager.getManager();
					IApiBaseline[] baselines = apiManager.getApiBaselines();
					boolean dispose = true;
					for (int i = 0; i < baselines.length; i++) {
						if (baseline.equals(baselines[i])) {
							dispose = false;
							break;
						}
					}
					if (dispose) {
						baseline.dispose();
					}
				}
			}
			else {
				localmonitor.setWorkRemaining(10);
			}
			if(isSpecified(ApiUseLaunchDelegate.CREATE_HTML)) {
				localmonitor.setTaskName(Messages.ApiUseScanJob_generating_html_reports);
				String htmlPath = rootpath.append("html").toOSString(); //$NON-NLS-1$
				
				int reportType = ApiUseLaunchDelegate.REPORT_KIND_PRODUCER;
				if (this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_PRODUCER) == ApiUseLaunchDelegate.REPORT_KIND_CONSUMER){
					reportType = ApiUseLaunchDelegate.REPORT_KIND_CONSUMER;
				}
				
				performReportCreation(
						reportType,
						isSpecified(ApiUseLaunchDelegate.CLEAN_HTML),
						htmlPath,
						xmlPath,
						isSpecified(ApiUseLaunchDelegate.DISPLAY_REPORT),
						getStrings(this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, (List)null)),
						getStrings(this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, (List)null)),
						localmonitor.newChild(10));
			}
			
		} catch (CoreException e) {
			return e.getStatus();
		}
		finally {
			localmonitor.done();
		}
		return Status.OK_STATUS;
	}
	
	private String[] getStrings(List list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return (String[]) list.toArray(new String[list.size()]);
	}
	
	/**
	 * Throws a new {@link CoreException} with the given message
	 * @param message
	 * @throws CoreException
	 */
	void abort(String message) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiUIPlugin.PLUGIN_ID, message));
	}

	/**
	 * Creates a new {@link IApiBaseline} from the location set in the backing launch configuration
	 * @param kind
	 * @param monitor
	 * @return the new {@link IApiBaseline}
	 * @throws CoreException
	 */
	private IApiBaseline createApiBaseline(int kind, IProgressMonitor monitor) throws CoreException {
		ApiBaselineManager bmanager = ApiBaselineManager.getManager();
		switch (kind) {
			case ApiUseLaunchDelegate.KIND_API_BASELINE:
				String name = this.configuration.getAttribute(ApiUseLaunchDelegate.BASELINE_NAME, (String)null);
				if (name == null) {
					abort(Messages.ApiUseScanJob_baseline_name_missing);
				}
				IApiBaseline baseline = bmanager.getApiBaseline(name);
				if (baseline == null) {
					abort(MessageFormat.format(Messages.ApiUseScanJob_baseline_does_not_exist, new String[]{name}));
				}
				return baseline;
			case ApiUseLaunchDelegate.KIND_INSTALL_PATH:
				String path = this.configuration.getAttribute(ApiUseLaunchDelegate.INSTALL_PATH, (String)null);
				if (path == null) {
					abort(Messages.ApiUseScanJob_unspecified_install_path);
				}
				File file = new File(path);
				if (!file.exists() || !file.isDirectory()) {
					abort(MessageFormat.format(Messages.ApiUseScanJob_intall_dir_does_no_exist, new String[]{path}));
				}
				return createBaseline(path, monitor);
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION:
				String memento = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, (String)null);
				if (memento == null) {
					abort(Messages.ApiUseScanJob_target_unspecified);
				}
				ITargetPlatformService service = getTargetService();
				ITargetHandle handle = service.getTarget(memento);
				ITargetDefinition definition = handle.getTargetDefinition();
				return createBaseline(definition, monitor);
			default:
				abort(Messages.ApiUseScanJob_target_api_unspecified);
		}
		return null;
	}
		
	/**
	 * Returns the target service or <code>null</code>
	 * 
	 * @return service or <code>null</code>
	 */
	private ITargetPlatformService getTargetService() {
		return (ITargetPlatformService) ApiPlugin.getDefault().acquireService(ITargetPlatformService.class.getName());
	}
	
	/**
	 * Collects the context of reference ids and scope elements in one pass
	 * @param baseline the baseline to check components from
	 * @param ids the reference ids to consider
	 * @param scope the scope of elements to search
	 * @param monitor
	 * @throws CoreException
	 */
	private void getContext(IApiBaseline baseline, Set ids, Set scope, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_collecting_target_components, 10);
		this.notsearched = new TreeSet(Util.componentsorter);
		String regex = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, (String)null);
		Pattern pattern = getPattern(regex);
		regex = this.configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, (String)null);
		Pattern pattern2 = getPattern(regex);
		IApiComponent[] components = baseline.getApiComponents();
		localmonitor.setWorkRemaining(components.length);
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_checking_component, component.getSymbolicName()));
			Util.updateMonitor(localmonitor, 1);
			if (acceptComponent(component, pattern, true)) {
				ids.add(component.getSymbolicName());
			}
			if (acceptComponent(component, pattern2, false)) {
				scope.add(component);
			}
			else {
				localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_skipping_component, component.getSymbolicName()));
				this.notsearched.add(new SkippedComponent(component.getSymbolicName(), component.getVersion(), null));
			}
		}
	}
	
	/**
	 * Returns a pattern for the given regular expression or <code>null</code> if none.
	 * 
	 * @param regex expression, <code>null</code> or empty
	 * @return associated pattern or <code>null</code>
	 */
	private Pattern getPattern(String regex) {
		if (regex == null) {
			return null;
		}
		if (regex.trim().length() == 0) {
			return null;
		}
		return Pattern.compile(regex);
	}
	
	/**
	 * Returns if we should add the given component to our search scope
	 * @param component
	 * @param pattern
	 * @param allowresolve
	 * @return
	 * @throws CoreException
	 */
	boolean acceptComponent(IApiComponent component, Pattern pattern, boolean allowresolve) throws CoreException {
		if(!allowresolve) {
			ResolverError[] errors = component.getErrors();
			if(errors != null) {
				this.notsearched.add(new SkippedComponent(component.getSymbolicName(), component.getVersion(), errors)); 
				return false;
			}
		}
		if(component.isSystemComponent()) {
			return false;
		}
		if(pattern != null) {
			return pattern.matcher(component.getSymbolicName()).matches();
		}
		return true;
	}
	
	
	
	/**
	 * Returns if the given search modifier is set in the backing {@link ILaunchConfiguration}
	 * @param modifier
	 * @return true if the modifier is set, false otherwise
	 * @throws CoreException
	 */
	private boolean isSpecified(int modifier) throws CoreException {
		int modifiers = configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_MODIFIERS, 0);
		return (modifiers & modifier) > 0;
	}
	
	/**
	 * Performs the report creation
	 * @param reportType what report converter to use, either {@link ApiUseLaunchDelegate#REPORT_KIND_PRODUCER} or {@link ApiUseLaunchDelegate#REPORT_KIND_CONSUMER}
	 * @param cleanh
	 * @param hlocation
	 * @param rlocation
	 * @param openhtml
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	void performReportCreation(int reportType, 
			boolean cleanh, 
			String hlocation, 
			String rlocation, 
			boolean openhtml,
			String[] topatterns,
			String[] frompatterns,
			IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_creating_html_reports, 10);
		if(cleanh) {
			cleanReportLocation(hlocation, localmonitor.newChild(5));
		}
		try {
			UseReportConverter converter = null;
			if (reportType == ApiUseLaunchDelegate.REPORT_KIND_CONSUMER){
				converter = new ConsumerReportConvertor(hlocation, rlocation, topatterns, frompatterns);
			} else {
				converter = new UseReportConverter(hlocation, rlocation, topatterns, frompatterns);
			}
			
			converter.convert(null, localmonitor.newChild(5));
			if(openhtml) {
				final File index = converter.getReportIndex();
				if(index != null) {
					UIJob ujob = new UIJob(Util.EMPTY_STRING){
						public IStatus runInUIThread(IProgressMonitor monitor) {
							IEditorDescriptor edesc = null;
							try {
								edesc = IDE.getEditorDescriptor(index.getName());
								IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
								IDE.openEditor(window.getActivePage(), 
										index.toURI(), 
										edesc.getId(), 
										true);
							} catch (PartInitException e) {
								e.printStackTrace();
							}
							return Status.OK_STATUS;
						}
					};
					ujob.setPriority(Job.INTERACTIVE);
					ujob.setSystem(true);
					ujob.schedule();
				}
			}
		}
		catch (OperationCanceledException oce) {
			//re-throw
			throw oce;
		}
		catch(Exception e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Cleans the report location specified by the parameter reportLocation
	 * @param monitor
	 */
	void cleanReportLocation(String location, IProgressMonitor monitor) {
		File file = new File(location);
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_deleting_old_reports, IProgressMonitor.UNKNOWN);
		if(file.exists()) {
			Util.updateMonitor(localmonitor, 0);
			scrubReportLocation(file, localmonitor);
			localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_deleting_root_folder, file.getName()));
		}
	}
	
	/**
	 * Cleans the location if it exists
	 * @param file
	 * @param monitor
	 */
	void scrubReportLocation(File file, IProgressMonitor monitor) {
		if(file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				monitor.subTask(NLS.bind(Messages.ApiUseScanJob_deleteing_file, files[i].getPath()));
				Util.updateMonitor(monitor, 0);
				if(files[i].isDirectory()) {
					scrubReportLocation(files[i], monitor);
				}
				else {
					files[i].delete();
				}
			}
			file.delete();
		}
	}	
	
	/**
	 * Creates an API baseline from a target definition.
	 * 
	 * @param definition
	 * @param monitor progress monitor
	 */
	private IApiBaseline createBaseline(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_reading_target, 10);
		definition.resolve(localmonitor.newChild(2));
		Util.updateMonitor(localmonitor, 1);
		TargetBundle[] bundles = definition.getBundles();
		List components = new ArrayList();
		IApiBaseline profile = ApiModelFactory.newApiBaseline(definition.getName());
		localmonitor.setWorkRemaining(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			Util.updateMonitor(localmonitor, 1);
			if (bundles[i].getStatus().isOK() && !bundles[i].isSourceBundle()) {
				IApiComponent component = ApiModelFactory.newApiComponent(profile, URIUtil.toFile(bundles[i].getBundleInfo().getLocation()).getAbsolutePath());
				if (component != null) {
					components.add(component);
				}
			}
		}
		profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
		return profile;
	}	
	
	/**
	 * Creates a baseline at an install location
	 * 
	 * @param path
	 * @param monitor
	 */
	private IApiBaseline createBaseline(String installLocation, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_scanning, 10);
		IApiBaseline baseline = ApiModelFactory.newApiBaseline(this.configuration.getName());
		IApiComponent[] components = ApiModelFactory.addComponents(baseline, installLocation, localmonitor);
		if (components.length == 0){
			abort(MessageFormat.format(Messages.ApiUseScanJob_no_bundles, new String[]{installLocation}));
		}
		return baseline;
	}	
	
}
