/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

import com.ibm.icu.text.MessageFormat;

/**
 * The reference analyzer
 * 
 * @since 1.1
 */
public class ReferenceAnalyzer {

	/**
	 * Natural log of 2.
	 */
	private static final double LOG2 = Math.log(2);
	
	/**
	 * Empty result collection.
	 */
	private static final IApiProblem[] EMPTY_RESULT = new IApiProblem[0];
	/**
	 * No problem detector to use
	 */
	private static final IApiProblemDetector[] NO_PROBLEM_DETECTORS = new IApiProblemDetector[0];
	
	/**
	 * Visits each class file, extracting references.
	 */
	class Visitor extends ApiTypeContainerVisitor {
		
		private IProgressMonitor fMonitor = null;
		
		public Visitor(IProgressMonitor monitor) {
			fMonitor = monitor;
		}
		public boolean visitPackage(String packageName) {
			fMonitor.subTask(MessageFormat.format(BuilderMessages.ReferenceAnalyzer_checking_api_used_by, new String[]{packageName}));
			return true;
		}
		public void endVisitPackage(String packageName) {
			fMonitor.worked(1);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.model.component.IClassFile)
		 */
		public void visit(String packageName, IApiTypeRoot classFile) {
			if (!fMonitor.isCanceled()) {
				try {
					IApiType type = classFile.getStructure();
					if(type == null) {
						//do nothing for bad class files
						return;
					}
					//don't process inner/anonymous/local types, this is done in the extractor
					if(type.isMemberType() || type.isLocal() || type.isAnonymous()) {
						return;
					}
					List references = type.extractReferences(fAllReferenceKinds, null);
					// keep potential matches
					Iterator iterator = references.iterator();
					while (iterator.hasNext()) {
						IReference ref = (IReference) iterator.next();
						// compute index of interested problem detectors
						int index = getLog2(ref.getReferenceKind());
						IApiProblemDetector[] detectors = fIndexedDetectors[index];
						boolean added = false;
						if (detectors != null) {
							for (int i = 0; i < detectors.length; i++) {
								IApiProblemDetector detector = detectors[i];
								if (detector.considerReference(ref)) {
									if (!added) {
										fReferences.add(ref);
										added = true;
									}
								}
							}
						}
					}
				} catch (CoreException e) {
					fStatus.add(e.getStatus());
				}
			}
		}
	}
	
	/**
	 * Scan status
	 */
	MultiStatus fStatus;	
	
	/**
	 * Bit mask of reference kinds that problem detectors care about.
	 */
	int fAllReferenceKinds = 0;
	
	/**
	 * List of references to consider/resolve.
	 */
	List fReferences = new LinkedList();
	
	/**
	 * Problem detectors indexed by the log base 2 of each reference kind they
	 * are interested in. Provides a fast way to hand references off to interested
	 * problem detectors.
	 */
	IApiProblemDetector[][] fIndexedDetectors;
	
	/**
	 * Indexes the problem detectors by the reference kinds they are interested in.
	 * For example, a detector interested in a
	 * {@link org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers#REF_INSTANTIATE}
	 * will be in the 26th index (0x1 << 27, which is 2 ^ 26).
	 * Also initializes the bit mask of all interesting reference kinds.
	 * 
	 * @param detectors problem detectors
	 */
	void indexProblemDetectors(IApiProblemDetector[] detectors) {
		fIndexedDetectors = new IApiProblemDetector[32][];
		for (int i = 0; i < detectors.length; i++) {
			IApiProblemDetector detector = detectors[i];
			int kinds = detector.getReferenceKinds();
			fAllReferenceKinds |= kinds;
			int mask = 0x1;
			for (int bit = 0; bit < 32; bit++) {
				if ((mask & kinds) > 0) {
					IApiProblemDetector[] indexed = fIndexedDetectors[bit];
					if (indexed == null) {
						fIndexedDetectors[bit] = new IApiProblemDetector[]{detector};
					} else {
						IApiProblemDetector[] next = new IApiProblemDetector[indexed.length + 1];
						System.arraycopy(indexed, 0, next, 0, indexed.length);
						next[indexed.length] = detector;
						fIndexedDetectors[bit] = next;
					}
				}
				mask = mask << 1;
			}
		}
	}
	
	/**
	 * log 2 (x) = ln(x) / ln(2)
	 * 
	 * @param bitConstant a single bit constant (0x1 << n)
	 * @return log base 2 of the constant (the power of 2 the constant is equal to)
	 */
	int getLog2(int bitConstant) {
		double logX = Math.log(bitConstant);
		double pow = logX / LOG2;
		return (int)Math.round(pow);
	}	
	
	/**
	 * Scans the given scope extracting all reference information.
	 * 
	 * @param scope scope to scan
	 * @param monitor progress monitor
	 * @exception CoreException if the scan fails
	 */
	void extractReferences(IApiTypeContainer scope, IProgressMonitor monitor) throws CoreException {
		fStatus = new MultiStatus(ApiPlugin.PLUGIN_ID, 0, BuilderMessages.ReferenceAnalyzer_api_analysis_error, null); 
		String[] packageNames = scope.getPackageNames();
		SubMonitor localMonitor = SubMonitor.convert(monitor, packageNames.length);
		ApiTypeContainerVisitor visitor = new Visitor(localMonitor);
		long start = System.currentTimeMillis();
		try {
			scope.accept(visitor);
		} catch (CoreException e) {
			fStatus.add(e.getStatus());
		}
		long end = System.currentTimeMillis();
		if (!fStatus.isOK()) {
			throw new CoreException(fStatus);
		}
		localMonitor.done();
		if (ApiPlugin.DEBUG_REFERENCE_ANALYZER) {
			System.out.println("Reference Analyzer: extracted " + fReferences.size() + " references in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}	

	/**
	 * Analyzes the given {@link IApiComponent} within the given {@link IApiTypeContainer} (scope) and returns 
	 * a collection of detected {@link IApiProblem}s or an empty collection, never <code>null</code>
	 * @param component
	 * @param scope
	 * @param monitor
	 * @return the collection of detected {@link IApiProblem}s or an empty collection, never <code>null</code>
	 * @throws CoreException
	 */
	public IApiProblem[] analyze(IApiComponent component, IApiTypeContainer scope, IProgressMonitor monitor) throws CoreException {
		// build problem detectors
		IApiProblemDetector[] detectors = buildProblemDetectors(component, ProblemDetectorBuilder.K_ALL, monitor);
		// analyze
		try {
			// 1. extract references
			SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.ReferenceAnalyzer_analyzing_api, 3);
			localMonitor.subTask(BuilderMessages.ReferenceAnalyzer_analyzing_api_checking_use); 
			extractReferences(scope, localMonitor);
			localMonitor.worked(1);
			if (localMonitor.isCanceled()) {
				return EMPTY_RESULT;
			}
			// 2. resolve problematic references
			localMonitor.subTask(BuilderMessages.ReferenceAnalyzer_analyzing_api_checking_use);
			if (fReferences.size() != 0) {
				ReferenceResolver.resolveReferences(fReferences, localMonitor);
			}
			localMonitor.worked(1);
			if (localMonitor.isCanceled()) {
				return EMPTY_RESULT;
			}		
			// 3. create problems
			List allProblems = new LinkedList();
			localMonitor.subTask(BuilderMessages.ReferenceAnalyzer_analyzing_api_checking_use);
			for (int i = 0; i < detectors.length; i++) {
				IApiProblemDetector detector = detectors[i];
				List problems = detector.createProblems();
				allProblems.addAll(problems);
				if (localMonitor.isCanceled()) {
					return EMPTY_RESULT;
				}
			}
			IApiProblem[] array = (IApiProblem[]) allProblems.toArray(new IApiProblem[allProblems.size()]);
			localMonitor.worked(1);
			localMonitor.done();
			return array;
		} finally {
			// clean up
			fIndexedDetectors = null;
			fReferences.clear();
		}
	}
	
	/**
	 * Returns the collection of problem detectors for the given reference kind
	 * @param referencekind
	 * @return
	 */
	public IApiProblemDetector[] getProblemDetectors(int referencekind) {
		if(fIndexedDetectors != null) {
			int index = getLog2(referencekind);
			if(index > -1 && index < fIndexedDetectors.length) {
				IApiProblemDetector[] detectors = fIndexedDetectors[index];
				if(detectors != null) {
					return detectors;
				}
			}
			return NO_PROBLEM_DETECTORS;
		}
		return NO_PROBLEM_DETECTORS;
	}
	
	/**
	 * Builds problem detectors to use when analyzing the given component.
	 * 
	 * @param component component to be analyzed
	 * @param kindmask the kinds of detectors to build. See {@link ProblemDetectorBuilder} for kinds
	 * @param monitor 
	 * 
	 * @return problem detectors
	 */
	public IApiProblemDetector[] buildProblemDetectors(IApiComponent component, int kindmask, IProgressMonitor monitor) {
		try {
			long start = System.currentTimeMillis();
			IApiComponent[] components = component.getBaseline().getPrerequisiteComponents(new IApiComponent[]{component});
			final ProblemDetectorBuilder visitor = new ProblemDetectorBuilder(component, kindmask);
			for (int i = 0; i < components.length; i++) {
				Util.updateMonitor(monitor);
				IApiComponent prereq = components[i];
				if (!prereq.equals(component)) {
					visitor.setOwningComponent(prereq);
					try {
						prereq.getApiDescription().accept(visitor, monitor);
					} catch (CoreException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
			}
			long end = System.currentTimeMillis();
			if (ApiPlugin.DEBUG_REFERENCE_ANALYZER) {
				System.out.println("Time to build problem detectors: " + (end-start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
			}		
			// add names from the leak component as well
			ApiDescriptionVisitor nameVisitor = new ApiDescriptionVisitor() {
				/* (non-Javadoc)
				 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
				 */
				public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
					if (element.getElementType() == IElementDescriptor.PACKAGE) {
						if (VisibilityModifiers.isPrivate(description.getVisibility())) {
							visitor.addNonApiPackageName(((IPackageDescriptor)element).getName());
						}
					}
					return false;
				}
			};
			component.getApiDescription().accept(nameVisitor, null);
			List detectors = visitor.getProblemDetectors();
			int size = detectors.size();
			if (size == 0) {
				return NO_PROBLEM_DETECTORS;
			}
			IApiProblemDetector[] array = (IApiProblemDetector[]) detectors.toArray(new IApiProblemDetector[size]);
			indexProblemDetectors(array);
			return array;
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return NO_PROBLEM_DETECTORS;
	}
}
