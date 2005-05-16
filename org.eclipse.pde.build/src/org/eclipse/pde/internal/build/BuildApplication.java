package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.*;

public class BuildApplication implements IPlatformRunnable {

	public Object run(Object args) throws Exception {
		Platform.endSplash();
		IPlatformRunnable antRunner = getAntRunner();
		args = updateArgs((String[]) args);
		return antRunner.run(args);
	}

	private Object updateArgs(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			String string = args[i];
			if (string.equals("-f") || string.equals("-buildfile")) //$NON-NLS-1$ //$NON-NLS-2$
				return args;
		}
		int length = args.length;
		String[] result = new String[length + 2];
		System.arraycopy(args, 0, result, 0, length);
		result[length] = "-f"; //$NON-NLS-1$
		URL buildURL = BundleHelper.getDefault().find(new Path("/scripts/build.xml")); //$NON-NLS-1$
		result[length + 1] = Platform.asLocalURL(buildURL).getFile();
		return result;
	}

	private IPlatformRunnable getAntRunner() throws CoreException {
		IExtension ext = Platform.getExtensionRegistry().getExtension("org.eclipse.ant.core.antRunner"); //$NON-NLS-1$
		if (ext == null)
			return null;
		IConfigurationElement element = ext.getConfigurationElements()[0];
		return (IPlatformRunnable) element.createExecutableExtension("run"); //$NON-NLS-1$
	}
}
