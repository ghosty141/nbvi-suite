/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.openide.windows;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 * Manages window system.
 * Allows the work with window system components, i.e. <code>Mode</code>s, <code>TopComponentGroup</code>s
 * and provides handling of operations provided over <code>TopComponent</code>s.
 * <p><p>
 * <b><font color="red"><em>Important note: Do not provide implementation of this abstract class unless you are window system provider!</em></font></b>
 *
 * @author Jaroslav Tulach
 */
public abstract class WindowManager extends Object implements Serializable {
    /** property change of workspaces.
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public static final String PROP_WORKSPACES = "workspaces"; // NOI18N

    /** property change of current workspace.
     * @deprecated Do not use. Workspaces are not supported anymore.
     */
    @Deprecated
    public static final String PROP_CURRENT_WORKSPACE = "currentWorkspace"; // NOI18N

    /** Name of property for modes in the workspace.
     * @since 4.13 */
    public static final String PROP_MODES = "modes"; // NOI18N

    /** Instance of dummy window manager. */
    private static WindowManager dummyInstance;
    static final long serialVersionUID = -4133918059009277602L;

    /** The top component which is currently active */
    private Reference<TopComponent> activeComponent = new WeakReference<TopComponent>(null);

    /** the registry */
    private TopComponent.Registry registry;

    /** Singleton instance accessor method for window manager. Provides entry
     * point for further work with window system API of the system.
     *
     * @return instance of window manager installed in the system
     * @since 2.10
     */
    public static final WindowManager getDefault() {
        WindowManager wmInstance = Lookup.getDefault().lookup(WindowManager.class);

        return (wmInstance != null) ? wmInstance : getDummyInstance();
    }

    private static synchronized WindowManager getDummyInstance() {
        if (dummyInstance == null) {
            dummyInstance = new DummyWindowManager();
        }

        return dummyInstance;
    }

    /** Finds mode of specified name.
     * @return <code>Mode</code> whith the specified name is or <code>null</code>
     *          if there does not exist such <code>Mode</code> inside window system.
     * @since 4.13 */
    public abstract Mode findMode(String name);

    /** Finds mode which contains specified <code>TopComponent</code>.
     * @return <code>Mode</code> which contains specified <code>TopComponent</code> or <code>null</code>
     *          if the <code>TopComponent</code> is not added into any <code>Mode</code> inside window system.
     * @since 4.13 */
    public abstract Mode findMode(TopComponent tc);

    /** Gets set of all <code>Mode</code>S added into window system.
     * @since 4.13 */
    public abstract Set<? extends Mode> getModes();

    /**
     * Gets the NetBeans Main Window.
    * This should ONLY be used for:
    * <UL>
    *   <LI>using the Main Window as the parent for dialogs</LI>
    *   <LI>using the Main Window's position for preplacement of windows</LI>
    * </UL>
     * Since version 6.36 the default implementation in org.netbeans.core.windows
     * module first checks already opened Frames (see Frame.getFrames()) and if
     * there is a Frame named 'NbMainWindow' then it is reused as NetBeans main
     * window. Otherwise a new Frame is created instead.
    * @return the Main Window
    */
    public abstract Frame getMainWindow();

    /** Called after a Look&amp;Feel change to update the NetBeans UI.
    * Should call {@link javax.swing.JComponent#updateUI} on all opened windows.
    */
    public abstract void updateUI();

    /** Create a component manager for the given top component.
    * @param c the component
    * @return the manager to handle opening, closing and selecting the component
    */
    protected abstract WindowManager.Component createTopComponentManager(TopComponent c);

    /** Access method for registry of all components in the system.
    * @return the registry
    */
    protected TopComponent.Registry componentRegistry() {
        return Lookup.getDefault().lookup(TopComponent.Registry.class);
    }

    /** Getter for component registry.
    * @return the registry
    */
    public synchronized TopComponent.Registry getRegistry() {
        if (registry != null) {
            return registry;
        }

        registry = componentRegistry();

        return registry;
    }

    /** Creates new workspace.
     * @param name the name of the workspace
     * @return new workspace
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public final Workspace createWorkspace(String name) {
        return createWorkspace(name, name);
    }

    /** Creates new workspace with I18N support.
     * Note that it will not be displayed until {@link #setWorkspaces} is called
     * with an array containing the new workspace.
     * @param name the code name (used for internal purposes)
     * @param displayName the display name
     * @return the new workspace
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public abstract Workspace createWorkspace(String name, String displayName);

    /** Finds workspace given its name.
     * @param name the (code) name of workspace to find
     * @return workspace or null if not found
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public abstract Workspace findWorkspace(String name);

    /**
     * Gets a list of all workspaces.
     * @return an array of all known workspaces
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public abstract Workspace[] getWorkspaces();

    /** Sets new array of workspaces.
     * In conjunction with {@link #getWorkspaces}, this may be used to reorder
     * workspaces, or add or remove workspaces.
     * @param workspaces An array consisting of new workspaces.
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public abstract void setWorkspaces(Workspace[] workspaces);

    /**
     * Gets the current workspace.
     * @return the currently active workspace
     * @see Workspace#activate
     * @deprecated Do not use. Workspaces are not supported anymore. */
    @Deprecated
    public abstract Workspace getCurrentWorkspace();

    /** Finds <code>TopComponentGroup</code> of given name.
     * @return instance of TopComponetnGroup or null
     * @since 4.13 */
    public abstract TopComponentGroup findTopComponentGroup(String name);

    //
    // You can add implementation to this class (+firePropertyChange), or implement it in subclass
    // Do as you want.
    //

    /**
     * Attaches a listener for changes in workspaces.
     * @param l the new listener
     */
    public abstract void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Removes a listener for changes in workspaces.
     * @param l the listener to remove
     */
    public abstract void removePropertyChangeListener(PropertyChangeListener l);

    /** Finds top component manager for given top component.
     * @param tc top component to find manager for.
     * @return component manager for given top component.
     * @deprecated Do not use anymore.
     * See {@link WindowManager.Component} deprecation.
     */
    @Deprecated
    protected static final Component findComponentManager(TopComponent tc) {
        return null;
    }

    /** Activate a component. The top component containers should inform
    * the top component that it is active via a call to this method through
    * derived window manager implementation.
    * @param tc the top component to activate;
    * or <code>null</code> to deactivate all top components
    */
    protected void activateComponent(TopComponent tc) {
        // check
        if (getActiveComponent() == tc) {
            return;
        }

        TopComponent old = getActiveComponent();
        // deactivate old if possible
        if (old != null) {
            try {
                old.componentDeactivated();
            } catch (Throwable th) {
                logThrowable(th, "[Winsys] TopComponent " + old.getClass().getName() // NOI18N
                         +" throws runtime exception from its componentDeactivated() method.\nPlease repair it!"); // NOI18N
            }
        }

        setActiveComponent(tc);
        TopComponent newTC = getActiveComponent();

        if (newTC != null) {
            try {
                newTC.componentActivated();
            } catch (Throwable th) {
                logThrowable(th, "[Winsys] TopComponent " + newTC.getClass().getName() // NOI18N
                         +" throws runtime exception from its componentActivated() method.\nPlease repair it!"); // NOI18N
            }
        }
    }

    /** Notifies component that it was opened (and wasn't opened on any
     * workspace before). Top component manager that implements Component
     * inner interface of this class should send open notifications via
     * calling this method
     * @param tc the top component to be notified
     */
    protected void componentOpenNotify(TopComponent tc) {
        try {
            tc.componentOpened();
        } catch (Throwable th) {
            logThrowable(th, "[Winsys] TopComponent " + tc.getClass().getName() // NOI18N
                 +" throws exception/error from its componentOpened() method.\nPlease repair it!"); // NOI18N
        }
    }

    /** Notifies component that it was closed (and is not opened on any
     * workspace anymore). Top component manager that implements Component
     * inner interface of this class should send close notifications via
     * calling this method
     * @param tc the top component to be notified
     */
    protected void componentCloseNotify(TopComponent tc) {
        try {
            tc.componentClosed();
        } catch (Throwable th) {
            logThrowable(th, "[Winsys] TopComponent " + tc.getClass().getName() // NOI18N
                     +" throws exception/error from its componentClosed() method.\nPlease repair it!"); // NOI18N
        }

        if (tc == getActiveComponent()) {
            activateComponent(null);
        }
    }

    /** Notifies <code>TopComponent</code> it is about to be shown.
     * @param tc <code>TopComponent</code> to be notified
     * @see TopComponent#componentShowing
     * @since 2.18 */
    protected void componentShowing(TopComponent tc) {
        try {
            tc.componentShowing();
        } catch (Throwable th) {
            logThrowable(th, "[Winsys] TopComponent " + tc.getClass().getName() // NOI18N
                    +" throws runtime exception from its componentShowing() method.\nPlease repair it!"); // NOI18N
        }
    }

    /** Notifies <code>TopComponent</code> it was hidden.
     * @param tc <code>TopComponent</code> to be notified
     * @see TopComponent#componentHidden
     * @since 2.18 */
    protected void componentHidden(TopComponent tc) {
        try {
            tc.componentHidden();
        } catch (Throwable th) {
            logThrowable(th, "[Winsys] TopComponent " + tc.getClass().getName() // NOI18N
                    +" throws runtime exception from its componentHidden() method.\nPlease repair it!"); // NOI18N
        }
    }
    
    /** #113158: even errors may come
     * from TopComponent.componentOpened or componentClosed.
     */
    private static void logThrowable (Throwable th, String message) {
        if (th instanceof ThreadDeath || th instanceof OutOfMemoryError) {
            // let us R.I.P. :-)
            throw (Error) th;
        }
        Logger.getLogger(WindowManager.class.getName()).log(Level.WARNING, message, th);
    }

    /** Provides opening of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> to open
     * @since 4.13 */
    protected abstract void topComponentOpen(TopComponent tc);
    
    /** Opens given TopComponent at given position in the mode. TopComponent is inserted at given
     * position, positions of already opened TopComponents in the same mode are
     * incremented.
     * 
     * <ul>
     *    <li>Does no operation if TopComponent is already opened.</li>
     *    <li>For position value less then 0, TopComponent is opened at position 0, the very first one.</li>
     *    <li>For position value greater then count of opened TopComponents in the mode,
     *          TopComponent is opened at last position</li>
     * </ul>
     * 
     * @param tc TopComponent which is opened.  
     * @param position Index of the requested position.
     * @since 6.15
     */
    protected void topComponentOpenAtTabPosition(TopComponent tc, int position) {
        topComponentOpen(tc);
    }
    
    /** Gives position index of given TopComponent in the mode. Result is
     * undefined for closed TopComponents.
     * 
     * @param tc TopComponent for which position is returned. 
     * @return Index of position.
     * @since 6.15
     */
    protected int topComponentGetTabPosition(TopComponent tc) {
        Mode mode = findMode(tc);
        if (mode == null || !topComponentIsOpened(tc)) {
            return -1;
        }
        
        TopComponent[] tcs = mode.getTopComponents();
        for (int i = 0; i < tcs.length; i++) {
            if (tcs[i] == tc) {
                return i;
            }
        }

        return -1;
    }

    /** Provides closing of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> to close
     * @since 4.13 */
    protected abstract void topComponentClose(TopComponent tc);

    /** Provides activation of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> to activate
     * @since 4.13 */
    protected abstract void topComponentRequestActive(TopComponent tc);

    /** Provides selection of specfied <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> to set visible (select)
     * @since 4.13 */
    protected abstract void topComponentRequestVisible(TopComponent tc);

    /** Informs about change of display name of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> which display name has changed
     * @param displayName newly changed display name value
     * @since 4.13 */
    protected abstract void topComponentDisplayNameChanged(TopComponent tc, String displayName);
    
    /** Informs about change of html display name of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> which display name has changed
     * @param htmlDisplayName newly changed html display name value
     * @since 6.4 */
    protected abstract void topComponentHtmlDisplayNameChanged(TopComponent tc, String htmlDisplayName);

    /** Informs about change of tooltip of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> which tooltip has changed
     * @param toolTip newly changed tooltip value
     * @since 4.13 */
    protected abstract void topComponentToolTipChanged(TopComponent tc, String toolTip);

    /** Informs about chagne of icon of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> which icon has changed
     * @param icon newly chaned icon value
     * @since 4.13 */
    protected abstract void topComponentIconChanged(TopComponent tc, Image icon);

    /** Informs about change of activated nodes of specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> which activated nodes has chagned
     * @param activatedNodes newly chaged activated nodes value
     * @since 4.13 */
    protected abstract void topComponentActivatedNodesChanged(TopComponent tc, Node[] activatedNodes);

    /** Indicates whether specified <code>TopComponent</code> is opened.
     * @param tc specified <code>TopComponent</code>
     * @since 4.13 */
    protected abstract boolean topComponentIsOpened(TopComponent tc);

    /** Gets default list of actions which appear in popup menu of TopComponent.
     * The popup menu which is handled by window systsm implementation, typically at tab.
     * @param tc <code>TopComponent</code> for which the default actions to provide
     * @since 4.13 */
    protected abstract javax.swing.Action[] topComponentDefaultActions(TopComponent tc);

    /** Returns unique ID for specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> the component for which is ID returned
     * @param preferredID first approximation used for ID
     * @return unique <code>TopComponent</code> ID
     * @since 4.13 */
    protected abstract String topComponentID(TopComponent tc, String preferredID);

    /**
     * Cause this TopComponent's tab to flash or otherwise draw the users' attention
     * to it.
     * Note to WindowManager providers: This method not abstract for backward compatibility reasons,
     * please override and provide implementation.
     * @param tc A TopComponent
     * @since 5.1 */
    protected void topComponentRequestAttention(TopComponent tc) {
    }

    /**
     * Attempts to bring the parent <code>Window</code> of the given <code>TopComponent</code>
     * to front of other windows.
     * @see java.awt.Window#toFront()
     * @since 5.8
     */
    protected void topComponentToFront(TopComponent tc) {
        Window parentWindow = SwingUtilities.getWindowAncestor(tc);

        // be defensive, although w probably will always be non-null here
        if (null != parentWindow) {
            if (parentWindow instanceof Frame) {
                Frame parentFrame = (Frame) parentWindow;
                int state = parentFrame.getExtendedState();

                if ((state & Frame.ICONIFIED) > 0) {
                    parentFrame.setExtendedState(state & ~Frame.ICONIFIED);
                }
            }

            parentWindow.toFront();
        }
    }

    /**
     * Stop this TopComponent's tab from flashing if it is flashing.
     * Note to WindowManager providers: This method not abstract for backward compatibility reasons,
     * please override and provide implementation.
     *
     * @param tc A TopComponent
     * @since 5.1 */
    protected void topComponentCancelRequestAttention(TopComponent tc) {
    }

    /** Returns unique ID for specified <code>TopComponent</code>.
     * @param tc <code>TopComponent</code> the component for which is ID returned
     * @return unique <code>TopComponent</code> ID
     * @since 4.13 */
    public String findTopComponentID(TopComponent tc) {
        return topComponentID(tc, tc.preferredID());
    }

    /** Returns <code>TopComponent</code> for given unique ID.
     * @param tcID unique <code>TopComponent</code> ID
     * @return <code>TopComponent</code> instance corresponding to unique ID
     * @since 4.15 */
    public abstract TopComponent findTopComponent(String tcID);
    
    /** Provides support for executing a piece of code when UI of the window
     * system is ready. 
     * The behaviour is similar to {@link EventQueue#invokeLater}
     * moreover it is guaranteed that only one Runnable runs at given time.
     * This method can be invoked from any thread.
     *
     * <p class="non-normative">
     * The typical usecase is to call this method during startup of NetBeans
     * based application. The default manager then waits till the main window
     * is opened and then executes all the registered methods one by one.
     * </p>
     * 
     * <b>Usage:</b>
     * <pre>
     *  // some initialization method
     *  public static void init () {
     *     WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
     *        public void run() {
     *           // code to be invoked when system UI is ready
     *        }
     *     );
     *  }
     * </pre>
     * 
     * Note to WindowManager providers: This method is not abstract for backward compatibility reasons,
     * please override and provide implementation.
     * 
     * @param run the runnable that executes piece of code when UI of the system is ready
     * @since 6.8
     */
    public void invokeWhenUIReady(Runnable run) {
        EventQueue.invokeLater(run);
    }
    
    /**
     * <p>Check whether the given TopComponent will be/is docked into an 'editor' Mode.</p>
     * <p>Please note that some TopComponents may be docked into 'editor' modes as well as 
     * 'view' modes, see method isTopComponentAllowedToMoveAnywhere().</p>
     * 
     * @param tc TopComponent to check.
     * @return True if there is a Mode that the TopComponent will be/is docked to and
     * the Mode is of 'editor' kind (i.e. holds editor windows).
     * @since 6.13
     */
    public boolean isEditorTopComponent( TopComponent tc ) {
        return false;
    }
    
    /**
     * <p>Check whether the given TopComponent is opened and docked into an 'editor' Mode. 
     * It is safe to call this method outside the event dispatch thread.</p>
     * <p>Please note that some TopComponents may be docked into 'editor' modes as well as 
     * 'view' modes, see method isTopComponentAllowedToMoveAnywhere().</p>
     * 
     * @param tc TopComponent to check.
     * @return True if the TopComponent is opened and the Mode it is docked 
     * is of 'editor' kind (i.e. holds editor windows).
     * @since 6.16
     */
    public boolean isOpenedEditorTopComponent( TopComponent tc ) {
        return false;
    }

    /**
     * Convenience method to retrieve the list of all opened TopComponents for
     * given mode.
     * @param mode Mode to get the list of TopComponents from.
     * @return Array of TopComponents that are opened in given mode.
     * @since 6.28
     */
    public TopComponent[] getOpenedTopComponents( Mode mode ) {
        TopComponent[] allTcs = mode.getTopComponents();
        List<TopComponent> openedTcs = new ArrayList<TopComponent>(allTcs.length);
        for( TopComponent tc : allTcs ) {
            if( tc.isOpened() ) {
                openedTcs.add(tc);
            }
        }
        return openedTcs.toArray(new TopComponent[openedTcs.size()]);
    }

    /**
     * <p>Check whether the given Mode holds editor windows.</p>
     * <p>Please note that some TopComponents may be docked into 'editor' modes as well as 
     * 'view' modes, see method isTopComponentAllowedToMoveAnywhere().</p>
     * 
     * @param mode Mode to check.
     * @return True the Mode contains editor windows.
     * @since 6.13
     */
    public boolean isEditorMode( Mode mode ) {
        return false;
    }

    private TopComponent getActiveComponent() {
        return activeComponent.get();
    }

    private void setActiveComponent(TopComponent activeComponent) {
        this.activeComponent = new WeakReference<TopComponent>(activeComponent);
    }
    
    /** A manager that handles operations on top components.
     * It is always attached to a {@link TopComponent}.
     * @deprecated Do not use anymore. This interface is replaced by bunch of protected methods
     * which name starts with topComponent prefix, i.e. {@link #topComponentOpen}, {@link #topComponentClose} etc. */
    @SuppressWarnings("deprecation")
    @Deprecated
    protected interface Component extends java.io.Serializable {
        /**
         * Do not use.
         * @deprecated Only public by accident.
         */
        @Deprecated
        /* public static final */ long serialVersionUID = 0L;

        /** Open the component on current workspace */
        public void open();

        /**
         * Opens this component on a given workspace.
         * @param workspace the workspace on which to open it
         */
        public void open(Workspace workspace);

        /**
         * Closes this component on a given workspace.
         * @param workspace the workspace on which to close it
         */
        public void close(Workspace workspace);

        /** Called when the component requests focus. Moves it to be visible.
        */
        public void requestFocus();

        /** Set this component visible but not selected or focused if possible.
        * If focus is in other container (multitab) or other pane (split) in
        * the same container it makes this component only visible eg. it selects
        * tab with this component.
        * If focus is in the same container (multitab) or in the same pane (split)
        * it has the same effect as requestFocus().
        */
        public void requestVisible();

        /** Get the set of activated nodes.
        * @return currently activated nodes for this component
        */
        public Node[] getActivatedNodes();

        /** Set the set of activated nodes for this component.
        * @param nodes new set of activated nodes
        */
        public void setActivatedNodes(Node[] nodes);

        /** Called when the name of the top component changes.
        */
        public void nameChanged();

        /** Set the icon of the top component.
        * @param icon the new icon
        */
        public void setIcon(final Image icon);

        /**
         * Gets the icon associated with this component.
         * @return the icon
         */
        public Image getIcon();

        /**
         * Gets a list of workspaces where this component is currently open.
         * @return the set of workspaces where the managed component is open
         */
        public Set<Workspace> whereOpened();
    }
    //////////////////////////////////////////////////////////////////////
    //
    // REVIEWERS:
    //        - For side, could have enum WindowManager.Side
    //          (then have Orientation in WindowManager instead of SiblingState
    //
    //        - The WeightCalculator is removed from these addMode* API
    //          to simplify implementation.
    //          It can be put back and handled here,
    //          then performance optimized at a later date.
    //
    //        - The original proposal returned the new mode, but that isn't
    //          a pattern I see around. Should it return a mode?
    //
    //////////////////////////////////////////////////////////////////////

    /**
     * Add a new Mode, with a TopComponent in it, on a side of the specified
     * mode. Constants from JSplitPane are used to specify the side:
     * TOP, BOTTOM, LEFT, RIGHT.
     *
     * @param mode the new mode is next to this mode
     * @param side which side to attach the new mode.
     * @param tc TopComponent to put into the new mode.
     */
    abstract public void addModeOnSide(Mode mode, String side, TopComponent tc);

    /**
     * Add a new Mode, with a TopComponent in it, that touches the side
     * of the specified mode. The new mode is either wider or higher
     * than the mode.
     * Constants from JSplitPane are used to specify the side:
     * TOP, BOTTOM, LEFT, RIGHT.
     * When mode's container's orientation is the same as
     * the specified side, this method does the same thing as addModeOnSide.
     *
     * @param mode
     * @param side
     * @param tc
     */
    abstract public void addModeAround(Mode mode, String side, TopComponent tc);

    //////////////////////////////////////////////////////////////////////
    //
    // REVIEWERS:
    //          The restriction that the specified mode's weight must
    //          be changed can be relaxed so that a modification to any
    //          weight will trigger a change.
    //
    //          To do that is trivial if in
    //              ViewRequest(null, View.CHANGE_MODE_CONSTRAINTS_CHANGED,
    //                  oldConstraints, newConstraints)
    //          it doesn't matter if oldContstraints == newConstraints. Or
    //          if oldConstraints can simply be set to null.
    //          If the type of ViewRequest requires accurate old/new then
    //          the restriction can still be relaxed by keeping track
    //          of the constraints for each of the modes.
    //
    //////////////////////////////////////////////////////////////////////

    /**
     * Change the sizing weights of a mode, and optionally its siblings,
     * within a splitter. If the weight for the specified mode is not
     * changed then no action takes place even if sibling weights are
     * changed.
     * @param mode
     * @param wc provides new size informaiton
     * @return true if the sizes changed
     */
    abstract public boolean adjustSizes(Mode mode, WeightCalculator wc);

    /**
     * Used with the adjustSizes method to provide new weights.
     */
    public interface WeightCalculator {
        /**
         * Invoked by the window system to get the new weights.
         * @param currentState splitter and sibling information
         * @return new weights
         */
        List<Double> getWeights(SiblingState currentState);
    }

    /**
     * Information about a splitter's children.
     */
    public interface SiblingState {
        enum Orientation { HORIZONTAL, VERTICAL; }
        /**
         * Splitter orientation.
         * @return the splitter orientation
         */
        Orientation getOrientation();
        /**
         * @return the index of the mode to change
         */
        int getTargetIndex();
        /**
         * The current weights of the splitter's children, either top to bottom
         * or left to right depending on orientation. The returned list
         * can be modified.
         * @return current weights
         */
        List<Double> getWeights();
    }
}
