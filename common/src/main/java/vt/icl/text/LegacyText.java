package vt.icl.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

/**
 * Builds a {@link Text} out of a string carrying legacy colour codes.
 * <p>
 * Both {@code &} and the section sign are accepted as the escape character, so
 * a value typed into a JSON config reads the same as it would in any other
 * server config. On top of the sixteen vanilla codes the parser understands
 * {@code &#RRGGBB}: the font renderer has no inline notation for a hex colour,
 * which only survives as a real {@link Style}, so it has to be resolved here.
 * <p>
 * A colour code clears the decorations before it, the way it does everywhere
 * else, and {@code &r} goes back to the base style rather than to nothing —
 * that keeps the caller's {@code Formatting} in charge of whatever the string
 * does not colour itself.
 */
public final class LegacyText {
    private static final char ALT_ESCAPE = '&';
    private static final char SECTION_ESCAPE = '§';
    private static final int HEX_LENGTH = 6;

    private LegacyText() {
    }

    /**
     * @param input text with legacy codes, may be null or empty
     * @param base  style applied to whatever the input does not style itself,
     *              and the style {@code &r} returns to; null for none
     */
    public static MutableText parse(String input, Formatting base) {
        Style baseStyle = base == null ? Style.EMPTY : Style.EMPTY.withFormatting(base);
        MutableText result = Text.empty().setStyle(baseStyle);
        if (input == null || input.isEmpty()) {
            return result;
        }

        StringBuilder run = new StringBuilder();
        Style style = baseStyle;
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);
            if ((c != ALT_ESCAPE && c != SECTION_ESCAPE) || i + 1 >= input.length()) {
                run.append(c);
                i++;
                continue;
            }

            TextColor hex = readHex(input, i + 1);
            if (hex != null) {
                style = startRun(result, run, style, baseStyle.withColor(hex));
                i += HEX_LENGTH + 2;
                continue;
            }

            Formatting formatting = Formatting.byCode(Character.toLowerCase(input.charAt(i + 1)));
            if (formatting == null) {
                run.append(c);
                i++;
                continue;
            }

            style = startRun(result, run, style, apply(style, baseStyle, formatting));
            i += 2;
        }

        if (!run.isEmpty()) {
            result.append(Text.literal(run.toString()).setStyle(style));
        }
        return result;
    }

    /** Closes the run held in the buffer under the old style and opens the next one. */
    private static Style startRun(MutableText result, StringBuilder run, Style current, Style next) {
        if (!run.isEmpty()) {
            result.append(Text.literal(run.toString()).setStyle(current));
            run.setLength(0);
        }
        return next;
    }

    private static Style apply(Style current, Style base, Formatting formatting) {
        if (formatting == Formatting.RESET) {
            return base;
        }
        if (formatting.isColor()) {
            return base.withColor(formatting);
        }
        return switch (formatting) {
            case BOLD -> current.withBold(true);
            case ITALIC -> current.withItalic(true);
            case UNDERLINE -> current.withUnderline(true);
            case STRIKETHROUGH -> current.withStrikethrough(true);
            case OBFUSCATED -> current.withObfuscated(true);
            default -> current;
        };
    }

    /**
     * Reads {@code #RRGGBB} starting at {@code start}.
     *
     * @return the colour, or null if what follows is not a full hex triplet
     */
    private static TextColor readHex(String input, int start) {
        if (start + HEX_LENGTH >= input.length() || input.charAt(start) != '#') {
            return null;
        }
        int rgb = 0;
        for (int i = start + 1; i <= start + HEX_LENGTH; i++) {
            int digit = Character.digit(input.charAt(i), 16);
            if (digit < 0) {
                return null;
            }
            rgb = rgb << 4 | digit;
        }
        return TextColor.fromRgb(rgb);
    }

    /** Strips every code, for log lines and other places with no styling. */
    public static String strip(String input) {
        return input == null ? "" : parse(input, null).getString();
    }

}
