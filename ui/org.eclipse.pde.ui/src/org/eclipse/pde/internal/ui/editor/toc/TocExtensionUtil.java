package org.eclipse.pde.internal.ui.editor.toc;

import java.util.HashSet;
import java.util.Locale;

import org.eclipse.core.runtime.IPath;

public class TocExtensionUtil {
	public static final String[] pageExtensions = {"htm","shtml","html","xhtml"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public static final String tocExtension = "xml"; //$NON-NLS-1$
	private static HashSet pageExtensionSet = new HashSet(3);

	private static void populateHashSet()
	{	for(int i = 0; i < pageExtensions.length; ++i)
		{	pageExtensionSet.add(pageExtensions[i]);
		}
	}
	
	public static boolean hasValidPageExtension(IPath path)
	{	String fileExtension = path.getFileExtension();	
		if(fileExtension != null)
		{	fileExtension = fileExtension.toLowerCase(Locale.ENGLISH);
			if(pageExtensionSet.isEmpty())
			{	populateHashSet();
			}
			
			return pageExtensionSet.contains(fileExtension);
		}

		return false;
	}

	public static boolean hasValidTocExtension(IPath path)
	{	String fileExtension = path.getFileExtension();
		return fileExtension != null && fileExtension.equals(tocExtension); 
	}
}
