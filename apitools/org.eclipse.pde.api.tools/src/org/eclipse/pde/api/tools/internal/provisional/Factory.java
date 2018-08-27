/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional;

import java.text.MessageFormat;
import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.builder.TypeScope;
import org.eclipse.pde.api.tools.internal.descriptors.ComponentDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.PackageDescriptorImpl;
import org.eclipse.pde.api.tools.internal.model.CompositeApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor;
import org.eclipse.pde.api.tools.internal.search.ReferenceDescriptor;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Factory to create API model objects.
 *
 * @since 1.0
 */
public class Factory {

	/**
	 * Returns a component descriptor for the {@link IApiComponent} with the
	 * given id and an undefined version. The given id does not have to be the
	 * id of a component that actually exists: no resolution or lookup of any
	 * kind is done with the descriptor.
	 *
	 * @param componentid
	 * @return a new component descriptor
	 */
	public static IComponentDescriptor componentDescriptor(String componentid) {
		return new ComponentDescriptorImpl(componentid, null);
	}

	/**
	 * Returns a component descriptor for the {@link IApiComponent} with the
	 * given id and version. The given id does not have to be the id of a
	 * component that actually exists: no resolution or lookup of any kind is
	 * done with the descriptor.
	 *
	 * @param componentid
	 * @param version version descriptor or <code>null</code> if none
	 * @return a new component descriptor
	 */
	public static IComponentDescriptor componentDescriptor(String componentid, String version) {
		return new ComponentDescriptorImpl(componentid, version);
	}

	/**
	 * Returns a package descriptor for the package with the given name. An
	 * empty string indicates the default package. Package names are dot
	 * qualified.
	 *
	 * @param packageName package name
	 * @return an {@link IPackageDescriptor} for the package
	 */
	public static IPackageDescriptor packageDescriptor(String packageName) {
		return new PackageDescriptorImpl(packageName);
	}

	/**
	 * Utility method to create a type descriptor for a type with the given
	 * fully qualified name. Package names are dot qualified and type names are
	 * '$'-qualified.
	 *
	 * @param fullyQualifiedName
	 * @return an {@link ITypeDescriptor} for the type
	 */
	public static IReferenceTypeDescriptor typeDescriptor(String fullyQualifiedName) {
		String packageName = Signatures.getPackageName(fullyQualifiedName);
		String typeName = Signatures.getTypeName(fullyQualifiedName);
		return packageDescriptor(packageName).getType(typeName);
	}

	/**
	 * Utility method to create a type descriptor for a method contained within
	 * the given type
	 *
	 * @param typename the name of the enclosing type for the method
	 * @param name the name of the method
	 * @param signaturethe signature of the method
	 * @return an {@link IMethodDescriptor} for the method
	 */
	public static IMethodDescriptor methodDescriptor(String typename, String name, String signature) {
		IReferenceTypeDescriptor type = typeDescriptor(typename);
		return type.getMethod(name, signature);
	}

	/**
	 * Utility method to create a type descriptor for a field contained within
	 * the given type
	 *
	 * @param typename the name of the enclosing type for the field
	 * @param name the name of the field
	 * @return an {@link IFieldDescriptor} for the field
	 */
	public static IFieldDescriptor fieldDescriptor(String typename, String name) {
		IReferenceTypeDescriptor type = typeDescriptor(typename);
		return type.getField(name);
	}

	/**
	 * Creates a new {@link IReferenceDescriptor} object
	 *
	 * @param origincomponent the component where the reference comes from
	 * @param originmember the member where the reference comes from
	 * @param line the line number of the reference or -1 if unknown
	 * @param targetcomponent the component the reference is to
	 * @param targetmember the member the reference is to
	 * @param kind the kind of the reference. See
	 *            {@link org.eclipse.pde.api.tools.internal.provisional.builder.IReference}
	 *            for a complete list of kinds
	 * @param flags the flags of the reference. See
	 *            {@link org.eclipse.pde.api.tools.internal.provisional.builder.IReference}
	 *            for a complete list of flags
	 * @param visibility the visibility of the reference. See
	 *            {@link VisibilityModifiers} for a complete list of
	 *            visibilities
	 * @param messages a listing of {@link IApiProblem} messages associated with
	 *            this reference descriptor
	 * @return a new {@link IReferenceDescriptor}
	 * @since 1.1
	 */
	public static IReferenceDescriptor referenceDescriptor(IComponentDescriptor origincomponent, IMemberDescriptor originmember, int line, IComponentDescriptor targetcomponent, IMemberDescriptor targetmember, int kind, int flags, int visibility, String[] messages) {
		return new ReferenceDescriptor(origincomponent, originmember, line, targetcomponent, targetmember, kind, flags, visibility, messages);
	}

	/**
	 * Returns a scope containing all elements in the given components.
	 *
	 * @param components API components
	 * @return search scope
	 * @throws CoreException if the baseline of the given components is disposed
	 */
	public static IApiTypeContainer newScope(IApiComponent[] components) throws CoreException {
		LinkedList<IApiTypeContainer> compList = new LinkedList<>();
		for (IApiComponent component : components) {
			compList.add(component);
		}
		CompositeApiTypeContainer scope = new CompositeApiTypeContainer(components[0].getBaseline(), compList);
		return scope;
	}

	/**
	 * Returns a new scope containing the specified types in the given
	 * component.
	 *
	 * @param component API component
	 * @param types reference types
	 * @return search scope
	 */
	public static IApiTypeContainer newTypeScope(IApiComponent component, IReferenceTypeDescriptor[] types) {
		return new TypeScope(component, types);
	}

	/**
	 * Returns a method descriptor with a resolved signature for the given
	 * method descriptor with an unresolved signature.
	 *
	 * @param descriptor method to resolve
	 * @return resolved method descriptor or the same method descriptor if
	 *         unable to resolve
	 * @exception CoreException if unable to resolve the method and a class file
	 *                container was provided for this purpose
	 */
	public static IMethodDescriptor resolveMethod(IApiTypeContainer container, IMethodDescriptor descriptor) throws CoreException {
		if (container != null) {
			IReferenceTypeDescriptor type = descriptor.getEnclosingType();
			IApiTypeRoot classFile = container.findTypeRoot(type.getQualifiedName());
			if (classFile != null) {
				IApiType structure = classFile.getStructure();
				if (structure != null) {
					IApiMethod[] methods = structure.getMethods();
					for (IApiMethod method : methods) {
						if (descriptor.getName().equals(method.getName())) {
							String signature = method.getSignature();
							String descriptorSignature = descriptor.getSignature().replace('/', '.');
							if (Signatures.matchesSignatures(descriptorSignature, signature.replace('/', '.'))) {
								return descriptor.getEnclosingType().getMethod(method.getName(), signature);
							}
							String genericSignature = method.getGenericSignature();
							if (genericSignature != null) {
								if (Signatures.matchesSignatures(descriptorSignature, genericSignature.replace('/', '.'))) {
									return descriptor.getEnclosingType().getMethod(method.getName(), signature);
								}
							}
						}
					}
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, MessageFormat.format("Unable to resolve method signature: {0}", descriptor.toString()), null)); //$NON-NLS-1$
		}
		return descriptor;
	}
}
