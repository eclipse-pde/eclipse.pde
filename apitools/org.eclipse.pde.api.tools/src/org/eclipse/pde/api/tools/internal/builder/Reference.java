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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor;
import org.eclipse.pde.api.tools.internal.search.UseReportConverter;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Implementation of a reference from one member to another.
 * 
 * @since 1.0.0
 */
public class Reference implements IReference {
	
	/**
	 * Line number where the reference occurred.
	 */
	private int fSourceLine = -1;
	
	/**
	 * Member where the reference occurred.
	 */
	private IApiMember fSourceMember;
	
	/**
	 * One of the valid {@link org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers}
	 */
	private int fKind;
	
	/**
	 * Flags for the reference
	 */
	private int fFlags = 0;
	
	/**
	 * One of the valid type, method, field.
	 */
	private int fType;
	
	/**
	 * Name of the referenced type
	 */
	private String fTypeName;
	
	/**
	 * Name of the referenced member or <code>null</code>
	 */
	private String fMemberName;
	
	/**
	 * Signature of the referenced method or <code>null</code>
	 */
	private String fSignature;
	
	/**
	 * Resolved reference or <code>null</code>
	 */
	private IApiMember fResolved;
	
	/**
	 * Resolvable status
	 */
	private boolean fStatus = true;
	
	/**
	 * List of problems that have been reported against this problem
	 */
	private List fProblems = null;
	
	/**
	 * Adds the given collection of {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}s
	 * to the backing listing.
	 * 
	 * @param problems the list of problems to add - <code>null</code> is not accepted.
	 * @return <code>true</code> if the problems were all added, <code>false</code> otherwise
	 * @since 1.1
	 */
	public boolean addProblems(IApiProblem problem) {
		if(problem == null) {
			return false;
		}
		if(fProblems == null) {
			fProblems = new ArrayList(2);
		}
		if(fProblems.contains(problem)) {
			return false;
		}
		return fProblems.add(problem);
	}
	
	/**
	 * Returns the complete listing of {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}s
	 * recorded for this reference or <code>null</code> if none have been reported.
	 * 
	 * @return the listing of {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}s or <code>null</code>
	 * @since 1.1
	 */
	public List getProblems() {
		return fProblems;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getLineNumber()
	 */
	public int getLineNumber() {
		return fSourceLine;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getMember()
	 */
	public IApiMember getMember() {
		return fSourceMember;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferenceKind()
	 */
	public int getReferenceKind() {
		return fKind;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IReference#getReferenceFlags()
	 */
	public int getReferenceFlags() {
		return fFlags;
	}
	
	/**
	 * OR's the given set of new flags with the current set of flags
	 * @param newflags
	 */
	public void setFlags(int newflags) {
		fFlags |= newflags;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferenceType()
	 */
	public int getReferenceType() {
		return fType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferencedMember()
	 */
	public IApiMember getResolvedReference() {
		return fResolved;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferencedMemberName()
	 */
	public String getReferencedMemberName() {
		return fMemberName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferencedSignature()
	 */
	public String getReferencedSignature() {
		return fSignature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IReference#getReferencedTypeName()
	 */
	public String getReferencedTypeName() {
		return fTypeName;
	}

	/**
	 * Creates and returns a method reference.
	 * 
	 * @param origin where the reference occurred from
	 * @param typeName name of the referenced type where virtual method lookup begins
	 * @param methodName name of the referenced method
	 * @param signature signature of the referenced method
	 * @param kind kind of method reference
	 */
	public static Reference methodReference(IApiMember origin, String typeName, String methodName, String signature, int kind) {
		Reference ref = new Reference();
		ref.fSourceMember = origin;
		ref.fTypeName = typeName;
		ref.fMemberName = methodName;
		ref.fSignature = signature;
		ref.fKind = kind;
		ref.fType = IReference.T_METHOD_REFERENCE;
		return ref;
	}
	
	/**
	 * Creates and returns a field reference.
	 * 
	 * @param origin where the reference occurred from
	 * @param typeName name of the referenced type where field lookup begins
	 * @param fieldName name of the referenced field
	 * @param kind kind of field reference
	 */
	public static Reference fieldReference(IApiMember origin, String typeName, String fieldName, int kind) {
		Reference ref = new Reference();
		ref.fSourceMember = origin;
		ref.fTypeName = typeName;
		ref.fMemberName = fieldName;
		ref.fKind = kind;
		ref.fType = IReference.T_FIELD_REFERENCE;
		return ref;
	}	
	
	/**
	 * Creates and returns a type reference.
	 * 
	 * @param origin where the reference occurred from
	 * @param typeName name of the referenced type
	 * @param kind kind of reference
	 */
	public static Reference typeReference(IApiMember origin, String typeName, int kind) {
		Reference ref = new Reference();
		ref.fSourceMember = origin;
		ref.fTypeName = typeName;
		ref.fKind = kind;
		ref.fType = IReference.T_TYPE_REFERENCE;
		return ref;
	}	
	
	/**
	 * Creates and returns a type reference.
	 * 
	 * @param origin where the reference occurred from
	 * @param typeName name of the referenced type
	 * @param signature extra type signature information
	 * @param kind kind of reference
	 */
	public static Reference typeReference(IApiMember origin, String typeName, String signature, int kind) {
		Reference ref = typeReference(origin, typeName, kind);
		ref.fSignature = signature;
		return ref;
	}	
	
	/**
	 * Sets the line number - used by the reference extractor.
	 * 
	 * @param line line number
	 */
	void setLineNumber(int line) {
		fSourceLine = line;
	}
	
	/**
	 * Resolves this reference
	 * @throws CoreException
	 */
	public void resolve() throws CoreException {
		if (!this.fStatus) {
			return;
		}
		if (fResolved == null) {
			IApiComponent sourceComponent = getMember().getApiComponent();
			if(sourceComponent != null) {
				IApiTypeRoot result = Util.getClassFile(
						sourceComponent.getBaseline().resolvePackage(sourceComponent, Signatures.getPackageName(getReferencedTypeName())),
						getReferencedTypeName());
				if(result != null) {
					IApiType type = result.getStructure();
					if(type == null) {
						//cannot resolve a type that is in a bad classfile
						return;
					}
					switch (getReferenceType()) {
					case IReference.T_TYPE_REFERENCE:
						fResolved = type;
						break;
					case IReference.T_FIELD_REFERENCE:
						resolveField(type, getReferencedMemberName());
						break;
					case IReference.T_METHOD_REFERENCE:
						resolveVirtualMethod(type, getReferencedMemberName(), getReferencedSignature());
						break;
					}
				}
			}
		}
		// TODO: throw exception on failure
	}
	public boolean resolve(int eeValue) throws CoreException {
		IApiComponent sourceComponent = StubApiComponent.getStubApiComponent(eeValue);
		if (sourceComponent == null) {
			// if there is no source component for the ee value, the reference is considered as resolved
			return true;
		}
		IApiTypeRoot result = Util.getClassFile(
				new IApiComponent[] { sourceComponent },
				getReferencedTypeName());
		if(result != null) {
			IApiType type = result.getStructure();
			if(type == null) {
				return false;
			}
			switch (getReferenceType()) {
			case IReference.T_TYPE_REFERENCE:
				return true;
			case IReference.T_FIELD_REFERENCE:
				return resolveField(type, getReferencedMemberName());
			case IReference.T_METHOD_REFERENCE:
				return resolveMethod(sourceComponent, type, getReferencedMemberName(), getReferencedSignature());
			}
		}
		return false;
	}	
	/**
	 * Resolves the field in the parent class hierarchy
	 * @param type the initial type to search
	 * @param fieldame the name of the field
	 * @return true if the field resolved
	 * @throws CoreException
	 * @since 1.1
	 */
	private boolean resolveField(IApiType type, String fieldame) throws CoreException {
		IApiField field = type.getField(fieldame);
		if(field != null) {
			fResolved = field;
			return true;
		}
		IApiType superT = type.getSuperclass();
		if (superT != null) {
			return resolveField(superT, fieldame);
		}
		return false;
	}
	/**
	 * Resolves a virtual method and returns whether the method lookup was successful.
	 * We need to resolve the actual type that implements the method - i.e. do the virtual
	 * method lookup.
	 * 
	 * @param callSiteComponent the component where the method call site was located
	 * @param typeName referenced type name
	 * @param methodName referenced method name
	 * @param methodSignature referenced method signature
	 * @returns whether the lookup succeeded
	 * @throws CoreException if something goes terribly wrong
	 */
	private boolean resolveVirtualMethod(IApiType type, String methodName, String methodSignature) throws CoreException {
		IApiMethod target = type.getMethod(methodName, methodSignature);
		if (target != null) {
			if (target.isSynthetic()) {
				// don't resolve references to synthetic methods
				return false;
			} else {
				fResolved = target;
				return true;
			}
		}
		if (getReferenceKind() == IReference.REF_INTERFACEMETHOD) {
			// resolve method in super interfaces rather than class
			IApiType[] interfaces = type.getSuperInterfaces();
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; i++) {
					if (resolveVirtualMethod(interfaces[i], methodName, methodSignature)) {
						return true;
					}
				}
			}
		} else {
			IApiType superT = type.getSuperclass();
			if (superT != null) {
				return resolveVirtualMethod(superT, methodName, methodSignature);
			}
		}
		return false;
	}		

	/**
	 * Resolves a method and returns whether the method lookup was successful.
	 * We need to resolve the actual type that implements the method - i.e. do the virtual
	 * method lookup.
	 * 
	 * @param callSiteComponent the component where the method call site was located
	 * @param typeName referenced type name
	 * @param methodName referenced method name
	 * @param methodSignature referenced method signature
	 * @returns whether the lookup succeeded
	 * @throws CoreException if something goes terribly wrong
	 */
	private boolean resolveMethod(IApiComponent sourceComponent, IApiType type, String methodName, String methodSignature) throws CoreException {
		IApiMethod target = type.getMethod(methodName, methodSignature);
		if (target != null) {
			if (target.isSynthetic()) {
				// don't resolve references to synthetic methods
				return false;
			} else {
				return true;
			}
		}
		switch(this.fKind) {
			case IReference.REF_INTERFACEMETHOD : 
				// resolve method in super interfaces rather than class
				String[] interfacesNames = type.getSuperInterfaceNames();
				if (interfacesNames != null) {
					for (int i = 0, max = interfacesNames.length; i < max; i++) {
						IApiTypeRoot classFile = Util.getClassFile(
								new IApiComponent[] { sourceComponent },
								interfacesNames[i]);
						if (classFile == null) {
							ApiPlugin.logErrorMessage("Class file for " + interfacesNames[i] + " was not found for " + sourceComponent.getName()); //$NON-NLS-1$ //$NON-NLS-2$
							return false;
						}
						IApiType superinterface = classFile.getStructure();
						if (superinterface != null && resolveMethod(sourceComponent, superinterface, methodName, methodSignature)) {
							return true;
						}
					}
				}
				break;
			case IReference.REF_STATICMETHOD :
				String superclassName = type.getSuperclassName();
				if (superclassName != null) {
					IApiTypeRoot classFile = Util.getClassFile(
							new IApiComponent[] { sourceComponent },
							superclassName);
					if (classFile == null) {
						ApiPlugin.logErrorMessage("Class file for " + superclassName + " was not found for " + sourceComponent.getName()); //$NON-NLS-1$ //$NON-NLS-2$
						return false;
					}
					IApiType superclass = classFile.getStructure();
					boolean resolved = resolveMethod(sourceComponent, superclass, methodName, methodSignature);
					if (resolved) {
						return resolved;
					}
				}
				break;
			case IReference.REF_VIRTUALMETHOD :
			case IReference.REF_SPECIALMETHOD :
				// check polymorphic methods: polymorphic method signature is ([Ljava/lang/Object;)Ljava/lang/Object;
				target = type.getMethod(methodName, "([Ljava/lang/Object;)Ljava/lang/Object;"); //$NON-NLS-1$
				if (target != null) {
					if (methodName.equals(target.getName()) && target.isPolymorphic()) {
						return true;
					}
				}
				superclassName = type.getSuperclassName();
				if (superclassName != null) {
					IApiTypeRoot classFile = Util.getClassFile(
							new IApiComponent[] { sourceComponent },
							superclassName);
					if (classFile == null) {
						ApiPlugin.logErrorMessage("Class file for " + superclassName + " was not found for " + sourceComponent.getName()); //$NON-NLS-1$ //$NON-NLS-2$
						return false;
					}
					IApiType superclass = classFile.getStructure();
					boolean resolved = resolveMethod(sourceComponent, superclass, methodName, methodSignature);
					if (resolved) {
						return resolved;
					}
				}
				if (Flags.isAbstract(type.getModifiers())) {
					interfacesNames = type.getSuperInterfaceNames();
					if (interfacesNames != null) {
						for (int i = 0, max = interfacesNames.length; i < max; i++) {
							IApiTypeRoot classFile = Util.getClassFile(
									new IApiComponent[] { sourceComponent },
									interfacesNames[i]);
							if (classFile == null) {
								ApiPlugin.logErrorMessage("Class file for " + interfacesNames[i] + " was not found for " + sourceComponent.getName()); //$NON-NLS-1$ //$NON-NLS-2$
								return false;
							}
							IApiType superinterface = classFile.getStructure();
							if (superinterface != null && resolveMethod(sourceComponent, superinterface, methodName, methodSignature)) {
								return true;
							}
						}
					}
				}
		}
		return false;
	}
	/**
	 * Used by the search engine when resolving multiple references.
	 * 
	 * @param resolution resolved reference
	 */
	public void setResolution(IApiMember resolution) {
		fResolved = resolution;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("From: "); //$NON-NLS-1$
		IApiMember member = getMember();
		buf.append(member.getHandle().toString());
		buf.append(" [line: ").append(getLineNumber()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		if (getResolvedReference() == null) {
			buf.append("\nUnresolved To: "); //$NON-NLS-1$
			buf.append(getReferencedTypeName());
			if (getReferencedMemberName() != null) {
				buf.append('#');
				buf.append(getReferencedMemberName());
			}
			if (getReferencedSignature() != null) {
				buf.append('#');
				buf.append(getReferencedSignature());
			}
		} else {
			buf.append("\nResolved To: "); //$NON-NLS-1$
			buf.append(getResolvedReference().getHandle().toString());
		}
		buf.append("\nKind: "); //$NON-NLS-1$
		buf.append(Reference.getReferenceText(getReferenceKind()));
		return buf.toString();
	}
	
	public void setResolveStatus(boolean value) {
		this.fStatus = value;
	}

	/**
	 * Returns the string representation for the given reference kind or 
	 * <code>UKNOWN_KIND</code> if the kind cannot be determined.
	 * 
	 * @param kind the kid(s) to get the display text for
	 * @return the string for the reference kind
	 * @since 1.0.1
	 */
	public static final String getReferenceText(int kind) {
		StringBuffer buffer = new StringBuffer();
		if((kind & IReference.REF_EXTENDS) > 0) {
				buffer.append("EXTENDS"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_IMPLEMENTS) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("IMPLEMENTS"); //$NON-NLS-1$
		}
		if ((kind & IReference.REF_SPECIALMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("INVOKED_SPECIAL"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_STATICMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("INVOKED_STATIC"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PUTFIELD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("PUT_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PUTSTATIC) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("PUT_STATIC_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_FIELDDECL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("DECLARED_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PARAMETERIZED_TYPEDECL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("DECLARED_PARAMETERIZED_TYPE"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PARAMETERIZED_FIELDDECL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("DECLARED_PARAMETERIZED_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PARAMETERIZED_METHODDECL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("DECLARED_PARAMETERIZED_METHOD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PARAMETER) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("PARAMETER"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_LOCALVARIABLEDECL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("LOCAL_VAR_DECLARED"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PARAMETERIZED_VARIABLE) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("DECLARED_PARAMETERIZED_VARIABLE"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_THROWS) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("THROWS"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_CHECKCAST) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("CASTS"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_ARRAYALLOC) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("ALLOCATES_ARRAY"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_CATCHEXCEPTION) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("CATCHES_EXCEPTION"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_GETFIELD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("GETS_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_GETSTATIC) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("GETS_STATIC_FIELD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_INSTANCEOF) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("INSTANCEOF"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_INTERFACEMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("INTERFACE_METHOD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_CONSTRUCTORMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("CONSTRUCTOR_METHOD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_LOCALVARIABLE) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("LOCAL_VARIABLE"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_PASSEDPARAMETER) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("PASSED_PARAMETER"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_RETURNTYPE) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("RETURN_TYPE"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_VIRTUALMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("VIRTUAL_METHOD"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_CONSTANTPOOL) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("CONSTANT_POOL"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_INSTANTIATE) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("INSTANTIATION"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_OVERRIDE) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("OVERRIDE"); //$NON-NLS-1$
		}
		if((kind & IReference.REF_SUPER_CONSTRUCTORMETHOD) > 0) {
			if(buffer.length() != 0) {
				buffer.append(" | "); //$NON-NLS-1$
			}
			buffer.append("SUPER_CONSTRUCTORMETHOD"); //$NON-NLS-1$
		}
		if(buffer.length() == 0) {
			buffer.append(Util.UNKNOWN_KIND);
		}
		return buffer.toString();
	}
	
	/**
	 * Builds a reference descriptor from this reference or <code>null</code>.
	 * 
	 * @return corresponding reference descriptor or <code>null</code> if unresolved
	 * @throws CoreException if unable to resolve visibility
	 */
	public IReferenceDescriptor getReferenceDescriptor() throws CoreException {
		IApiMember res = getResolvedReference();
		if (res == null) {
			return null;
		}
		IApiComponent rcomponent = res.getApiComponent();
		IApiDescription description = rcomponent.getApiDescription();
		IApiAnnotations annot = description.resolveAnnotations(getResolvedReference().getHandle());
		int visibility = -1;
		IApiComponent mcomponent = getMember().getApiComponent();
		if(annot != null) {
			visibility = annot.getVisibility();
			if(annot.getVisibility() == VisibilityModifiers.PRIVATE) {
				IApiComponent host = mcomponent.getHost();
				if(host != null && host.getSymbolicName().equals(rcomponent.getSymbolicName())) {
					visibility = UseReportConverter.FRAGMENT_PERMISSIBLE;
				}
				else {
					IApiAccess access = description.resolveAccessLevel(
							Factory.componentDescriptor(mcomponent.getSymbolicName()),  // component descriptors in API description are not version qualified
							getResolvedReference().getHandle().getPackage());
					if(access != null && access.getAccessLevel() == IApiAccess.FRIEND) {
						visibility = VisibilityModifiers.PRIVATE_PERMISSIBLE;
					}
				}
			}
		}
		else {
			//overflow for those references that cannot be resolved
			visibility = VisibilityModifiers.ALL_VISIBILITIES;
		}
		String[] messages = null;
		if(fProblems != null) {
			messages = new String[fProblems.size()];
			for (int i = 0; i < messages.length; i++) {
				messages[i] = ((IApiProblem)fProblems.get(i)).getMessage();
			}
		}
		return Factory.referenceDescriptor(
				(IComponentDescriptor)mcomponent.getHandle(),
				getMember().getHandle(),
				getLineNumber(),
				(IComponentDescriptor)rcomponent.getHandle(),
				res.getHandle(),
				getReferenceKind(),
				getReferenceFlags(),
				visibility,
				messages);
	}
}
