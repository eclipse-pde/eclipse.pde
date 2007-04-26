/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.jarprocessor;

import java.io.File;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.update.internal.jarprocessor.Main.Options;

/**
 * This task provides massaging facilities for jar files.
 * It supports: signing, unsigning, normalization, packing
 * 	- 
 */
public class JarProcessorTask extends Task {
	private Options options = new Options();
	private Properties signArgs = new Properties();

	public static final String ALIAS = "alias"; //$NON-NLS-1$
	public static final String KEYSTORE = "keystore"; //$NON-NLS-1$
	public static final String STOREPASS = "storepass"; //$NON-NLS-1$
	public static final String UNSIGN = "unsign"; //$NON-NLS-1$
	public static final String SIGN = "sign"; //$NON-NLS-1$

	private static final String FAKE_COMMAND = "fake"; //$NON-NLS-1$

	public void setAlias(String alias) {
		signArgs.setProperty(ALIAS, alias);
	}

	public void setKeystore(String keystore) {
		signArgs.setProperty(KEYSTORE, keystore);
	}

	public void setJar(File jar) {
		options.input = jar;
		options.outputDir = jar.getParentFile().getAbsolutePath();
	}

	public void setStorepass(String storepass) {
		signArgs.setProperty(STOREPASS, storepass);
	}

	public void setPack(boolean pack) {
		options.pack = pack;
	}

	public void setNormalize(boolean normalize) {
		options.repack = normalize;
	}

	public void setUnsign(boolean unsign) {
		if (unsign) {
			signArgs.put(UNSIGN, Boolean.TRUE.toString());
			options.signCommand = FAKE_COMMAND;
		}
	}

	public void setSign(boolean sign) {
		if (sign) {
			signArgs.put(SIGN, Boolean.TRUE.toString());
			options.signCommand = FAKE_COMMAND;
		}
	}

	private void adjustAndValidateConfiguration() {
		//Sign and pack implies a normalization
		if (options.signCommand != null && options.pack)
			options.repack = true;

		//Check that alias, and storepass are set
		if (options.signCommand != null && signArgs.getProperty(UNSIGN) == null) {
			if (signArgs.getProperty(ALIAS) == null)
				throw new BuildException("Alias must be set"); //$NON-NLS-1$

			if (signArgs.getProperty(STOREPASS) == null)
				throw new BuildException("Storepass must be set"); //$NON-NLS-1$
		}
	}

	public void execute() {
		options.processAll = true;
		adjustAndValidateConfiguration();
		new AntBasedProcessorExecutor(signArgs, getProject(), getTaskName()).runJarProcessor(options);
	}

	public void setVerbose(boolean verbose) {
		options.verbose = verbose;
	}
}
