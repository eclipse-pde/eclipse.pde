/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.build.Utils;

/**
 * Wrapper class for the Ant javac task.
 */
public class JavacTask implements ITask {

	protected String classpathId;
	protected String bootclasspath;
	protected String destdir;
	protected String failonerror;
	protected String[] srcdir;
	protected String verbose;
	protected String includeAntRuntime;
	protected String fork;
	protected String debug;
	protected String source;
	protected String target;
	protected String compileArgs;
	protected String specificCompileArgs;
	protected String compileArgsFile;
	protected String encoding;
	protected String logExtension;
	protected String errorProperty;
	protected String[] excludes;
	protected String compilerAdapter;
	protected String warningProperties;
	private boolean adapterUseLog = false;
	private boolean adapterUseArgFile = false;

	/**
	 * Default constructor for the class.
	 */
	public JavacTask() {
		super();
	}

	/**
	 * @see ITask#print(AntScript)
	 */
	public void print(AntScript script) {
		script.printTab();
		script.print("<javac"); //$NON-NLS-1$
		script.printAttribute("destdir", destdir, false); //$NON-NLS-1$
		script.printAttribute("failonerror", failonerror, false); //$NON-NLS-1$
		script.printAttribute("verbose", verbose, false); //$NON-NLS-1$
		script.printAttribute("fork", fork, false); //$NON-NLS-1$
		script.printAttribute("debug", debug, false); //$NON-NLS-1$
		script.printAttribute("includeAntRuntime", includeAntRuntime, false); //$NON-NLS-1$
		script.printAttribute("bootclasspath", bootclasspath, false); //$NON-NLS-1$
		script.printAttribute("source", source, false); //$NON-NLS-1$
		script.printAttribute("target", target, false); //$NON-NLS-1$
		script.printAttribute("encoding", encoding, false); //$NON-NLS-1$
		script.printAttribute("errorProperty", errorProperty, false); //$NON-NLS-1$
		script.printAttribute("compiler", compilerAdapter, false); //$NON-NLS-1$
		script.println(">"); //$NON-NLS-1$

		script.indent++;

		if (compileArgs != null) {
			script.println("<compilerarg line=\"" + compileArgs + "\" compiler=\"" + Utils.getPropertyFormat(IXMLConstants.PROPERTY_BUILD_COMPILER) + "\"/>"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

		if (specificCompileArgs != null) {
			script.printTabs();
			script.print("<compilerarg"); //$NON-NLS-1$
			script.printAttribute("line", specificCompileArgs, true); //$NON-NLS-1$
			script.printAttribute("compiler", compilerAdapter, false); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}
		script.println("<classpath refid=\"" + classpathId + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$

		for (int i = 0; i < srcdir.length; i++) {
			script.printTab();
			script.print("<src path="); //$NON-NLS-1$
			script.printQuotes(srcdir[i]);
			script.println("/>"); //$NON-NLS-1$
		}

		for (int i = 0; excludes != null && i < excludes.length; i++) {
			script.printTab();
			script.print("<exclude "); //$NON-NLS-1$
			script.printAttribute("name", excludes[i], true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}

		if (warningProperties != null) {
			script.printTab();
			script.print("<compilerarg"); //$NON-NLS-1$
			script.printAttribute("line", "-properties '" + warningProperties + "'", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (compilerAdapter != null && adapterUseArgFile)
				script.printAttribute("compiler", compilerAdapter, true); //$NON-NLS-1$
			else
				script.printAttribute("compiler", IXMLConstants.JDT_COMPILER_ADAPTER, true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}

		if (compileArgsFile != null) {
			script.printTabs();
			script.print("<compilerarg"); //$NON-NLS-1$
			script.printAttribute("value", "@" + compileArgsFile, true); //$NON-NLS-1$ //$NON-NLS-2$
			if (compilerAdapter != null && adapterUseArgFile)
				script.printAttribute("compiler", compilerAdapter, true); //$NON-NLS-1$
			else
				script.printAttribute("compiler", IXMLConstants.JDT_COMPILER_ADAPTER, true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}

		if (destdir != null) {
			script.printTabs();
			script.print("<compilerarg"); //$NON-NLS-1$
			script.printAttribute("line", "-log '" + destdir + logExtension + "'", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (compilerAdapter != null && adapterUseLog)
				script.printAttribute("compiler", compilerAdapter, true); //$NON-NLS-1$
			else
				script.printAttribute("compiler", IXMLConstants.JDT_COMPILER_ADAPTER, true); //$NON-NLS-1$
			script.println("/>"); //$NON-NLS-1$
		}
		script.indent--;
		script.printEndTag("javac"); //$NON-NLS-1$
	}

	/**
	 * Set the javac task classpath refid to be the given value.
	 * @param classpathId
	 */
	public void setClasspathId(String classpathId) {
		this.classpathId = classpathId;
	}

	/**
	 * Set the javac task boot classpath to be the given value.
	 * 
	 * @param bootclasspath the boot classpath attribute
	 */
	public void setBootClasspath(String bootclasspath) {
		this.bootclasspath = bootclasspath;
	}

	/**
	 * Set the javac task destination directory to be the given value.
	 * 
	 * @param destdir the destination directory
	 */
	public void setDestdir(String destdir) {
		this.destdir = destdir;
	}

	/**
	 * Set the javac task failOnError attribute to be the given value. Valid values
	 * are <code>"true"</code> and <code>"false"</code>.
	 * 
	 * @param failonerror either <code>"true"</code> or <code>"false"</code>
	 */
	public void setFailOnError(String failonerror) {
		this.failonerror = failonerror;
	}

	/**
	 * Set the javac task includeAntRuntime attribute to be the given value. Valid
	 * values are <code>"no"</code> and <code>"yes"</code>.
	 * 
	 * @param include either <code>"no"</code> or <code>"yes"</code>
	 */
	public void setIncludeAntRuntime(String include) {
		this.includeAntRuntime = include;
	}

	/**
	 * Set the javac task source directory attribute to be the given value.
	 * 
	 * @param srcdir the source directory
	 */
	public void setSrcdir(String[] srcdir) {
		this.srcdir = srcdir;
	}

	/**
	 * Set patterns to exclude from compilation 
	 * @param excludes
	 */
	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	/**
	 * Set the javac task verbose attribute to be the given value. Valid values
	 * are <code>"true"</code> and <code>"false"</code>.
	 * 
	 * @param verbose either <code>"true"</code> or <code>"false"</code>
	 */
	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}

	/**
	 * Set the javac task fork attribute to be the given value. Valid values
	 * are <code>"true"</code> and <code>"false"</code>.
	 * 
	 * @param fork either <code>"true"</code> or <code>"false"</code>
	 */
	public void setFork(String fork) {
		this.fork = fork;
	}

	/**
	 * Set the javac task debug attribute to be the given value. Valid values
	 * are <code>"on"</code> and <code>"off"</code>.
	 * 
	 * @param debug either <code>"on"</code> or <code>"off"</code>
	 */
	public void setDebug(String debug) {
		this.debug = debug;
	}

	/**
	 * Set the javac task source attribute to be the given value.
	 * 
	 * @param source either <code>"1.3"</code> or <code>"1.4"</code>
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Set the javac task target attribute to be the given value. 
	 * 
	 * @param target either <code>"1.3"</code> or <code>"1.4"</code>
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	public void setCompileArgs(String args) {
		this.compileArgs = args;
	}

	public void setSpecificCompileArgs(String args) {
		this.specificCompileArgs = args;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setLogExtension(String extension) {
		this.logExtension = extension;
	}

	public void setCompileArgsFile(String file) {
		this.compileArgsFile = file;
	}

	public void setErrorProperty(String errorProperty) {
		this.errorProperty = errorProperty;
	}

	public void setCompilerAdapter(String compilerAdapter) {
		this.compilerAdapter = compilerAdapter;
	}

	public void setAdapterUseLog(boolean useLog) {
		this.adapterUseLog = useLog;

	}

	public void setAdapterArgFile(boolean useFile) {
		this.adapterUseArgFile = useFile;

	}

	public void setWarningProperties(String warningProperties) {
		this.warningProperties = warningProperties;
	}
}
