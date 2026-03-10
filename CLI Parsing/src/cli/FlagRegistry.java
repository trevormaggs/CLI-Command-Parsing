package cli;

import java.text.ParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A central repository for all registered {@link FlagRule} definitions.
 *
 * <p>
 * The registry manages flag definitions, ensuring that each flag name is unique. It provides the
 * lookup mechanism used by handlers during the parsing phase to match raw tokens to defined rules.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 26 February 2026
 */
public final class FlagRegistry implements Iterable<FlagRule>
{
    private final Map<String, FlagRule> flagMap;

    /**
     * Initialises an empty registry.
     * 
     * <p>
     * Employs a {@link TreeMap} internally to maintain alphabetical order, facilitating clearer
     * diagnostic and debug output.
     * </p>
     */
    public FlagRegistry()
    {
        this.flagMap = new TreeMap<>();
    }

    /**
     * Initialises a registry with a starting rule.
     *
     * @param rule
     *        the first rule to register
     */
    public FlagRegistry(FlagRule rule)
    {
        this();
        addRule(rule);
    }

    /**
     * Registers a new flag rule.
     *
     * @param rule
     *        the rule to add
     * @throws IllegalArgumentException
     *         if the flag name is already defined in this registry
     */
    public void addRule(FlagRule rule)
    {
        if (existsFlag(rule.getFlagName()))
        {
            throw new IllegalArgumentException("Flag [" + rule.getFlagName() + "] already defined");
        }

        flagMap.put(rule.getFlagName(), rule);
    }

    /**
     * Checks if a flag is already defined in the registry.
     *
     * @param name
     *        the flag identifier with or without dashes, for example: "v", "-v", or "--verbose"
     * @return {@code true} if a rule exists for this flag
     */
    public boolean existsFlag(String name)
    {
        if (name == null)
        {
            return false;
        }

        return flagMap.containsKey(CommandFlagParser.stripLeadingDashes(name));
    }

    /**
     * Retrieves a rule by name.
     *
     * @param name
     *        the flag identifier with or without dashes
     * @return the associated {@link FlagRule}, or null if not found
     */
    public FlagRule getRule(String name)
    {
        if (name != null)
        {
            return flagMap.get(CommandFlagParser.stripLeadingDashes(name));
        }

        return null;
    }

    /**
     * Returns the number of registered flag rules.
     *
     * @return the size
     */
    public int getRuleSize()
    {
        return flagMap.size();
    }

    /**
     * Associates a value with a defined flag and marks it as handled.
     *
     * @param name
     *        the flag identifier with or without dashes
     * @param value
     *        the value to assign. {@code null} is permitted for boolean switches
     *
     * @throws ParseException
     *         if the flag is unrecognised or does not permit arguments
     */
    public void assignValue(String name, String value) throws ParseException
    {
        FlagRule rule = getRule(name);

        if (rule != null)
        {
            if (value != null)
            {
                if (rule.expectsArgument())
                {
                    rule.addValue(value);
                }

                else
                {
                    throw new ParseException("Flag [" + name + "] does not accept values", 0);
                }
            }

            rule.setFlagHandled();
        }

        else
        {
            throw new ParseException("Unknown flag [" + name + "]", 0);
        }
    }

    /**
     * Marks a flag as handled without assigning a value (typical for boolean flags).
     *
     * @param name
     *        the flag identifier with or without dashes
     *
     * @throws ParseException
     *         if the flag is unknown
     */
    public void acknowledgeFlag(String name) throws ParseException
    {
        assignValue(name, null);
    }

    /**
     * Returns an iterator over the collection of defined rules.
     *
     * @return an Iterator resource
     */
    @Override
    public java.util.Iterator<FlagRule> iterator()
    {
        return flagMap.values().iterator();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (FlagRule rule : flagMap.values())
        {
            sb.append(rule);
        }

        return sb.toString();
    }
}