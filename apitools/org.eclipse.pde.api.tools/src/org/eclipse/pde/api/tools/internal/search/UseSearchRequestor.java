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
package org.eclipse.pde.api.tools.internal.search;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.AntFilterStore;
import org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector;
import org.eclipse.pde.api.tools.internal.builder.ProblemDetectorBuilder;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.builder.ReferenceAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;

/**
 * Default implementation of an {@link IApiSearchRequestor} to use with the
 * {@link ApiSearchEngine}. This requestor returns a search scope composed of
 * the dependent (visible) {@link IApiComponent}s for the given
 * {@link IApiElement}
 *
 * <p>
 * The references are filtered based on api filter stores. The filters may come
 * from a .api_filters found in the component or a separate filter location set
 * in the ant task via {@link #setFilterRoot(String)}. If filter files are found
 * in both locations, the filters at both will be applied.
 * </p>
 *
 * @since 1.0.0
 */
public class UseSearchRequestor implements IApiSearchRequestor {

	/**
	 * The backing elements to search with
	 */
	private Set<String> fComponentIds = null;

	/**
	 * The current {@link IApiFilterStore} from the current
	 * {@link IApiComponent} context we are visiting.
	 */
	private IApiFilterStore currentStore = null;

	/**
	 * The current {@link IApiFilterStore} for the current {@link IApiComponent}
	 * context that we are visiting. The filter store will be created by finding
	 * each component's filter file in the root filter location
	 * {@link #antFilterRoot}.
	 */
	private IApiFilterStore antStore = null;

	/**
	 * The root directory of the .api_filters files that should be used to
	 * filter references.
	 *
	 * The .api_filters files specify specific problems to ignore during api
	 * analysis. During the use scan, the problem filters will be used to filter
	 * the use scan results.
	 *
	 * The root is specified using an absolute path. The root needs to contain
	 * the following structure:
	 *
	 * <pre>
	 * root
	 *  |
	 *  +-- component name (i.e. org.eclipse.jface)
	 *         |
	 *         +--- .api_filters
	 * </pre>
	 */
	private String antFilterRoot = null;

	/**
	 * The mask to use while searching
	 */
	private int fSearchMask = 0;

	/**
	 * The search scope for this requestor
	 */
	private IApiScope fScope = null;

	/**
	 * Patterns for jar API type roots to not scan
	 */
	private String[] jarPatterns = null;

	/**
	 * The default {@link ReferenceAnalyzer} for detecting illegal API use
	 *
	 * @see #includesIllegalUse()
	 */
	ReferenceAnalyzer fAnalyzer = null;

	/**
	 * Constructor
	 *
	 * @param elements an array of {@link IApiElement}s for the search engine to
	 *            use
	 * @param scope the raw list of {@link IApiElement}s to extract references
	 *            from
	 * @param searchkinds the kinds of references to search for. <br>
	 *            Options include:
	 *            <ol>
	 *            <li>{@link #INCLUDE_API}</li>
	 *            <li>{@link #INCLUDE_INTERNAL}</li>
	 *            </ol>
	 */
	public UseSearchRequestor(Set<String> elementnames, IApiElement[] scope, int searchkinds) {
		fSearchMask = searchkinds;
		fComponentIds = elementnames;
		fAnalyzer = new ReferenceAnalyzer();
		prepareScope(scope);
	}

	@Override
	public boolean acceptComponent(IApiComponent component) {
		try {
			if (!component.isSystemComponent() && getScope().encloses(component)) {
				if (includesIllegalUse()) {
					fAnalyzer.buildProblemDetectors(component, ProblemDetectorBuilder.K_USE, null);
				}
				currentStore = component.getFilterStore();
				antStore = antFilterRoot != null ? new AntFilterStore(antFilterRoot, component.getSymbolicName()) : null;
				return true;
			}
		} catch (CoreException ce) {
			// do nothing, return false
		}
		currentStore = null;
		return false;
	}

	@Override
	public boolean acceptContainer(IApiTypeContainer container) {
		return considerTypeContainer(container);
	}

	@Override
	public boolean acceptMember(IApiMember member) {
		// don't consider inner types, as they are considered with the root type
		switch (member.getType()) {
			case IApiElement.TYPE: {
				IApiType type = (IApiType) member;
				return !(type.isMemberType() || type.isLocal());
			}
			default: {
				return true;
			}
		}

	}

	/**
	 * Returns if the given {@link IApiTypeContainer} should be processed
	 *
	 * @param container
	 * @return true if the container should be processed false otherwise
	 */
	boolean considerTypeContainer(IApiTypeContainer container) {
		if (jarPatterns != null && container != null) {
			if (container.getContainerType() == IApiTypeContainer.ARCHIVE) {
				String[] pparts = null;
				for (String jarPattern : jarPatterns) {
					pparts = jarPattern.split(":"); //$NON-NLS-1$
					if (pparts.length != 2) {
						continue;
					}
					if (container.getApiComponent().getSymbolicName().equals(pparts[0])) {
						if (container.getName().endsWith(pparts[1])) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean acceptReference(IReference reference) {
		try {
			IApiMember member = reference.getResolvedReference();
			if (member != null) {
				IApiComponent component = member.getApiComponent();
				if (!fComponentIds.contains(component.getSymbolicName()) || component.equals(reference.getMember().getApiComponent())) {
					return false;
				}
				if (isIllegalUse(reference) || (includesAPI() && includesInternal())) {
					return true;
				}
				IApiAnnotations annots = component.getApiDescription().resolveAnnotations(member.getHandle());
				if (annots != null) {
					int vis = annots.getVisibility();
					if (VisibilityModifiers.isAPI(vis) && includesAPI()) {
						return true;
					} else if (VisibilityModifiers.isPrivate(vis) && includesInternal()) {
						return true;
					}
				}
			}
		} catch (CoreException ce) {
			ApiPlugin.log(ce);
		}
		return false;
	}

	/**
	 * Returns true if the given reference is an illegal usage reference iff
	 * illegal use is part of the search mask.
	 *
	 * @param reference
	 * @return true if the reference is illegal use false otherwise
	 * @since 1.1
	 */
	boolean isIllegalUse(IReference reference) {
		IApiProblemDetector[] detectors = fAnalyzer.getProblemDetectors(reference.getReferenceKind());
		for (IApiProblemDetector detector : detectors) {
			if (detector.considerReference(reference)) {
				Reference ref = (Reference) reference;
				ref.setFlags(IReference.F_ILLEGAL);
				try {
					IApiProblem pb = ((AbstractProblemDetector) detector).checkAndCreateProblem(reference);
					if (pb != null && !isFiltered(pb)) {
						ref.addProblems(pb);
					} else {
						return false;
					}
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns if the given problem is filtered
	 *
	 * @param problem
	 * @return <code>true</code> is filtered, false otherwise
	 */
	boolean isFiltered(IApiProblem problem) {
		return (currentStore != null && currentStore.isFiltered(problem)) || (antStore != null && antStore.isFiltered(problem));
	}

	@Override
	public int getReferenceKinds() {
		return IReference.MASK_REF_ALL & ~IReference.REF_CONSTANTPOOL;
	}

	/**
	 * Prepares the search scope based on the available entries in the
	 * constructor
	 *
	 * @param elements
	 */
	private void prepareScope(IApiElement[] elements) {
		if (elements != null) {
			fScope = new ApiScope();
			for (IApiElement element : elements) {
				fScope.addElement(element.getApiComponent());
			}
		}
	}

	@Override
	public IApiScope getScope() {
		return fScope;
	}

	@Override
	public boolean includesAPI() {
		return (fSearchMask & INCLUDE_API) > 0;
	}

	@Override
	public boolean includesInternal() {
		return (fSearchMask & INCLUDE_INTERNAL) > 0;
	}

	@Override
	public boolean includesIllegalUse() {
		return (fSearchMask & INCLUDE_ILLEGAL_USE) > 0;
	}

	/**
	 * The patterns for jar names to exclude from the search
	 *
	 * @param patterns
	 */
	public void setJarPatterns(String[] patterns) {
		jarPatterns = patterns;
	}

	/**
	 * Sets the root directory of the .api_filters files that should be used to
	 * filter references.
	 *
	 * The .api_filters files specify specific problems to ignore during api
	 * analysis. During the use scan, the problem filters will be used to filter
	 * the use scan results. If .api_filter files are found inside the component
	 * those filters will be applied in addition to any found at this filter
	 * root.
	 *
	 * The root is specified using an absolute path. The root needs to contain
	 * the following structure:
	 *
	 * <pre>
	 * root
	 *  |
	 *  +-- component name (i.e. org.eclipse.jface)
	 *         |
	 *         +--- .api_filters
	 * </pre>
	 *
	 * @param filterRoot the absolute string path to the root of the filters
	 */
	public void setFilterRoot(String filterRoot) {
		antFilterRoot = filterRoot;
	}
}
