package org.eclipse.pde.internal.codegen;

import org.eclipse.core.resources.*;
import java.io.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.jdt.core.*;
import java.io.PrintWriter;
import org.eclipse.pde.internal.PDEPlugin;

public class AttributeClassCodeGenerator extends JavaCodeGenerator {
	private ISchemaAttribute attInfo;
	private IJavaProject javaProject;
	private IType expectedType;
	private Vector requiredMethods;
	private Vector requiredImports;

public AttributeClassCodeGenerator(
	IJavaProject javaProject,
	IFolder sourceFolder,
	String fullyQualifiedClassName, 
	ISchemaAttribute attInfo) {
	super(javaProject.getProject(), sourceFolder, fullyQualifiedClassName);
	this.attInfo = attInfo;
	this.javaProject = javaProject;
	requiredImports = new Vector();
}
private void addAbstractMethod(IMethod method) throws JavaModelException {
	IMethod matchingMethod = findMatchingMethod(method);
	if (matchingMethod == null)
		requiredMethods.addElement(method);
}
private void addImports(PrintWriter writer) {
	for (int i=0; i<requiredImports.size(); i++) {
		String type = requiredImports.elementAt(i).toString();
		writer.println("import "+type+";");
	}
	writer.println();
}
private void addRequiredMethodsFor(String typeName) throws JavaModelException {
	IType type = findTypeForName(typeName);
	if (type != null)
		addRequiredMethodsFor(type);
}
private void addRequiredMethodsFor(IType type) throws JavaModelException {
	// Check the super-interfaces
	String[] interfaceNames = type.getSuperInterfaceNames();
	for (int i = 0; i < interfaceNames.length; i++) {
		addRequiredMethodsFor(interfaceNames[i]);
	}

	if (type.isClass()) {
		// Check the superclass
		String superclassName = type.getSuperclassName();
		if (!superclassName.equals("java.lang.Object"))
			addRequiredMethodsFor(superclassName);
	}

	IMethod[] methods = type.getMethods();

	for (int i = 0; i < methods.length; i++) {
		IMethod method = methods[i];
		if (type.isClass()) {
			int flags = method.getFlags();
			if (Flags.isPublic(flags) || Flags.isProtected(flags)) {
				if (Flags.isAbstract(flags)) {
					addAbstractMethod(method);
				} else {
					// check if this method is implementing
					// required abstract method
					removeImplementedMethod(method);
				}
			}
		} else
			addAbstractMethod(method);
	}
}
private String calculateReturnValue(String signature) {
	switch (signature.charAt(0)) {
		case Signature.C_BOOLEAN :
			return "false";
		case Signature.C_BYTE :
			return "0";
		case Signature.C_CHAR :
			return "0";
		case Signature.C_DOUBLE :
			return "(double)0.0";
		case Signature.C_FLOAT :
			return "(float)0.0";
		case Signature.C_INT :
			return "0";
		case Signature.C_LONG :
			return "(long)0";
		case Signature.C_SHORT :
			return "(short)0";
		case Signature.C_VOID :
			return null;
		case Signature.C_ARRAY :
		case Signature.C_RESOLVED :
		case Signature.C_UNRESOLVED :
			return "null";
		default :
			return null;
	}
}
private IMethod findMatchingMethod(IMethod method)
	throws JavaModelException {
	for (int i = 0; i < requiredMethods.size(); i++) {
		IMethod requiredMethod = (IMethod) requiredMethods.elementAt(i);
		if (requiredMethod.getElementName().equals(method.getElementName())
			&& requiredMethod.getSignature().equals(method.getSignature())) {
			return requiredMethod;
		}
	}
	return null;
}
private void findRequiredMethods() {
	String expectedTypeName = attInfo.getBasedOn();
	try {
		expectedType = findTypeForName(expectedTypeName);
		requiredMethods = new Vector();
		addRequiredMethodsFor(expectedType);
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}
private IType findTypeForName(String typeName) throws JavaModelException {
	IType type = null;
	String fileName = typeName.replace('.', '/') + ".java";

	IJavaElement element = javaProject.findElement(new Path(fileName));
	if (element == null)
		return null;
	if (element instanceof IClassFile) {
		type = ((IClassFile) element).getType();
	} else
		if (element instanceof ICompilationUnit) {
			IType[] types = ((ICompilationUnit) element).getTypes();
			type = types[0];
		}
	return type;
}
public void generateContents(
	String packageName,
	String className,
	PrintWriter writer) {
	try {
		findRequiredMethods();
		String methodsBuffer = null;
		if (requiredMethods != null) {
			methodsBuffer = generateMethods();
		}
		String extending = expectedType.isInterface() ? " implements " : " extends ";

		requiredImports.addElement(expectedType.getFullyQualifiedName());

		writer.println("package " + packageName + ";");
		writer.println();
		addImports(writer);
		writer.println("/**");
		writer.println(" * Insert the type's description here.");
		writer.println(" * @see " + expectedType.getElementName());
		writer.println(" */");
		writer.println(
			"public class "
				+ className
				+ extending
				+ getSimpleName(expectedType.getFullyQualifiedName())
				+ " {");
		writer.println("  /**");
		writer.println("   * The constructor.");
		writer.println("   */");
		writer.println("   public " + className + "() {");
		writer.println("   }");
		if (methodsBuffer != null) {
			writer.println();
			writer.print(methodsBuffer);
		}

		writer.println("}");
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}
private String generateMethods() throws JavaModelException {
	ByteArrayOutputStream bstream = new ByteArrayOutputStream();
	PrintWriter writer = new PrintWriter(bstream, true);
	if (requiredMethods != null) {
		for (int i = 0; i < requiredMethods.size(); i++) {
			if (i>0) writer.println();
			IMethod method = (IMethod) requiredMethods.elementAt(i);
			generateRequiredMethod(method, writer);
		}
	}
	writer.close();
	return bstream.toString();
}
private void generateRequiredMethod(IMethod method, PrintWriter writer)
	throws JavaModelException {
	int flags = method.getFlags();
	boolean isProtected = Flags.isProtected(flags);
	String access = isProtected ? "protected" : "public";
	String returnType = parseSignature(method.getReturnType());
	writer.println("  /**");
	writer.println("   * Insert the method's description here.");
	writer.println(
		"   * @see "
			+ getSimpleName(expectedType.getElementName())
			+ "#"
			+ method.getElementName());
	writer.println("   */");
	writer.print(
		"   " + access + " " + returnType + " " + method.getElementName() + "("); 
	String[] parameterNames = method.getParameterNames();
	String[] parameterTypes = method.getParameterTypes();
	for (int i = 0; i < method.getNumberOfParameters(); i++) {
		if (i > 0)
			writer.print(", ");
		writer.print(parseSignature(parameterTypes[i]));
		writer.print(" " + parameterNames[i]);
	}

	writer.print(") ");
	String[] exceptionTypes = method.getExceptionTypes();
	for (int i = 0; i < exceptionTypes.length; i++) {
		if (i == 0)
			writer.print("throws ");
		else
			writer.print(", ");
		writer.print(parseSignature(exceptionTypes[i]));
	}
	writer.println(" {");
	String returnValue = calculateReturnValue(method.getReturnType());
	if (returnValue != null)
		writer.println("      return " + returnValue + ";");
	writer.println("   }");
}
private String getSimpleName(String fullyQualifiedName) {
	int dot = fullyQualifiedName.lastIndexOf('.');
	if (dot != -1)
		return fullyQualifiedName.substring(dot + 1);
	else
		return fullyQualifiedName;
}
private String parseSignature(String signature) {
	int dimensions = 0;
	StringBuffer buffer = new StringBuffer();
	int nameLoc = 0;
	boolean inTypeName = false;

	for (int i = 0; i < signature.length(); i++) {
		char c = signature.charAt(i);
		if (inTypeName) {
			if (c == Signature.C_NAME_END) {
				String typeName = signature.substring(nameLoc, i);
				String shortTypeName = getSimpleName(typeName);
				if (shortTypeName.length() < typeName.length()) {
					// Ditching package prefix - must add import
					if (!requiredImports.contains(typeName))
						requiredImports.addElement(typeName);
				}
				buffer.append(shortTypeName);
				inTypeName = false;
			}
			continue;
		}
		switch (c) {
			case Signature.C_BOOLEAN :
				buffer.append("boolean");
				break;
			case Signature.C_BYTE :
				buffer.append("byte");
				break;
			case Signature.C_CHAR :
				buffer.append("char");
				break;
			case Signature.C_DOUBLE :
				buffer.append("double");
				break;
			case Signature.C_FLOAT :
				buffer.append("float");
				break;
			case Signature.C_INT :
				buffer.append("int");
				break;
			case Signature.C_LONG :
				buffer.append("long");
				break;
			case Signature.C_SHORT :
				buffer.append("short");
				break;
			case Signature.C_VOID :
				buffer.append("void");
				break;
			case Signature.C_ARRAY :
				dimensions++;
				break;
			case Signature.C_RESOLVED :
				nameLoc = i + 1;
				inTypeName = true;
				break;
			case Signature.C_UNRESOLVED :
				nameLoc = i + 1;
				inTypeName = true;
				break;
		}
	}
	for (int i = 0; i < dimensions; i++) {
		if (i == 0)
			buffer.append(" ");
		buffer.append("[]");
	}
	return buffer.toString();
}
private void removeImplementedMethod(IMethod method)
	throws JavaModelException {
	IMethod matchingMethod = findMatchingMethod(method);
	if (matchingMethod != null)
		requiredMethods.remove(matchingMethod);
}
}
