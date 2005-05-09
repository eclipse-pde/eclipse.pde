/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.IModelChangedEvent;

public class ExportPackageObject extends PackageObject {
    
    private static final String INTERNAL = "x-internal"; //$NON-NLS-1$
    private static final String FRIENDS = "x-friends"; //$NON-NLS-1$
    
    private static final long serialVersionUID = 1L;
    
    private boolean fInternal;

    private TreeMap fFriends = new TreeMap();
     
    public ExportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
        fInternal = "true".equals(element.getDirective(INTERNAL)); //$NON-NLS-1$
        processFriends(element.getDirective(FRIENDS));
    }
    
    public ExportPackageObject(ManifestHeader header, IPackageFragment fragment, String versionAttribute) {
        super(header, fragment.getElementName(), null, versionAttribute);
    }
    
    private void processFriends(String value) {
        if (value != null) {
            String[] friends = ManifestElement.getArrayFromList(value);
            for (int i = 0; i < friends.length; i++) {
                PackageFriend friend = new PackageFriend(this, friends[i]);
                fFriends.put(friend.getName(), friend);
            }
        }
    }

    protected void appendSupportedAttributes(StringBuffer buffer) {
       super.appendSupportedAttributes(buffer);
       if (fInternal) {
           buffer.append(";"); //$NON-NLS-1$
           buffer.append(INTERNAL);
           buffer.append(":=true"); //$NON-NLS-1$
       }
       if (fFriends.size() > 0) {
           buffer.append(";"); //$NON-NLS-1$
           buffer.append(FRIENDS);
           buffer.append(":=\""); //$NON-NLS-1$
           Iterator iter = fFriends.keySet().iterator();
           while (iter.hasNext()) {
               buffer.append(iter.next().toString());
               if (iter.hasNext())
                   buffer.append(","); //$NON-NLS-1$
           }
           buffer.append("\""); //$NON-NLS-1$
       }
    }

    public boolean isInternal() {
        return fInternal;
    }

    public void setInternal(boolean internal) {
        boolean old = fInternal;
        fInternal = internal;
        firePropertyChanged(this, INTERNAL, Boolean.valueOf(old), Boolean.valueOf(internal));
    }
    
    public PackageFriend[] getFriends() {
        return (PackageFriend[])fFriends.values().toArray(new PackageFriend[fFriends.size()]);
    }
    
    public void addFriend(PackageFriend friend) {
        fFriends.put(friend.getName(), friend);
        fireStructureChanged(friend, IModelChangedEvent.INSERT);        
    }
    
    public void removeFriend(PackageFriend friend) {
        fFriends.remove(friend.getName());
        fireStructureChanged(friend, IModelChangedEvent.REMOVE);       
    }
    
    public boolean hasFriend(String name) {
        return fFriends.containsKey(name);
    }
    
    protected boolean skipDirective(String directive) {
        return INTERNAL.equals(directive) || FRIENDS.equals(directive);
    }

}
