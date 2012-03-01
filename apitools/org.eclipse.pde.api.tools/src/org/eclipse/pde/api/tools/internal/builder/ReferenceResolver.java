/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;

/**
 * Utility class used to resolve {@link IReference}s
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ReferenceResolver {

	/**
	 * Constructor
	 * Private constructor, no instantiate
	 */
	private ReferenceResolver() {}
	
	/**
	 * Resolves retained references.
	 * 
	 * @param references list of {@link IReference} to resolve
	 * @param progress monitor
	 * @throws CoreException if something goes wrong
	 */
	public static void resolveReferences(List/*<IReference>*/ references, IProgressMonitor monitor) throws CoreException {
		// sort references by target type for 'shared' resolution
		int refcount = references.size();
		Map sigtoref = new HashMap(refcount);
		
		List refs = null;
		IReference ref = null;
		String key = null;
		List methodDecls = new ArrayList(refcount);
		long start = System.currentTimeMillis();
		Iterator iterator = references.iterator();
		while (iterator.hasNext()) {
			ref = (IReference) iterator.next();
			if (ref.getReferenceKind() == IReference.REF_OVERRIDE) {
				methodDecls.add(ref);
			} else {
				key = createSignatureKey(ref);
				refs = (List) sigtoref.get(key);
				if(refs == null) {
					refs = new ArrayList(20);
					sigtoref.put(key, refs);
				}
				refs.add(ref);
			}
		}
		if (monitor.isCanceled()) {
			return;
		}
		long end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: split into " + methodDecls.size() + " method overrides and " + sigtoref.size() + " unique references (" + (end - start) + "ms)");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		// resolve references
		start = System.currentTimeMillis();
		resolveReferenceSets(sigtoref, monitor);
		end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: resolved unique references in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
		// resolve method overrides
		start = System.currentTimeMillis();
		iterator = methodDecls.iterator();
		while (iterator.hasNext()) {
			Reference reference = (Reference) iterator.next();
			reference.resolve();
		}
		end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: resolved method overrides in " + (end - start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Resolves the collect sets of references.
	 * @param map the mapping of keys to sets of {@link IReference}s
	 * @throws CoreException if something bad happens
	 */
	private static void resolveReferenceSets(Map map, IProgressMonitor monitor) throws CoreException {
		Iterator iterator = map.values().iterator();
		List refs = null;
		IReference ref= null;
		while (iterator.hasNext()) {
			if (monitor.isCanceled()) {
				return;
			}
			refs = (List) iterator.next();
			ref = (IReference) refs.get(0);
			((Reference)ref).resolve();
			IApiMember resolved = ref.getResolvedReference();
			if (resolved != null) {
				Iterator iterator2 = refs.iterator();
				while (iterator2.hasNext()) {
					Reference ref2 = (Reference) iterator2.next();
					ref2.setResolution(resolved);
				}
			}
		}
	}
	
	/**
	 * Creates a unique string key for a given reference.
	 * The key is of the form "component X references type/member"
	 * <pre>
	 * [component_id]#[type_name](#[member_name]#[member_signature])
	 * </pre>
	 * @param reference reference
	 * @return a string key for the given reference.
	 */
	private static String createSignatureKey(IReference reference) throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(reference.getMember().getApiComponent().getSymbolicName());
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(reference.getReferencedTypeName());
		switch (reference.getReferenceType()) {
		case IReference.T_FIELD_REFERENCE:
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedMemberName());
			break;
		case IReference.T_METHOD_REFERENCE:
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedMemberName());
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(reference.getReferencedSignature());
			break;
		}
		return buffer.toString();
	}	
}
