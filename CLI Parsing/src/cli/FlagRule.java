package cli;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A stateful POJO (Plain Old Java Object) designed to represent a specific command-line flag
 * definition and its subsequent parsed state.
 *
 * <p>
 * This class acts as a data container and state tracker. It maintains the configuration (name,
 * type, requirements) and captures the results of the lexical analysis, including associated values
 * and handled status.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 26 February 2026
 */
public class FlagRule
{
    final private FlagType flagType;
    final private boolean longFlag;
    final private String flagName;
    final private List<String> values;
    private boolean handled;
    private boolean separator;

    /**
     * Constructs a new flag rule.
     *
     * @param name
     *        the raw flag name, for example: "-v" or "--verbose". Leading dashes are stripped
     * @param type
     *        the policy for handling arguments and separators
     *
     * @throws ParseException
     *         if the flag format is invalid or a short flag is multi-character
     */
    public FlagRule(String name, FlagType type) throws ParseException
    {
        if (name == null || !name.matches("\\-{1,2}[^\\-].*$"))
        {
            throw new ParseException("Flag [" + name + "] is invalid", 0);
        }

        this.flagName = CommandFlagParser.stripLeadingDashes(name);
        this.flagType = type;
        this.longFlag = name.startsWith("--");
        this.values = new ArrayList<>();

        if (!longFlag && name.length() > 2)
        {
            throw new ParseException("Short flag rules must be a single character, for example: '-v'. Found [" + name + "]", 0);
        }
    }

    /**
     * Retrieves the name of this flag.
     *
     * @return the flag name
     */
    public String getFlagName()
    {
        return flagName;
    }

    /**
     * Retrieves the type of this flag.
     *
     * @return the {@link FlagType}, which defines this flag
     */
    public FlagType getFlagType()
    {
        return flagType;
    }

    /**
     * Associates a non-null value with this command flag.
     *
     * @param value
     *        the string value parsed from the command line to be associated with this rule
     */
    public void addValue(String value)
    {
        if (value != null)
        {
            values.add(value);
        }
    }

    /**
     * Checks if this flag has any values assigned to it.
     *
     * @return true if the flag has at least one assigned value, false otherwise
     */
    public boolean hasValueAssigned()
    {
        return (!values.isEmpty());
    }

    /**
     * Returns the number of values stored within this flag.
     *
     * @return the count of assigned values
     */
    public int getSize()
    {
        return values.size();
    }

    /**
     * Returns true if the flag is a short flag type, for example: -v.
     *
     * @return true if it is a short flag
     */
    public boolean isShortFlag()
    {
        return !longFlag;
    }

    /**
     * Returns true if the flag is a long flag type, for example: --verbose.
     *
     * @return true if it is a long flag
     */
    public boolean isLongFlag()
    {
        return longFlag;
    }

    /**
     * Returns whether this flag requires or permits at least one argument.
     *
     * @return true if an argument is expected or optional, false otherwise
     */
    public boolean expectsArgument()
    {
        switch (flagType)
        {
            case ARG_REQUIRED:
            case ARG_OPTIONAL:
            case SEP_REQUIRED:
            case SEP_OPTIONAL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Sets a status indicating that this flag has been handled, including processing any
     * associated arguments or values.
     */
    public void setFlagHandled()
    {
        handled = true;
    }

    /**
     * Indicates whether this flag has been marked as handled during the current parsing phase.
     *
     * @return {@code true} if the processing of this flag is complete, otherwise {@code false}
     */
    public boolean isFlagHandled()
    {
        return handled;
    }

    /**
     * Indicates whether this flag is mandatory for the current parsing operation, according to the
     * defined constraints.
     *
     * @return {@code true} if the flag is required, otherwise {@code false}
     */
    public boolean isRequired()
    {
        return (flagType == FlagType.ARG_REQUIRED || flagType == FlagType.SEP_REQUIRED);
    }

    /**
     * Checks if this flag is bound by the value separator (=) rule, according to the defined
     * constraints.
     *
     * @return {@code true} if the value separator is required or permitted, otherwise {@code false}
     */
    public boolean expectsSeparator()
    {
        return (flagType == FlagType.SEP_REQUIRED || flagType == FlagType.SEP_OPTIONAL);
    }

    /**
     * Records the presence of a value separator ({@code =}) detected during the lexical parsing of
     * this specific flag instance.
     * 
     * <p>
     * This state is used by the parser to validate syntax constraints (e.g., {@code SEP_REQUIRED})
     * on a per-occurrence basis.
     * </p>
     *
     * @param found
     *        {@code true} if a separator was explicitly present in the token, {@code false}
     *        otherwise
     */
    public void setSeparator(boolean found)
    {
        separator = found;
    }

    /**
     * Queries whether a value separator ({@code =}) was detected for this flag.
     *
     * @return {@code true} if a separator was present in the command line, otherwise {@code false}
     */
    public boolean hasSeparator()
    {
        return separator;
    }

    /**
     * Returns a value from the specified position in the list.
     *
     * @param index
     *        the index of the value to retrieve
     * @return the value at the specified position
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of bounds
     */
    public String getValue(int index)
    {
        if (index >= 0 && index < values.size())
        {
            return values.get(index);
        }

        throw new IndexOutOfBoundsException("Index [" + index + "] is out of bounds");
    }

    /**
     * Retrieves all values currently associated with this flag rule.
     *
     * @return an unmodifiable {@link List} containing all assigned string values, or an empty list
     *         if no values have been associated with this flag
     */
    public List<String> getAllValues()
    {
        return (values.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(values));
    }

    /**
     * Returns a string representation of this flag rule, detailing its name, type, current parsing
     * status, and any associated values.
     *
     * @return a formatted string containing the flag's definition and parsed state
     */
    @Override
    public String toString()
    {
        return String.format("Flag: %-16sType: %-16sStatus: %-16sRule: %-16sValue: %s%n", flagName, (longFlag ? "Long" : "Short"), (handled ? "PARSED" : "PENDING"), flagType, String.join(", ", values));
    }
}