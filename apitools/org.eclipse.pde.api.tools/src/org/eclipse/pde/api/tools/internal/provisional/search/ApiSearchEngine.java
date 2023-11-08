/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.AntFilterStore;
import org.eclipse.pde.api.tools.internal.builder.ReferenceResolver;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.search.SearchMessages;

/**
 * Engine used to search for API use
 *
 * @since 1.0.0
 */
public final class ApiSearchEngine {

	/**
	 * Default empty array for no search matches
	 */
	public static final IReference[] NO_REFERENCES = new IReference[0];

	/**
	 * Visitor used to extract references from the component is is passed to
	 */
	class ReferenceExtractor extends ApiTypeContainerVisitor {
		static final int COLLECTOR_MAX = 2500;
		private List<IReference> collector = null;
		private IApiSearchRequestor requestor = null;
		private IApiSearchReporter reporter = null;
		IApiElement element = null;
		private SubMonitor monitor = null;

		/**
		 * Constructor
		 */
		public ReferenceExtractor(IApiSearchRequestor requestor, IApiSearchReporter reporter, IApiElement element, IProgressMonitor monitor) {
			collector = new ArrayList<>();
			this.requestor = requestor;
			this.reporter = reporter;
			this.element = element;
			this.monitor = SubMonitor.convert(monitor);
		}

		@Override
		public boolean visit(IApiTypeContainer container) {
			return requestor.acceptContainer(container);
		}

		@Override
		public void visit(String packageName, IApiTypeRoot typeroot) {
			if (monitor.isCanceled()) {
				return;
			}
			try {
				IApiType type = typeroot.getStructure();
				if (type == null || !requestor.acceptMember(type)) {
					return;
				}
				collector.addAll(acceptReferences(requestor, type, getResolvedReferences(requestor, type, monitor.split(1)), monitor.split(1)));
			} catch (CoreException ce) {
				ApiPlugin.log(ce);
			}
		}

		@Override
		public void end(String packageName, IApiTypeRoot typeroot) {
			if (this.collector.size() >= COLLECTOR_MAX) {
				reportResults();
			}
		}

		@Override
		public boolean visit(IApiComponent component) {
			return requestor.acceptComponent(component);
		}

		@Override
		public void endVisitPackage(String packageName) {
			reportResults();
		}

		private void reportResults() {
			reporter.reportResults(this.element, collector.toArray(new IReference[collector.size()]));
			collector.clear();
		}
	}

	/**
	 * Simple string used for reporting what is being searched
	 */
	private String fRequestorContext = null;

	/**
	 * Returns the set of resolved references for the given {@link IApiType}
	 *
	 * @param requestor
	 * @param type
	 * @param monitor
	 * @return The listing of resolved references from the given
	 *         {@link IApiType}
	 * @throws CoreException
	 */
	List<IReference> getResolvedReferences(IApiSearchRequestor requestor, IApiType type, IProgressMonitor monitor) throws CoreException {
		String name = type.getSimpleName() == null ? SearchMessages.ApiSearchEngine_anonymous_type : type.getSimpleName();
		SubMonitor localmonitor = SubMonitor.convert(monitor, MessageFormat.format(SearchMessages.ApiSearchEngine_extracting_refs_from, name), 2);
		List<IReference> refs = type.extractReferences(requestor.getReferenceKinds(), localmonitor.split(1));
		ReferenceResolver.resolveReferences(refs, localmonitor.split(1));
		return refs;
	}

	/**
	 * Runs the given list of references through the search requestor to
	 * determine if they should be kept or not
	 *
	 * @param requestor
	 * @param type
	 * @param references
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	List<IReference> acceptReferences(IApiSearchRequestor requestor, IApiType type, List<IReference> references, IProgressMonitor monitor) throws CoreException {
		ArrayList<IReference> refs = new ArrayList<>();
		IReference ref = null;
		SubMonitor localmonitor = SubMonitor.convert(monitor, references.size());
		IApiMember member = null;
		for (Iterator<IReference> iter = references.iterator(); iter.hasNext() && !monitor.isCanceled();) {
			if (localmonitor.isCanceled()) {
				return Collections.emptyList();
			}
			ref = iter.next();
			member = ref.getResolvedReference();
			if (member == null) {
				continue;
			}
			localmonitor.setTaskName(MessageFormat.format(SearchMessages.ApiSearchEngine_searching_for_use_from, fRequestorContext, type.getName()));
			if (requestor.acceptReference(ref, monitor)) {
				refs.add(ref);
			}
			localmonitor.worked(1);
		}
		return refs;
	}

	/**
	 * Searches for all accepted {@link IReference}s from the given
	 * {@link IApiElement}
	 *
	 * @param requestor
	 * @param element
	 * @param reporter search reporter to output xml results as references are
	 *            found
	 * @param monitor
	 * @return the collection of accepted {@link IReference}s or an empty list,
	 *         never <code>null</code>
	 * @throws CoreException
	 */
	private void searchReferences(IApiSearchRequestor requestor, IApiElement element, IApiSearchReporter reporter, IProgressMonitor monitor) throws CoreException {
		List<IReference> refs = null;
		SubMonitor localmonitor = SubMonitor.convert(monitor, 3);
		switch (element.getType())
			{
			case IApiElement.TYPE: {
				if (localmonitor.isCanceled()) {
					reporter.reportResults(element, NO_REFERENCES);
				}
				IApiType type = (IApiType) element;
				refs = acceptReferences(requestor, type, getResolvedReferences(requestor, type, localmonitor.split(1)),
						localmonitor.split(1));
				reporter.reportResults(element, refs.toArray(new IReference[refs.size()]));
				break;
			}
			case IApiElement.COMPONENT: {
				if (localmonitor.isCanceled()) {
					reporter.reportResults(element, NO_REFERENCES);
				}
				ReferenceExtractor visitor = new ReferenceExtractor(requestor, reporter, element,
						localmonitor.split(1));
				IApiComponent comp = (IApiComponent) element;
				comp.accept(visitor);
				comp.close();
				localmonitor.split(1);
				break;
			}
			case IApiElement.FIELD:
			case IApiElement.METHOD: {
				if (localmonitor.isCanceled()) {
					reporter.reportResults(element, NO_REFERENCES);
				}
				IApiMember member = (IApiMember) element;
				IApiType type = member.getEnclosingType();
				if (type != null) {
					refs = acceptReferences(requestor, type,
							getResolvedReferences(requestor, type, localmonitor.split(1)), localmonitor.split(1));
				}
				if (refs != null) {
					reporter.reportResults(element, refs.toArray(new IReference[refs.size()]));
				}
				break;
			}
			default:
				break;
			}
		localmonitor.split(1);
	}

	/**
	 * Searches for all of the use of API or internal code from the given
	 * {@link IApiComponent} within the given {@link IApiBaseline}
	 *
	 * @param baseline the baseline to search within
	 * @param requestor the requestor to use for the search
	 * @param reporter the reporter to use when reporting any search results to
	 *            the user
	 * @param monitor the monitor to report progress to
	 * @throws CoreException if the search fails
	 */
	public void search(IApiBaseline baseline, IApiSearchRequestor requestor, IApiSearchReporter reporter, IProgressMonitor monitor) throws CoreException {
		if (baseline == null || reporter == null || requestor == null) {
			return;
		}
		IApiScope scope = requestor.getScope();
		if (scope == null) {
			return;
		}
		AntFilterStore.filteredAPIProblems.clear();
		fRequestorContext = computeContext(requestor);
		IApiElement[] scopeelements = scope.getApiElements();
		SubMonitor localmonitor = SubMonitor.convert(monitor, MessageFormat.format(SearchMessages.ApiSearchEngine_searching_projects, fRequestorContext), scopeelements.length * 2 + 1);
		long start = System.currentTimeMillis();
		long loopstart = 0;
		String taskname = null;
		MultiStatus mstatus = null;
		for (int i = 0; i < scopeelements.length; i++) {
			try {
				taskname = MessageFormat.format(SearchMessages.ApiSearchEngine_searching_project,
						scopeelements[i].getApiComponent().getSymbolicName(), fRequestorContext);
				localmonitor.setTaskName(taskname);
				if (ApiPlugin.DEBUG_SEARCH_ENGINE) {
					loopstart = System.currentTimeMillis();
					System.out.println("Searching " + scopeelements[i].getApiComponent().getSymbolicName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				searchReferences(requestor, scopeelements[i], reporter, localmonitor.split(1));
				localmonitor.setTaskName(taskname);
				if (localmonitor.isCanceled()) {
					reporter.reportResults(scopeelements[i], NO_REFERENCES);
					return;
				}
				localmonitor.worked(1);
				if (ApiPlugin.DEBUG_SEARCH_ENGINE) {
					System.out.println(Math.round((((float) (i + 1)) / scopeelements.length) * 100) + "% done in " //$NON-NLS-1$
							+ (System.currentTimeMillis() - loopstart) + " ms"); //$NON-NLS-1$
				}
			} catch (CoreException ce) {
				if (mstatus == null) {
					mstatus = new MultiStatus(ApiPlugin.PLUGIN_ID, IStatus.ERROR, null, null);
				}
				mstatus.add(Status.error(ce.getMessage(), ce));
			}
		}
		if (ApiPlugin.DEBUG_SEARCH_ENGINE) {
			System.out.println("Total Search Time: " + ((System.currentTimeMillis() - start) / 1000) + " seconds"); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (mstatus != null) {
			throw new CoreException(mstatus);
		}
	}

	/**
	 * Computes the process context (label)
	 *
	 * @param requestor
	 * @return the label describing the process for the progress monitor
	 */
	String computeContext(IApiSearchRequestor requestor) {
		String context = SearchMessages.ApiSearchEngine_api_internal;
		if (requestor.includesAPI()) {
			if (requestor.includesInternal()) {
				if (requestor.includesIllegalUse()) {
					return context;
				} else {
					context = SearchMessages.ApiSearchEngine_api_and_internal;
				}
			} else if (!requestor.includesIllegalUse()) {
				context = SearchMessages.ApiSearchEngine_api;
			} else {
				context = SearchMessages.ApiSearchEngine_api_and_illegal;
			}
		} else if (requestor.includesInternal()) {
			if (requestor.includesIllegalUse()) {
				context = SearchMessages.ApiSearchEngine_internal_and_illegal;
			} else {
				context = SearchMessages.ApiSearchEngine_internal;
			}
		} else if (requestor.includesIllegalUse()) {
			context = SearchMessages.ApiSearchEngine_illegal;
		}
		return context;
	}
}
