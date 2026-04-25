package cli;

import util.PeekingIterator;

/**
 * Provides a concrete strategy for processing short-form flags based on POSIX conventions.
 *
 * <p>
 * This handler acts as a lexical processor designed to identify short flags, prefixed by a single
 * dash ({@code -}), and extracts their associated values from the token stream. It supports
 * multiple standard formats:
 * </p>
 *
 * <ul>
 * <li><b>Clustered:</b> {@code -vh} (Equivalent to {@code -v -h})</li>
 * <li><b>Attached/Glued:</b> {@code -p8080} (Where {@code 8080} is the argument for {@code p})</li>
 * <li><b>Explicit Separator:</b> {@code -o=file.txt}</li>
 * </ul>
 *
 * <p>
 * Upon identifying a flag, the handler notifies the {@link FlagListener} using the Observer design
 * pattern. By passing the flag's name and its associated value (if any), the handler effectively
 * decouples lexical extraction from state management and validation logic.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 2 March 2026
 */
public class ShortFlagHandler implements FlagHandler
{
    /**
     * Processes a short-form flag and its potential arguments by consuming tokens from the iterator
     * provided.
     *
     * @param tokens
     *        the iterator over normalised tokens, supporting look-ahead operations
     * @param registry
     *        the registry containing the valid {@link FlagRule} definitions
     * @param observer
     *        the event-driven listener notified when a flag or unrecognised token is identified
     */
    @Override
    public void handle(PeekingIterator<String> tokens, FlagRegistry registry, FlagListener observer)
    {
        String rawToken = tokens.next();
        String content = CommandFlagParser.stripLeadingDashes(rawToken);

        for (int i = 0; i < content.length(); i++)
        {
            String name = String.valueOf(content.charAt(i));
            FlagRule rule = registry.getRule(name);

            if (rule == null)
            {
                observer.onUnrecognisedFlag(content.substring(i));
                return;
            }

            String value = null;
            boolean hasSeparator = false;

            if (rule.expectsArgument())
            {
                // Scenario 1: Attached/Stitched (-p8080, -p=8080, -vofile.xlsx)
                if (i + 1 < content.length())
                {
                    String suffix = content.substring(i + 1);

                    if (suffix.startsWith("="))
                    {
                        hasSeparator = true;

                        // Note: substring returns an empty string if the index equals the string
                        // length. Be aware of this subtle quirk.
                        value = suffix.substring(1);
                    }

                    else
                    {
                        value = suffix;
                    }
                }

                // Scenario 2: Spaced value (-p 8080 or -o scopeos.txt)
                else if (tokens.hasNext() && !CommandFlagParser.isOption(tokens.peek()))
                {
                    value = tokens.next();
                }

                observer.onDiscovery(name, value, hasSeparator);

                // Critical: argument-taking flags ALWAYS terminate the cluster
                break;
            }

            else
            {
                // ARG_BLANK - always false for separator in a cluster like -abc
                observer.onDiscovery(name, null, false);
            }
        }
    }
}