package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.eclipse.pde.internal.build.Policy;
/**
 * Compiler adapter for the JDT Compiler. 
 * 
 * FIXME: should remove this one and use the one provided by jdt.core
 */
public class JDTCompilerAdapter extends DefaultCompilerAdapter {
	private static String compilerClass = "org.eclipse.jdt.internal.compiler.batch.Main";
/**
 * Performs a compile using the JDT batch compiler 
 */
public boolean execute() throws BuildException {
	attributes.log(Policy.bind("info.usingJdtCompiler"), Project.MSG_VERBOSE);
	Commandline cmd = setupJavacCommand();

	try {
		Class c = Class.forName(compilerClass);
		Method compile = c.getMethod("main", new Class[] { String[].class });
		compile.invoke(null, new Object[] { cmd.getArguments()});
	} catch (ClassNotFoundException cnfe) {
		throw new BuildException(Policy.bind("error.missingJDTCompiler"));
	} catch (Exception ex) {
		throw new BuildException(ex);
	}
	return true;
}
/**
 * Performs a compile using the JDT batch compiler 
 */
protected Path getCompileClasspath() {
	includeAntRuntime = false;
	includeJavaRuntime = false;
	Path result = super.getCompileClasspath();
	// add in rt.jar.  We don't want all of the runtime stuff since in VAJ and VAME there is 
	// more on the development-time classpath than we really would like.
	result.addExisting(new Path(null, System.getProperty("java.home") + "/lib/rt.jar"));
	return result;
}
protected Commandline setupJavacCommand() {
	Commandline cmd = new Commandline();
	cmd.createArgument().setValue("-noExit");

	if (deprecation == true)
		cmd.createArgument().setValue("-deprecation");

	if (destDir != null) {
		cmd.createArgument().setValue("-d");
		cmd.createArgument().setFile(destDir.getAbsoluteFile());
	}

	cmd.createArgument().setValue("-classpath");
	cmd.createArgument().setPath(getCompileClasspath());

	if (target != null) {
		cmd.createArgument().setValue("-target");
		cmd.createArgument().setValue(target);
	}

	if (debug)
		cmd.createArgument().setValue("-g");

	if (optimize)
		cmd.createArgument().setValue("-O");

	if (bootclasspath != null) {
		cmd.createArgument().setValue("-bootclasspath");
		cmd.createArgument().setPath(bootclasspath);
	}

	if (extdirs != null) {
		cmd.createArgument().setValue("-extdirs");
		cmd.createArgument().setPath(extdirs);
	}

	if (verbose) {
		cmd.createArgument().setValue("-log");
		cmd.createArgument().setValue(destDir.getAbsolutePath() + ".log");
	}

	if (!attributes.getFailonerror())
		cmd.createArgument().setValue("-proceedOnError");

	cmd.createArgument().setValue("-warn:constructorName,packageDefaultMethod,maskedCatchBlocks,deprecation");
	cmd.createArgument().setValue("-time");
	cmd.createArgument().setValue("-noImportError");
	cmd.createArgument().setValue("-g");

	logAndAddFilesToCompile(cmd);
	return cmd;
}
}
