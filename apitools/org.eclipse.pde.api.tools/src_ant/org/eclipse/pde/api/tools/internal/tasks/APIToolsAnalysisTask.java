/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Ant task to run the API tool verification during Eclipse build.
 */
public class APIToolsAnalysisTask extends CommonUtilsTask {
	/**
	 * This filter store is only used to filter problem using existing filters.
	 * It doesn't add or remove any filters.
	 */
	private static class AntFilterStore implements IApiFilterStore {
		private static final String GLOBAL = "!global!"; //$NON-NLS-1$
		private static int loadIntegerAttribute(Element element, String name) {
			String value = element.getAttribute(name);
			if(value.length() == 0) {
				return -1;
			}
			try {
				int number = Integer.parseInt(value);
				return number;
			}
			catch(NumberFormatException nfe) {}
			return -1;
		}
		private boolean debug;

		private Map fFilterMap;

		public AntFilterStore(boolean debug, String filtersRoot, String componentID) {
			this.initialize(filtersRoot, componentID);
		}

		public void addFiltersFor(IApiProblem[] problems) {
			// do nothing
		}

		public void addFilters(IApiProblemFilter[] filters) {
			// do nothing
		}

		private boolean argumentsEquals(String[] problemMessageArguments,
				String[] filterProblemMessageArguments) {
			// filter problems message arguments are always simple name
			// problem message arguments are fully qualified name outside the IDE
			int length = problemMessageArguments.length;
			if (length == filterProblemMessageArguments.length) {
				for (int i = 0; i < length; i++) {
					String problemMessageArgument = problemMessageArguments[i];
					String filterProblemMessageArgument = filterProblemMessageArguments[i];
					if (problemMessageArgument.equals(filterProblemMessageArgument)) {
						continue;
					}
					int index = problemMessageArgument.lastIndexOf('.');
					int filterProblemIndex = filterProblemMessageArgument.lastIndexOf('.');
					if (index == -1) {
						if (filterProblemIndex == -1) {
							return false; // simple names should match
						}
						if (filterProblemMessageArgument.substring(filterProblemIndex + 1).equals(problemMessageArgument)) {
							continue;
						} else {
							return false;
						}
					} else if (filterProblemIndex != -1) {
						return false; // fully qualified name should match
					} else {
						if (problemMessageArgument.substring(index + 1).equals(filterProblemMessageArgument)) {
							continue;
						} else {
							return false;
						}
					}
				}
				return true;
			}
			return false;
		}

		public void dispose() {
			// do nothing
		}

		public IApiProblemFilter[] getFilters(IResource resource) {
			return null;
		}

		public IResource[] getResources() {
			return null;
		}

		/**
		 * Initialize the filter store using the given component id
		 */
		private void initialize(String filtersRoot, String componentID) {
			if(fFilterMap != null) {
				return;
			}
			if(this.debug) {
				System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
			}
			fFilterMap = new HashMap(5);
			String xml = null;
			InputStream contents = null;
			try {
				File filterFileParent = new File(filtersRoot, componentID);
				if (!filterFileParent.exists()) {
					return;
				}
				contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
				xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
			}
			catch(IOException ioe) {}
			finally {
				if (contents != null) {
					try {
						contents.close();
					} catch(IOException e) {
						// ignore
					}
				}
			}
			if(xml == null) {
				return;
			}
			Element root = null;
			try {
				root = Util.parseDocument(xml);
			}
			catch(CoreException ce) {
				ApiPlugin.log(ce);
			}
			if (root == null) {
				return;
			}
			if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
				return;
			}
			String component = root.getAttribute(IApiXmlConstants.ATTR_ID);
			if(component.length() == 0) {
				return;
			}
			String versionValue = root.getAttribute(IApiXmlConstants.ATTR_VERSION);
			int version = 0;
			if(versionValue.length() != 0) {
				try {
					version = Integer.parseInt(versionValue);
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			if (version < 2) {
				// we discard all filters since there is no way to retrieve the type name
				return;
			}
			NodeList resources = root.getElementsByTagName(IApiXmlConstants.ELEMENT_RESOURCE);
			ArrayList newfilters = new ArrayList();
			ArrayList comments = new ArrayList();
			for(int i = 0; i < resources.getLength(); i++) {
				Element element = (Element) resources.item(i);
				String typeName = element.getAttribute(IApiXmlConstants.ATTR_TYPE);
				if (typeName.length() == 0) {
					// if there is no type attribute, an empty string is returned
					typeName = null;
				}
				String path = element.getAttribute(IApiXmlConstants.ATTR_PATH);
				NodeList filters = element.getElementsByTagName(IApiXmlConstants.ELEMENT_FILTER);
				for(int j = 0; j < filters.getLength(); j++) {
					element = (Element) filters.item(j);
					int id = loadIntegerAttribute(element, IApiXmlConstants.ATTR_ID);
					if(id <= 0) {
						continue;
					}
					String[] messageargs = null;
					NodeList elements = element.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
					if (elements.getLength() != 1) continue;
					Element messageArguments = (Element) elements.item(0);
					NodeList arguments = messageArguments.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
					int length = arguments.getLength();
					messageargs = new String[length];
					String comment = element.getAttribute(IApiXmlConstants.ATTR_COMMENT);
					comments.add((comment.length() < 1 ? null : comment));
					for (int k = 0; k < length; k++) {
						Element messageArgument = (Element) arguments.item(k);
						messageargs[k] = messageArgument.getAttribute(IApiXmlConstants.ATTR_VALUE);
					}
					newfilters.add(ApiProblemFactory.newApiProblem(path, typeName, messageargs, null, null, -1, -1, -1, id));
				}
			}
			internalAddFilters(componentID, (IApiProblem[]) newfilters.toArray(new IApiProblem[newfilters.size()]),
					(String[]) comments.toArray(new String[comments.size()]));
			newfilters.clear();
		}

		/**
		 * Internal use method that allows auto-persisting of the filter file to be turned on or off
		 * @param problems the problems to add the the store
		 * @param persist if the filters should be auto-persisted after they are added
		 */
		private void internalAddFilters(String componentID, IApiProblem[] problems, String[] comments) {
			if(problems == null) {
				if(this.debug) {
					System.out.println("null problems array not addding filters"); //$NON-NLS-1$
				}
				return;
			}
			for(int i = 0; i < problems.length; i++) {
				IApiProblem problem = problems[i];
				IApiProblemFilter filter = new ApiProblemFilter(componentID, problem, comments[i]);
				String typeName = problem.getTypeName();
				if (typeName == null) {
					typeName = GLOBAL;
				}
				Set filters = (Set) fFilterMap.get(typeName);
				if(filters == null) {
					filters = new HashSet();
					fFilterMap.put(typeName, filters);
				}
				filters.add(filter);
			}
		}

		public boolean isFiltered(IApiProblem problem) {
			if (this.fFilterMap == null || this.fFilterMap.isEmpty()) return false;
			String typeName = problem.getTypeName();
			if (typeName == null || typeName.length() == 0) {
				typeName = GLOBAL;
			}
			Set filters = (Set) this.fFilterMap.get(typeName);
			if (filters == null) {
				return false;
			}
			for (Iterator iterator = filters.iterator(); iterator.hasNext();) {
				IApiProblemFilter filter = (IApiProblemFilter) iterator.next();
				if (problem.getCategory() == IApiProblem.CATEGORY_USAGE) {
					// write our own matching implementation
					return matchUsageProblem(filter.getUnderlyingProblem(), problem);
				} else if (matchFilters(filter.getUnderlyingProblem(), problem)) {
					return true;
				}
			}
			return false;
		}

		private boolean matchFilters(IApiProblem filterProblem, IApiProblem problem) {
			if (problem.getId() == filterProblem.getId() && argumentsEquals(problem.getMessageArguments(), filterProblem.getMessageArguments())) {
				String typeName = problem.getTypeName();
				String filteredProblemTypeName = filterProblem.getTypeName();
				if (typeName == null) {
					if (filteredProblemTypeName != null) {
						return false;
					}
					return true;
				} else if (filteredProblemTypeName == null) {
					return false;
				}
				return typeName.equals(filteredProblemTypeName);
			}
			return false;
		}

		private boolean matchUsageProblem(IApiProblem filterProblem, IApiProblem problem) {
			if (problem.getId() == filterProblem.getId()) {
				// check arguments
				String problemPath = problem.getResourcePath();
				String filterProblemPath = filterProblem.getResourcePath();
				if (problemPath == null) {
					if (filterProblemPath != null) {
						return false;
					}
				} else if (filterProblemPath == null) {
					return false;
				} else if (!new Path(problemPath).equals(new Path(filterProblemPath))) {
					return false;
				}
				String problemTypeName = problem.getTypeName();
				String filterProblemTypeName = filterProblem.getTypeName();
				if (problemTypeName == null) {
					if (filterProblemTypeName != null) {
						return false;
					}
				} else if (filterProblemTypeName == null) {
					return false;
				} else if (!problemTypeName.equals(filterProblemTypeName)) {
					return false;
				}
				return argumentsEquals(problem.getMessageArguments(), filterProblem.getMessageArguments());
			}
			return false;
		}
		public boolean removeFilters(IApiProblemFilter[] filters) {
			return false;
		}
	}
	private static class Summary {
		List apiBundleVersionProblems = new ArrayList();
		List apiCompatibilityProblems = new ArrayList();
		List apiUsageProblems = new ArrayList();
		String componentID;

		public Summary(String componentID, IApiProblem[] apiProblems) {
			this.componentID = componentID;
			for (int i = 0, max = apiProblems.length; i < max; i++) {
				IApiProblem problem = apiProblems[i];
				switch(problem.getCategory()) {
					case IApiProblem.CATEGORY_COMPATIBILITY :
						apiCompatibilityProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_USAGE :
						apiUsageProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_VERSION :
						apiBundleVersionProblems.add(problem);
				}
			}
		}
		private void dumpProblems(String title, List problemsList,
				PrintWriter printWriter) {
			printWriter.println(title);
			if (problemsList.size() != 0) {
				for (Iterator iterator = problemsList.iterator(); iterator.hasNext(); ) {
					IApiProblem problem = (IApiProblem) iterator.next();
					printWriter.println(problem.getMessage());
				}
			} else {
				printWriter.println("None"); //$NON-NLS-1$
			}
		}
		public String getDetails() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);

			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printWriter.println("Details for " + this.componentID + ":"); //$NON-NLS-1$//$NON-NLS-2$
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			dumpProblems("Usage", apiUsageProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Compatibility", apiCompatibilityProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Bundle versions", apiBundleVersionProblems, printWriter); //$NON-NLS-1$
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}
		public String getTitle() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			printTitle(printWriter);

			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}
		private void printTitle(PrintWriter printWriter) {
			printWriter.print("Results for " + this.componentID + " : "); //$NON-NLS-1$ //$NON-NLS-2$
			printWriter.print('(');
			printWriter.print("total: "); //$NON-NLS-1$
			printWriter.print(
					  apiUsageProblems.size()
					+ apiBundleVersionProblems.size()
					+ apiCompatibilityProblems.size());
			printWriter.print(',');
			printWriter.print("usage: "); //$NON-NLS-1$
			printWriter.print(apiUsageProblems.size());
			printWriter.print(',');
			printWriter.print("compatibility: "); //$NON-NLS-1$
			printWriter.print(apiCompatibilityProblems.size());
			printWriter.print(',');
			printWriter.print("bundle version: "); //$NON-NLS-1$
			printWriter.print(apiBundleVersionProblems.size());
			printWriter.println(')');
		}
		public String toString() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			printTitle(printWriter);

			dumpProblems("Usage", apiUsageProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Compatibility", apiCompatibilityProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Bundle versions", apiBundleVersionProblems, printWriter); //$NON-NLS-1$

			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}
	}
	/**
	 * Stores integer counts for types of problems reported
	 */
	private static class ProblemCounter{
		int total, warnings, errors;
		public ProblemCounter() {
			total = warnings = errors = 0;
		}
		public void addProblem(int severity){
			total++;
			if (severity == ApiPlugin.SEVERITY_ERROR){
				errors++;
			} else if (severity == ApiPlugin.SEVERITY_WARNING){
				warnings++;
			}
		}
	}
	public static final String BUNDLE_VERSION = "bundleVersion"; //$NON-NLS-1$
	public static final String COMPATIBILITY = "compatibility"; //$NON-NLS-1$
	
	private static final Summary[] NO_SUMMARIES = new Summary[0];
	public static final String USAGE = "usage"; //$NON-NLS-1$

	private FilteredElements excludedElements;
	private FilteredElements includedElements;
	private String filters;
	private Properties properties;

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
			summaries[i++] = createProblemSummary((String) entry.getKey(), (IApiProblem[]) entry.getValue());
		}
		if (this.debug) {
			dumpSummaries(summaries);
		}
		return summaries;
	}
	private Summary createProblemSummary(String componentID, IApiProblem[] apiProblems) {
		return new Summary(componentID, apiProblems);
	}
	private void dumpReport(Summary[] summaries, List bundlesNames) {
		ProblemCounter counter = new ProblemCounter();
		for (int i = 0, max = summaries.length; i < max; i++) {
			Summary summary = summaries[i];
			String contents = null;
			String componentID = summary.componentID;
			if (this.excludedElements != null
					&& (this.excludedElements.containsExactMatch(componentID)
						|| this.excludedElements.containsPartialMatch(componentID))) {
				continue;
			}
			if (this.includedElements != null && !this.includedElements.isEmpty()
					&& !(this.includedElements.containsExactMatch(componentID)
						|| this.includedElements.containsPartialMatch(componentID))) {
				continue;
			}
			try {
				Document document = Util.newDocument();
				Element report = document.createElement(IApiXmlConstants.ELEMENT_API_TOOL_REPORT);
				report.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
				report.setAttribute(IApiXmlConstants.ATTR_COMPONENT_ID, componentID);
				document.appendChild(report);
				
				Element category = document.createElement(IApiXmlConstants.ATTR_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_COMPATIBILITY));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, COMPATIBILITY);
				insertAPIProblems(category, document, summary.apiCompatibilityProblems, counter);
				report.appendChild(category);

				category = document.createElement(IApiXmlConstants.ATTR_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_USAGE));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, USAGE);
				insertAPIProblems(category, document, summary.apiUsageProblems, counter);
				report.appendChild(category);
				
				category = document.createElement(IApiXmlConstants.ATTR_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_VERSION));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, BUNDLE_VERSION);
				insertAPIProblems(category, document, summary.apiBundleVersionProblems, counter);
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
		if (bundlesNames != null && bundlesNames.size() != 0) {
			String contents = null;
			try {
				Document document = Util.newDocument();
				Element report = document.createElement(IApiXmlConstants.ELEMENT_API_TOOL_REPORT);
				report.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
				document.appendChild(report);
				
				for (Iterator iterator = bundlesNames.iterator(); iterator.hasNext();) {
					String bundleName = (String) iterator.next();
					if (this.excludedElements == null || !this.excludedElements.containsPartialMatch(bundleName)
							&& (this.includedElements == null || this.includedElements.isEmpty() || this.includedElements.containsPartialMatch(bundleName))) {
						Element bundle = document.createElement(IApiXmlConstants.ELEMENT_BUNDLE);
						bundle.setAttribute(IApiXmlConstants.ATTR_NAME, bundleName);
						report.appendChild(bundle);
					}
				}
				contents = Util.serializeDocument(document);
			} catch (DOMException e) {
				throw new BuildException(e);
			} catch (CoreException e) {
				throw new BuildException(e);
			}
			if (contents != null) {
				saveReport("allNonApiBundles", contents, "report.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// Write out problem count file
		String contents = null;
		try {
			Document document = Util.newDocument();
			Element root = document.createElement(IApiXmlConstants.ELEMENT_REPORTED_COUNT);
			document.appendChild(root);
			root.setAttribute(IApiXmlConstants.ATTR_TOTAL, Integer.toString(counter.total));
			root.setAttribute(IApiXmlConstants.ATTR_COUNT_WARNINGS, Integer.toString(counter.warnings));
			root.setAttribute(IApiXmlConstants.ATTR_COUNT_ERRORS, Integer.toString(counter.errors));
			contents = Util.serializeDocument(document);
		} catch (DOMException e) {
			throw new BuildException(e);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
		if (contents != null) {
			saveReport(null, contents, "counts.xml"); //$NON-NLS-1$
		}
	}
	private void dumpSummaries(Summary[] summaries) {
		for (int i = 0, max = summaries.length; i < max; i++) {
			System.out.println(summaries[i].getTitle());
		}
		for (int i = 0, max = summaries.length; i < max; i++) {
			System.out.println(summaries[i].getDetails());
		}
	}
	/**
	 * Run the api tools verification task
	 * 
	 * @throws BuildException exception is thrown if anything goes wrong during the verification
	 */
	public void execute() throws BuildException {
		if (this.referenceBaselineLocation == null
				|| this.currentBaselineLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				NLS.bind(Messages.printArguments,
					new String[] {
						this.referenceBaselineLocation,
						this.currentBaselineLocation,
						this.reportLocation,
					})
			);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.debug) {
			System.out.println("reference : " + this.referenceBaselineLocation); //$NON-NLS-1$
			System.out.println("baseline to compare : " + this.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
			if (this.filters != null) {
				System.out.println("filter store : " + this.filters); //$NON-NLS-1$
			} else {
				System.out.println("No filter store"); //$NON-NLS-1$
			}
			if (this.excludeListLocation != null) {
				System.out.println("exclude list location : " + this.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
			if (this.includeListLocation != null) {
				System.out.println("include list location : " + this.includeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No include list location"); //$NON-NLS-1$
			}
		}
		// unzip reference
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
		}
		File referenceInstallDir = extractSDK(REFERENCE, this.referenceBaselineLocation);

		File baselineInstallDir = extractSDK(CURRENT, this.currentBaselineLocation);
		if (this.debug) {
			System.out.println("Preparation of baseline installation : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		// run the comparison
		// create baseline for the reference
		IApiBaseline referenceBaseline = createBaseline(REFERENCE_BASELINE_NAME, referenceInstallDir.getAbsolutePath(), this.eeFileLocation);
		IApiBaseline currentBaseline = createBaseline(CURRENT_BASELINE_NAME, baselineInstallDir.getAbsolutePath(), this.eeFileLocation);
		
		if (this.excludeListLocation != null) {
			this.excludedElements = CommonUtilsTask.initializeFilteredElements(this.excludeListLocation, currentBaseline, this.debug);
			if (this.debug) {
				System.out.println("===================================================================================="); //$NON-NLS-1$
				System.out.println("Excluded elements list:"); //$NON-NLS-1$
				System.out.println(this.excludedElements);
			}
		}
		if (this.includeListLocation != null) {
			this.includedElements = CommonUtilsTask.initializeFilteredElements(this.includeListLocation, currentBaseline, this.debug);
			if (this.debug) {
				System.out.println("===================================================================================="); //$NON-NLS-1$
				System.out.println("Included elements list:"); //$NON-NLS-1$
				System.out.println(this.includedElements);
			}
		}
		if (this.debug) {
			System.out.println("Creation of both baselines : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		Map allProblems = new HashMap();
		List allNonApiBundles = new ArrayList();
		List allApiBundles = new ArrayList();
		try {
			IApiComponent[] apiComponents = currentBaseline.getApiComponents();
			int length = apiComponents.length;
			Set visitedApiComponentNames = new HashSet();
			for (int i = 0; i < length; i++) {
				IApiComponent apiComponent = apiComponents[i];
				String name = apiComponent.getSymbolicName();
				visitedApiComponentNames.add(name);
				if (apiComponent.isSystemComponent()) {
					continue;
				}
				if (!Util.isApiToolsComponent(apiComponent)) {
					allNonApiBundles.add(name);
					continue;
				}
				allApiBundles.add(name);
				BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
				try {
					analyzer.analyzeComponent(null, getFilterStore(name), this.properties, referenceBaseline, apiComponent, new BuildContext(), new NullProgressMonitor());
					IApiProblem[] problems = analyzer.getProblems();
					// remove duplicates
					problems = removeDuplicates(problems);
					if (problems.length != 0) {
						allProblems.put(name, problems);
					}
				} catch(RuntimeException e) {
					ApiPlugin.log(e);
					throw e;
				} finally {
					analyzer.dispose();
				}
			}
			if (debug) {
				System.out.println("Total number of components in current baseline :" + length); //$NON-NLS-1$
				System.out.println("Total number of api tools components in current baseline :" + allApiBundles.size()); //$NON-NLS-1$
				System.out.println("Details:"); //$NON-NLS-1$
				Collections.sort(allApiBundles);
				for (Iterator iterator = allApiBundles.iterator(); iterator.hasNext(); ) {
					System.out.println(iterator.next());
				}
				System.out.println("=============================================================================="); //$NON-NLS-1$
				System.out.println("Total number of non-api tools components in current baseline :" + allNonApiBundles.size()); //$NON-NLS-1$
				System.out.println("Details:"); //$NON-NLS-1$
				Collections.sort(allNonApiBundles);
				for (Iterator iterator = allNonApiBundles.iterator(); iterator.hasNext(); ) {
					System.out.println(iterator.next());
				}
			}
			IApiComponent[] baselineApiComponents = referenceBaseline.getApiComponents();
			for (int i = 0, max = baselineApiComponents.length; i < max; i++) {
				IApiComponent apiComponent = baselineApiComponents[i];
				String id = apiComponent.getSymbolicName();
				if (!visitedApiComponentNames.remove(id)) {
					//remove component in the current baseline
					IApiProblem problem = ApiProblemFactory.newApiProblem(id,
							null,
							new String[] { id },
							new String[] {
								IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
								IApiMarkerConstants.API_MARKER_ATTR_ID
							},
							new Object[] {
								id,
								new Integer(IApiMarkerConstants.COMPATIBILITY_MARKER_ID),
							},
							0,
							-1,
							-1,
							IApiProblem.CATEGORY_COMPATIBILITY,
							IDelta.API_BASELINE_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_COMPONENT);
					allProblems.put(id, new IApiProblem[] { problem });
				}
			}
		} finally {
			if (this.debug) {
				System.out.println("API tools verification check : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
			referenceBaseline.dispose();
			currentBaseline.dispose();
			StubApiComponent.disposeAllCaches();
			deleteBaseline(this.referenceBaselineLocation, referenceInstallDir);
			deleteBaseline(this.currentBaselineLocation, baselineInstallDir);
			if (this.debug) {
				System.out.println("Cleanup : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		Summary[] summaries = createAllSummaries(allProblems);

		try {
			dumpReport(summaries, allNonApiBundles);
		} catch(RuntimeException e) {
			ApiPlugin.log(e);
			throw e;
		}
	}
	private IApiProblem[] removeDuplicates(IApiProblem[] problems) {
		int length = problems.length;
		if (length <= 1) return problems;
		Set uniqueProblems = new HashSet(length);
		List allProblems = null;
		for (int i = 0; i < length; i++) {
			IApiProblem apiProblem = problems[i];
			String message = apiProblem.getMessage();
			if (!uniqueProblems.contains(message)) {
				if (allProblems == null) {
					allProblems = new ArrayList(length);
				}
				uniqueProblems.add(message);
				allProblems.add(apiProblem);
			}
		}
		return (IApiProblem[]) allProblems.toArray(new IApiProblem[allProblems.size()]);
	}
	private IApiFilterStore getFilterStore(String name) {
		if (this.filters == null) return null;
		return new AntFilterStore(this.debug, this.filters, name);
	}
	/**
	 * Returns an element that contains all the api problem nodes.
	 *
	 * @param document the given xml document
	 * @param problems the given problem to dump into the document
	 * @param counter a counter object to which the reported problems can be added
	 * @return an element that contains all the api problem nodes or null if an error occured
	 */
	private void insertAPIProblems(Element root, Document document, List problems, ProblemCounter counter) throws CoreException {
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
		for(Iterator iterator = problems.iterator(); iterator.hasNext(); ) {
			IApiProblem problem = (IApiProblem) iterator.next();
			int severity = getSeverity(problem);
			counter.addProblem(severity);
			element = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM);
			element.setAttribute(IApiXmlConstants.ATTR_TYPE_NAME, String.valueOf(problem.getTypeName()));
			element.setAttribute(IApiXmlConstants.ATTR_ID, Integer.toString(problem.getId()));
			element.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(problem.getLineNumber()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_START, Integer.toString(problem.getCharStart()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_END, Integer.toString(problem.getCharEnd()));
			element.setAttribute(IApiXmlConstants.ATTR_ELEMENT_KIND, Integer.toString(problem.getElementKind()));
			element.setAttribute(IApiXmlConstants.ATTR_SEVERITY, Integer.toString(severity));
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
	 * By default, we return a warning severity.
	 * @param problem the given problem
	 * @return the problem's severity
	 */
	private int getSeverity(IApiProblem problem) {
		if (this.properties != null) {
			String key = ApiProblemFactory.getProblemSeverityId(problem);
			if (key != null) {
				String value = this.properties.getProperty(key, null);
				if (value != null) {
					if (ApiPlugin.VALUE_ERROR.equals(value)) {
						return ApiPlugin.SEVERITY_ERROR;
					}
				}
			}
		}
		return ApiPlugin.SEVERITY_WARNING;
	}
	
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the execution environment file to use.
	 * <p>By default, an execution environment file corresponding to a JavaSE-1.6 execution environment
	 * is used.</p>
	 * <p>The file is specified using an absolute path. This is optional.</p> 
	 *
	 * @param eeFileLocation the given execution environment file
	 */
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	/**
	 * Set the exclude list location.
	 * 
	 * <p>The exclude list is used to know what bundles should excluded from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the excluded elements.</p>
	 * <p>The format of the exclude list file looks like this:</p>
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
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param excludeListLocation the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	
	/**
	 * Set the include list location.
	 * 
	 * <p>The include list is used to know what bundles should included from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the included elements.</p>
	 * <p>The format of the include list file looks like this:</p>
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
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param includeListLocation the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}
	
	/**
	 * Set the root directory of API filters to use during the analysis.
	 * 
	 * <p>The argument is the root directory of the .api_filters files that should be used to filter potential
	 * problems created by the API Tools analysis. The root needs to contain the following structure:</p>
	 * <pre>
	 * root
	 *  |
	 *  +-- component name (i.e. org.eclipse.jface)
	 *         |
	 *         +--- .api_filters
	 * </pre>
	 *
	 * @param filters the root of the .api_filters files
	 */
	public void setFilters(String filters) {
		this.filters = filters; 
	}
	/**
	 * Set the preferences for the task.
	 * 
	 * <p>The preferences are used to configure problem severities. Problem severities have
	 * three possible values: Ignore, Warning, or Error. The set of problems detected is defined
	 * by corresponding problem preference keys in API tools.</p>
	 * <p>If the given location doesn't exist, the preferences won't be set.</p>
	 * <p>Lines starting with '#' are ignored. The format of the preferences file looks like this:</p>
	 * <pre>
	 * #Thu Nov 20 17:35:06 EST 2008
	 * ANNOTATION_ELEMENT_TYPE_ADDED_METHOD_WITHOUT_DEFAULT_VALUE=Ignore
	 * ANNOTATION_ELEMENT_TYPE_CHANGED_TYPE_CONVERSION=Ignore
	 * ANNOTATION_ELEMENT_TYPE_REMOVED_FIELD=Ignore
	 * ANNOTATION_ELEMENT_TYPE_REMOVED_METHOD=Ignore
	 * ANNOTATION_ELEMENT_TYPE_REMOVED_TYPE_MEMBER=Warning
	 * API_COMPONENT_ELEMENT_TYPE_REMOVED_API_TYPE=Ignore
	 * API_COMPONENT_ELEMENT_TYPE_REMOVED_TYPE=Ignore
	 * CLASS_ELEMENT_TYPE_ADDED_METHOD=Error
	 * CLASS_ELEMENT_TYPE_ADDED_RESTRICTIONS=Ignore
	 * CLASS_ELEMENT_TYPE_ADDED_TYPE_PARAMETER=Ignore
	 * CLASS_ELEMENT_TYPE_CHANGED_CONTRACTED_SUPERINTERFACES_SET=Ignore
	 * ...
	 * </pre>
	 * <p>The keys can be found in {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes}.</p>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param preferencesLocation the location of the preference file
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
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
	/**
	 * Set the location of the current product or baseline that you want to compare against
	 * the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the baseline to analyze
	 */
	public void setProfile(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}
	/**
	 * Set the location of the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.referenceBaselineLocation = baselineLocation;
	}
	/**
	 * Set the output location where the reports will be generated.
	 * 
	 * <p>Once the task is completed, reports are available in this directory using a structure
	 * similar to the filter root. A sub-folder is created for each component that has problems
	 * to be reported. Each sub-folder contains a file called "report.xml". </p>
	 * 
	 * <p>A special folder called "allNonApiBundles" is also created in this folder that contains a xml file called
	 * "report.xml". This file lists all the bundles that are not using the API Tools nature.</p>
	 * 
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
}
