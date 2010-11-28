/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is jvi - vi editor clone.
 *
 * The Initial Developer of the Original Code is Ernie Rael.
 * Portions created by Ernie Rael are
 * Copyright (C) 2000-2010 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */

package org.netbeans.modules.jvi.completion;

import com.raelity.jvi.core.ColonCommands;
import com.raelity.jvi.core.Options;
import com.raelity.jvi.options.DebugOption;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.ErrorManager;

/**
 * Command Name completion after ":"
 * 
 * @author Ernie Rael <err at raelity.com>
 */
public class CommandNameTask implements CompletionTask
{
    private static DebugOption dbgCompl;

    JTextComponent jtc;
    List<CommandNameItem> query;

    // These variables are used by CommandNameItem
    private int startOffset;

    public CommandNameTask(JTextComponent jtc)
    {
        this.jtc = jtc;
        dbgCompl = (DebugOption)Options.getOption(Options.dbgCompletion);
    }

    @Override
    public void query(CompletionResultSet resultSet)
    {
        if(CcCompletion.isEditAlternate(jtc.getDocument())) {
            resultSet.finish();
            return;
        }
        buildQueryResult();
        filterResult(resultSet, "QUERY CN");
    }

    @Override
    public void refresh(CompletionResultSet resultSet)
    {
        if(resultSet == null) {
            dbgCompl.println("REFRESH CN with null resultSet");
            return;
        }
        if(CcCompletion.isEditAlternate(jtc.getDocument())) {
            resultSet.finish();
            return;
        }
        if(query == null)
            buildQueryResult();
        filterResult(resultSet, "REFRESH CN");
    }

    @Override
    public void cancel()
    {
        dbgCompl.println("CANCEL CN:");
        Completion.get().hideAll();
    }

    private void buildQueryResult()
    {
        List<String> names = ColonCommands.getNameList();
        List<String> abrevs = ColonCommands.getAbrevList();

        query = new ArrayList<CommandNameItem>();
        startOffset = 0;
        for(int i = 0; i < names.size(); i++) {
            query.add(new CommandNameItem(names.get(i), abrevs.get(i)));
        }
    }

    private void filterResult(CompletionResultSet resultSet, String tag)
    {
        String dbsString = "";
        try {
            Document doc = jtc.getDocument();
            String text = doc.getText(0, doc.getLength());
            if (dbgCompl.getBoolean())
                dbsString = tag + ": \'" + text + "\'";
            int off = 0;
            int caretOffset = text.length();
            // skip white space
            for (; off < caretOffset; off++) {
                if (!Character.isWhitespace(text.charAt(off)))
                    break;
            }
            String filter = text.substring(off, caretOffset);
            if (dbgCompl.getBoolean())
                dbsString += ", filter \'" + filter + "\'";
            resultSet.setAnchorOffset(off);
            for (CommandNameItem item : query) {
                String checkItem = item.name;
                if (filter.regionMatches(true, 0,
                                         item.name, 0, filter.length())) {
                    resultSet.addItem(item);
                }
            }
        } catch (BadLocationException ex) {
        }
        if (dbgCompl.getBoolean()) {
            dbsString += ", result: " + resultSet;
            System.err.println(dbsString);
        }
        resultSet.finish();
    }

    private class CommandNameItem implements CompletionItem
    {
        private String name;
        private String abrev;
        private String nameLabel;

        CommandNameItem(String name, String abrev)
        {
            this.name = name;
            this.abrev = abrev;
            StringBuilder sb = new StringBuilder();
            nameLabel = "<html>"
                        //+ "&nbsp;&nbsp;"
                        + "<b>"
                        + abrev
                        + "</b>"
                        + name.substring(abrev.length())
                        + "</html>";
        }

        @Override
        public void defaultAction(JTextComponent jtc)
        {
            if (dbgCompl.getBoolean())
                System.err.println("DEFAULT ACTION CN: \'" + name + "\'");
            try {
                CcCompletion.ceInSubstitute = true;
                doSubstitute(jtc);
            } finally {
                CcCompletion.ceInSubstitute = false;
            }
            Completion.get().hideAll();
            //
            // NEEDSWORK: only execute if no arguments
            //
            ///// // Go for it
            ///// Action act = jtc.getKeymap().getAction(CommandLine.EXECUTE_KEY);
            ///// if (act != null)
            /////     act.actionPerformed(new ActionEvent(jtc,
            /////                                         ActionEvent.ACTION_PERFORMED,
            /////                                         "\n"));
        }

        private void doSubstitute(JTextComponent jtc)
        {
            Document doc = jtc.getDocument();
            int caretOffset = doc.getLength(); // clear to end of line
            String value = name;
            try {
                doc.remove(startOffset, caretOffset - startOffset);
                doc.insertString(startOffset, value, null);
                jtc.setCaretPosition(startOffset + value.length());
            } catch (BadLocationException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }

        @Override
        public void processKeyEvent(KeyEvent evt)
        {
            if (dbgCompl.getBoolean())
                System.err.println("ViCompletionItem CN: \'" + name + "\' " +
                        evt.paramString());
            if (evt.getID() == KeyEvent.KEY_PRESSED &&
                    evt.getKeyChar() == KeyEvent.VK_TAB) {
                // The logic in CompletionImpl that does getInsertPrefix
                // sets caretOffset from selection start. So if "e#n" is
                // selected and tab is entered, then caretOffset gets set to 0
                // and anchorOffset is 2 (as always) this ends up with a "-2"
                // -2 used in a String.subsequence....
                // If TAB, get rid of selection and position at end of text
                JTextComponent jtc = (JTextComponent)evt.getSource();
                jtc.setCaretPosition(jtc.getDocument().getLength());
            }
            ///// if (evt.getKeyCode() == KeyEvent.VK_DOWN ||
            /////         evt.getKeyCode() == KeyEvent.VK_UP)
            /////     hack++;
            if (evt.getID() == KeyEvent.KEY_TYPED &&
                    evt.getKeyChar() == KeyEvent.VK_TAB)
                evt.consume();
        }

        @Override
        public int getPreferredWidth(Graphics g, Font font)
        {
            return CompletionUtilities.getPreferredWidth(nameLabel, null,
                                                         g, font);
        }

        @Override
        public void render(Graphics g, Font defaultFont, Color defaultColor,
                           Color backgroundColor, int width, int height,
                           boolean selected)
        {
            if (dbgCompl.getBoolean(Level.FINER))
                System.err.println("RENDER CN: \'" + name + "\', selected " +
                        selected);
            Graphics2D g2 = (Graphics2D)g;
            CompletionUtilities.renderHtml(
                    null, nameLabel, null,
                    g,
                    defaultFont,
                    //selected ? Color.white : CommandNameItem.fieldColor,
                    defaultColor,
                    width, height, selected);
        }

        @Override
        public CompletionTask createDocumentationTask()
        {
            return null;
        }

        @Override
        public CompletionTask createToolTipTask()
        {
            return null;
        }

        @Override
        public boolean instantSubstitution(JTextComponent component)
        {
            return false;
        }

        @Override
        public int getSortPriority()
        {
            return 0;
        }

        @Override
        public CharSequence getSortText()
        {
            return true ? abrev : name;
        }

        @Override
        public CharSequence getInsertPrefix()
        {
            return name.toLowerCase();
        }
    }

}
