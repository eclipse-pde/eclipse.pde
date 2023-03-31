/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
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
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

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
	Set<SkippedComponent> notsearched = null;

	/**
	 * @param name
	 */
	public ApiUseScanJob(ILaunchConfiguration configuration) {
		super(MessageFormat.format(Messages.ApiUseScanJob_api_use_report, configuration.getName()));
		this.configuration = configuration;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// Build API baseline
		SubMonitor localmonitor = SubMonitor.convert(monitor);
		try {
			IPath rootpath = null;
			String xmlPath = this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATH, (String) null);
			if (xmlPath == null) {
				abort(Messages.ApiUseScanJob_missing_xml_loc);
			}
			rootpath = new Path(xmlPath);
			int kind = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_KIND, 0);
			if (kind != ApiUseLaunchDelegate.KIND_HTML_ONLY) {
				localmonitor.setTaskName(Messages.ApiUseScanJob_preparing_for_scan);
				localmonitor.setWorkRemaining((isSpecified(ApiUseLaunchDelegate.CREATE_HTML) ? 14 : 13));
				// create baseline
				IApiBaseline baseline = createApiBaseline(kind, localmonitor.split(1));
				Set<String> ids = new HashSet<>();
				TreeSet<IApiComponent> scope = new TreeSet<>(Util.componentsorter);
				getContext(baseline, ids, scope, localmonitor.split(2));
				int kinds = 0;
				if (isSpecified(ApiUseLaunchDelegate.MOD_API_REFERENCES)) {
					kinds |= IApiSearchRequestor.INCLUDE_API;
				}
				if (isSpecified(ApiUseLaunchDelegate.MOD_INTERNAL_REFERENCES)) {
					kinds |= IApiSearchRequestor.INCLUDE_INTERNAL;
				}
				if (isSpecified(ApiUseLaunchDelegate.MOD_ILLEGAL_USE)) {
					kinds |= IApiSearchRequestor.INCLUDE_ILLEGAL_USE;
				}
				UseSearchRequestor requestor = new UseSearchRequestor(ids, scope.toArray(new IApiElement[scope.size()]), kinds);
				List<String> jars = this.configuration.getAttribute(ApiUseLaunchDelegate.JAR_PATTERNS_LIST, (List<String>) null);
				String[] sjars = getStrings(jars);
				requestor.setJarPatterns(sjars);
				List<String> api = this.configuration.getAttribute(ApiUseLaunchDelegate.API_PATTERNS_LIST, (List<String>) null);
				String[] sapi = getStrings(api);
				String filterRoot = this.configuration.getAttribute(ApiUseLaunchDelegate.FILTER_ROOT, (String) null);
				requestor.setFilterRoot(filterRoot);
				List<String> internal = this.configuration.getAttribute(ApiUseLaunchDelegate.INTERNAL_PATTERNS_LIST, (List<String>) null);
				String[] sinternal = getStrings(internal);
				if (sapi != null || sinternal != null) {
					// modify API descriptions
					IApiComponent[] components = baseline.getApiComponents();
					ApiDescriptionModifier visitor = new ApiDescriptionModifier(sinternal, sapi);
					for (IApiComponent component : components) {
						if (!component.isSystemComponent() && !component.isSourceComponent()) {
							visitor.setApiDescription(component.getApiDescription());
							component.getApiDescription().accept(visitor, null);
						}
					}
				}
				xmlPath = rootpath.append("xml").toOSString(); //$NON-NLS-1$
				if (isSpecified(ApiUseLaunchDelegate.CLEAN_XML)) {
					localmonitor.setTaskName(Messages.ApiUseScanJob_cleaning_xml_loc);
					scrubReportLocation(new File(xmlPath), localmonitor.split(1));
				}
				UseMetadata data = new UseMetadata(kinds, this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, (String) null), this.configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, (String) null), baseline.getLocation(), xmlPath, sapi, sinternal, sjars, this.configuration.getAttribute(ApiUseLaunchDelegate.FILTER_ROOT, (String) null), DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()), this.configuration.getAttribute(ApiUseLaunchDelegate.DESCRIPTION, (String) null));
				IApiSearchReporter reporter = new XmlSearchReporter(xmlPath, false);
				try {
					ApiSearchEngine engine = new ApiSearchEngine();
					engine.search(baseline, requestor, reporter, localmonitor.split(6));
				} finally {
					reporter.reportNotSearched(ApiUseScanJob.this.notsearched.toArray(new IApiElement[ApiUseScanJob.this.notsearched.size()]));
					reporter.reportMetadata(data);
					reporter.reportCounts();
					// Dispose the baseline if it's not managed (it's temporary)
					ApiBaselineManager apiManager = ApiBaselineManager.getManager();
					IApiBaseline[] baselines = apiManager.getApiBaselines();
					boolean dispose = true;
					for (IApiBaseline baseline2 : baselines) {
						if (baseline.equals(baseline2)) {
							dispose = false;
							break;
						}
					}
					if (dispose) {
						baseline.dispose();
					}
				}
			} else {
				localmonitor.setWorkRemaining(10);
			}
			if (isSpecified(ApiUseLaunchDelegate.CREATE_HTML)) {
				localmonitor.setTaskName(Messages.ApiUseScanJob_generating_html_reports);
				String htmlPath = rootpath.append("html").toOSString(); //$NON-NLS-1$

				int reportType = ApiUseLaunchDelegate.REPORT_KIND_PRODUCER;
				if (this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TYPE, ApiUseLaunchDelegate.REPORT_KIND_PRODUCER) == ApiUseLaunchDelegate.REPORT_KIND_CONSUMER) {
					reportType = ApiUseLaunchDelegate.REPORT_KIND_CONSUMER;
				}

				performReportCreation(reportType, isSpecified(ApiUseLaunchDelegate.CLEAN_HTML), htmlPath, xmlPath, isSpecified(ApiUseLaunchDelegate.DISPLAY_REPORT), getStrings(this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_TO_PATTERNS_LIST, (List<String>) null)), getStrings(this.configuration.getAttribute(ApiUseLaunchDelegate.REPORT_PATTERNS_LIST, (List<String>) null)), localmonitor.split(10));
			}

		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	private String[] getStrings(List<String> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Throws a new {@link CoreException} with the given message
	 *
	 * @param message
	 * @throws CoreException
	 */
	void abort(String message) throws CoreException {
		throw new CoreException(Status.error(message));
	}

	/**
	 * Creates a new {@link IApiBaseline} from the location set in the backing
	 * launch configuration
	 *
	 * @param kind
	 * @param monitor
	 * @return the new {@link IApiBaseline}
	 * @throws CoreException
	 */
	private IApiBaseline createApiBaseline(int kind, IProgressMonitor monitor) throws CoreException {
		ApiBaselineManager bmanager = ApiBaselineManager.getManager();
		switch (kind) {
			case ApiUseLaunchDelegate.KIND_API_BASELINE:
				String name = this.configuration.getAttribute(ApiUseLaunchDelegate.BASELINE_NAME, (String) null);
				if (name == null) {
					abort(Messages.ApiUseScanJob_baseline_name_missing);
				}
				IApiBaseline baseline = bmanager.getApiBaseline(name);
				if (baseline == null) {
					abort(MessageFormat.format(Messages.ApiUseScanJob_baseline_does_not_exist, name));
				}
				return baseline;
			case ApiUseLaunchDelegate.KIND_INSTALL_PATH:
				String path = this.configuration.getAttribute(ApiUseLaunchDelegate.INSTALL_PATH, (String) null);
				if (path == null) {
					abort(Messages.ApiUseScanJob_unspecified_install_path);
				}
				File file = new File(path);
				if (!file.exists() || !file.isDirectory()) {
					abort(MessageFormat.format(Messages.ApiUseScanJob_intall_dir_does_no_exist, path));
				}
				return createBaseline(path, monitor);
			case ApiUseLaunchDelegate.KIND_TARGET_DEFINITION:
				String memento = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_HANDLE, (String) null);
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
		return ApiPlugin.getDefault().acquireService(ITargetPlatformService.class);
	}

	/**
	 * Collects the context of reference ids and scope elements in one pass
	 *
	 * @param baseline the baseline to check components from
	 * @param ids the reference ids to consider
	 * @param scope the scope of elements to search
	 * @param monitor
	 * @throws CoreException
	 */
	private void getContext(IApiBaseline baseline, Set<String> ids, Set<IApiComponent> scope, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_collecting_target_components, 10);
		this.notsearched = new TreeSet<>(Util.componentsorter);
		String regex = this.configuration.getAttribute(ApiUseLaunchDelegate.TARGET_SCOPE, (String) null);
		Pattern pattern = getPattern(regex);
		regex = this.configuration.getAttribute(ApiUseLaunchDelegate.SEARCH_SCOPE, (String) null);
		Pattern pattern2 = getPattern(regex);
		IApiComponent[] components = baseline.getApiComponents();
		localmonitor.setWorkRemaining(components.length);
		for (IApiComponent component : components) {
			localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_checking_component, component.getSymbolicName()));
			localmonitor.split(1);
			if (acceptComponent(component, pattern, true)) {
				ids.add(component.getSymbolicName());
			}
			if (acceptComponent(component, pattern2, false)) {
				scope.add(component);
			} else {
				localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_skipping_component, component.getSymbolicName()));
				this.notsearched.add(new SkippedComponent(component.getSymbolicName(), component.getVersion(), null));
			}
		}
	}

	/**
	 * Returns a pattern for the given regular expression or <code>null</code>
	 * if none.
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
	 *
	 * @param component
	 * @param pattern
	 * @param allowresolve
	 * @return
	 * @throws CoreException
	 */
	boolean acceptComponent(IApiComponent component, Pattern pattern, boolean allowresolve) throws CoreException {
		if (!allowresolve) {
			ResolverError[] errors = component.getErrors();
			if (errors != null) {
				this.notsearched.add(new SkippedComponent(component.getSymbolicName(), component.getVersion(), errors));
				return false;
			}
		}
		if (component.isSystemComponent()) {
			return false;
		}
		if (pattern != null) {
			return pattern.matcher(component.getSymbolicName()).matches();
		}
		return true;
	}

	/**
	 * Returns if the given search modifier is set in the backing
	 * {@link ILaunchConfiguration}
	 *
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
	 *
	 * @param reportType what report converter to use, either
	 *            {@link ApiUseLaunchDelegate#REPORT_KIND_PRODUCER} or
	 *            {@link ApiUseLaunchDelegate#REPORT_KIND_CONSUMER}
	 * @param cleanh
	 * @param hlocation
	 * @param rlocation
	 * @param openhtml
	 * @param monitor
	 * @throws OperationCanceledException
	 */
	void performReportCreation(int reportType, boolean cleanh, String hlocation, String rlocation, boolean openhtml, String[] topatterns, String[] frompatterns, IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_creating_html_reports, 10);
		if (cleanh) {
			cleanReportLocation(hlocation, localmonitor.split(5));
		}
		try {
			UseReportConverter converter = null;
			if (reportType == ApiUseLaunchDelegate.REPORT_KIND_CONSUMER) {
				converter = new ConsumerReportConvertor(hlocation, rlocation, topatterns, frompatterns);
			} else {
				converter = new UseReportConverter(hlocation, rlocation, topatterns, frompatterns);
			}

			converter.convert(null, localmonitor.split(5));
			if (openhtml) {
				final File index = converter.getReportIndex();
				if (index != null) {
					UIJob ujob = UIJob.create(Util.EMPTY_STRING, m -> {
						IEditorDescriptor edesc = IDE.getEditorDescriptor(index.getName(), true, true);
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						IDE.openEditor(window.getActivePage(), index.toURI(), edesc.getId(), true);
					});
					ujob.setPriority(Job.INTERACTIVE);
					ujob.setSystem(true);
					ujob.schedule();
				}
			}
		} catch (OperationCanceledException oce) {
			// re-throw
			throw oce;
		} catch (Exception e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Cleans the report location specified by the parameter reportLocation
	 *
	 * @param monitor
	 */
	void cleanReportLocation(String location, IProgressMonitor monitor) {
		File file = new File(location);
		SubMonitor localmonitor = SubMonitor.convert(monitor, Messages.ApiUseScanJob_deleting_old_reports, 1);
		if (file.exists()) {
			scrubReportLocation(file, localmonitor.split(1));
			localmonitor.subTask(NLS.bind(Messages.ApiUseScanJob_deleting_root_folder, file.getName()));
		}
	}

	/**
	 * Cleans the location if it exists
	 *
	 * @param file
	 * @param monitor
	 */
	void scrubReportLocation(File file, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				subMonitor.setWorkRemaining(files.length);
				for (File file2 : files) {
					SubMonitor iterationMonitor = subMonitor.split(1);
					iterationMonitor.subTask(NLS.bind(Messages.ApiUseScanJob_deleteing_file, file2.getPath()));
					if (file2.isDirectory()) {
						scrubReportLocation(file2, iterationMonitor);
					} else {
						file2.delete();
					}
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
		definition.resolve(localmonitor.split(2));
		TargetBundle[] bundles = definition.getBundles();
		List<IApiComponent> components = new ArrayList<>();
		IApiBaseline profile = ApiModelFactory.newApiBaseline(definition.getName());
		localmonitor.setWorkRemaining(bundles.length);
		for (TargetBundle bundle : bundles) {
			localmonitor.split(1);
			if (bundle.getStatus().isOK() && !bundle.isSourceBundle()) {
				IApiComponent component = ApiModelFactory.newApiComponent(profile, URIUtil.toFile(bundle.getBundleInfo().getLocation()).getAbsolutePath());
				if (component != null) {
					components.add(component);
				}
			}
		}
		profile.addApiComponents(components.toArray(new IApiComponent[components.size()]));
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
		if (components.length == 0) {
			abort(MessageFormat.format(Messages.ApiUseScanJob_no_bundles, installLocation));
		}
		return baseline;
	}

}
