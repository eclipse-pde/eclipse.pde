/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.ApiElement;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * A search scope containing only types from one component. More efficient than
 * a general purpose scope.
 *
 * @since 1.0
 */
public class TypeScope extends ApiElement implements IApiTypeContainer {

	/**
	 * Associated component
	 */
	private IApiComponent fComponent;

	/**
	 * Map of package names to associated type descriptors
	 */
	private Map<String, Set<IReferenceTypeDescriptor>> fPackageToTypes;

	/**
	 * Constructs a new class file container/search scope on the given types.
	 *
	 * @param component API component
	 * @param types types within the component
	 */
	public TypeScope(IApiComponent component, IReferenceTypeDescriptor[] types) {
		super(component, IApiElement.API_TYPE_CONTAINER, null);
		fComponent = component;
		fPackageToTypes = new HashMap<>();
		for (IReferenceTypeDescriptor type : types) {
			String name = type.getPackage().getName();
			Set<IReferenceTypeDescriptor> set = fPackageToTypes.get(name);
			if (set == null) {
				set = new HashSet<>();
				fPackageToTypes.put(name, set);
			}
			set.add(type);
		}
	}

	@Override
	public String[] getPackageNames() throws CoreException {
		Set<String> pkgs = fPackageToTypes.keySet();
		String[] result = new String[pkgs.size()];
		pkgs.toArray(result);
		Arrays.sort(result);
		return result;
	}

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(fComponent)) {
			for (Entry<String, Set<IReferenceTypeDescriptor>> entry : fPackageToTypes.entrySet()) {
				String pkg = entry.getKey();
				if (visitor.visitPackage(pkg)) {
					for (IReferenceTypeDescriptor type : entry.getValue()) {
						IApiTypeRoot classFile = fComponent.findTypeRoot(type.getQualifiedName());
						if (classFile != null) {
							visitor.visit(pkg, classFile);
							visitor.end(pkg, classFile);
						}
					}
				}
				visitor.endVisitPackage(pkg);
			}
		}
		visitor.end(fComponent);
	}

	@Override
	public void close() throws CoreException {
		//
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		IReferenceTypeDescriptor descriptor = Factory.typeDescriptor(qualifiedName);
		Set<IReferenceTypeDescriptor> types = fPackageToTypes.get(descriptor.getPackage().getName());
		if (types != null && types.contains(descriptor)) {
			return fComponent.findTypeRoot(qualifiedName);
		}
		return null;
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		if (fComponent.getSymbolicName().equals(id)) {
			return findTypeRoot(qualifiedName);
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("*** Type Search Scope ***\n"); //$NON-NLS-1$
		buffer.append("Component: ").append(fComponent); //$NON-NLS-1$
		if (fPackageToTypes != null) {
			for (Entry<String, Set<IReferenceTypeDescriptor>> entry : fPackageToTypes.entrySet()) {
				buffer.append("Package: ").append(entry.getKey()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("Types: ").append(entry.getValue().toString()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return buffer.toString();
	}

	@Override
	public int getContainerType() {
		return IApiTypeContainer.COMPONENT;
	}
}