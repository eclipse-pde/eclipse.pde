/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	 * Constructor Private constructor, no instantiate
	 */
	private ReferenceResolver() {
	}

	/**
	 * Resolves retained references.
	 *
	 * @param references list of {@link IReference} to resolve
	 * @param progress monitor
	 * @throws CoreException if something goes wrong
	 */
	public static void resolveReferences(List<IReference> references, IProgressMonitor monitor) throws CoreException {
		// sort references by target type for 'shared' resolution
		int refcount = references.size();
		Map<String, List<IReference>> sigtoref = new LinkedHashMap<>(refcount);

		List<IReference> refs = null;
		String key = null;
		List<Reference> methodDecls = new ArrayList<>(refcount);
		long start = System.currentTimeMillis();
		for (IReference ref : references) {
			if (ref.getReferenceKind() == IReference.REF_OVERRIDE) {
				methodDecls.add((Reference) ref);
			} else {
				key = createSignatureKey(ref);
				refs = sigtoref.get(key);
				if (refs == null) {
					refs = new ArrayList<>(20);
					sigtoref.put(key, refs);
				}
				refs.add(ref);
			}
		}

		long end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: split into " + methodDecls.size() + " method overrides and " + sigtoref.size() + " unique references (" + (end - start) + "ms)"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		// resolve references
		start = System.currentTimeMillis();
		resolveReferenceSets(sigtoref, monitor);
		end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: resolved unique references in " + (end - start) + "ms"); //$NON-NLS-1$//$NON-NLS-2$
		}
		// resolve method overrides
		start = System.currentTimeMillis();
		for (Reference reference : methodDecls) {
			reference.resolve();
		}
		end = System.currentTimeMillis();
		if (ApiPlugin.DEBUG_REFERENCE_RESOLVER) {
			System.out.println("Reference resolver: resolved method overrides in " + (end - start) + "ms"); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/**
	 * Resolves the collect sets of references.
	 *
	 * @param map the mapping of keys to sets of {@link IReference}s
	 * @throws CoreException if something bad happens
	 */
	private static void resolveReferenceSets(Map<String, List<IReference>> map, IProgressMonitor monitor) throws CoreException {
		IReference ref = null;
		for (List<IReference> refs : map.values()) {
			ref = refs.get(0);
			((Reference) ref).resolve();
			IApiMember resolved = ref.getResolvedReference();
			if (resolved != null) {
				for (IReference ref2 : refs) {
					((Reference) ref2).setResolution(resolved);
				}
			}
		}
	}

	/**
	 * Creates a unique string key for a given reference. The key is of the form
	 * "component X references type/member"
	 *
	 * <pre>
	 * [component_id]#[type_name](#[member_name]#[member_signature])
	 * </pre>
	 *
	 * @param reference reference
	 * @return a string key for the given reference.
	 */
	private static String createSignatureKey(IReference reference) {
		StringBuilder buffer = new StringBuilder();
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
			default:
				break;
		}
		return buffer.toString();
	}
}
