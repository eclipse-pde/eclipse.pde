/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.search.ApiUseReportConverter;
import org.eclipse.pde.api.tools.internal.search.ApiUseSearchRequestor;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.search.XMLApiSearchReporter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

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
			localmonitor.setTaskName(Messages.ApiUseScanJob_preparing_for_scan);
			localmonitor.setWorkRemaining((isSpecified(ApiUseLaunchDelegate.CREATE_HTML) ? 12 : 11));
			// create baseline
			IApiBaseline baseline = createApiBaseline(localmonitor.newChild(1));
			Set targetIds = getTargetComponentIds(baseline, localmonitor.newChild(1));
			IApiComponent[] components = getSearchScope(baseline, localmonitor.newChild(1));			
			int kinds = 0;
			if (isSpecified(ApiUseLaunchDelegate.MOD_API_REFERENCES)) {
				kinds |= IApiSearchRequestor.INCLUDE_API;
			}
			if (isSpecified(ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES)) {
				kinds |= IApiSearchRequestor.INCLUDE_INTERNAL;
			}
			IApiSearchRequestor requestor = new ApiUseSearchRequestor(targetIds, components, kinds);
			IPath rootpath = null;
			String xmlPath = this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATH, (String)null);
			if (xmlPath == null) {
				abort(Messages.ApiUseScanJob_missing_xml_loc);
			}
			rootpath = new Path(xmlPath); 
			xmlPath = rootpath.append("xml").toOSString(); //$NON-NLS-1$
			if(isSpecified(ApiUseLaunchDelegate.CLEAN_XML)) {
				localmonitor.setTaskName(Messages.ApiUseScanJob_cleaning_xml_loc);
				scrubReportLocation(new File(xmlPath), localmonitor.newChild(1));
			}
			IApiSearchReporter reporter = new XMLApiSearchReporter(
					xmlPath, 
					false);
			
			ApiSearchEngine engine = new ApiSearchEngine();
			engine.search(baseline, requestor, reporter, localmonitor.newChild(6));
			reporter.reportNotSearched((IApiElement[]) ApiUseScanJob.this.notsearched.toArray(new IApiElement[ApiUseScanJob.this.notsearched.size()]));
			if(isSpecified(ApiUseLaunchDelegate.CREATE_HTML)) {
				String htmlPath = rootpath.append("html").toOSString(); //$NON-NLS-1$
				performReportCreation(
						isSpecified(ApiUseLaunchDelegate.CLEAN_HTML),
						htmlPath,
						xmlPath,
						isSpecified(ApiUseLaunchDelegate.DISPLAY_REPORT),
						localmonitor.newChild(10));
			}
			
			// Dispose the baseline if it's not managed (it's temporary)
			int kind = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_KIND, 0);
			if (kind != ApiUseLaunchDelegate.KIND_WORKSPACE) {
				// never dispose the workspace baseline
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
		} catch (CoreException e) {
			return e.getStatus();
		}
		finally {
			localmonitor.done();
		}
		return Status.OK_STATUS;
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
	 * @param monitor
	 * @return the new {@link IApiBaseline}
	 * @throws CoreException
	 */
	private IApiBaseline createApiBaseline(IProgressMonitor monitor) throws CoreException {
		ApiBaselineManager bmanager = ApiBaselineManager.getManager();
		int kind = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_KIND, 0);
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
				return createBaseline(new Path(file.getAbsolutePath()), monitor);
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION:
				String memento = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, (String)null);
				if (memento == null) {
					abort(Messages.ApiUseScanJob_target_unspecified);
				}
				ITargetPlatformService service = getTargetService();
				ITargetHandle handle = service.getTarget(memento);
				ITargetDefinition definition = handle.getTargetDefinition();
				return createBaseline(definition, monitor);
			case ApiUseLaunchDelegate.KIND_WORKSPACE:
				return bmanager.getWorkspaceBaseline();
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
		return (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
	}
	
	/**
	 * Returns the set of components to extract references to
	 * @param baseline
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	private Set getTargetComponentIds(IApiBaseline baseline, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_collecting_target_components, 10);
		Set set = new HashSet();
		String regex = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, (String)null);
		// add all
		Pattern pattern = null;
		if(regex != null) {
			pattern = Pattern.compile(regex);
		}
		IApiComponent[] components = baseline.getApiComponents();
		localmonitor.setWorkRemaining(components.length);
		for (int i = 0; i < components.length; i++) {
			Util.updateMonitor(localmonitor, 1);
			IApiComponent component = components[i];
			if (acceptComponent(component, pattern, true)) {
				localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_adding_component, component.getId()));
				set.add(component.getId());
			}
		}
		return set;
	}
	
	/**
	 * Returns the scope to extract references from
	 * @param baseline
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	private IApiComponent[] getSearchScope(IApiBaseline baseline, IProgressMonitor monitor) throws CoreException {
		String regex = this.configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, (String)null);
		this.notsearched = new TreeSet(Util.componentsorter);
		List list = new ArrayList();
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_creating_search_scope, 2);
		Pattern pattern = null;
		if(regex != null) {
			pattern = Pattern.compile(regex);
		}
		// search all (but remove system libs, they can never reference bundle code)
		IApiComponent[] components = baseline.getApiComponents();
		localmonitor.setWorkRemaining(components.length);
		for (int i = 0; i < components.length; i++) {
			Util.updateMonitor(localmonitor, 1);
			IApiComponent component = components[i];
			if (acceptComponent(component, pattern, false)) {
				localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_adding_component, component.getId()));
				list.add(component);
			}
			else {
				this.notsearched.add(new SkippedComponent(component.getId(), component.getVersion(), null));
			}
		}
		return (IApiComponent[]) list.toArray(new IApiComponent[list.size()]);
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
				this.notsearched.add(new SkippedComponent(component.getId(), component.getVersion(), errors)); 
				return false;
			}
		}
		if(component.isSystemComponent()) {
			return false;
		}
		if(pattern != null) {
			return pattern.matcher(component.getId()).matches();
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
	 * @param cleanh
	 * @param hlocation
	 * @param rlocation
	 * @param openhtml
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	void performReportCreation(boolean cleanh, 
			String hlocation, 
			String rlocation, 
			boolean openhtml, 
			IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_creating_html_reports, 10);
		if(cleanh) {
			cleanReportLocation(hlocation, localmonitor.newChild(5));
		}
		try {
			ApiUseReportConverter converter = new ApiUseReportConverter(hlocation, rlocation);
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
		IResolvedBundle[] bundles = definition.getBundles();
		List components = new ArrayList();
		IApiBaseline profile = ApiModelFactory.newApiBaseline(definition.getName());
		localmonitor.setWorkRemaining(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			Util.updateMonitor(localmonitor, 1);
			if (!bundles[i].isSourceBundle()) {
				IApiComponent component = ApiModelFactory.newApiComponent(profile, URIUtil.toFile(bundles[i].getBundleInfo().getLocation()).getAbsolutePath());
				if (component != null) {
					localmonitor.setTaskName(NLS.bind(Messages.ApiUseScanJob_adding_component, component.getId()));
					components.add(component);
				}
			}
		}
		profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
		return profile;
	}	
	
	/**
	 * Creates a baseline at an install location.
	 * 
	 * @param path
	 * @param monitor
	 */
	private IApiBaseline createBaseline(IPath path, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiProfileWizardPage_0, 10);
		File plugins = path.append("plugins").toFile(); //$NON-NLS-1$
		ITargetPlatformService service = (ITargetPlatformService) ApiUIPlugin.getDefault().acquireService(ITargetPlatformService.class.getName());
		IBundleContainer container = service.newProfileContainer(path.toOSString(), null);
		// treat as an installation, if that fails, try plug-ins directory
		ITargetDefinition definition = service.newTarget();
		container.resolve(definition, localmonitor.newChild(1));
		Util.updateMonitor(localmonitor, 1);
		IResolvedBundle[] bundles = container.getBundles();
		List components = new ArrayList();
		IApiBaseline profile = ApiModelFactory.newApiBaseline(this.configuration.getName());
		Util.updateMonitor(localmonitor, 1);
		if (bundles.length > 0) {
			// an installation
			localmonitor.setWorkRemaining(bundles.length);
			for (int i = 0; i < bundles.length; i++) {
				Util.updateMonitor(localmonitor, 1);
				if (!bundles[i].isSourceBundle()) {
					IApiComponent component = ApiModelFactory.newApiComponent(profile, URIUtil.toFile(bundles[i].getBundleInfo().getLocation()).getAbsolutePath());
					if (component != null) {
						components.add(component);
					}
				}
			}
		} else {
			// scan directory
			if (!plugins.exists() || !plugins.isDirectory()) {
				plugins = path.toFile();
			}
			File[] files = scanLocation(plugins);
			Util.updateMonitor(localmonitor, 1);
			localmonitor.setWorkRemaining(files.length);
			for (int i = 0; i < files.length; i++) {
				Util.updateMonitor(localmonitor, 1);
				IApiComponent component = ApiModelFactory.newApiComponent(profile, files[i].getPath());
				if (component != null) {
					components.add(component);
				}
			}
		}
		profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
		return profile;
	}	
	
	/**
	 * Scan given directory for plug-ins
	 * @param directory
	 * @return Files of plug-ins/features
	 */
	private File[] scanLocation(File directory) {
		if(!directory.exists() && !directory.isDirectory()) {
			return new File[0];
		}
		HashSet result = new HashSet();
		File[] children = directory.listFiles();
		if (children != null) {
			for (int j = 0; j < children.length; j++) {
				result.add(children[j]);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}	
}
