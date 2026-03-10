package cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acts as a lexical pre-processor that transforms raw, fragmented command-line arguments into a
 * sequence of cohesive, logical tokens. This analyser scans the raw input array to identify
 * structural boundaries, acting as a sanitised buffer for downstream parsing components.
 * 
 * <p>
 * A core feature of this analyser is <b>token reconstruction</b>. It transparently re-assembles
 * fragmented inputs, such as {@code ["--temp", "=", "yes"]}, into a single "stitched" and validated
 * token ({@code "--temp=yes"}). This ensures the command logic receives a consistent stream of data
 * regardless of how the shell delimited the original input in the OS environment.
 * </p>
 * 
 * <p>
 * The processor focuses on three primary operations:
 * </p>
 * 
 * <ol>
 * <li><b>Identification:</b> It identifies and marks the boundaries of flags and options. For
 * example, a long flag (--platform) followed by a value or a set of values.</li>
 * <li><b>Normalisation:</b> It handles sanitisation to eliminate "noise" and breaks down input into
 * semantically correct individual tokens. This allows the logic layer to digest them cleanly.</li>
 * <li><b>Filtering:</b> If required, it strips off unnecessary characters, such as excessive
 * whitespace, empty quotes, or redundant delimiters (e.g., consecutive commas).</li>
 * </ol>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 2 March 2026
 */
public class LexicalCommandProcessor
{
    private final List<String> tokens;

    /**
     * Constructs a new instance designed to sanitise and normalise the specified command line
     * arguments.
     *
     * @param args
     *        the command line arguments
     */
    public LexicalCommandProcessor(String[] args)
    {
        tokens = (args != null && args.length > 0 ? normalise(args) : Collections.emptyList());
    }

    /**
     * Provides the normalised tokens for further processing.
     *
     * @return the list of normalised tokens
     */
    public List<String> getTokens()
    {
        return tokens;
    }

    /**
     * Flattens the command line arguments into a single string.
     *
     * @return the flattened string with individual tokens separated by a single whitespace
     */
    public String flattenArguments()
    {
        return "[" + String.join(" ", tokens) + "]";
    }

    /**
     * Returns a formatted string representation of the amalgamated tokens.
     * 
     * <p>
     * This provides a vertical list of each token in the sequence, which is useful for diagnostic
     * output and debugging during the tokenisation phase.
     * </p>
     *
     * @return a string describing the current state of the token buffer, or "No tokens" if the
     *         buffer is empty
     */
    @Override
    public String toString()
    {
        if (tokens.isEmpty())
        {
            return "No tokens";
        }

        StringBuilder sb = new StringBuilder(256);

        for (String token : tokens)
        {
            sb.append("Token: ");
            sb.append(token);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Performs sanitisation of the specified array of arguments and returns a list of cleaned
     * tokens to provide normalisation. It also takes care of merging tokens ending/starting with
     * '=' or ',' into single logical units.
     *
     * @param args
     *        the command line arguments
     * @return a list of normalised tokens
     */
    private List<String> normalise(String[] args)
    {
        StringBuilder sb = new StringBuilder();
        List<String> result = new ArrayList<>();

        for (int i = 0; i < args.length; i++)
        {
            boolean joinNext = false;
            String current = args[i];

            if (current.isEmpty())
            {
                continue;
            }

            sb.append(current);

            // Check if we should stitch the next token to the current item
            if (i + 1 < args.length)
            {
                String next = args[i + 1];

                if (CommandFlagParser.isOption(current) && CommandFlagParser.isNegativeNumber(next))
                {
                    joinNext = true;
                }

                else if (!CommandFlagParser.isOption(next))
                {
                    if (current.endsWith("=") || next.startsWith("=") || current.endsWith(",") || next.startsWith(","))
                    {
                        joinNext = true;
                    }
                }
            }

            if (!joinNext)
            {
                String token = sweepCommas(sb.toString());

                if (!token.isEmpty())
                {
                    result.add(token);
                }

                sb.setLength(0);
            }
        }

        return result;
    }

    /**
     * Using regex to collapse multiple commas into one and removes leading/trailing commas from the
     * merged argument.
     * 
     * Basically, it collapses multiple commas into a single comma, removes commas that are
     * erroneously glued to an equals sign, for example: --range=,108 is cleaned to range=108 and
     * finally strips out leading/trailing commas.
     * 
     * @param input
     *        the un-filtered string containing multiple commas
     * @return a cleaned and formatted string
     */
    private static String sweepCommas(String input)
    {
        String swept = "";

        if (input != null)
        {
            swept = input.replaceAll("[,\\s]+", ",");
            swept = swept.replaceAll("=[\\s,]+", "=");
            swept = swept.replaceAll("^,|,$", "");
        }

        return swept;
    }
}