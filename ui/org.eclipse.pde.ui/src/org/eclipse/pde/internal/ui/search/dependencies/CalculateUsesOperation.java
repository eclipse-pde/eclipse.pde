/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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

	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		try {
			Collection packages = getPublicExportedPackages();
			if (packages.isEmpty())
				return;
			Map pkgsAndUses = findPackageReferences(packages, monitor);
			if (monitor.isCanceled()) {
				return;
			}
			handleSetUsesDirectives(pkgsAndUses);
		} finally {
			monitor.done();
		}
	}

	protected Collection getPublicExportedPackages() {
		IBundle bundle = fModel.getBundleModel().getBundle();
		IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header == null)
			return Collections.EMPTY_SET;

		ArrayList list = new ArrayList();
		ExportPackageObject[] pkgs = ((ExportPackageHeader) header).getPackages();
		for (int i = 0; i < pkgs.length; i++) {
			// don't calculate uses directive on private packages
			if (!pkgs[i].isInternal())
				list.add(pkgs[i].getName());
		}
		return list;
	}

	protected Map findPackageReferences(Collection packages, IProgressMonitor monitor) {
		IJavaProject jp = JavaCore.create(fProject);
		HashMap pkgsAndUses = new HashMap();
		IPackageFragment[] frags = PDEJavaHelper.getPackageFragments(jp, Collections.EMPTY_SET, false);
		monitor.beginTask("", frags.length * 2); //$NON-NLS-1$
		for (int i = 0; i < frags.length; i++) {
			if (monitor.isCanceled()) {
				return pkgsAndUses;
			}
			monitor.subTask(NLS.bind(PDEUIMessages.CalculateUsesOperation_calculatingDirective, frags[i].getElementName()));
			if (packages.contains(frags[i].getElementName())) {
				HashSet pkgs = new HashSet();
				pkgsAndUses.put(frags[i].getElementName(), pkgs);
				try {
					findReferences(frags[i].getCompilationUnits(), pkgs, new SubProgressMonitor(monitor, 1), false);
					findReferences(frags[i].getClassFiles(), pkgs, new SubProgressMonitor(monitor, 1), true);
				} catch (JavaModelException e) {
				}
			} else
				monitor.worked(2);
		}
		return pkgsAndUses;
	}

	protected void findReferences(ITypeRoot[] roots, Set pkgs, IProgressMonitor monitor, boolean binary) throws JavaModelException {
		monitor.beginTask("", roots.length); //$NON-NLS-1$
		for (int i = 0; i < roots.length; i++) {
			findReferences(roots[i].findPrimaryType(), pkgs, binary);
			monitor.worked(1);
		}
	}

	protected void findReferences(IType type, Set pkgs, boolean binary) throws JavaModelException {
		if (type == null)
			return;
		// ignore private classes
		if (Flags.isPrivate(type.getFlags()))
			return;

		IMethod[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (!Flags.isPrivate(methods[i].getFlags())) {
				String methodSignature = methods[i].getSignature();
				addPackages(Signature.getThrownExceptionTypes(methodSignature), pkgs, type, binary);
				addPackages(Signature.getParameterTypes(methodSignature), pkgs, type, binary);
				addPackage(Signature.getReturnType(methodSignature), pkgs, type, binary);
			}
		}
		IField[] fields = type.getFields();
		for (int i = 0; i < fields.length; i++)
			if (!Flags.isPrivate(fields[i].getFlags()))
				addPackage(fields[i].getTypeSignature(), pkgs, type, binary);
		addPackage(type.getSuperclassTypeSignature(), pkgs, type, binary);
		addPackages(type.getSuperInterfaceTypeSignatures(), pkgs, type, binary);

		// make sure to check sub classes defined in the class
		IType[] subTypes = type.getTypes();
		for (int i = 0; i < subTypes.length; i++) {
			findReferences(subTypes[i], pkgs, binary);
		}
	}

	protected final void addPackage(String typeSignature, Set pkgs, IType type, boolean binary) throws JavaModelException {
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

	protected final void addPackages(String[] typeSignatures, Set pkgs, IType type, boolean binary) throws JavaModelException {
		for (int i = 0; i < typeSignatures.length; i++)
			addPackage(typeSignatures[i], pkgs, type, binary);
	}

	protected void handleSetUsesDirectives(Map pkgsAndUses) {
		if (pkgsAndUses.isEmpty())
			return;
		setUsesDirectives(pkgsAndUses);
	}

	protected void setUsesDirectives(Map pkgsAndUses) {
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

	protected String getDirectiveValue(String pkgName, Map pkgsAndUses) {
		Set usesPkgs = (Set) pkgsAndUses.get(pkgName);
		usesPkgs.remove(pkgName);
		StringBuffer buffer = null;
		Iterator it = usesPkgs.iterator();
		while (it.hasNext()) {
			String usedPkgName = (String) it.next();
			if (usedPkgName.startsWith("java.")) { //$NON-NLS-1$
				// we should not include java.* packages (bug 167968)
				it.remove();
				continue;
			}
			if (buffer == null)
				buffer = new StringBuffer();
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
