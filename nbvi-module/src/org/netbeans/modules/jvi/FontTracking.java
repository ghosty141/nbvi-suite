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
 * Copyright (C) 2011 Ernie Rael.  All Rights Reserved.
 *
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package org.netbeans.modules.jvi;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.EditorStyleConstants;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.settings.storage.api.EditorSettings;
import org.netbeans.modules.editor.settings.storage.api.FontColorSettingsFactory;

/**
 * Check all fonts for the editor.
 *
 * NEEDSWORK: multiple mime-types for a single JEP
 *
 * options.editor/src/org/netbeans/modules/options/
 *              colors/SyntaxColoringPanel.java
 *
 * @author Ernie Rael <err at raelity.com>
 */
final class FontTracking {
    private static final Logger LOG = Logger.getLogger(FontTracking.class.getName());

    private static enum CATS { MIME, ALL }
    private static enum Style {
        Family(StyleConstants.FontFamily),
        Size(StyleConstants.FontSize),
        Bold(StyleConstants.Bold),
        Italic(StyleConstants.Italic);
        private Object key;
        private Style(Object key) {
            this.key = key;
        }
    }
    private final JEditorPane jep;
    private final Map<WH, Set<FontTrack>> sizeMap
            = new HashMap<WH, Set<FontTrack>>();
    private final Set<AttributeSet> vari = new HashSet<AttributeSet>();
    // following could be made final if move the init code into constructor
    private CATS currentLanguage;
    private String currentProfile;
    private MimePath mimePath;
    private List<AttributeSet> categoriesMime;
    private List<AttributeSet> categoriesAllLanguages;

    // following used while create a font
    private FontParams curFontParams;
    private ParamData curParamSource;

    //private Collection<AttributeSet> categoriesDefaults;

    private FontTracking(JEditorPane jep)
    {
        this.jep = jep;
    }

    static boolean check(JEditorPane jep)
    {
        if(jep.getGraphics() == null)
            return false;
        FontTracking ft = new FontTracking(jep);
        ft.init();
        ft.fontCheck();
        return true;
    }

    private void init()
    {
        EditorSettings es = EditorSettings.getDefault();
        currentProfile = es.getCurrentKeyMapProfile();
        currentLanguage = CATS.MIME;

        // Get the categories for the mimetype(s) of the JEP
        mimePath = MimePath.parse(NbEditorUtilities.getMimeType(jep));
        String [] mimeTypes = new String[mimePath.size()];
        for(int i = 0; i < mimeTypes.length; i++) {
            mimeTypes[i] = mimePath.getMimeType(i);
        }
        FontColorSettingsFactory fcs = es.getFontColorSettings(mimeTypes);
        categoriesMime = new ArrayList<AttributeSet>(fcs.getAllFontColors(currentProfile));

        // All Languages
        categoriesAllLanguages = new ArrayList<AttributeSet>(
                es.getFontColorSettings(new String[] {""})
                  .getAllFontColors(currentProfile));
    }

    private void fontCheck()
    {
        // dump("All languages", categoriesAllLanguages);
        // dump(mimePath.toString(), categoriesMime);
        dumpSource("all categories resolve source", categoriesMime);
        System.err.println("");
        System.err.println("");

        Object o = EditorStyleConstants.DisplayName;
        //
        boolean error = false;
        curFontParams = null;
        for(AttributeSet c : categoriesMime) {
            Font f = getFont(c);
            FontTrack ft = new FontTrack(c, f);
            if(!ft.isFixed) {
                System.err.println("");
                vari.add(c);
                continue;
            }
            if(!sizeMap.containsKey(ft.wh)) {
                sizeMap.put(ft.wh, new HashSet<FontTrack>());
            }
            Set<FontTrack> s = sizeMap.get(ft.wh);
            s.add(ft);
        }
        // if there is anything in "vari" or there's more
        // than one size, then there's a problem.
        //
        // Where there are problems, determine the base of the resolve tree
        // and weed out duplicates
        //
        StringBuilder sb = new StringBuilder();
        FontParams fp;
        Font f;
        sb.setLength(0);
        if(!vari.isEmpty()) {
            Set<ParamSource> roots = new HashSet<ParamSource>();
            for(AttributeSet c : vari) {
                fp = new FontParams();
                f = getFont(c, fp);
                roots.add(fp.get(Style.Family).ps);
            }
            sb.append("The following have a variable size font:\n");
            for(ParamSource ps : roots) {
                sb.append("    ");
                dump(sb, ps).append('\n');
            }
        }
        if(sizeMap.size() > 1) {
            Set<FontSizeParams> roots = new HashSet<FontSizeParams>();
            // determine which entry has the most, use it as the base
            WH wh01 = null;
            FontTrack ft;
            Set<FontTrack> ftSet;
            int count = 0;
            for(WH wh : sizeMap.keySet()) {
                ftSet = sizeMap.get(wh);
                if(ftSet.size() > count) {
                    count = ftSet.size();
                    wh01 = wh;
                }
            }
            //
            ftSet = sizeMap.get(wh01);
            ft = ftSet.iterator().next();
            fp = new FontParams();
            f = getFont(getCategory(CATS.MIME, ft.nameAttr), fp);
            sb.append("Assuming base size of ");
            dumpSize(sb, f, wh01).append('\n');
            roots.clear();
            for(WH wh : sizeMap.keySet()) {
                if(wh.equals(wh01))
                    continue;
                ftSet = sizeMap.get(wh);
                for(FontTrack ft01 : ftSet) {
                    fp = new FontParams();
                    f = getFont(getCategory(CATS.MIME, ft01.nameAttr), fp);
                    roots.add(new FontSizeParams(fp.get(Style.Family),
                                                 fp.get(Style.Size)));
                }
            }
            sb.append("The following generate a different size font:\n");
            for(FontSizeParams fsp : roots) {
                sb.append("\n  = ");
                sb.append("Family ");
                sb.append(String.format("%-20s", fsp.family.val));
                dump(sb, fsp.family.ps).append(" ");
                dumpSize(sb, fsp.family.ps, wh01).append('\n');
                sb.append("    ");
                sb.append("  Size ");
                sb.append(String.format("%-20s", fsp.size.val));
                dump(sb, fsp.size.ps).append(" ");
                dumpSize(sb, fsp.size.ps, wh01).append('\n');
            }
            // for(ParamSource ps : roots) {
            //     sb.append("    ");
            //     dump(sb, ps).append(" ");
            //     dumpSize(sb, ps).append('\n');
            // }
        }
        if(sb.length() != 0) {
            System.err.println(sb.toString());
        }
    }

    private void dump(String tag, Collection<AttributeSet> attrs)
    {
        FontParams fp = null;
        System.err.println("========== " + tag + " ==========");
        for(AttributeSet c : attrs) {
            System.err.println("    "
                    + c.getAttribute(StyleConstants.NameAttribute)
                    + "  " + getFont(c, fp));
            Enumeration<?> attributeNames = c.getAttributeNames();
            while(attributeNames.hasMoreElements()) {
                Object o = attributeNames.nextElement();
                System.err.println("        " + o
                        + ":" + c.getAttribute(o));
            }
        }
    }

    private void dumpSource(String tag, Collection<AttributeSet> attrs)
    {
        StringBuilder sb = new StringBuilder();
        System.err.println("========== " + tag + " ==========");
        for(AttributeSet c : attrs) {
            FontParams fp = new FontParams();
            Font f = getFont(c, fp);
            FontTrack ft = new FontTrack(c, f);
            sb.setLength(0);
            System.err.println(String.format("%-35s %s %s",
                    c.getAttribute(StyleConstants.NameAttribute),
                    dumpSize(sb, f, ft.wh).toString(), f));
            sb.setLength(0);
            dump(sb, fp);
            System.err.print(sb.toString());
        }
    }

    private void dump(StringBuilder sb, FontParams fp)
    {
        for(Style style : Style.values()) {
            dump(sb, fp, style);
        }
    }

    private StringBuilder dump(StringBuilder sb, ParamSource ps)
    {
        String name;
        String lang;
        if(ps.cats == null && ps.categoryName == null) {
            name = "none";
            lang = "none";
        } else {
            name = (String)getCategory(ps.cats, ps.categoryName)
                    .getAttribute(EditorStyleConstants.DisplayName);
            lang = ps.cats == CATS.MIME ? mimePath.toString() : "AllLanguages";
        }
        sb.append(String.format("%30s/%s", name, lang));
        return sb;
    }

    private StringBuilder dumpSize(StringBuilder sb, ParamSource ps)
    {
        return dumpSize(sb, ps, null);
    }

    private StringBuilder dumpSize(StringBuilder sb, Font f, WH wh)
    {
        return dumpSize(sb, f, wh, null);
    }

    private StringBuilder dumpSize(StringBuilder sb, ParamSource ps, WH expect)
    {
        AttributeSet c = getCategory(ps.cats, ps.categoryName);
        Font f = getFont(c);
        FontTrack ft = new FontTrack(c, f);
        return dumpSize(sb, f, ft.wh, expect);
    }

    private StringBuilder dumpSize(StringBuilder sb, Font f, WH wh, WH expect)
    {
        sb.append(f.getSize()).append(" (fm=")
                .append(wh.w).append('/').append(wh.h).append(')');
        if(expect != null && !wh.equals(expect))
            sb.append(" ***");
        return sb;
    }

    private void dump(StringBuilder sb, FontParams fp, Style s)
    {
        ParamData pd = fp.get(s);
        String sKey = s.toString();

        sb.append("    ");
        sb.append(String.format("%-10s %-10s ", sKey, pd.val));
        dump(sb, pd.ps);
        if(pd.name != null) {
            // resolved by getDefaults Defaults
            sb.append(" (getDefaults ").append(pd.name).append(')');
        }
        sb.append('\n');
    }

    private Font getFont (AttributeSet category, FontParams fp) {
        curFontParams = fp;
        try {
            return getFont(category);
        } finally {
            curFontParams = null;
        }
    }

    private Font getFont (AttributeSet category) {
        String name = (String) getValue (currentLanguage, category, Style.Family);
        if (name == null) {
            name = "Monospaced";
            addFontParam(Style.Family, null, null, name);
        }                        // NOI18N
        Integer size = (Integer) getValue (currentLanguage, category, Style.Size);
        if (size == null) {
            size = getDefaultFontSize ();
            addFontParam(Style.Size, null, null, size);
        }
        Boolean bold = (Boolean) getValue (currentLanguage, category, Style.Bold);
        if (bold == null) {
            bold = Boolean.FALSE;
            addFontParam(Style.Bold, null, null, bold);
        }
        Boolean italic = (Boolean) getValue (currentLanguage, category, Style.Italic);
        if (italic == null) {
            italic = Boolean.FALSE;
            addFontParam(Style.Italic, null, null, italic);
        }
        int style = bold.booleanValue () ? Font.BOLD : Font.PLAIN;
        if (italic.booleanValue ()) style += Font.ITALIC;
        return new Font (name, style, size.intValue ());
    }

    private Object getValue (CATS cats, AttributeSet category, Style s) {
        if (category.isDefined (s.key)) {
            Object value = category.getAttribute (s.key);
            addFontParam(s, cats, category, value);
            return value;
        }
        return getDefault (cats, category, s);
    }

    private Object getDefault (CATS cats, AttributeSet category, Style s) {
	String name = (String) category.getAttribute (EditorStyleConstants.Default);
	if (name == null) name = "default";

	// 1) search current language
        if (!name.equals (category.getAttribute (StyleConstants.NameAttribute))
                ) {
            AttributeSet defaultAS = getCategory(cats, name);
            if (defaultAS != null)
                return getValue (cats, defaultAS, s);
        }

        // 2) search default language
        if(cats != CATS.ALL) {
            AttributeSet defaultAS = getCategory(CATS.ALL, name);
            if (defaultAS != null)
                return getValue(CATS.ALL, defaultAS, s);
        }

        Object value = null;
        if (s == Style.Family) {
            value = "Monospaced"; // NOI18N
        } else if (s == Style.Size) {
            value = getDefaultFontSize ();
        }
        if(value != null)
            addFontParam(s, cats, category, value, name);
        return value;
    }

    private static Integer defaultFontSize;
    private static Integer getDefaultFontSize () {
        if (defaultFontSize == null) {
            defaultFontSize = (Integer) UIManager.get("customFontSize"); // NOI18N
            if (defaultFontSize == null) {
                int s = UIManager.getFont ("TextField.font").getSize (); // NOI18N
                if (s < 12) s = 12;
                defaultFontSize = new Integer (s);
            }
        }
        return defaultFontSize;
    }

    private List<AttributeSet> getCategories(CATS cats)
    {
        return cats == CATS.MIME ? categoriesMime : categoriesAllLanguages;
    }

    // NEEDSWORK: make map
    private AttributeSet getCategory (CATS cats, String name) {
        for(AttributeSet c : getCategories(cats)) {
            if (c.getAttribute (StyleConstants.NameAttribute).equals (name))
                return c;
        }
        return null;
    }

    private void addFontParam(Style s, CATS cats,
                              AttributeSet cat, Object val)
    {
        if(curFontParams == null)
            return;
        curFontParams.put(s, new ParamData(cats, cat, val, null));
    }

    private void addFontParam(Style s, CATS cats,
                              AttributeSet cat, Object val, String name)
    {
        if(curFontParams == null)
            return;
        curFontParams.put(s, new ParamData(cats, cat, val, name));
    }

    private static class ParamSource {
        final CATS cats;           // either mime or AllLang or null if default
        final String categoryName; // like indent, number, keyword, ...

        public ParamSource(CATS cats, String categoryName)
        {
            this.cats = cats;
            this.categoryName = categoryName;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final ParamSource other = (ParamSource)obj;
            if(this.cats != other.cats) {
                return false;
            }
            if(this.categoryName != other.categoryName &&
                    (this.categoryName == null ||
                    !this.categoryName.equals(other.categoryName))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash = 59 * hash + (this.cats != null ? this.cats.hashCode() : 0);
            hash =
                    59 * hash +
                    (this.categoryName != null ? this.categoryName.hashCode() : 0);
            return hash;
        }

    }

    private static class ParamData {
        final ParamSource ps; // mime-allLang, categoryName
        final Object val;
        final String name;         // if getDefault

        public ParamData(CATS cats, AttributeSet cat, Object val, String name)
        {
            // this.cats = cats;
            // this.categoryName = cat != null
            //             ? cat.getAttribute(StyleConstants.NameAttribute) : null;
            this.ps = new ParamSource(cats,
                    (String)(cat != null
                    ? cat.getAttribute(StyleConstants.NameAttribute) : null));
            this.val = val;
            this.name = null;
        }
    }

    private static class FontSizeParams {
        private ParamData family;
        private ParamData size;

        FontSizeParams(ParamData family, ParamData size)
        {
            this.family = family;
            this.size = size;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final FontSizeParams other = (FontSizeParams)obj;
            if(this.family != other.family &&
                    (this.family == null || !this.family.equals(other.family))) {
                return false;
            }
            if(this.size != other.size &&
                    (this.size == null || !this.size.equals(other.size))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash =
                    53 * hash +
                    (this.family != null ? this.family.hashCode() : 0);
            hash = 53 * hash + (this.size != null ? this.size.hashCode() : 0);
            return hash;
        }

    }
//            {
//        // key is StyleConstant for familty, size, bold, italic
//        Map<Style, ParamData> map = new EnumMap<Style, ParamData>(Style.class);
//    }

    private static class WH {
        private final int w;
        private final int h;

        public WH(int w, int h)
        {
            this.w = w;
            this.h = h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final WH other = (WH)obj;
            if(this.w != other.w) {
                return false;
            }
            if(this.h != other.h) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 29 * hash + this.w;
            hash = 29 * hash + this.h;
            return hash;
        }

    }

    private class FontTrack {
        private final WH wh;
        private final String nameAttr;
        private final boolean isFixed;

        public FontTrack(AttributeSet c, Font f)
        {
            this.nameAttr = (String)c.getAttribute(StyleConstants.NameAttribute);
            FontMetrics fm = jep.getGraphics().getFontMetrics(f);
            // can't use font family to determine fixed width
            // so if first 'n' characters are the same size (except 0)
            // then assume fixed width
            boolean flag = true;
            int w01 = 0;
            int[] widths = fm.getWidths();
            for(int w : widths) {
                if(w == 0)
                    continue;
                if(w01 == 0)
                    w01 = w;
                if(w01 != w) {
                    flag = false;
                    break;
                }
            }
            this.isFixed = flag;
            this.wh = isFixed ? new WH(w01, fm.getHeight())
                              : new WH(0,0);
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final FontTrack other = (FontTrack)obj;
            if(this.wh != other.wh &&
                    (this.wh == null || !this.wh.equals(other.wh))) {
                return false;
            }
            if(this.nameAttr != other.nameAttr &&
                    (this.nameAttr == null ||
                    !this.nameAttr.equals(other.nameAttr))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 97 * hash + (this.wh != null ? this.wh.hashCode() : 0);
            hash =
                    97 * hash +
                    (this.nameAttr != null ? this.nameAttr.hashCode() : 0);
            return hash;
        }

    }

    private static final class FontParams implements Map<Style, ParamData> {
        private final Map<Style, ParamData> map = new EnumMap<Style, ParamData>(Style.class);

        @Override
        public String toString()
        {
            return map.toString();
        }

        @Override
        public Collection<ParamData> values()
        {
            return map.values();
        }

        @Override
        public int size()
        {
            return map.size();
        }

        @Override
        public ParamData remove(Object key)
        {
            return map.remove(key);
        }

        @Override
        public void putAll(Map<? extends Style, ? extends ParamData> m)
        {
            map.putAll(m);
        }

        @Override
        public ParamData put(Style key, ParamData value)
        {
            return map.put(key, value);
        }

        @Override
        public Set<Style> keySet()
        {
            return map.keySet();
        }

        @Override
        public boolean isEmpty()
        {
            return map.isEmpty();
        }

        @Override
        public int hashCode()
        {
            return map.hashCode();
        }

        @Override
        public ParamData get(Object key)
        {
            return map.get(key);
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o)
        {
            return map.equals(o);
        }

        @Override
        public Set<Entry<Style, ParamData>> entrySet()
        {
            return map.entrySet();
        }

        @Override
        public boolean containsValue(Object value)
        {
            return map.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key)
        {
            return map.containsKey(key);
        }

        @Override
        public void clear()
        {
            map.clear();
        }

    }

}
