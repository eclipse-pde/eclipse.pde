/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.AntFilterStore;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Ant task to run the API tool verification during Eclipse build.
 */
public class APIToolsAnalysisTask extends CommonUtilsTask {

	private static class Summary {
		List<IApiProblem> apiBundleVersionProblems = new ArrayList<>();
		List<IApiProblem> apiCompatibilityProblems = new ArrayList<>();
		List<IApiProblem> apiUsageProblems = new ArrayList<>();
		String componentID;

		public Summary(String componentID, IApiProblem[] apiProblems) {
			this.componentID = componentID;
			for (IApiProblem problem : apiProblems) {
				switch (problem.getCategory()) {
					case IApiProblem.CATEGORY_COMPATIBILITY:
						apiCompatibilityProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_USAGE:
						apiUsageProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_VERSION:
						apiBundleVersionProblems.add(problem);
						break;
					default:
						break;
				}
			}
		}

		@Override
		public String toString() {
			return getDetails();
		}

		public String getDetails() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);

			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printTitle(printWriter);
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			dumpProblems("Usage:", apiUsageProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Compatibility:", apiCompatibilityProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Bundle Versions:", apiBundleVersionProblems, printWriter); //$NON-NLS-1$
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}

		private void printTitle(PrintWriter printWriter) {
			printWriter.print(this.componentID + " : "); //$NON-NLS-1$
			printWriter.print("Total: "); //$NON-NLS-1$
			printWriter.print(apiUsageProblems.size() + apiBundleVersionProblems.size() + apiCompatibilityProblems.size());
			printWriter.print(" (Usage: "); //$NON-NLS-1$
			printWriter.print(apiUsageProblems.size());
			printWriter.print(", Compatibility: "); //$NON-NLS-1$
			printWriter.print(apiCompatibilityProblems.size());
			printWriter.print(", Bundle version: "); //$NON-NLS-1$
			printWriter.print(apiBundleVersionProblems.size());
			printWriter.print(')');
			printWriter.println();
		}

		private void dumpProblems(String title, List<IApiProblem> problemsList, PrintWriter printWriter) {
			if (problemsList.isEmpty()) {
				return;
			}
			printWriter.println(title);
			for (IApiProblem problem : problemsList) {
				printWriter.println(problem.getMessage());
			}
		}
	}

	/**
	 * Stores integer counts for types of problems reported
	 */
	private static class ProblemCounter {
		int total, warnings, errors;

		public ProblemCounter() {
			total = warnings = errors = 0;
		}

		public void addProblem(int severity) {
			total++;
			if (severity == ApiPlugin.SEVERITY_ERROR) {
				errors++;
			} else if (severity == ApiPlugin.SEVERITY_WARNING) {
				warnings++;
			}
		}
	}

	public static final String BUNDLE_VERSION = "bundleVersion"; //$NON-NLS-1$
	public static final String COMPATIBILITY = "compatibility"; //$NON-NLS-1$
	public static final String COMPONENT_RESOLUTION = "componentResolution"; //$NON-NLS-1$

	private static final Summary[] NO_SUMMARIES = new Summary[0];
	public static final String USAGE = "usage"; //$NON-NLS-1$

	private FilteredElements excludedElements;
	private FilteredElements includedElements;
	private String filters;
	private Properties properties;

	/**
	 * When <code>true</code>, components containing resolver errors will still
	 * be included in the analysis. A list of bundles with resolver errors will
	 * be included in the output xml. Set to <code>true</code> by default.
	 */
	private boolean processUnresolvedBundles = true;

	private Summary[] createAllSummaries(Map<String, IApiProblem[]> allProblems) {
		Set<Map.Entry<String, IApiProblem[]>> entrySet = allProblems.entrySet();
		int size = entrySet.size();
		if (size == 0) {
			return NO_SUMMARIES;
		}
		List<Entry<String, IApiProblem[]>> allEntries = new ArrayList<>();
		allEntries.addAll(entrySet);
		Collections.sort(allEntries, (entry1, entry2) -> {
			return entry1.getKey().compareTo(entry2.getKey());
		});
		Summary[] summaries = new Summary[size];
		int i = 0;
		for (Map.Entry<String, IApiProblem[]> entry : allEntries) {
			summaries[i++] = createProblemSummary(entry.getKey(), entry.getValue());
		}
		return summaries;
	}

	private Summary createProblemSummary(String componentID, IApiProblem[] apiProblems) {
		return new Summary(componentID, apiProblems);
	}

	private void dumpReport(Summary[] summaries, List<String> nonAPIBundleNames, Map<String, Object> bundlesWithErrors) {
		ProblemCounter counter = new ProblemCounter();
		for (Summary summary : summaries) {
			String contents = null;
			String componentID = summary.componentID;

			// Filtering should be done during analysis to save time, but filter
			// results anyways
			if (isFiltered(componentID)) {
				continue;
			}

			if (this.debug) {
				System.out.println(summary.getDetails());
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

				if (bundlesWithErrors != null && bundlesWithErrors.containsKey(componentID)) {
					category = document.createElement(IApiXmlConstants.ATTR_CATEGORY);
					category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION));
					category.setAttribute(IApiXmlConstants.ATTR_VALUE, COMPONENT_RESOLUTION);
					ResolverError[] errors = (ResolverError[]) bundlesWithErrors.get(componentID);
					for (ResolverError e : errors) {
						Element error = document.createElement(IApiXmlConstants.ELEMENT_RESOLVER_ERROR);
						error.setAttribute(IApiXmlConstants.ATTR_MESSAGE, e.toString());
						category.appendChild(error);
					}
					report.appendChild(category);
				}

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

		// Write out a list of components skipped because they aren't API Tools
		// enabled
		if (nonAPIBundleNames != null && nonAPIBundleNames.size() != 0) {
			String contents = null;
			try {
				Document document = Util.newDocument();
				Element report = document.createElement(IApiXmlConstants.ELEMENT_API_TOOL_REPORT);
				report.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
				document.appendChild(report);
				for (String bundleName : nonAPIBundleNames) {
					if (!isFiltered(bundleName)) {
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
				saveReport("Skipped Bundles", contents, "report.xml"); //$NON-NLS-1$ //$NON-NLS-2$
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

	/**
	 * Run the api tools verification task
	 *
	 * @throws BuildException exception is thrown if anything goes wrong during
	 *             the verification
	 */
	@Override
	public void execute() throws BuildException {
		if (this.referenceBaselineLocation == null || this.currentBaselineLocation == null || this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(Messages.printArguments, new String[] {
					this.referenceBaselineLocation,
					this.currentBaselineLocation, this.reportLocation, }));
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
				System.out.println("=============================================================================="); //$NON-NLS-1$
				System.out.println("Excluded elements list:"); //$NON-NLS-1$
				System.out.println(this.excludedElements);
			}
		}
		if (this.includeListLocation != null) {
			this.includedElements = CommonUtilsTask.initializeFilteredElements(this.includeListLocation, currentBaseline, this.debug);
			if (this.debug) {
				System.out.println("=============================================================================="); //$NON-NLS-1$
				System.out.println("Included elements list:"); //$NON-NLS-1$
				System.out.println(this.includedElements);
			}
		}
		if (this.debug) {
			System.out.println("Creation of both baselines : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		Map<String, IApiProblem[]> allProblems = new HashMap<>();
		List<String> allNonApiBundles = new ArrayList<>();
		List<String> allApiBundles = new ArrayList<>();
		Map<String, Object> bundlesWithErrors = new LinkedHashMap<>();
		try {
			IApiComponent[] apiComponents = currentBaseline.getApiComponents();
			int length = apiComponents.length;
			Set<String> visitedApiComponentNames = new HashSet<>();
			for (int i = 0; i < length; i++) {
				IApiComponent apiComponent = apiComponents[i];
				String name = apiComponent.getSymbolicName();
				visitedApiComponentNames.add(name);

				if (isFiltered(name)) {
					continue;
				}
				if (apiComponent.isSystemComponent()) {
					continue;
				}
				if (!Util.isApiToolsComponent(apiComponent)) {
					if (Util.hasJavaPackages(apiComponent)) {
						allNonApiBundles.add(name);
					}
					continue;
				}

				// If the component has resolver errors the results may not be
				// accurate, store problems in other category
				try {
					ResolverError[] resolverErrors = apiComponent.getErrors();
					if (resolverErrors != null && resolverErrors.length > 0) {
						bundlesWithErrors.put(name, apiComponent.getErrors());
						if (!processUnresolvedBundles) {
							// If the user has turned off the setting, do not
							// process bundles with resolver errors
							continue;
						}
					}
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
					throw new BuildException(e);
				}

				allApiBundles.add(name);
				BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
				try {
					analyzer.setContinueOnResolverError(true);
					analyzer.analyzeComponent(null, getFilterStore(name), this.properties, referenceBaseline, apiComponent, new BuildContext(), new NullProgressMonitor());
					IApiProblem[] problems = analyzer.getProblems();
					// remove duplicates
					problems = removeDuplicates(problems);
					if (problems.length != 0) {
						allProblems.put(name, problems);
					} else if (this.debug) {
						System.out.println(name + " has no problems"); //$NON-NLS-1$
					}
				} catch (RuntimeException e) {
					ApiPlugin.log(e);
					throw e;
				} finally {
					analyzer.dispose();
				}
			}
			if (debug) {
				System.out.println("=========================="); //$NON-NLS-1$
				System.out.println("Total number of components in current baseline :" + length); //$NON-NLS-1$
				System.out.println("=========================="); //$NON-NLS-1$
				System.out.println("Total number of api tools components in current baseline :" + allApiBundles.size()); //$NON-NLS-1$
				System.out.println("Details:"); //$NON-NLS-1$
				Collections.sort(allApiBundles);
				for (String string : allApiBundles) {
					System.out.println(string);
				}
				System.out.println("=========================="); //$NON-NLS-1$
				System.out.println("Total number of non-api tools components in current baseline :" + allNonApiBundles.size()); //$NON-NLS-1$
				System.out.println("Details:"); //$NON-NLS-1$
				Collections.sort(allNonApiBundles);
				for (String string : allNonApiBundles) {
					System.out.println(string);
				}
				System.out.println("=========================="); //$NON-NLS-1$
				System.out.println("Total number of components with resolver errors :" + bundlesWithErrors.size()); //$NON-NLS-1$
				System.out.println("Details:"); //$NON-NLS-1$
				List<String> names = new ArrayList<>();
				names.addAll(bundlesWithErrors.keySet());
				Collections.sort(names);
				for (String name : names) {
					System.out.println(name);
					ResolverError[] errors = (ResolverError[]) bundlesWithErrors.get(name);
					for (ResolverError error : errors) {
						System.out.println(error);
					}
				}
				System.out.println("=========================="); //$NON-NLS-1$
			}

			// Check if any components have been removed from the baseline
			IApiComponent[] baselineApiComponents = referenceBaseline.getApiComponents();
			for (IApiComponent apiComponent : baselineApiComponents) {
				String id = apiComponent.getSymbolicName();
				if (!visitedApiComponentNames.remove(id)) {
					// A component has been removed. Apply any include/exclude
					// filters
					if (!isFiltered(id)) {
						IApiProblem problem = ApiProblemFactory.newApiProblem(id, null, new String[] { id }, new String[] {
								IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
								IApiMarkerConstants.API_MARKER_ATTR_ID }, new Object[] {
								id,
								Integer.valueOf(IApiMarkerConstants.COMPATIBILITY_MARKER_ID), }, 0, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_BASELINE_ELEMENT_TYPE, IDelta.REMOVED, IDelta.API_COMPONENT);
						allProblems.put(id, new IApiProblem[] { problem });
					}
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
			dumpReport(summaries, allNonApiBundles, bundlesWithErrors);
		} catch (RuntimeException e) {
			ApiPlugin.log(e);
			throw e;
		}
	}

	/**
	 * Returns <code>true</code if the given component should be filtered from
	 * results of this task. This may be because the name is a match or partial
	 * match to the exlude list or the name is not a match to the include list.
	 * If no include or exclude list is provided, no filtering is done and
	 * <code>false</code> is returned.
	 *
	 * @param componentID name of the api component (symbolic name of a bundle)
	 * @return whether the given component should be filtered out of results
	 */
	private boolean isFiltered(String componentID) {
		if (this.excludedElements != null && (this.excludedElements.containsExactMatch(componentID) || this.excludedElements.containsPartialMatch(componentID))) {
			return true;
		}
		if (this.includedElements != null && !this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(componentID) || this.includedElements.containsPartialMatch(componentID))) {
			return true;
		}
		return false;
	}

	private IApiProblem[] removeDuplicates(IApiProblem[] problems) {
		int length = problems.length;
		if (length <= 1) {
			return problems;
		}
		Set<String> uniqueProblems = new HashSet<>(length);
		List<IApiProblem> allProblems = null;
		for (int i = 0; i < length; i++) {
			IApiProblem apiProblem = problems[i];
			String message = apiProblem.getMessage();
			if (!uniqueProblems.contains(message)) {
				if (allProblems == null) {
					allProblems = new ArrayList<>(length);
				}
				uniqueProblems.add(message);
				allProblems.add(apiProblem);
			}
		}
		return allProblems.toArray(new IApiProblem[allProblems.size()]);
	}

	private IApiFilterStore getFilterStore(String name) {
		if (this.filters == null) {
			return null;
		}
		return new AntFilterStore(this.filters, name);
	}

	/**
	 * Returns an element that contains all the API problem nodes.
	 *
	 * @param document the given XML document
	 * @param problems the given problem to dump into the document
	 * @param counter a counter object to which the reported problems can be
	 *            added
	 * @return an element that contains all the api problem nodes or null if an
	 *         error occurred
	 *
	 * @throws CoreException
	 */
	private void insertAPIProblems(Element root, Document document, List<IApiProblem> problems, ProblemCounter counter) throws CoreException {
		Element apiProblems = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEMS);
		root.appendChild(apiProblems);
		Element element = null;
		// sort the problem by type name
		Collections.sort(problems, (p1, p2) -> {
			return p1.getTypeName().compareTo(p2.getTypeName());
		});
		for (IApiProblem problem : problems) {
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
	 *
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
	 * <p>
	 * The possible values are: <code>true</code>, <code>false</code>
	 * </p>
	 * <p>
	 * Default is <code>false</code>.
	 * </p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue);
	}

	/**
	 * Set the execution environment file to use.
	 * <p>
	 * By default, an execution environment file corresponding to a JavaSE-1.6
	 * execution environment is used.
	 * </p>
	 * <p>
	 * The file is specified using an absolute path. This is optional.
	 * </p>
	 *
	 * @param eeFileLocation the given execution environment file
	 */
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
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
	 * @param excludeListLocation the given location for the excluded list file
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
	 * @param includeListLocation the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}

	/**
	 * Set the root directory of API filters to use during the analysis.
	 *
	 * <p>
	 * The argument is the root directory of the .api_filters files that should
	 * be used to filter potential problems created by the API Tools analysis.
	 * The root needs to contain the following structure:
	 * </p>
	 *
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
	 * <p>
	 * The keys can be found in
	 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes}
	 * .
	 * </p>
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
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
	 * @param baselineLocation the given location for the baseline to analyze
	 */
	public void setProfile(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}

	/**
	 * Set the location of the reference baseline.
	 *
	 * <p>
	 * It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that
	 * corresponds to the Eclipse installation folder. This is the directory is
	 * which you can find the Eclipse executable.
	 * </p>
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 *
	 * @param baselineLocation the given location for the reference baseline to
	 *            analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.referenceBaselineLocation = baselineLocation;
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
	 * bundles that are not using the API Tools nature.
	 * </p>
	 *
	 * @param baselineLocation the given location for the reference baseline to
	 *            analyze
	 */
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}

	/**
	 * Set whether to continue analyzing an api component (bundle) even if it
	 * has resolver errors such as missing dependencies. The results of the
	 * analysis may not be accurate. A list of the resolver errors found for
	 * each bundle will be included in the xml output and incorporated into the
	 * html output of the report conversion task. Defaults to <code>true</code>
	 *
	 * @param processUnresolvedBundles whether to continue processing a bundle
	 *            that has resolver errors
	 */
	public void setProcessUnresolvedBundles(boolean processUnresolvedBundles) {
		this.processUnresolvedBundles = processUnresolvedBundles;
	}

}
