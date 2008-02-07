/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.internal.launching.EEVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.objectweb.asm.Opcodes;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A Utility class to use for API tools
 * 
 * @since 1.0.0
 */
public final class Util {
	
	/**
	 * Registry strategy for testing and non-framework use
	 * @since
	 */
	static class APIRegistryStrategy extends RegistryStrategy {
		String pfiles = null,
			   cont = null;
		/**
		 * Constructor
		 * @throws CoreException
		 */
		public APIRegistryStrategy() throws CoreException {
			super(null, null);
			pfiles = System.getProperty("pluginfiles"); //$NON-NLS-1$
			if(pfiles == null) {
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.getPluginIdentifier(), UtilMessages.Util_2));
			}
			cont = System.getProperty("contributors"); //$NON-NLS-1$
			if(cont == null) {
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.getPluginIdentifier(), UtilMessages.Util_3));
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.spi.RegistryStrategy#onStart(org.eclipse.core.runtime.IExtensionRegistry, boolean)
		 */
		public void onStart(IExtensionRegistry registry, boolean loadedFromCache) {
			super.onStart(registry, loadedFromCache);
			//parse the system properties for -Dpluginfiles and -Dcontributors
			String[] paths = pfiles.split(","); //$NON-NLS-1$
			String[] contributors = cont.split(","); //$NON-NLS-1$
			if(paths.length == contributors.length) {
				IContributor contributor = null;
				File file = null;
				for(int i = 0; i < paths.length; i++) {
					file = new File(paths[i]);
					if(file.exists() && file.isFile()) {
						try {
							contributor = new RegistryContributor(contributors[i], contributors[i], null, null);
							registry.addContribution(new FileInputStream(file), contributor, false, null, null, null);
						}
						catch(FileNotFoundException fnfe) {
							ApiPlugin.log(fnfe);
						}
					}
					
				}
			}
		}
	}
	
	/**
	 * Class that runs a build in the workspace or the given project
	 */
	private static final class BuildJob extends Job {
		private final IProject fProject;
		/**
		 * Constructor
		 * @param name
		 * @param project
		 */
		private BuildJob(String name, IProject project) {
			super(name);
			fProject = project;
		}
		
		public boolean belongsTo(Object family) {
			return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
		}
		
		/**
		 * Returns if this build job is covered by another build job
		 * @param other
		 * @return true if covered by another build job, false otherwise
		 */
		public boolean isCoveredBy(BuildJob other) {
			if (other.fProject == null) {
				return true;
			}
			return fProject != null && fProject.equals(other.fProject);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (getClass()) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
		        Job[] buildJobs = Job.getJobManager().find(ResourcesPlugin.FAMILY_MANUAL_BUILD);
		        for (int i= 0; i < buildJobs.length; i++) {
		        	Job curr= buildJobs[i];
		        	if (curr != this && curr instanceof BuildJob) {
		        		BuildJob job= (BuildJob) curr;
		        		if (job.isCoveredBy(this)) {
		        			curr.cancel(); // cancel all other build jobs of our kind
		        		}
		        	}
				}
			}
			try {
				if (fProject != null) {
					monitor.beginTask(UtilMessages.Util_0+fProject.getName(), 2); 
					fProject.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor,1));
					//ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor,1));
				} else {
					monitor.beginTask(UtilMessages.Util_1, 2); 
					ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor, 2));
				}
			} catch (CoreException e) {
				return e.getStatus();
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}
			finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}
	}
	
	public static boolean DEBUG;

	public static final String EMPTY_STRING = "";//$NON-NLS-1$
	public static final String DEFAULT_PACKAGE_NAME = EMPTY_STRING;
	
	public static final String DOT_CLASS_SUFFIX = ".class"; //$NON-NLS-1$

	/**
	 * Constant representing the default size to read from an input stream
	 */
	private static final int DEFAULT_READING_SIZE = 8192;
	
	private static final String JAVA_LANG_OBJECT = "java.lang.Object"; //$NON-NLS-1$
	private static final String JAVA_LANG_RUNTIMEEXCEPTION = "java.lang.RuntimeException"; //$NON-NLS-1$
	public static final String LINE_DELIMITER = System.getProperty("line.separator"); //$NON-NLS-1$
	
	public static final String UNKNOWN_ELEMENT_TYPE = "unknown_element_type"; //$NON-NLS-1$

	public static final String UNKNOWN_FLAGS = "unknown_flags"; //$NON-NLS-1$
	public static final String UNKNOWN_KIND = "unknown_kind"; //$NON-NLS-1$
	
	public static final IClassFile[] NO_CLASS_FILES = new IClassFile[0];

	static {
		String property = System.getProperty("DEBUG"); //$NON-NLS-1$
		DEBUG = property != null && property.equalsIgnoreCase("TRUE"); //$NON-NLS-1$
	}

	/**
	 * Throws an exception with the given message and underlying exception.
	 * 
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, exception);
		throw new CoreException(status);
	}

	/**
	 * Appends a property to the given string buffer with the given key and value
	 * in the format "key=value\n".
	 * 
	 * @param buffer buffer to append to
	 * @param key key
	 * @param value value
	 */
	private static void appendProperty(StringBuffer buffer, String key, String value) {
		buffer.append(key);
		buffer.append('=');
		buffer.append(value);
		buffer.append('\n');
	}

	/**
	 * Collects all of the deltas from the given parent delta
	 * @param delta
	 * @return
	 */
	public static List collectAllDeltas(IDelta delta) {
		final List list = new ArrayList();
		delta.accept(new DeltaVisitor() {
			public void endVisit(IDelta localDelta) {
				if (localDelta.getChildren().length == 0) {
					list.add(localDelta);
				}
				super.endVisit(localDelta);
			}
		});
		return list;
	}
	
	/**
	 * Collects files into the collector array list
	 * 
	 * @param root
	 *            the root to collect the files from
	 * @param collector
	 *            the collector to place matches into
	 * @param fileFilter
	 *            the filter for files or <code>null</code> to accept all
	 *            files
	 */
	private static void collectAllFiles(File root, ArrayList collector, FileFilter fileFilter) {
		File[] files = root.listFiles(fileFilter);
		for (int i = 0; i < files.length; i++) {
			final File currentFile = files[i];
			if (currentFile.isDirectory()) {
				collectAllFiles(currentFile, collector, fileFilter);
			} else {
				collector.add(currentFile);
			}
		}
	}

	/**
	 * Convert fully qualified signature to unqualified one.
	 * The descriptor can be dot or slashed based.
	 * 
	 * @param descriptor the given descriptor to convert
	 * @return the converted signature
	 */
	public static String dequalifySignature(String signature) {
		StringBuffer buffer = new StringBuffer();
		char[] chars = signature.toCharArray();
		for (int i = 0, max = chars.length; i < max; i++) {
			char currentChar = chars[i];
			switch(currentChar) {
				case 'L' : {
					buffer.append('Q');
					// read reference type
					int lastDotPosition = i;
					i++;
					while(i < chars.length && (currentChar = chars[i]) != ';') {
						switch(currentChar) {
							case '/' :
							case '.' :
								lastDotPosition = i;
								break;
						}
						i++;
					}
					buffer.append(chars, lastDotPosition + 1, i - lastDotPosition - 1);
					buffer.append(';');
					break;
				}
				case 'Q': {
					while(i < chars.length && currentChar != ';') {
						buffer.append(currentChar);
						currentChar = chars[++i];
					}
				}
				default: {
					buffer.append(currentChar);
				}
			}
		}
		return String.valueOf(buffer);
	}

	/**
	 * Returns if the specified signature is qualified or not.
	 * Qualification is determined if there is a token in the signature the begins with an 'L'.
	 * @param signature
	 * @return true if the signature is qualified, false otherwise
	 */
	public static boolean isQualifiedSignature(String signature) {
		StringTokenizer tokenizer = new StringTokenizer(signature, "();IJCSBDFTZ!["); //$NON-NLS-1$
		if(tokenizer.hasMoreTokens()) {
			return tokenizer.nextToken().charAt(0) == 'L';
		}
		return false;
	}
	
	/**
	 * Copies the given file to the new file
	 * @param file
	 * @param newFile
	 * @return if the copy succeeded
	 */
	public static boolean copy(File file, File newFile) {
		byte[] bytes = null;
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file));
			bytes = Util.getInputStreamAsByteArray(inputStream, -1);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		if (bytes != null) {
			BufferedOutputStream outputStream = null;
			try {
				outputStream = new BufferedOutputStream(new FileOutputStream(newFile));
				outputStream.write(bytes);
				outputStream.flush();
			} catch (FileNotFoundException e) {
				ApiPlugin.log(e);
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch(IOException e) {
						// ignore
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Creates an ee file using the default ee id set in the workspace, or 'JavaSE 1.6' if there
	 * is no default ee id set or the framework is not running
	 * 
	 * @return a new ee file
	 * @throws IOException
	 * @throws CoreException
	 */
	public static File createDefaultEEFile() throws IOException, CoreException {
		return createEEFile(getDefaultEEId());
	}
	
	/**
	 * Resolves an EE file given the id of the environment
	 * @param eeid
	 * @return
	 * @throws IOException
	 */
	public static File createEEFile(String eeid) throws IOException, CoreException {
		if (ApiPlugin.isRunningInFramework()) {
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment environment = manager.getEnvironment(eeid);
			IVMInstall[] compatibleVMs = environment.getCompatibleVMs();
			IVMInstall jre = null;
			for (int i = 0; i < compatibleVMs.length; i++) {
				IVMInstall install = compatibleVMs[i];
				if (environment.isStrictlyCompatible(install)) {
					jre = install;
					break;
				}
			}
			if (jre == null && compatibleVMs.length > 0) {
				jre = compatibleVMs[0];
			}
			if (jre == null) {
				jre = JavaRuntime.getDefaultVMInstall();
			}
			String string = Util.generateEEContents(jre, eeid);
			File eeFile = File.createTempFile("eed", ".ee"); //$NON-NLS-1$ //$NON-NLS-2$
			eeFile.deleteOnExit();
			FileOutputStream outputStream = new FileOutputStream(eeFile);
			outputStream.write(string.getBytes("UTF-8")); //$NON-NLS-1$
			outputStream.close();
			return eeFile;
		} else {
			String fileName = System.getProperty("ee.file"); //$NON-NLS-1$
			if (fileName == null) {
				abort("Could not retrieve the ee.file property", null); //$NON-NLS-1$
			}
			return new File(fileName);
		}
	}

	/**
	 * Returns whether the objects are equal, accounting for either one being <code>null</code>.
	 * 
	 * @param o1
	 * @param o2
	 * @return whether the objects are equal, or both are <code>null</code>
	 */
	public static boolean equalsOrNull(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		return o1.equals(o2);
	}

	/**
	 * Returns an execution environment description for the given VM.
	 *  
	 * @param vm JRE to create an definition for
	 * @return an execution environment description for the given VM
	 * @throws IOException if unable to generate description
	 */
	public static String generateEEContents(IVMInstall vm, String eeId) throws IOException {
		StringBuffer buffer = new StringBuffer();
		appendProperty(buffer, EEVMType.PROP_JAVA_HOME, vm.getInstallLocation().getCanonicalPath());
		StringBuffer paths = new StringBuffer();
		LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(vm);
		for (int i = 0; i < libraryLocations.length; i++) {
			LibraryLocation lib = libraryLocations[i];
			paths.append(lib.getSystemLibraryPath().toOSString());
			if (i < (libraryLocations.length - 1)) {
				paths.append(File.pathSeparatorChar);
			}
		}
		appendProperty(buffer, EEVMType.PROP_BOOT_CLASS_PATH, paths.toString());
		appendProperty(buffer, EEVMType.PROP_CLASS_LIB_LEVEL, eeId);
		return  buffer.toString();
	}
	
	/**
	 * Returns an array of all of the files from the given root that are
	 * accepted by the given file filter. If the file filter is null all files
	 * within the given root are returned.
	 * 
	 * @param root
	 * @param fileFilter
	 * @return the list of files from within the given root
	 */
	public static File[] getAllFiles(File root, FileFilter fileFilter) {
		ArrayList files = new ArrayList();
		if (root.isDirectory()) {
			collectAllFiles(root, files, fileFilter);
			File[] result = new File[files.size()];
			files.toArray(result);
			return result;
		}
		return null;
	}

	/**
	 * Returns XML for the component's current API description.
	 * 
	 * @param apiComponent API component
	 * @return XML for the API description
	 * @throws CoreException if something goes terribly wrong 
	 */
	public static String getApiDescriptionXML(IApiComponent apiComponent) throws CoreException {
		ApiSettingsXmlVisitor xmlVisitor = new ApiSettingsXmlVisitor(apiComponent);
		apiComponent.getApiDescription().accept(xmlVisitor);
		return xmlVisitor.getXML();
	}

	/**
	 * Returns a build job
	 * @param project The project to build or <code>null</code> to build the workspace.
	 * @return the build job
	 */
	public static Job getBuildJob(final IProject project) {
		Job buildJob = new BuildJob(UtilMessages.Util_4, project);
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true);
		return buildJob;
	}
	
	public static IClassFile getClassFile(IApiComponent[] components, String typeName) {
		if (components == null) return null;
		for (int i = 0, max = components.length; i < max; i++) {
			IApiComponent apiComponent = components[i];
			if (apiComponent != null) {
				try {
					IClassFile classFile = apiComponent.findClassFile(typeName);
					if (classFile != null) {
						return classFile;
					}
				} catch (CoreException e) {
					// ignore
				}
			}
		}
		return null;
	}

	public static IApiComponent getComponent(IApiComponent[] components, String typeName) {
		if (components == null) return null;
		for (int i = 0, max = components.length; i < max; i++) {
			IApiComponent apiComponent = components[i];
			if (apiComponent != null) {
				try {
					IClassFile classFile = apiComponent.findClassFile(typeName);
					if (classFile != null) {
						return apiComponent;
					}
				} catch (CoreException e) {
					// ignore
				}
			}
		}
		return null;
	}

	/**
	 * @return the id of the default execution environment specified in the workspace,
	 * or 'JavaSE-1.6' if none can be derived 
	 */
	public static String getDefaultEEId() {
		String eeid = "JavaSE-1.6"; //$NON-NLS-1$
		if(ApiPlugin.isRunningInFramework()) {
			eeid = JavaRuntime.getExecutionEnvironmentId(JavaRuntime.newDefaultJREContainerPath());
			if(eeid == null) {
				eeid = "JavaSE-1.6"; //$NON-NLS-1$
			}
		}
		return eeid;
	}
	/**
	 * Return a string that represents the element type of the given delta.
	 * Returns {@link #UNKNOWN_ELEMENT_TYPE} if the element type cannot be determined.
	 * 
	 * @param delta the given delta
	 * @return a string that represents the element type of the given delta.
	 */
	public static String getDeltaElementType(IDelta delta) {
		return getDeltaElementType(delta.getElementType());
	}

	/**
	 * Return a string that represents the given element type
	 * Returns {@link #UNKNOWN_ELEMENT_TYPE} if the element type cannot be determined.
	 * 
	 * @param elementType the given element type
	 * @return a string that represents the given element type.
	 */
	public static String getDeltaElementType(int elementType) {
		switch(elementType) {
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				return "ANNOTATION_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.INTERFACE_ELEMENT_TYPE :
				return "INTERFACE_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.ENUM_ELEMENT_TYPE :
				return "ENUM_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.API_COMPONENT_ELEMENT_TYPE :
				return "API_COMPONENT_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.API_PROFILE_ELEMENT_TYPE :
				return "API_PROFILE_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
				return "CONSTRUCTOR_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.MEMBER_ELEMENT_TYPE :
				return "MEMBER_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.METHOD_ELEMENT_TYPE :
				return "METHOD_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.FIELD_ELEMENT_TYPE :
				return "FIELD_ELEMENT_TYPE"; //$NON-NLS-1$
			case IDelta.CLASS_ELEMENT_TYPE :
				return "CLASS_ELEMENT_TYPE"; //$NON-NLS-1$
		}
		return UNKNOWN_ELEMENT_TYPE;
	}

	/**
	 * Return a string that represents the flags of the given delta.
	 * Returns {@link #UNKNOWN_FLAGS} if the flags cannot be determined.
	 * 
	 * @param delta the given delta
	 * @return a string that represents the flags of the given delta.
	 */
	public static String getDeltaFlagsName(IDelta delta) {
		return getDeltaFlagsName(delta.getFlags());
	}
	
	/**
	 * Return a string that represents the given flags
	 * Returns {@link #UNKNOWN_FLAGS} if the flags cannot be determined.
	 * 
	 * @param flags the given delta's flags
	 * @return a string that represents the given flags.
	 */
	public static String getDeltaFlagsName(int flags) {
		switch(flags) {
			case IDelta.ABSTRACT_TO_NON_ABSTRACT : return "ABSTRACT_TO_NON_ABSTRACT"; //$NON-NLS-1$
			case IDelta.ANNOTATION_DEFAULT_VALUE : return "ANNOTATION_DEFAULT_VALUE"; //$NON-NLS-1$
			case IDelta.API_COMPONENT : return "API_COMPONENT"; //$NON-NLS-1$
			case IDelta.ARRAY_TO_VARARGS : return "ARRAY_TO_VARARGS"; //$NON-NLS-1$
			case IDelta.CHECKED_EXCEPTION : return "CHECKED_EXCEPTION"; //$NON-NLS-1$
			case IDelta.CLASS_BOUND : return "CLASS_BOUND"; //$NON-NLS-1$
			case IDelta.CLINIT : return "CLINIT"; //$NON-NLS-1$
			case IDelta.CONSTRUCTOR : return "CONSTRUCTOR"; //$NON-NLS-1$
			case IDelta.CONTRACTED_SUPERCLASS_SET : return "CONTRACTED_SUPERCLASS_SET"; //$NON-NLS-1$
			case IDelta.CONTRACTED_SUPERINTERFACES_SET : return "CONTRACTED_SUPERINTERFACES_SET"; //$NON-NLS-1$
			case IDelta.DECREASE_ACCESS : return "DECREASE_ACCESS"; //$NON-NLS-1$
			case IDelta.DUPLICATED_TYPE : return "DUPLICATED_TYPE"; //$NON-NLS-1$
			case IDelta.ENUM_CONSTANT : return "ENUM_CONSTANT"; //$NON-NLS-1$
			case IDelta.EXECUTION_ENVIRONMENT : return "EXECUTION_ENVIRONMENT"; //$NON-NLS-1$
			case IDelta.EXPANDED_SUPERCLASS_SET : return "EXPANDED_SUPERCLASS_SET"; //$NON-NLS-1$
			case IDelta.EXPANDED_SUPERINTERFACES_SET : return "EXPANDED_SUPERINTERFACES_SET"; //$NON-NLS-1$
			case IDelta.FIELD : return "FIELD"; //$NON-NLS-1$
			case IDelta.FIELD_MOVED_UP : return "FIELD_MOVED_UP"; //$NON-NLS-1$
			case IDelta.FINAL_TO_NON_FINAL : return "FINAL_TO_NON_FINAL"; //$NON-NLS-1$
			case IDelta.FINAL_TO_NON_FINAL_NON_STATIC : return "FINAL_TO_NON_FINAL_NON_STATIC"; //$NON-NLS-1$
			case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT : return "FINAL_TO_NON_FINAL_STATIC_CONSTANT"; //$NON-NLS-1$
			case IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT : return "FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT"; //$NON-NLS-1$
			case IDelta.INCREASE_ACCESS : return "INCREASE_ACCESS"; //$NON-NLS-1$
			case IDelta.INTERFACE_BOUND : return "INTERFACE_BOUND"; //$NON-NLS-1$
			case IDelta.INTERFACE_BOUNDS : return "INTERFACE_BOUNDS"; //$NON-NLS-1$
			case IDelta.METHOD : return "METHOD"; //$NON-NLS-1$
			case IDelta.METHOD_MOVED_UP : return "METHOD_MOVED_UP"; //$NON-NLS-1$
			case IDelta.METHOD_WITH_DEFAULT_VALUE : return "METHOD_WITH_DEFAULT_VALUE"; //$NON-NLS-1$
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE : return "METHOD_WITHOUT_DEFAULT_VALUE"; //$NON-NLS-1$
			case IDelta.NATIVE_TO_NON_NATIVE : return "NATIVE_TO_NON_NATIVE"; //$NON-NLS-1$
			case IDelta.NON_ABSTRACT_TO_ABSTRACT : return "NON_ABSTRACT_TO_ABSTRACT"; //$NON-NLS-1$
			case IDelta.NON_FINAL_TO_FINAL : return "NON_FINAL_TO_FINAL"; //$NON-NLS-1$
			case IDelta.NON_NATIVE_TO_NATIVE : return "NON_NATIVE_TO_NATIVE"; //$NON-NLS-1$
			case IDelta.NON_STATIC_TO_STATIC : return "NON_STATIC_TO_STATIC"; //$NON-NLS-1$
			case IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED : return "NON_SYNCHRONIZED_TO_SYNCHRONIZED"; //$NON-NLS-1$
			case IDelta.NON_TRANSIENT_TO_TRANSIENT : return "NON_TRANSIENT_TO_TRANSIENT"; //$NON-NLS-1$
			case IDelta.STATIC_TO_NON_STATIC : return "STATIC_TO_NON_STATIC"; //$NON-NLS-1$
			case IDelta.SUPERCLASS : return "SUPERCLASS"; //$NON-NLS-1$
			case IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED : return "SYNCHRONIZED_TO_NON_SYNCHRONIZED"; //$NON-NLS-1$
			case IDelta.TO_ANNOTATION : return "TO_ANNOTATION"; //$NON-NLS-1$
			case IDelta.TO_CLASS : return "TO_CLASS"; //$NON-NLS-1$
			case IDelta.TO_ENUM : return "TO_ENUM"; //$NON-NLS-1$
			case IDelta.TO_INTERFACE : return "TO_INTERFACE"; //$NON-NLS-1$
			case IDelta.TRANSIENT_TO_NON_TRANSIENT : return "TRANSIENT_TO_NON_TRANSIENT"; //$NON-NLS-1$
			case IDelta.TYPE : return "TYPE"; //$NON-NLS-1$
			case IDelta.TYPE_MEMBER : return "TYPE_MEMBER"; //$NON-NLS-1$
			case IDelta.TYPE_PARAMETER : return "TYPE_PARAMETER"; //$NON-NLS-1$
			case IDelta.TYPE_PARAMETER_NAME : return "TYPE_PARAMETER_NAME"; //$NON-NLS-1$
			case IDelta.TYPE_PARAMETERS : return "TYPE_PARAMETERS"; //$NON-NLS-1$
			case IDelta.UNCHECKED_EXCEPTION : return "UNCHECKED_EXCEPTION"; //$NON-NLS-1$
			case IDelta.VALUE : return "VALUE"; //$NON-NLS-1$
			case IDelta.VARARGS_TO_ARRAY : return "VARARGS_TO_ARRAY"; //$NON-NLS-1$
			case IDelta.RESTRICTIONS : return "RESTRICTIONS"; //$NON-NLS-1$
		}
		return UNKNOWN_FLAGS;
	}

	/**
	 * Return a string that represents the kind of the given delta.
	 * Returns {@link #UNKNOWN_KIND} if the kind cannot be determined.
	 * 
	 * @param delta the given delta
	 * @return a string that represents the kind of the given delta.
	 */
	public static String getDeltaKindName(IDelta delta) {
		return getDeltaKindName(delta.getKind());
	}

	/**
	 * Return a string that represents the given kind.
	 * Returns {@link #UNKNOWN_KIND} if the kind cannot be determined.
	 * 
	 * @param delta the given kind
	 * @return a string that represents the given kind.
	 */
	public static String getDeltaKindName(int kind) {
		switch(kind) {
			case IDelta.ADDED :
				return "ADDED"; //$NON-NLS-1$
			case IDelta.ADDED_EXTEND_RESTRICTION :
				return "ADDED_EXTEND_RESTRICTION"; //$NON-NLS-1$
			case IDelta.ADDED_IMPLEMENT_RESTRICTION :
				return "ADDED_IMPLEMENT_RESTRICTION"; //$NON-NLS-1$
			case IDelta.ADDED_NON_VISIBLE :
				return "ADDED_NON_VISIBLE"; //$NON-NLS-1$
			case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
				return "ADDED_NOT_EXTEND_RESTRICTION"; //$NON-NLS-1$
			case IDelta.ADDED_NOT_EXTEND_RESTRICTION_STATIC :
				return "ADDED_NOT_EXTEND_RESTRICTION_STATIC"; //$NON-NLS-1$
			case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
				return "ADDED_NOT_IMPLEMENT_RESTRICTION"; //$NON-NLS-1$
			case IDelta.CHANGED :
				return "CHANGED"; //$NON-NLS-1$
			case IDelta.CHANGED_NOT_EXTEND_RESTRICTION :
				return "CHANGED_NOT_EXTEND_RESTRICTION"; //$NON-NLS-1$
			case IDelta.CHANGED_NON_VISIBLE :
				return "CHANGED_NON_VISIBLE"; //$NON-NLS-1$
			case IDelta.CHANGED_VISIBILITY :
				return "CHANGED_VISIBILITY"; //$NON-NLS-1$
			case IDelta.REMOVED :
				return "REMOVED"; //$NON-NLS-1$
			case IDelta.REMOVED_EXTEND_RESTRICTION :
				return "REMOVED_EXTEND_RESTRICTION"; //$NON-NLS-1$
			case IDelta.REMOVED_NON_VISIBLE :
				return "REMOVED_NON_VISIBLE"; //$NON-NLS-1$
		}
		return UNKNOWN_KIND;
	}

	/**
	 * Returns the preference key for the given element type, the given kind and the given flags.
	 * 
	 * @param elementType the given element type (retrieved using {@link IDelta#getElementType()}
	 * @param kind the given kind (retrieved using {@link IDelta#getKind()}
	 * @param flags the given flags (retrieved using {@link IDelta#getFlags()}
	 * @return the preference key for the given element type, the given kind and the given flags.
	 */
	public static String getDeltaPrefererenceKey(int elementType, int kind, int flags) {
		StringBuffer buffer = new StringBuffer(Util.getDeltaElementType(elementType));
		buffer.append('_').append(Util.getDeltaKindName(kind));
		if (flags != -1) buffer.append('_').append(Util.getDeltaFlagsName(flags));
		return String.valueOf(buffer);
	}

	/**
	 * Returns the details of the api delta as a string
	 * @param delta
	 * @return the details of the delta as a string
	 */
	public static String getDetail(IDelta delta) {
		StringBuffer buffer = new StringBuffer();
		switch(delta.getElementType()) {
			case IDelta.CLASS_ELEMENT_TYPE :
				buffer.append("class"); //$NON-NLS-1$
				break;
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				buffer.append("annotation"); //$NON-NLS-1$
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE :
				buffer.append("interface"); //$NON-NLS-1$
				break;
			case IDelta.API_COMPONENT_ELEMENT_TYPE :
				buffer.append("api component"); //$NON-NLS-1$
				break;
			case IDelta.API_PROFILE_ELEMENT_TYPE :
				buffer.append("api profile"); //$NON-NLS-1$
				break;
			case IDelta.METHOD_ELEMENT_TYPE:
				buffer.append("method"); //$NON-NLS-1$
				break;
			case IDelta.MEMBER_ELEMENT_TYPE :
				buffer.append("member"); //$NON-NLS-1$
				break;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
				buffer.append("constructor"); //$NON-NLS-1$
				break;
			case IDelta.ENUM_ELEMENT_TYPE :
				buffer.append("enum"); //$NON-NLS-1$
				break;
			case IDelta.FIELD_ELEMENT_TYPE :
				buffer.append("field"); //$NON-NLS-1$
				break;
		}
		buffer.append(' ');
		switch(delta.getKind()) {
			case IDelta.ADDED :
				buffer.append("added"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
				buffer.append("added no implementation restriction"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_IMPLEMENT_RESTRICTION :
				buffer.append("added implementation restriction"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_EXTEND_RESTRICTION :
				buffer.append("added extend restriction"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
				buffer.append("added no extend restriction"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_NON_VISIBLE :
				buffer.append("added non visible"); //$NON-NLS-1$
				break;
			case IDelta.ADDED_NOT_EXTEND_RESTRICTION_STATIC :
				buffer.append("added no extend restriction static"); //$NON-NLS-1$
				break;
			case IDelta.REMOVED :
				buffer.append("removed"); //$NON-NLS-1$
				break;
			case IDelta.REMOVED_EXTEND_RESTRICTION :
				buffer.append("removed extend restriction"); //$NON-NLS-1$
				break;
			case IDelta.REMOVED_NON_VISIBLE :
				buffer.append("removed non visible"); //$NON-NLS-1$
				break;
			case IDelta.CHANGED :
				buffer.append("changed"); //$NON-NLS-1$
				break;
			case IDelta.CHANGED_NOT_EXTEND_RESTRICTION :
				buffer.append("changed no extend restriction"); //$NON-NLS-1$
				break;
			case IDelta.CHANGED_NON_VISIBLE :
				buffer.append("changed non visible"); //$NON-NLS-1$
				break;
			case IDelta.CHANGED_VISIBILITY :
				buffer.append("changed visibility"); //$NON-NLS-1$
				break;
			default:
				buffer.append("unknown kind"); //$NON-NLS-1$
			break;
		}
		buffer.append(' ').append(delta.getTypeName()).append("#").append(delta.getKey()); //$NON-NLS-1$
		return String.valueOf(buffer);
	}

	/**
	 * Returns the {@link IDocument} for the specified {@link ICompilationUnit}
	 * @param cu
	 * @return the {@link IDocument} for the specified {@link ICompilationUnit}
	 * @throws CoreException
	 */
	public static IDocument getDocument(ICompilationUnit cu) throws CoreException {
		if (cu.getOwner() == null) {
			IFile file= (IFile) cu.getResource();
			if (file.exists()) {
				ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
				IPath path= cu.getPath();
				bufferManager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
				return bufferManager.getTextFileBuffer(path, LocationKind.IFILE).getDocument();
			}
		}
		return new org.eclipse.jface.text.Document(cu.getSource());
	}

	/**
	 * Retrieve EE properties from the ee.file property.
	 * @param eeFileProperty the given ee.file property
	 *
	 * @return the corresponding properties or null if none
	 */
	public static Properties getEEProfile(File eeFileProperty) {
		if (!eeFileProperty.exists()) return null;
		EEVMType.clearProperties(eeFileProperty);
		String ee = EEVMType.getProperty(EEVMType.PROP_CLASS_LIB_LEVEL, eeFileProperty);

		if (ee == null) return null;

		String profileName = ee + ".profile"; //$NON-NLS-1$
		InputStream stream = Util.class.getResourceAsStream("profiles/" + profileName); //$NON-NLS-1$
		if (stream != null) {
			try {
				Properties profile = new Properties();
				profile.load(stream);
				return profile;
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					stream.close();
				} catch(IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve EE properties for the given ee name.
	 * @param eeName the given ee name
	 *
	 * @return the corresponding properties or null if none
	 */
	public static Properties getEEProfile(String eeName) {
		if (eeName == null) return null;

		String profileName = eeName + ".profile"; //$NON-NLS-1$
		InputStream stream = Util.class.getResourceAsStream("profiles/" + profileName); //$NON-NLS-1$
		if (stream != null) {
			try {
				Properties profile = new Properties();
				profile.load(stream);
				return profile;
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					stream.close();
				} catch(IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return null;
	}

	/**
	 * Returns the {@link IExtensionRegistry} to use for internal functionality that requires plugins, but also has to work
	 * with OSGi.
	 * 
	 * @return the {@link IExtensionRegistry} 
	 * @throws CoreException if something bad happens
	 */
	public static IExtensionRegistry getExtensionRegistry() throws CoreException {
		if(ApiPlugin.isRunningInFramework()) {
			return Platform.getExtensionRegistry();
		}
		else {
			//we need one of our own
			IExtensionRegistry registry = RegistryFactory.createRegistry(new APIRegistryStrategy(), null, null);
			return registry;
		}
	}

	/**
	 * Returns the name to display for an {@link IApiProblemFilter}.
	 * Used primarily in UI context for label providers
	 * @param filter
	 * @return a formatted filter name or null if the type of the filter is unknown
	 */
	public static final String getFormattedFilterName(IApiProblemFilter filter) {
		IElementDescriptor desc = filter.getElement();
		switch(desc.getElementType()) {
			case IElementDescriptor.T_PACKAGE: {
				return ((IPackageDescriptor)desc).getName();
			}
			case IElementDescriptor.T_REFERENCE_TYPE: {
				return ((IReferenceTypeDescriptor)desc).getName();
			}
			case IElementDescriptor.T_METHOD: {
				IMethodDescriptor method = (IMethodDescriptor) desc;
				return Signature.toString(method.getSignature(), method.getName(), null, false, false);
			}
			case IElementDescriptor.T_FIELD: {
				return ((IFieldDescriptor) desc).getName();
			}
		}
		return null;
	}

	/**
	 * Returns the number of fragments for the given version value, -1 if the format is unknown.
	 * The version is formed like: [optional plugin name] major.minor.micro.qualifier.
	 * 
	 * @param version the given version value
	 * @return the number of fragments for the given version value or -1 if the format is unknown
	 * @throws IllegalArgumentException if version is null
	 */
	public static final int getFragmentNumber(String version) {
		if (version == null) throw new IllegalArgumentException("The given version should not be null"); //$NON-NLS-1$
		int index = version.indexOf(' ');
		char[] charArray = version.toCharArray();
		int length = charArray.length;
		if (index + 1 >= length) {
			return -1;
		}
		int counter = 1;
		for (int i = index + 1; i < length; i++) {
			switch(charArray[i]) {
				case '0' :
				case '1' :
				case '2' :
				case '3' :
				case '4' :
				case '5' :
				case '6' :
				case '7' :
				case '8' :
				case '9' :
					continue;
				case '.' :
					counter++;
					break;
				default :
					return -1;
			}
		}
		return counter;
	}

	public static IMember getIMember(IDelta delta, IJavaProject javaProject) {
		String typeName = delta.getTypeName();
		if (typeName == null) return null;
		IType type = null;
		try {
			type = javaProject.findType(typeName);
		} catch (JavaModelException e) {
			// ignore
		}
		if (type == null) return null;
		String key = delta.getKey();
		switch(delta.getElementType()) {
			case IDelta.FIELD_ELEMENT_TYPE : {
					IField field = type.getField(key);
					if (field.exists()) {
						return field;
					}
				}
				break;
			case IDelta.CLASS_ELEMENT_TYPE :
			case IDelta.ANNOTATION_ELEMENT_TYPE :
			case IDelta.INTERFACE_ELEMENT_TYPE :
			case IDelta.ENUM_ELEMENT_TYPE :
			case IDelta.MEMBER_ELEMENT_TYPE :
				// we report the marker on the type
				switch(delta.getKind()) {
					case IDelta.ADDED :
					case IDelta.ADDED_EXTEND_RESTRICTION :
					case IDelta.ADDED_IMPLEMENT_RESTRICTION :
					case IDelta.ADDED_NON_VISIBLE :
					case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
					case IDelta.ADDED_NOT_EXTEND_RESTRICTION_STATIC :
					case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
								IField field = type.getField(key);
								if (field.exists()) {
									return field;
								}
								break;
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
								int indexOf = key.indexOf('(');
								if (indexOf == -1) {
									return null;
								}
								int index = indexOf;
								String selector = key.substring(0, index);
								String descriptor = key.substring(index, key.length());
								return getMethod(type, selector, descriptor);
							case IDelta.TYPE_MEMBER :
								IType type2 = type.getType(key);
								if (type2.exists()) {
									return type2;
								}
						}
				}
				return type;
			case IDelta.METHOD_ELEMENT_TYPE :
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE : {
					int indexOf = key.indexOf('(');
					if (indexOf == -1) {
						return null;
					}
					int index = indexOf;
					String selector = key.substring(0, index);
					String descriptor = key.substring(index, key.length());
					return getMethod(type, selector, descriptor);
				}
		}
		return null;
	}

	private static IMember getMethod(IType type, String selector, String descriptor) {
		IMethod method = null;
		String[] parameterTypes = Signature.getParameterTypes(Util.dequalifySignature(descriptor));

		try {
			method = type.getMethod(selector, parameterTypes);
		} catch (IllegalArgumentException e) {
			ApiPlugin.log(e);
		}
		if (method == null) {
			return null;
		}
		if (method.exists()) {
			return method;
		} else {
			// try to check by selector
			IMethod[] methods = null;
			try {
				methods = type.getMethods();
			} catch (JavaModelException e) {
				ApiPlugin.log(e);
				return type;
			}
			List list = new ArrayList();
			for (int i = 0, max = methods.length; i < max; i++) {
				IMethod method2 = methods[i];
				if (selector.equals(method2.getElementName())) {
					list.add(method2);
				}
			}
			switch(list.size()) {
				case 0 :
					return type;
				case 1 :
					return (IMember) list.get(0);
				default:
					// need to find a matching parameters
					loop: for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
						IMethod method2 = (IMethod) iterator.next();
						String[] parameterTypes2 = method2.getParameterTypes();
						boolean found = false;
						if (parameterTypes2.length == parameterTypes.length) {
							for (int i = 0, max = parameterTypes.length; i < max; i++) {
								switch(parameterTypes[i].charAt(0)) {
									case Signature.C_UNRESOLVED :
										// object type
										int index = parameterTypes[i].lastIndexOf('$');
										int index2 = parameterTypes2[i].lastIndexOf('$');
										if (index == -1) {
											continue loop;
										}
										if (!parameterTypes[i].substring(index + 1, parameterTypes[i].length()).equals(
												parameterTypes2[i].substring(index2 + 1, parameterTypes2[i].length()))) {
											continue loop;
										}
										break;
									case Signature.C_ARRAY :
										// array type
										int counter = 0;
										int j = 0;
										while (parameterTypes[i].charAt(j) == '[') {
											counter++;
										}
										int counter2 = 0;
										j = 0;
										if (parameterTypes[j].charAt(0) != '[') {
											// not matching types
											continue loop;
										}
										while (parameterTypes2[i].charAt(j) == '[') {
											counter2++;
										}
										if (counter2 != counter) {
											// not matching types
											continue loop;
										}
										index = parameterTypes[i].lastIndexOf('$');
										index2 = parameterTypes2[i].lastIndexOf('$');
										if (index == -1) {
											continue loop;
										}
										if (!parameterTypes[i].substring(index + 1, parameterTypes[i].length()).equals(
												parameterTypes2[i].substring(index2 + 1, parameterTypes2[i].length()))) {
											continue loop;
										}
										break;
									case Signature.C_CHAR :
									case Signature.C_BOOLEAN :
									case Signature.C_BYTE :
									case Signature.C_DOUBLE :
									case Signature.C_FLOAT :
									case Signature.C_INT :
									case Signature.C_LONG :
									case Signature.C_SHORT :
									case Signature.C_VOID :
										if (parameterTypes[i].charAt(0) != parameterTypes2[i].charAt(0)) {
											// not matching types
											continue loop;
										}
								}
								continue loop;
							}
							found = true;
						}
						if (found) {
							return method2;
						}
					}
			}
		}
		return type;
	}

	/**
	 * Returns the given input stream as a byte array
	 * @param stream the stream to get as a byte array
	 * @param length the length to read from the stream or -1 for unknown
	 * @return the given input stream as a byte array
	 * @throws IOException
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				// read at least 8K
				int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE);
				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents,
							0,
							contents = new byte[contentsLength + amountRequested],
							0,
							contentsLength);
				}
				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);
				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);
			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case length is the actual
				// read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}
		return contents;
	}
	
	/**
	 * Returns the given input stream's contents as a character array.
	 * If a length is specified (i.e. if length != -1), this represents the number of bytes in the stream.
	 * Note the specified stream is not closed in this method
	 * @param stream the stream to get convert to the char array 
	 * @param length the length of the input stream, or -1 if unknown
	 * @param encoding the encoding to use when reading the stream
	 * @return the given input stream's contents as a character array.
	 * @throws IOException if a problem occurred reading the stream.
	 */
	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding) throws IOException {
		Charset charset = null;
		try {
			charset = Charset.forName(encoding);
		} catch (IllegalCharsetNameException e) {
			System.err.println("Illegal charset name : " + encoding); //$NON-NLS-1$
			return null;
		} catch(UnsupportedCharsetException e) {
			System.err.println("Unsupported charset : " + encoding); //$NON-NLS-1$
			return null;
		}
		CharsetDecoder charsetDecoder = charset.newDecoder();
		charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		byte[] contents = getInputStreamAsByteArray(stream, length);
		ByteBuffer byteBuffer = ByteBuffer.allocate(contents.length);
		byteBuffer.put(contents);
		byteBuffer.flip();
		return charsetDecoder.decode(byteBuffer).array();
	}
	
	public static IResource getManifestFile(IProject currentProject) {
		return currentProject.findMember("META-INF/MANIFEST.MF"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the string of the 'kind' from a given api problem marker
	 * @param kind
	 * @return the string representation of the kind of an api problem marker or {@link #UNKNOWN_KIND}
	 */
	public static final String getMarkerKind(IMarker marker) {
		StringBuffer buffer = new StringBuffer();
		int flags = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, -1);
		int kinds = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, -1);
		if(kinds != -1) {
			String kindstr = null;
			if(flags == 0) {
				kindstr = getReferenceKind(kinds);
			}
			else {
				kindstr = getDeltaKindName(kinds);
				if(UNKNOWN_KIND.equals(kindstr)) {
					//try the ref kinds: overlap
					kindstr = getReferenceKind(kinds);
				}
			}
			if(kindstr != null) { 
				buffer.append(kindstr);
			}
			if(kinds != IDelta.ADDED_NOT_EXTEND_RESTRICTION && 
				kinds != IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION && 
				flags > 0) {
					buffer.append("_").append(getDeltaFlagsName(flags)); //$NON-NLS-1$
			}
		}
		String value = buffer.toString();
		return (value.length() == 0 ? UNKNOWN_KIND : value);
	}
	
	/**
	 * Creates a method signature from a specified {@link MethodDeclaration}
	 * @param node
	 * @return the signature for the given method node or <code>null</code>
	 */
	public static String getMethodSignatureFromNode(MethodDeclaration node) {
		if(!node.isConstructor()) {
			List params = node.parameters();
			List rparams = new ArrayList(params.size());
			SingleVariableDeclaration param = null;
			String pname = null;
			for(Iterator iter = params.iterator(); iter.hasNext();) {
				param = (SingleVariableDeclaration) iter.next();
				pname = Util.getTypeSignature(param.getType());
				if(pname != null) {
					rparams.add(pname);
				}
			}
			if(rparams.size() == params.size()) {
				Type returnType = node.getReturnType2();
				if (returnType != null) {
					String rtype = Util.getTypeSignature(returnType);
					if(rtype != null) {
						return Signature.createMethodSignature((String[]) rparams.toArray(new String[rparams.size()]), rtype);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * The type name is dot-separated
	 * @param typeName the given type name
	 * @return the package name for the given type name or an empty string if none
	 */
	public static String getPackageName(String typeName) {
		int index = typeName.lastIndexOf('.');
		return index == -1 ? DEFAULT_PACKAGE_NAME : typeName.substring(0, index);
	}
	
	/**
	 * Returns the string representation for the given reference kind
	 * @param kind
	 * @return the string for the reference kind
	 */
	public static final String getReferenceKind(int kind) {
		switch(kind) {
			case ReferenceModifiers.REF_EXTENDS: {
				return "EXTENDS"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_IMPLEMENTS: {
				return "IMPLEMENTS"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_SPECIALMETHOD: {
				return "INVOKED_SPECIAL"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_STATICMETHOD: {
				return "INVOKED_STATIC"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PUTFIELD: {
				return "PUT_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PUTSTATIC: {
				return "PUT_STATIC_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_FIELDDECL: {
				return "DECLARED_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL: {
				return "DECLARED_PARAMETERIZED_TYPE"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PARAMETERIZED_FIELDDECL: {
				return "DECLARED_PARAMETERIZED_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PARAMETERIZED_METHODDECL: {
				return "DECLARED_PARAMETERIZED_METHOD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PARAMETER: {
				return "PARAMETER"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_LOCALVARIABLEDECL: {
				return "LOCAL_VAR_DECLARED"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PARAMETERIZED_VARIABLE: {
				return "DECLARED_PARAMETERIZED_VARIABLE"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_THROWS: {
				return "THROWS"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_CHECKCAST: {
				return "CASTS"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_ARRAYALLOC: {
				return "ALLOCATES_ARRAY"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_CATCHEXCEPTION: {
				return "CATCHES_EXCEPTION"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_GETFIELD: {
				return "GETS_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_GETSTATIC: {
				return "GETS_STATIC_FIELD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_INSTANCEOF: {
				return "INSTANCEOF"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_INTERFACEMETHOD: {
				return "INTERFACE_METHOD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_CONSTRUCTORMETHOD: {
				return "CONSTRUCTOR_METHOD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_LOCALVARIABLE: {
				return "LOCAL_VARIABLE"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_PASSEDPARAMETER: {
				return "PASSED_PARAMETER"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_RETURNTYPE: {
				return "RETURN_TYPE"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_VIRTUALMETHOD: {
				return "VIRTUAL_METHOD"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_CONSTANTPOOL: {
				return "CONSTANT_POOL"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_INSTANTIATE: {
				return "INSTANTIATION"; //$NON-NLS-1$
			}
			case ReferenceModifiers.REF_OVERRIDE : {
				return "OVERRIDE"; //$NON-NLS-1$
			}
		}
		return null;
	}	
	
	/**
	 * Returns a reference type for the given fully qualified type name.
	 * 
	 * @param fullyQualifiedName type name
	 * @return reference type
	 */
	public static IReferenceTypeDescriptor getType(String fullyQualifiedName) {
		int index = fullyQualifiedName.lastIndexOf('.');
		String pkg = index == -1 ? DEFAULT_PACKAGE_NAME : fullyQualifiedName.substring(0, index);
		String type = index == -1 ? fullyQualifiedName : fullyQualifiedName.substring(index + 1);
		return Factory.packageDescriptor(pkg).getType(type);
	}	
	
	/**
	 * Returns the simple name of the type, by stripping off the last '.' segment and returning it.
	 * This method assumes that qualified type names are '.' separated. If the type specified is a package
	 * than an empty string is returned.
	 * @param qualifiedname the fully qualified name of a type, '.' separated (e.g. a.b.c.Type)
	 * @return the simple name from the qualified name. For example if the qualified name is a.b.c.Type this method 
	 * will return Type (stripping off the package qualification)
	 */
	public static String getTypeName(String qualifiedname) {
		int idx = qualifiedname.lastIndexOf('.');
		idx++;
		if(idx > 0) {
			return qualifiedname.substring(idx, qualifiedname.length());
		}
		// default package
		return qualifiedname;
	}
	
	/**
	 * Processes the signature for the given {@link Type}
	 * @param type the type to process
	 * @return the signature for the type or <code>null</code> if one could not be 
	 * derived
	 */
	public static String getTypeSignature(Type type) {
		switch(type.getNodeType()) {
		case ASTNode.SIMPLE_TYPE: {
			return Signature.createTypeSignature(((SimpleType) type).getName().getFullyQualifiedName(), false);
		}
		case ASTNode.QUALIFIED_TYPE: {
			return Signature.createTypeSignature(((QualifiedType)type).getName().getFullyQualifiedName(), false);
		}
		case ASTNode.ARRAY_TYPE: {
			ArrayType a = (ArrayType) type;
			return Signature.createArraySignature(getTypeSignature(a.getElementType()), a.getDimensions());
		}
		case ASTNode.PARAMETERIZED_TYPE: {
			//we don't need to care about the other scoping types only the base type
			return getTypeSignature(((ParameterizedType) type).getType());
		}
		case ASTNode.PRIMITIVE_TYPE: {
			return Signature.createTypeSignature(((PrimitiveType)type).getPrimitiveTypeCode().toString(), false);
		}
		}
		return null;
	}
	
	public static boolean isAbstract(int accessFlags) {
		return (accessFlags & Opcodes.ACC_ABSTRACT) != 0;
	}
	
	public static boolean isAnnotation(int accessFlags) {
		return (accessFlags & Opcodes.ACC_ANNOTATION) != 0;
	}
	
	/**
	 * Returns if the specified file name is an archive name. A name is
	 * considered to be an archive name if it ends with either '.zip' or '.jar'
	 * 
	 * @param fileName
	 * @return true if the file name is an archive name false otherwise
	 */
	public static boolean isArchive(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".zip") || normalizedFileName.endsWith(".jar"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Returns if the flags are for a bridge
	 * @param accessFlags
	 * @return
	 */
	public static boolean isBridge(int accessFlags) {
		return (accessFlags & Opcodes.ACC_BRIDGE) != 0;
	}
	
	/**
	 * Returns if the flags are for a class
	 * @param accessFlags
	 * @return
	 */
	public static boolean isClass(int accessFlags) {
		return (accessFlags & (Opcodes.ACC_ENUM | Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE)) == 0;
	}
	
	/**
	 * Returns if the specified file name is for a class file. A name is
	 * considered to be a class file if it ends in '.class'
	 * 
	 * @param fileName
	 * @return true if the name is for a class file false otherwise
	 */
	public static boolean isClassFile(String fileName) {
		return fileName.toLowerCase().endsWith(DOT_CLASS_SUFFIX); 
	}

	public static boolean isDefault(int accessFlags) {
		// none of the private, protected or public bit is set
		return (accessFlags & (Opcodes.ACC_PRIVATE
		|	Opcodes.ACC_PROTECTED
		|	Opcodes.ACC_PUBLIC)) == 0;
	}
	
	public static boolean isDeprecated(int accessFlags) {
		return (accessFlags & Opcodes.ACC_DEPRECATED) != 0;
	}


	public static boolean isEnum(int accessFlags) {
		return (accessFlags & Opcodes.ACC_ENUM) != 0;
	}

	public static boolean isFinal(int accessFlags) {
		return (accessFlags & Opcodes.ACC_FINAL) != 0;
	}
	
	public static final boolean isGreatherVersion(String versionToBeChecked, String referenceVersion) {
		SinceTagVersion sinceTagVersion1 = null;
		SinceTagVersion sinceTagVersion2 = null;
		try {
			sinceTagVersion1 = new SinceTagVersion(versionToBeChecked);
			sinceTagVersion2 = new SinceTagVersion(referenceVersion);
		} catch (IllegalArgumentException e) {
			// We cannot compare the two versions as their format is unknown
			// TODO (olivier) should we report these as malformed tags?
			return false;
		}
		Version version1 = sinceTagVersion1.getVersion();
		Version version2 = sinceTagVersion2.getVersion();
		if (version1.getMajor() > version2.getMajor()) {
			return true;
		}
		if (version1.getMinor() > version2.getMinor()) {
			return true;
		}
		if (version1.getMicro() > version2.getMicro()) {
			return true;
		}
		String qualifier1 = version1.getQualifier();
		String qualifier2 = version2.getQualifier();
		if (qualifier1 == null) {
			return false;
		} else if (qualifier2 == null) {
			return true;
		} else {
			return qualifier1.compareTo(qualifier2) > 0;
		}
	}

	public static boolean isInterface(int accessFlags) {
		return (accessFlags & Opcodes.ACC_INTERFACE) != 0;
	}

	/**
	 * Returns if the specified file name is for a java source file. A name is
	 * considered to be a java source file if it ends in '.java'
	 * 
	 * @param fileName
	 * @return true if the name is for a java source file, false otherwise
	 */
	public static boolean isJavaFileName(String fileName) {
		return fileName.toLowerCase().endsWith(".java"); //$NON-NLS-1$
	}

	/**
	 * Returns if the given name is {@link java.lang.Object}
	 * @param name
	 * @return true if the name is java.lang.Object, false otherwise
	 */
	public static boolean isJavaLangObject(String name) {
		return name != null && name.equals(JAVA_LANG_OBJECT);
	}
	
	/**
	 * Return if the name is {@link java.lang.RuntimeException}
	 * @param name
	 * @return true if the name is java.lang.RuntimeException, false otherwise
	 */
	public static boolean isJavaLangRuntimeException(String name) {
		return name != null && name.equals(JAVA_LANG_RUNTIMEEXCEPTION);
	}

	public static boolean isNative(int accessFlags) {
		return (accessFlags & Opcodes.ACC_NATIVE) != 0;
	}

	public static boolean isPrivate(int accessFlags) {
		return (accessFlags & Opcodes.ACC_PRIVATE) != 0;
	}

	public static boolean isProtected(int accessFlags) {
		return (accessFlags & Opcodes.ACC_PROTECTED) != 0;
	}

	public static boolean isPublic(int accessFlags) {
		return (accessFlags & Opcodes.ACC_PUBLIC) != 0;
	}

	public static boolean isStatic(int accessFlags) {
		return (accessFlags & Opcodes.ACC_STATIC) != 0;
	}

	public static boolean isStrict(int accessFlags) {
		return (accessFlags & Opcodes.ACC_STRICT) != 0;
	}
	
	public static boolean isSynchronized(int accessFlags) {
		return (accessFlags & Opcodes.ACC_SYNCHRONIZED) != 0;
	}
	
	public static boolean isSynthetic(int accessFlags) {
		return (accessFlags & Opcodes.ACC_SYNTHETIC) != 0;
	}

	public static boolean isTransient(int accessFlags) {
		return (accessFlags & Opcodes.ACC_TRANSIENT) != 0;
	}
	
	public static boolean isVarargs(int accessFlags) {
		return (accessFlags & Opcodes.ACC_VARARGS) != 0;
	}
	
	public static boolean isVolatile(int accessFlags) {
		return (accessFlags & Opcodes.ACC_VOLATILE) != 0;
	}
	
	/**
	 * Returns a new XML document.
	 * 
	 * @return document
	 * @throws CoreException if unable to create a new document
	 */
	public static Document newDocument() throws CoreException {
		DocumentBuilderFactory dfactory= DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dfactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			abort("Unable to create new XML document.", e); //$NON-NLS-1$
		}
		Document doc= docBuilder.newDocument();
		return doc;
	}
	
	/**
	 * Parses the given string representing an XML document, returning its
	 * root element.
	 * 
	 * @param document XML document as a string
	 * @return the document's root element
	 * @throws CoreException if unable to parse the document
	 */
	public static Element parseDocument(String document) throws CoreException {
		Element root = null;
		InputStream stream = null;
		try{		
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			stream = new ByteArrayInputStream(document.getBytes());
			root = parser.parse(stream).getDocumentElement();
		} catch (ParserConfigurationException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (SAXException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to parse XML document.", e);  //$NON-NLS-1$
		} finally { 
			try{
				if (stream != null) {
					stream.close();
				}
			} catch(IOException e) {
				abort("Unable to parse XML document.", e);  //$NON-NLS-1$
			}
		}
		return root;
	}
	
	/**
	 * Save the given contents into the given file. The file parent folder must exist.
	 * 
	 * @param file the given file target
	 * @param contents the given contents
	 * @throws IOException if an IOException occurs while saving the file
	 */
	public static void saveFile(File file, String contents) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(contents);
			writer.flush();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Serializes the given XML document into a UTF-8 string.
	 * 
	 * @param document XML document to serialize
	 * @return a string representing the given document
	 * @throws CoreException if unable to serialize the document
	 */
	public static String serializeDocument(Document document) throws CoreException {
		try {
			ByteArrayOutputStream s = new ByteArrayOutputStream();
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");  //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4"); //$NON-NLS-1$ //$NON-NLS-2$
			DOMSource source = new DOMSource(document);
			StreamResult outputTarget = new StreamResult(s);
			transformer.transform(source, outputTarget);
			return s.toString("UTF8"); //$NON-NLS-1$	
		} catch (TransformerException e) {
			abort("Unable to serialize XML document.", e);   //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to serialize XML document.",e);  //$NON-NLS-1$
		}
		return null;
	}
}
