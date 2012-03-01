/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.search.MissingRefMetadata;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.Version;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Ant task to headlessly run the API Analysis builder and generate a report of
 * missing references in the API Use Scans
 */
public class MissingRefProblemsTask extends CommonUtilsTask {

	private FilteredElements excludedElements;
	private FilteredElements includedElements;
	private String apiUseScans;
	private String[] usescans;
	private Properties properties = new Properties();
	TreeSet notsearched = new TreeSet(Util.componentsorter);

	public static final String COMPATIBILITY = "compatibility"; //$NON-NLS-1$
	private static final Summary[] NO_SUMMARIES = new Summary[0];

	private static class Summary {
		String fComponentID;
		List fApiProblems;

		public Summary(String componentID, IApiProblem[] apiProblems) {
			fComponentID = componentID;
			fApiProblems = Arrays.asList(apiProblems);
		}
	}

	/**
	 * Run the api use scan problems task
	 * 
	 * @throws BuildException
	 *             exception is thrown if anything goes wrong during the
	 *             verification
	 */
	public void execute() throws BuildException {
		if (super.currentBaselineLocation == null || super.reportLocation == null || this.apiUseScans == null) {
			StringBuffer error = new StringBuffer(NLS.bind(Messages.MissingRefProblemsTask_missingArguments, new String[] {super.currentBaselineLocation, super.reportLocation,}));
			throw new BuildException(error.toString());
		}
		//scrub the directory each time
		File loc = new File(this.reportLocation);
		if(loc.exists()) {
			Util.delete(loc);
		}
		if (usescans != null && usescans.length > 0) {
			for (int i = 0; i < usescans.length; i++) {
				File file = new File(usescans[i].trim());
				if (!file.exists()) {
					StringBuffer error = new StringBuffer(Messages.MissingRefProblemsTask_invalidApiUseScanLocation);
					error.append(usescans[i]);
					throw new BuildException(error.toString());
				}
			}
		}

		if (super.debug) {
			System.out.println("profile to compare : " + super.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("report location : " + super.reportLocation); //$NON-NLS-1$

			if (super.excludeListLocation != null) {
				System.out.println("exclude list location : " + super.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
			if (super.includeListLocation != null) {
				System.out.println("include list location : " + super.includeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No include list location"); //$NON-NLS-1$
			}
		}
		// unzip profile
		long time = 0;
		if (super.debug) {
			time = System.currentTimeMillis();
		}

		File baselineInstallDir = extractSDK(CURRENT, super.currentBaselineLocation);
		if (super.debug) {
			System.out.println("Preparation of profile installation : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}

		// create baseline = profile
		IApiBaseline profile = createBaseline(CURRENT_BASELINE_NAME, baselineInstallDir.getAbsolutePath(), null);
		if (super.debug) {
			System.out.println("Creation of baseline : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		if (this.excludeListLocation != null) {
			this.excludedElements = CommonUtilsTask.initializeFilteredElements(this.excludeListLocation, profile, super.debug);
		}
		if (this.includeListLocation != null) {
			this.includedElements = CommonUtilsTask.initializeFilteredElements(this.includeListLocation, profile, super.debug);
		}
		UseScanManager.getInstance().setReportLocations(usescans);
		if (super.debug) {
			System.out.println("===================================================================================="); //$NON-NLS-1$
			System.out.println("API Use Scan locations:"); //$NON-NLS-1$
			for (int i = 0; i < usescans.length; i++) {
				System.out.println("Location " + (i+1) + " : " + usescans[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Map allProblems = new HashMap();
		try {
			IApiComponent[] apiComponents = profile.getApiComponents();
			int length = apiComponents.length;
			Set visitedApiComponentNames = new HashSet();
			for (int i = 0; i < length; i++) {
				IApiComponent apiComponent = apiComponents[i];
				String name = apiComponent.getSymbolicName();
				String version = apiComponent.getVersion();
				if(!acceptComponent(apiComponent)) {
					continue;
				}
				visitedApiComponentNames.add(name);
				BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
				try {
					if (this.properties.isEmpty()) {
						addDefaultProperties();
					}
					analyzer.checkExternalDependencies(apiComponent, new BuildContext(), this.properties, new NullProgressMonitor());
					IApiProblem[] problems = analyzer.getProblems();
					if (problems.length != 0) {
						allProblems.put(name +" ("+new Version(version).toString()+")", problems); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} catch (RuntimeException e) {
					ApiPlugin.log(e);
					throw e;
				} catch (CoreException e) {
					ApiPlugin.log(e);
				} finally {
					analyzer.dispose();
				}
			}
		} finally {
			if (super.debug) {
				System.out.println("Use scan reference check completed in: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
			profile.dispose();
			StubApiComponent.disposeAllCaches();
			deleteBaseline(super.currentBaselineLocation, baselineInstallDir);
			writeMetaData(new File(this.reportLocation, "meta.xml")); //$NON-NLS-1$
		}
		Summary[] summaries = createAllSummaries(allProblems);

		try {
			dumpReport(summaries);
			reportNotSearched(notsearched);
		} catch (RuntimeException e) {
			ApiPlugin.log(e);
			throw e;
		}
	}

	/**
	 * If the component should be scanned or not. If not than it is added to the 'not searched' listing
	 * 
	 * @param component
	 * @return <code>true</code> if the component should be scanned, <code>false</code> otherwise
	 */
	boolean acceptComponent(IApiComponent component) {
		String name = component.getSymbolicName();
		String version = component.getVersion();
		try {
			ResolverError[] errors = component.getErrors(); 
			if ((errors != null && errors.length > 0) || component.isSystemComponent() || !Util.isApiToolsComponent(component)) {
				notsearched.add(new SkippedComponent(name, version, errors));
				return false;
			}
			if (this.excludedElements != null && (this.excludedElements.containsExactMatch(name) || this.excludedElements.containsPartialMatch(name))) {
				notsearched.add(new SkippedComponent(name, version, errors));
				return false;
			}
			if (this.includedElements != null && !this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(name) || this.includedElements.containsPartialMatch(name))) {
				notsearched.add(new SkippedComponent(name, version, errors));
				return false;
			}
		}
		catch(CoreException ce) {
			notsearched.add(new SkippedComponent(name, version, null));
			return false;
		}
		return true;
	}
	
	private void addDefaultProperties() {
		this.properties.put(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY, ApiPlugin.VALUE_ERROR);
		this.properties.put(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY, ApiPlugin.VALUE_ERROR);
		this.properties.put(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY, ApiPlugin.VALUE_ERROR);
	}

	private void writeMetaData(File file) {
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				if (!file.createNewFile())
					return; // could not create meta.xml
			}
			if (super.debug) {
				System.out.println("Writing metadata to " + file.getAbsolutePath()); //$NON-NLS-1$ 
			}

			MissingRefMetadata metadata = new MissingRefMetadata(super.currentBaselineLocation, super.reportLocation, this.apiUseScans);
			metadata.serializeToFile(file);
		} catch (UnsupportedEncodingException e) {
			ApiPlugin.log(e);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	private Summary[] createAllSummaries(Map allProblems) {
		Set entrySet = allProblems.entrySet();
		int size = entrySet.size();
		if (size == 0) {
			return NO_SUMMARIES;
		}
		List allEntries = new ArrayList();
		allEntries.addAll(entrySet);
		Collections.sort(allEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;
				return ((String) entry1.getKey()).compareTo((String) entry2.getKey());
			}
		});
		Summary[] summaries = new Summary[size];
		int i = 0;
		for (Iterator iterator = allEntries.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			summaries[i++] = new Summary((String) entry.getKey(), (IApiProblem[]) entry.getValue());
		}
		return summaries;
	}

	private void dumpReport(Summary[] summaries) {
		for (int i = 0, max = summaries.length; i < max; i++) {
			Summary summary = summaries[i];
			String contents = null;
			String componentID = summary.fComponentID;

			try {
				Document document = Util.newDocument();
				Element report = document.createElement(IApiXmlConstants.ELEMENT_API_TOOL_REPORT);
				report.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
				report.setAttribute(IApiXmlConstants.ATTR_COMPONENT_ID, componentID);
				document.appendChild(report);

				Element category = document.createElement(IApiXmlConstants.ATTR_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, COMPATIBILITY);
				insertAPIProblems(category, document, summary.fApiProblems);
				report.appendChild(category);

				contents = Util.serializeDocument(document);
			} catch (DOMException e) {
				throw new BuildException(e);
			} catch (CoreException e) {
				throw new BuildException(e);
			}
			if (contents != null) {
				saveReport(componentID, contents, "report.xml"); //$NON-NLS-1$
			}
		}

	}

	public void reportNotSearched(Set notSearchedList) {
		if (notSearchedList.isEmpty()) {
			return;
		}
		BufferedWriter writer = null;
		try {
			if (this.debug) {
				System.out.println("Writing file for projects that were not searched..."); //$NON-NLS-1$
			}
			File rootfile = new File(reportLocation);
			if (!rootfile.exists()) {
				rootfile.mkdirs();
			}
			File file = new File(rootfile, "not_searched.xml"); //$NON-NLS-1$
			if (!file.exists()) {
				file.createNewFile();
			}
			Document doc = Util.newDocument();
			Element root = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENTS);
			root.setAttribute("ShowMissing", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);
			Element comp = null;
			for (Iterator iterator = notSearchedList.iterator(); iterator.hasNext();) {
				SkippedComponent component = (SkippedComponent) iterator.next();
				comp = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
				comp.setAttribute(IApiXmlConstants.ATTR_ID, component.getComponentId());
				comp.setAttribute(IApiXmlConstants.ATTR_VERSION, component.getVersion());
				comp.setAttribute(IApiXmlConstants.SKIPPED_DETAILS, component.getErrorDetails());
				root.appendChild(comp);
			}
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), IApiCoreConstants.UTF_8));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		} catch (FileNotFoundException fnfe) {
		} catch (IOException ioe) {
		} catch (CoreException ce) {
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void insertAPIProblems(Element root, Document document, List problems) throws CoreException {
		Element apiProblems = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEMS);
		root.appendChild(apiProblems);
		Element element = null;
		// sort the problem by type name
		Collections.sort(problems, new Comparator() {
			public int compare(Object o1, Object o2) {
				IApiProblem p1 = (IApiProblem) o1;
				IApiProblem p2 = (IApiProblem) o2;
				return p1.getTypeName().compareTo(p2.getTypeName());
			}
		});
		for (Iterator iterator = problems.iterator(); iterator.hasNext();) {
			IApiProblem problem = (IApiProblem) iterator.next();
			element = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM);
			element.setAttribute(IApiXmlConstants.ATTR_TYPE_NAME, String.valueOf(problem.getTypeName()));
			element.setAttribute(IApiXmlConstants.ATTR_ID, Integer.toString(problem.getId()));
			element.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(problem.getLineNumber()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_START, Integer.toString(problem.getCharStart()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_END, Integer.toString(problem.getCharEnd()));
			element.setAttribute(IApiXmlConstants.ATTR_ELEMENT_KIND, Integer.toString(problem.getElementKind()));
			element.setAttribute(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE, ApiProblem.getProblemElementKind(problem.getCategory(), problem.getElementKind()));
			element.setAttribute(IApiXmlConstants.ATTR_CATEGORY, ApiProblem.getProblemCategory(problem.getCategory()));
			element.setAttribute(IApiXmlConstants.ATTR_SEVERITY, this.properties.getProperty(ApiProblemFactory.getProblemSeverityId(problem)));
			element.setAttribute(IApiXmlConstants.ATTR_KIND, Integer.toString(problem.getKind()));
			element.setAttribute(IApiXmlConstants.ATTR_FLAGS, Integer.toString(problem.getFlags()));
			element.setAttribute(IApiXmlConstants.ATTR_MESSAGE, problem.getMessage());
			String[] extraMarkerAttributeIds = problem.getExtraMarkerAttributeIds();
			if (extraMarkerAttributeIds != null && extraMarkerAttributeIds.length != 0) {
				int length = extraMarkerAttributeIds.length;
				Object[] extraMarkerAttributeValues = problem.getExtraMarkerAttributeValues();
				Element extraArgumentsElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENTS);
				for (int j = 0; j < length; j++) {
					Element extraArgumentElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENT);
					extraArgumentElement.setAttribute(IApiXmlConstants.ATTR_ID, extraMarkerAttributeIds[j]);
					extraArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(extraMarkerAttributeValues[j]));
					extraArgumentsElement.appendChild(extraArgumentElement);
				}
				element.appendChild(extraArgumentsElement);
			}
			String[] messageArguments = problem.getMessageArguments();
			if (messageArguments != null && messageArguments.length != 0) {
				int length = messageArguments.length;
				Element messageArgumentsElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
				for (int j = 0; j < length; j++) {
					Element messageArgumentElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
					messageArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(messageArguments[j]));
					messageArgumentsElement.appendChild(messageArgumentElement);
				}
				element.appendChild(messageArgumentsElement);
			}
			apiProblems.appendChild(element);
		}
	}

	/**
	 * Set the debug value.
	 * <p>
	 * The possible values are: <code>true</code>, <code>false</code>
	 * </p>
	 * <p>
	 * Default is <code>false</code>.
	 * </p>
	 * 
	 * @param debugValue
	 *            the given debug value
	 */
	public void setDebug(boolean debugValue) {
		super.debug = debugValue;
	}

	/**
	 * Set the exclude list location.
	 * 
	 * <p>
	 * The exclude list is used to know what bundles should excluded from the
	 * xml report generated by the task execution. Lines starting with '#' are
	 * ignored from the excluded elements.
	 * </p>
	 * <p>
	 * The format of the exclude list file looks like this:
	 * </p>
	 * 
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 * 
	 * @param excludeListLocation
	 *            the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}

	/**
	 * Set the include list location.
	 * 
	 * <p>
	 * The include list is used to know what bundles should included from the
	 * xml report generated by the task execution. Lines starting with '#' are
	 * ignored from the included elements.
	 * </p>
	 * <p>
	 * The format of the include list file looks like this:
	 * </p>
	 * 
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 * 
	 * @param includeListLocation
	 *            the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}

	/**
	 * Set the preferences for the task.
	 * 
	 * <p>
	 * The preferences are used to configure problem severities. Problem
	 * severities have three possible values: Ignore, Warning, or Error. The set
	 * of problems detected is defined by corresponding problem preference keys
	 * in API tools.
	 * </p>
	 * <p>
	 * If the given location doesn't exist, the preferences won't be set.
	 * </p>
	 * <p>
	 * Lines starting with '#' are ignored. The format of the preferences file
	 * looks like this:
	 * </p>
	 * 
	 * <pre>
	 * #Thu Jan 11 17:03:09 IST 2011
	 * API_USE_SCAN_TYPE_SEVERITY=Error
	 * API_USE_SCAN_METHOD_SEVERITY=Ignore
	 * API_USE_SCAN_FIELD_SEVERITY=Ignore
	 * </pre>
	 * <p>
	 * The keys can be found in
	 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes}
	 * .
	 * </p>
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 * 
	 * @param preferencesLocation
	 *            the location of the preference file
	 */
	public void setPreferences(String preferencesLocation) {
		File preferencesFile = new File(preferencesLocation);
		if (!preferencesFile.exists()) {
			return;
		}
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(preferencesFile));
			Properties temp = new Properties();
			temp.load(inputStream);
			this.properties = temp;
		} catch (IOException e) {
			// ignore
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Set the location of the current product or baseline that you want to
	 * compare against the reference baseline.
	 * 
	 * <p>
	 * It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that
	 * corresponds to the Eclipse installation folder. This is the directory is
	 * which you can find the Eclipse executable.
	 * </p>
	 * 
	 * @param baselineLocation
	 *            the given location for the baseline to analyze
	 */
	public void setProfile(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}

	/**
	 * Comma-separated list of the locations of the API Use Scans that you want
	 * to check against the reference baseline.
	 * 
	 * <p>
	 * It can be a .zip file or a directory that corresponds to the API Use Scan report. 
	 * This is the directory is which you can find the XML folder.
	 * </p>
	 * 
	 * @param baselineLocation
	 *            the given location for the baseline to analyze
	 */
	public void setAPIUseScans(String apiUseScans) {
		this.apiUseScans = apiUseScans;
		this.usescans = apiUseScans.split(","); //$NON-NLS-1$		
	}

	/**
	 * Set the output location where the reports will be generated.
	 * 
	 * <p>
	 * Once the task is completed, reports are available in this directory using
	 * a structure similar to the filter root. A sub-folder is created for each
	 * component that has problems to be reported. Each sub-folder contains a
	 * file called "report.xml".
	 * </p>
	 * 
	 * <p>
	 * A special folder called "allNonApiBundles" is also created in this folder
	 * that contains a xml file called "report.xml". This file lists all the
	 * bundles that are not using the api tooling nature.
	 * </p>
	 * 
	 * @param baselineLocation
	 *            the given location for the reference baseline to analyze
	 */
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
}
