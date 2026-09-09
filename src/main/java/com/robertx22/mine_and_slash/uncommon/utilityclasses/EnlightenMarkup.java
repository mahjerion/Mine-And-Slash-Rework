package com.robertx22.mine_and_slash.uncommon.utilityclasses;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips Enlighten glossary markup - {@code [Display Text](term_id)} - down to its display text.
 * <p>
 * The Craft to Exile 2 pack writes that markup straight into lang strings (stat names included) for
 * Tooltips Core, which only resolves it on the text lines of a tooltip. Everything else would show the
 * raw brackets: the stat screen and map device panels (drawn with {@code GuiGraphics.drawString}),
 * chat when ChatPlus renders it (it draws with {@code Font.drawInBatch} directly), and tooltip image
 * components that draw their own text such as {@code SocketTooltip}. {@code FontStripMarkupMixin}
 * therefore runs every {@code Font.drawInBatch} and {@code Font.width} call through here. Tooltip lines
 * Tooltips Core has resolved reach the font as display text plus its own spans, so there is nothing
 * left to strip there and the hoverable terms keep working.
 * <p>
 * {@code [VAL1]} style placeholders are untouched: they are never followed by {@code (...)}.
 */
public class EnlightenMarkup {

    private static final Pattern MARKUP = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    /** the cheapest possible pre-check: every markup contains this pair */
    private static boolean mightContain(String s) {
        return s != null && s.indexOf("](") >= 0;
    }

    public static String strip(String text) {
        if (!mightContain(text)) {
            return text;
        }
        return MARKUP.matcher(text).replaceAll("$1");
    }

    /**
     * Only used for measuring ({@code Font.width(FormattedText)}), never for drawing, so dropping the
     * styles is fine: bold is the only style that changes glyph width, by a pixel.
     */
    public static FormattedText strip(FormattedText text) {
        if (text == null) {
            return null;
        }
        String s = text.getString();
        if (!mightContain(s)) {
            return text;
        }
        return FormattedText.of(strip(s));
    }

    /**
     * Same for an already-styled sequence. The characters are cut by index, so a markup whose brackets
     * and term fall under different styles is still removed cleanly and every kept character keeps the
     * style it had.
     */
    public static FormattedCharSequence strip(FormattedCharSequence seq) {
        if (seq == null) {
            return null;
        }

        // first pass: bail without allocating anything unless "](" shows up
        int[] prev = {-1};
        boolean[] found = {false};
        seq.accept((idx, style, cp) -> {
            if (prev[0] == ']' && cp == '(') {
                found[0] = true;
                return false;
            }
            prev[0] = cp;
            return true;
        });
        if (!found[0]) {
            return seq;
        }

        // second pass: collect the code points with their styles
        List<Integer> cps = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        seq.accept((idx, style, cp) -> {
            cps.add(cp);
            styles.add(style);
            sb.appendCodePoint(cp);
            return true;
        });

        String text = sb.toString();
        Matcher m = MARKUP.matcher(text);
        if (!m.find()) {
            return seq;
        }

        // char ranges to drop: the "[" and the "](term)" of every match
        List<int[]> cuts = new ArrayList<>();
        do {
            cuts.add(new int[]{m.start(), m.start(1)});
            cuts.add(new int[]{m.end(1), m.end()});
        } while (m.find());

        // rebuild run by run, skipping the cut chars, keeping styles
        List<FormattedCharSequence> parts = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style runStyle = null;
        int charOffset = 0;
        for (int i = 0; i < cps.size(); i++) {
            int cp = cps.get(i);
            Style style = styles.get(i);
            boolean cut = false;
            for (int[] c : cuts) {
                if (charOffset >= c[0] && charOffset < c[1]) {
                    cut = true;
                    break;
                }
            }
            charOffset += Character.charCount(cp);
            if (cut) {
                continue;
            }
            if (runStyle != null && !runStyle.equals(style) && run.length() > 0) {
                parts.add(FormattedCharSequence.forward(run.toString(), runStyle));
                run.setLength(0);
            }
            runStyle = style;
            run.appendCodePoint(cp);
        }
        if (run.length() > 0 && runStyle != null) {
            parts.add(FormattedCharSequence.forward(run.toString(), runStyle));
        }
        return FormattedCharSequence.composite(parts);
    }
}
