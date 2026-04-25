package cli;

import util.PeekingIterator;

/**
 * Provides a concrete strategy for processing long-form flags based on GNU conventions.
 * 
 * <p>
 * This handler identifies long flags, prefixed by double dashes ({@code --}), and extracts their
 * associated values from the token stream. It supports multiple formats:
 * </p>
 * 
 * <ul>
 * <li>Standard: {@code --flag value}</li>
 * <li>Explicit Separator: {@code --flag=value} or {@code --flag = value}</li>
 * <li>Attached/Glued: {@code --flagValue}</li>
 * </ul>
 * 
 * <p>
 * As discoveries are made, the handler communicates them back to the caller using the Observer
 * design pattern, decoupling lexical extraction from state management.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 2 March 2026
 */
public class LongFlagHandler implements FlagHandler
{
    /**
     * Processes a long-form flag and its potential arguments by consuming tokens from the iterator
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
        String name = null;
        String value = null;
        boolean hasSeparator = false;
        String rawContent = tokens.next();
        String content = CommandFlagParser.stripLeadingDashes(rawContent);

        // Scenario 1: Attached with separator (--flag=value)
        if (content.contains("="))
        {
            // --L=V and --L=V1,V2,V3 scenarios
            String[] parts = content.split("=", 2);

            name = parts[0];
            value = parts[1];
            hasSeparator = true;
        }

        // Scenario 2: Standard match (--flag, --flag value or --flag = value)
        else if (registry.existsFlag(content))
        {
            // --L or --L V or --L = V scenario
            name = content;

            if (tokens.hasNext() && !CommandFlagParser.isOption(tokens.peek()))
            {
                String next = tokens.next();

                if (next.equals("="))
                {
                    hasSeparator = true;

                    if (tokens.hasNext() && !CommandFlagParser.isOption(tokens.peek()))
                    {
                        value = tokens.next();
                    }
                }

                else
                {
                    value = next;
                }
            }
        }

        // Scenario 3: Glued long flag (--flagValue)
        else
        {
            // --LV scenario
            int bestMatchLength = -1;

            for (FlagRule rule : registry)
            {
                String ruleName = rule.getFlagName();

                if (rule.isLongFlag() && content.startsWith(ruleName))
                {
                    if (ruleName.length() > bestMatchLength)
                    {
                        name = ruleName;
                        value = content.substring(ruleName.length());
                        bestMatchLength = ruleName.length();
                    }
                }
            }
        }

        if (registry.existsFlag(name))
        {
            observer.onDiscovery(name, value, hasSeparator);
        }

        else
        {
            observer.onUnrecognisedFlag(rawContent);
        }
    }
}