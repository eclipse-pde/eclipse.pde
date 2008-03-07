/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassReader;

/**
 * Extracts references from an API component.
 * 
 * @since 1.0.0
 */
public class SearchEngine implements IApiSearchEngine {
	
	/**
	 * Constant used for controlling tracing in the search engine
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the search engine
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * Empty references collection.
	 */
	private static final IReference[] EMPTY_REF = new IReference[0];
	
	/**
	 * Visits each class file, extracting references.
	 */
	class Visitor extends ClassFileContainerVisitor {
		
		private IApiComponent fCurrentComponent = null;
		private IProgressMonitor fMonitor = null;
		
		public Visitor(IProgressMonitor monitor) {
			fMonitor = monitor;
		}

		public void end(IApiComponent component) {
			fCurrentComponent = null;
		}

		public boolean visit(IApiComponent component) {
			fCurrentComponent = component;
			return true;
		}
		
		public boolean visitPackage(String packageName) {
			fMonitor.subTask(SearchMessages.SearchEngine_0 + packageName);
			return true;
		}

		public void endVisitPackage(String packageName) {
			fMonitor.worked(1);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.model.component.IClassFile)
		 */
		public void visit(String packageName, IClassFile classFile) {
			if (!fMonitor.isCanceled()) {
				try {
					fScanner.scan(fCurrentComponent, classFile, fLocalRefs, fAllReferenceKinds);
					List references = fScanner.getReferenceListing();
					// keep potential matches
					Iterator iterator = references.iterator();
					while (iterator.hasNext()) {
						IReference ref = (IReference) iterator.next();
						for (int i = 0; i < fConditions.length; i++) {
							if (fConditions[i].isPotentialMatch(ref)) {
								fPotentialMatches[i].add(ref);
								// TODO: check other conditions for multiple matches?
								break;
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
	 * Class file scanner
	 */
	private ClassFileScanner fScanner;
		
	/**
	 * Scan status
	 */
	private MultiStatus fStatus;
		
	/**
	 * Potential matches for each search condition
	 */
	private List[] fPotentialMatches = null;
	
	/**
	 * Class file cache for resolution of virtual methods
	 */
	private LRUMap fCache = new LRUMap(300);
	private int fHits = 0;
	private int fMiss = 0;	
	
	/**
	 * Search criteria
	 */
	private IApiSearchCriteria[] fConditions = null;
	
	/**
	 * Mask of all reference kinds to consider based on all search conditions.
	 */
	private int fAllReferenceKinds = 0;
	
	/**
	 * Whether component local references need to be considered based on all conditions.
	 */
	private boolean fLocalRefs = false;
	
	/**
	 * Scans the given scope extracting all reference information.
	 * 
	 * @param scope scope to scan
	 * @param monitor progress monitor
	 * @exception CoreException if the scan fails
	 */
	private void extractReferences(IApiSearchScope scope, IProgressMonitor monitor) throws CoreException {
		fStatus = new MultiStatus(ApiPlugin.PLUGIN_ID, 0, SearchMessages.SearchEngine_1, null); 
		fScanner = ClassFileScanner.newScanner();
		String[] packageNames = scope.getPackageNames();
		SubMonitor localMonitor = SubMonitor.convert(monitor, packageNames.length);
		ClassFileContainerVisitor visitor = new Visitor(localMonitor);
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
		if (DEBUG) {
			int size = 0;
			for (int i = 0; i < fPotentialMatches.length; i++) {
				size += fPotentialMatches[i].size();
			}
			System.out.println("Search: extracted " + size + " references in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
	
	/**
	 * Creates a unique string key for a given location.
	 * The key is of the form:
	 * <pre>
	 * [component_id]#[type_name](#[member_name]#[member_signature])
	 * </pre>
	 * @param source
	 * @param target 
	 * @return a string key for the given location.
	 */
	private String createSignatureKey(ILocation source, ILocation target) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(source.getApiComponent().getId());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(target.getType().getQualifiedName());
		IMemberDescriptor member = target.getMember();
		if(!(member instanceof IReferenceTypeDescriptor)) {
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(member.getName());
			buffer.append("#"); //$NON-NLS-1$
			if (member instanceof IMethodDescriptor) {
				buffer.append(((IMethodDescriptor)member).getSignature());
			}
		}
		return buffer.toString();
	}
	
	/**
	 * Resolves all references.
	 * 
	 * @param referenceLists lists of {@link IReference} to resolve
	 * @param progress monitor
	 * @throws CoreException if something goes wrong
	 */
	private void resolveReferences(List[] referenceLists, IProgressMonitor monitor) throws CoreException {
		fHits = 0;
		fMiss = 0;
		// sort references by target type for 'shared' resolution
		Map sigtoref = new HashMap(50);
		
		List refs = null;
		IReference ref = null;
		String key = null;
		List methodDecls = new ArrayList(1000);
		long start = System.currentTimeMillis();
		for (int i = 0; i < referenceLists.length; i++) {
			Iterator references = referenceLists[i].iterator();
			while (references.hasNext()) {
				ref = (IReference) references.next();
				if (ref.getReferenceKind() == ReferenceModifiers.REF_OVERRIDE) {
					methodDecls.add(ref);
				} else {
					key = createSignatureKey(ref.getSourceLocation(), ref.getReferencedLocation());
					refs = (List) sigtoref.get(key);
					if(refs == null) {
						refs = new ArrayList(20);
						sigtoref.put(key, refs);
					}
					refs.add(ref);
				}
			}
		}
		if (monitor.isCanceled()) {
			return;
		}
		long end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Search: split into " + methodDecls.size() + " method overrides and " + sigtoref.size() + " unique references (" + (end - start) + "ms)");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		// resolve references
		start = System.currentTimeMillis();
		resolveReferenceSets(sigtoref, monitor);
		end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Search: resolved unique references in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
		// resolve method overrides
		start = System.currentTimeMillis();
		Iterator iterator = methodDecls.iterator();
		while (iterator.hasNext()) {
			Reference reference = (Reference) iterator.next();
			reference.resolve(this);
		}
		end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Search: resolved method overrides in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
			System.out.println("Search: class file method info cache hits: " + fHits + " misses: " + fMiss); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		fCache.clear();
		
	}
	
	/**
	 * Returns a class file reader for the given class file.
	 * 
	 * @param file class file
	 * @return class file reader
	 * @throws CoreException
	 */
	MethodExtractor getExtraction(IClassFile file) throws CoreException {
		MethodExtractor extractor= (MethodExtractor) fCache.get(file.getTypeName());
		if (extractor == null) {
			extractor = new MethodExtractor();
			ClassReader reader = new ClassReader(file.getContents());
			reader.accept(extractor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
			fCache.put(file.getTypeName(), extractor);
			fMiss++;
		} else {
			fHits++;
		}
		return extractor;
	}	
	
	/**
	 * Resolves the collect sets of references.
	 * @param map the mapping of keys to sets of {@link IReference}s
	 * @throws CoreException if something bad happens
	 */
	private void resolveReferenceSets(Map map, IProgressMonitor monitor) throws CoreException {
		Iterator types = map.keySet().iterator();
		String key = null;
		List refs = null;
		IReference ref= null;
		while (types.hasNext()) {
			if (monitor.isCanceled()) {
				return;
			}
			key = (String) types.next();
			refs = (List) map.get(key);
			ref = (IReference) refs.get(0);
			((Reference)ref).resolve(this);
			IApiAnnotations resolved = ref.getResolvedAnnotations();
			if (resolved != null) {
				Iterator iterator = refs.iterator();
				while (iterator.hasNext()) {
					Reference ref2 = (Reference) iterator.next();
					ref2.setResolution(resolved, ref.getResolvedLocation());
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchEngine#search(org.eclipse.pde.api.tools.search.IApiSearchScope, int[], int[], int[], org.eclipse.pde.api.tools.search.IApiSearchScope, boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IReference[] search(IApiSearchScope sourceScope,
			IApiSearchCriteria[] conditions, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor localMonitor = SubMonitor.convert(monitor,SearchMessages.SearchEngine_2, 3);
		fConditions = conditions;
		fPotentialMatches = new List[fConditions.length];
		for (int i = 0; i < conditions.length; i++) {
			IApiSearchCriteria condition = conditions[i];
			fAllReferenceKinds |= condition.getReferenceKinds();
			fLocalRefs = fLocalRefs | condition.isConsiderComponentLocalReferences();
			fPotentialMatches[i] = new LinkedList();
		}
		// 1. extract all references, filtering out kinds we don't care about
		localMonitor.subTask(SearchMessages.SearchEngine_3); 
		extractReferences(sourceScope, localMonitor);
		localMonitor.worked(1);
		if (localMonitor.isCanceled()) {
			return EMPTY_REF;
		}
		// 2. resolve the remaining references
		localMonitor.subTask(SearchMessages.SearchEngine_4);
		resolveReferences(fPotentialMatches, localMonitor);
		localMonitor.worked(1);
		if (localMonitor.isCanceled()) {
			return EMPTY_REF;
		}
		// 3. filter based on search conditions
		localMonitor.subTask(SearchMessages.SearchEngine_5);
		for (int i = 0; i < fPotentialMatches.length; i++) {
			List references = fPotentialMatches[i];
			if (!references.isEmpty()) {
				IApiSearchCriteria condition = fConditions[i];
				applyConditions(references, condition);
			}
			if (localMonitor.isCanceled()) {
				return EMPTY_REF;
			}
		}
		int size = 0;
		for (int i = 0; i < fPotentialMatches.length; i++) {
			size += fPotentialMatches[i].size();
		}
		IReference[] refs = new IReference[size];
		int index = 0;
		for (int i = 0; i < fPotentialMatches.length; i++) {
			List references = fPotentialMatches[i];
			Iterator iterator = references.iterator();
			while (iterator.hasNext()) {
				refs[index++] = (IReference) iterator.next();
			}
			references.clear();
		}
		fCache.clear();
		localMonitor.worked(1);
		localMonitor.done();
		return refs;
	}
	
	/**
	 * Iterates through the given references, removing those that do not match
	 * search conditions.
	 * 
	 * @param references
	 * @param condition condition to satisfy
	 */
	private void applyConditions(List references, IApiSearchCriteria condition) {
		Iterator iterator = references.iterator();
		while (iterator.hasNext()) {
			IReference ref = (IReference) iterator.next();
			ILocation location = ref.getResolvedLocation();
			boolean consider = true;
			if (location != null) {
				if (!fLocalRefs) {
					if (ref.getSourceLocation().getApiComponent().equals(location.getApiComponent())) {
						consider = false;
					}		
				}
			}
			boolean match = false;
			if (consider) {
				IApiAnnotations description = ref.getResolvedAnnotations();
				if (description != null) {
					if (condition.isMatch(ref)) {
						match = true;
					}
				} else {
					// TODO: unresolved (note unresolved REF_OVERRIDE's are OK)
				}
			}
			if (!match) {
				iterator.remove();
			}
		}
	}
	
	public void resolveReferences(IReference[] references, IProgressMonitor monitor) throws CoreException {
		List list = new ArrayList(references.length);
		for (int i = 0; i < references.length; i++) {
			list.add(references[i]);
		}
		resolveReferences(new List[]{list}, monitor);
	}

}
