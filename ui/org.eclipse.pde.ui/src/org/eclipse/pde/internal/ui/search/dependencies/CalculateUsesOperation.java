/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class CalculateUsesOperation extends WorkspaceModifyOperation {

	private IProject fProject;
	private IBundlePluginModelBase fModel;

	public CalculateUsesOperation(IProject project, IBundlePluginModelBase model) {
		fProject = project;
		fModel = model;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		try {
			Collection<String> packages = getPublicExportedPackages();
			if (packages.isEmpty())
				return;
			Map<String, HashSet<String>> pkgsAndUses = findPackageReferences(packages, monitor);
			if (monitor.isCanceled()) {
				return;
			}
			handleSetUsesDirectives(pkgsAndUses);
		} finally {
			monitor.done();
		}
	}

	protected Collection<String> getPublicExportedPackages() {
		IBundle bundle = fModel.getBundleModel().getBundle();
		IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header == null)
			return Collections.emptySet();

		ArrayList<String> list = new ArrayList<>();
		ExportPackageObject[] pkgs = ((ExportPackageHeader) header).getPackages();
		for (int i = 0; i < pkgs.length; i++) {
			// don't calculate uses directive on private packages
			if (!pkgs[i].isInternal())
				list.add(pkgs[i].getName());
		}
		return list;
	}

	protected Map<String, HashSet<String>> findPackageReferences(Collection<String> packages, IProgressMonitor monitor) {
		IJavaProject jp = JavaCore.create(fProject);
		HashMap<String, HashSet<String>> pkgsAndUses = new HashMap<>();
		IPackageFragment[] frags = PDEJavaHelper.getPackageFragments(jp, Collections.EMPTY_SET, false);
		SubMonitor subMonitor = SubMonitor.convert(monitor, frags.length * 2);
		for (IPackageFragment fragment : frags) {
			SubMonitor iterationMonitor = subMonitor.split(2);
			if (iterationMonitor.isCanceled()) {
				return pkgsAndUses;
			}
			iterationMonitor.subTask(
					NLS.bind(PDEUIMessages.CalculateUsesOperation_calculatingDirective, fragment.getElementName()));
			if (packages.contains(fragment.getElementName())) {
				HashSet<String> pkgs = new HashSet<>();
				pkgsAndUses.put(fragment.getElementName(), pkgs);
				try {
					findReferences(fragment.getCompilationUnits(), pkgs, iterationMonitor.split(1), false);
					findReferences(fragment.getClassFiles(), pkgs, iterationMonitor.split(1), true);
				} catch (JavaModelException e) {
				}
			}
		}
		return pkgsAndUses;
	}

	protected void findReferences(ITypeRoot[] roots, Set<String> pkgs, IProgressMonitor monitor, boolean binary) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, roots.length);
		for (ITypeRoot root : roots) {
			findReferences(root.findPrimaryType(), pkgs, binary, subMonitor.split(1));
		}
	}

	protected void findReferences(IType type, Set<String> pkgs, boolean binary, IProgressMonitor monitor)
			throws JavaModelException {
		if (type == null)
			return;
		// ignore private classes
		if (Flags.isPrivate(type.getFlags()))
			return;

		IMethod[] methods = type.getMethods();
		IField[] fields = type.getFields();
		IType[] subTypes = type.getTypes();
		SubMonitor subMonitor = SubMonitor.convert(monitor, methods.length * 3 + fields.length + 2 + subTypes.length);

		for (int i = 0; i < methods.length; i++) {
			if (!Flags.isPrivate(methods[i].getFlags())) {
				String methodSignature = methods[i].getSignature();
				addPackages(Signature.getThrownExceptionTypes(methodSignature), pkgs, type, binary,
						subMonitor.split(1));
				addPackages(Signature.getParameterTypes(methodSignature), pkgs, type, binary, subMonitor.split(1));
				addPackage(Signature.getReturnType(methodSignature), pkgs, type, binary, subMonitor.split(1));
			}
		}
		for (int i = 0; i < fields.length; i++) {
			if (!Flags.isPrivate(fields[i].getFlags()))
				addPackage(fields[i].getTypeSignature(), pkgs, type, binary, subMonitor.split(1));
		}
		addPackage(type.getSuperclassTypeSignature(), pkgs, type, binary, subMonitor.split(1));
		addPackages(type.getSuperInterfaceTypeSignatures(), pkgs, type, binary, subMonitor.split(1));

		// make sure to check sub classes defined in the class
		for (IType subType : subTypes) {
			findReferences(subType, pkgs, binary, subMonitor.split(1));
		}
	}

	protected final void addPackage(String typeSignature, Set<String> pkgs, IType type, boolean binary,
			IProgressMonitor monitor) throws JavaModelException {
		if (typeSignature == null)
			return;
		if (binary)
			typeSignature = typeSignature.replace('/', '.');
		// if typeSignature contains a '.', test to see if it is a subClass first.  If not, assume it is a fully qualified name
		if (typeSignature.indexOf('.') != -1) {
			try {
				String[][] temp = type.resolveType(new String(Signature.toCharArray(typeSignature.toCharArray())));
				if (temp != null) {
					pkgs.add(temp[0][0]);
					return;
				}
			} catch (IllegalArgumentException e) {
			}
			String pkg = Signature.getSignatureQualifier(typeSignature);
			if (pkg.length() > 0) {
				pkgs.add(pkg);
				return;
			}
			// if typeSignature does not contain a '.', then assume the package name is in an import statement and try to resolve through the type object
		} else {
			String typeName = Signature.getSignatureSimpleName(typeSignature);
			String[][] result = type.resolveType(typeName);
			if (result != null)
				pkgs.add(result[0][0]);
		}
	}

	protected final void addPackages(String[] typeSignatures, Set<String> pkgs, IType type, boolean binary,
			IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, typeSignatures.length);
		for (String typeSignature : typeSignatures)
			addPackage(typeSignature, pkgs, type, binary, subMonitor.split(1));
	}

	protected void handleSetUsesDirectives(Map<String, HashSet<String>> pkgsAndUses) {
		if (pkgsAndUses.isEmpty())
			return;
		setUsesDirectives(pkgsAndUses);
	}

	protected void setUsesDirectives(Map<String, HashSet<String>> pkgsAndUses) {
		IBundle bundle = fModel.getBundleModel().getBundle();
		IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		// header will not equal null b/c we would not get this far (ie. no exported packages so we would have returned earlier
		ExportPackageObject[] pkgs = ((ExportPackageHeader) header).getPackages();
		for (int i = 0; i < pkgs.length; i++) {
			if (!pkgsAndUses.containsKey(pkgs[i].getName()))
				continue;
			String value = getDirectiveValue(pkgs[i].getName(), pkgsAndUses);
			pkgs[i].setUsesDirective(value);
		}
	}

	protected String getDirectiveValue(String pkgName, Map<String, HashSet<String>> pkgsAndUses) {
		Set<String> usesPkgs = pkgsAndUses.get(pkgName);
		usesPkgs.remove(pkgName);
		StringBuilder buffer = null;
		Iterator<?> it = usesPkgs.iterator();
		while (it.hasNext()) {
			String usedPkgName = (String) it.next();
			if (usedPkgName.startsWith("java.")) { //$NON-NLS-1$
				// we should not include java.* packages (bug 167968)
				it.remove();
				continue;
			}
			if (buffer == null)
				buffer = new StringBuilder();
			else
				buffer.append(',');
			buffer.append(usedPkgName);
			it.remove();
		}
		if (usesPkgs.isEmpty())
			pkgsAndUses.remove(pkgName);
		return (buffer == null) ? null : buffer.toString();
	}

}
