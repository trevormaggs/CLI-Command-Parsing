package cli;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import common.PeekingIterator;

/**
 * This is the core access point for orchestrating the command-line parsing operations for
 * integration in your Java console-based development projects.
 * 
 * <p>
 * This class manages the lifecycle of command-line parsing through three distinct phases:
 * </p>
 * 
 * <ol>
 * <li><b>Definition:</b> Registering {@link FlagRule} objects using {@link #addDefinition}.</li>
 * <li><b>Tokenisation:</b> Breaking down raw input into manageable components.</li>
 * <li><b>Execution:</b> Processing tokens using polymorphic {@link FlagHandler} handlers.</li>
 * </ol>
 * 
 * <p>
 * The engine follows POSIX conventions for short flags (e.g., {@code -vh}) and GNU conventions for
 * long flags (e.g., {@code --verbose}). It supports clustered short flags, attached or "glued"
 * values, and explicit value separators ({@code =}).
 * </p>
 * 
 * <p>
 * Establish rules using {@link #addDefinition} prior to parsing. Constraints are defined by
 * {@link FlagType}:
 * </p>
 *
 * <table border="1">
 * <caption>Rule Definitions</caption>
 * <tr>
 * <th>Rule</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>ARG_BLANK</td>
 * <td>Boolean switch. No value permitted.</td>
 * </tr>
 * <tr>
 * <td>ARG_REQUIRED</td>
 * <td>Mandatory flag. Requires an associated value.</td>
 * </tr>
 * <tr>
 * <td>ARG_OPTIONAL</td>
 * <td>Optional flag. Accepts a value if provided.</td>
 * </tr>
 * <tr>
 * <td>SEP_REQUIRED</td>
 * <td>Value MUST be joined by an equals separator (e.g., {@code --flag=val}).</td>
 * </tr>
 * <tr>
 * <td>SEP_OPTIONAL</td>
 * <td>Value MAY be joined by an equals separator.</td>
 * </tr>
 * </table>
 *
 * <p>
 * Note that the {@code SEP} rules also recognise comma-separated arguments, allowing the parser to
 * construct a list of values for retrieval (e.g., {@code --value=12,24,36}).
 * </p>
 *
 * <p>
 * Examples of supported syntax:
 * </p>
 *
 * <ul>
 * <li>{@code cmd -g vulfeed.dat}</li>
 * <li>{@code cmd --csv}</li>
 * <li>{@code cmd -path /var/log/value.log}</li>
 * <li>{@code cmd --depth=7}</li>
 * <li>{@code cmd -b=3}</li>
 * <li>{@code cmd -ofile.txt} (Concatenated/Attached: {@code -o} is the option, {@code file.txt} is
 * the
 * argument)</li>
 * <li>{@code cmd -hv} (Clustered: Two standalone short options)</li>
 * </ul>
 *
 * <p>
 * The parser correctly identifies complex concatenated tokens, such as {@code -d787} (where
 * {@code -d} is the flag and {@code 787} is the value) or {@code -avb=747} (where {@code -a} and
 * {@code -v} are boolean switches and {@code -b} is assigned the value {@code 747}).
 * </p>
 *
 * @author Trevor Maggs
 * @version 2.0
 * @since 28 February 2026
 */
public class CommandFlagParser
{
    private final int MAX_OPERANDS = 16; // May change to higher limit if required
    private final String[] rawArgs;
    private final FlagRegistry registry;
    private final List<String> operands;
    private final FlagHandler longHandler;
    private final FlagHandler shortHandler;
    private boolean debug;
    private int maxOperands = 1;
    private final List<String> errors = new ArrayList<>();
    private final FlagListener observer = new FlagListener()
    {
        /**
         * Using the Observer pattern to decouple token parsing from state management.
         * This allows handlers to report multiple discoveries, for example: clustered
         * flags, during a single invocation without managing internal collection state.
         */
        @Override
        public void onDiscovery(String name, String value, boolean hasSeparator)
        {
            FlagRule rule = registry.getRule(name);

            if (rule != null)
            {
                String prefix = rule.isLongFlag() ? "--" : "-";

                rule.setSeparator(hasSeparator);

                if (rule.expectsSeparator() && !hasSeparator)
                {
                    errors.add("Flag [" + prefix + name + "] requires an explicit '=' separator");
                }

                // If the user provided a separator for a flag that doesn't use them (e.g.
                // ARG_BLANK)
                else if (hasSeparator && !rule.expectsSeparator())
                {
                    errors.add("Flag [" + prefix + name + "] does not permit an explicit '=' separator");
                }

                try
                {
                    processRangeValues(name, value);
                }

                catch (ParseException exc)
                {
                    errors.add(exc.getMessage());
                }
            }
        }

        /*
         * Unregistered tokens that look like flags are captured as operands to be validated or
         * rejected later.
         */
        @Override
        public void onUnrecognisedFlag(String token)
        {
            operands.add(token);
        }
    };

    /**
     * Initialises the parser with raw command-line arguments.
     *
     * @param args
     *        the command-line arguments
     * 
     * @throws IllegalArgumentException
     *         if args is null or empty
     */
    public CommandFlagParser(String[] args)
    {
        if (args == null || args.length == 0)
        {
            throw new IllegalArgumentException("Command arguments cannot be null or empty");
        }

        this.rawArgs = args;
        this.operands = new ArrayList<>();
        this.registry = new FlagRegistry();
        this.longHandler = new LongFlagHandler();
        this.shortHandler = new ShortFlagHandler();
    }

    /**
     * Strips leading dashes from a flag token.
     *
     * @param token
     *        the raw token, for example: "-v" or "--verbose"
     * @return the token content without leading '-' or '--'
     */
    public static String stripLeadingDashes(String token)
    {
        if (token.startsWith("--"))
        {
            return token.substring(2);
        }

        else if (token.startsWith("-"))
        {
            return token.substring(1);
        }

        return token;
    }

    /**
     * Determines if the specified token represents a real negative numeric value.
     * 
     * <p>
     * A real negative number must start with a single dash ('-') followed immediately by one or
     * more digits, with an optional single decimal point (e.g., "-5.7", "-0.5").
     * </p>
     * 
     * <p>
     * This method rejects non-numeric strings ("-a"), malformed numbers ("-2.2.2"), standalone
     * dashes ("-", "--"), and positive numbers.
     * </p>
     * 
     * @param token
     *        the raw command-line token to evaluate
     * @return {@code true} if the token matches the negative numeric pattern
     */
    public static boolean isNegativeNumber(String token)
    {
        if (token == null || token.isEmpty())
        {
            return false;
        }

        return token.matches("^-[0-9].*");
    }

    /**
     * Verifies the specified token represents a valid command flag or option.
     *
     * @param token
     *        the entry extracted from the command line
     * @return {@code true} if the token is identified as a valid flag or option
     */
    public static boolean isOption(String token)
    {
        if (token == null || token.length() < 2 || token.charAt(0) != '-' || isNegativeNumber(token))
        {
            return false;
        }

        // Because charAt(0) is already '-'
        char ch = (token.startsWith("--") && token.length() > 2 ? token.charAt(2) : token.charAt(1));

        return Character.isLetterOrDigit(ch);
    }

    /**
     * Queries whether the specified token represents a long flag starting with double leading
     * dashes ({@code --}), following the GNU convention.
     * 
     * <p>
     * This flag can optionally include an appended value, such as {@code --flag[value]} or
     * {@code --flag=value}.
     * </p>
     *
     * @param token
     *        the command-line token to evaluate
     * @return {@code true} if the token follows the GNU long-option format
     */
    public static boolean isLongOption(String token)
    {
        return isOption(token) && token.startsWith("--");
    }

    /**
     * Queries whether the specified token represents a valid short flag or option, following the
     * POSIX convention.
     * 
     * <p>
     * A short flag consists of a single character preceded by a single leading dash (e.g.,
     * {@code -S}). Also supports clustered flags and appended values, such as {@code -SV},
     * {@code -S=V}, {@code -S1S2V}, or {@code -S1S2=V1,V2,V3}.
     * </p>
     *
     * @param token
     *        the raw command-line token to evaluate
     * @return {@code true} if the token follows the POSIX short-option format
     */
    public static boolean isShortOption(String token)
    {
        return isOption(token) && !token.startsWith("--") && token.length() > 1;
    }

    /**
     * Enables or disables debugging mode.
     * 
     * <p>
     * This setting is propagated to downstream components to adjust their diagnostic output and
     * behavioural responses accordingly.
     * </p>
     * 
     * @param mode
     *        {@code true} to enable debugging, or {@code false} to disable (default)
     */
    public void setDebug(boolean mode)
    {
        this.debug = mode;
    }

    /**
     * Registers a new flag rule.
     * 
     * @param name
     *        the flag identifier. Valid dashes (e.g., "-v" or "--portal") must be provided
     * @param type
     *        the {@link FlagType} defining how arguments and separators are handled
     * 
     * @throws ParseException
     *         if the flag is already registered
     */
    public void addDefinition(String name, FlagType type) throws ParseException
    {
        registry.addRule(new FlagRule(name, type));
    }

    /**
     * Retrieves a flag rule if registered. Use it if value extraction is required.
     * 
     * @param name
     *        the flag identifier
     * @return the {@link FlagRule} if matched, or null if no such rule exists
     */
    public FlagRule getFlagRule(String name)
    {
        return registry.getRule(name);
    }

    /**
     * Sets the maximum number of arguments (operands) permitted. These are free-standing arguments
     * not associated with any specific flag or option. There is a maximum number of operands
     * applied to it.
     *
     * @param count
     *        the number of operands allowed. If exceeded during parsing, a ParseException
     *        will be thrown. Default is one argument allowed if not defined
     * 
     * @throws IllegalArgumentException
     *         if the specified count is out of bounds
     */
    public void setFreeArgumentLimit(int count)
    {
        if (count < 0 || count >= MAX_OPERANDS)
        {
            throw new IllegalArgumentException("Operand limit must be between 0 and " + MAX_OPERANDS);
        }

        maxOperands = count;
    }

    /**
     * Determines if the specified flag or option has been processed and is available for value
     * retrieval.
     * 
     * <p>
     * As an example, the following snippet checks if a particular {@code -f} option has been set.
     * </p>
     * 
     * <pre>
     * <code>
     * if (cli.existsFlag("-f"))
     * {
     *     // Do something here
     * }
     * </code>
     * </pre>
     * 
     * @param name
     *        the flag identifier with or without leading dashes
     * @return {@code true} if the flag exists or {@code false} if the flag is unrecognised
     */
    public boolean existsFlag(String name)
    {
        FlagRule rule = registry.getRule(stripLeadingDashes(name));

        return (rule == null ? false : rule.isFlagHandled());
    }

    /**
     * Retrieves the value of the specified flag if present.
     * 
     * <p>
     * Example: the following snippet retrieves the value of the {@code -f} flag.
     * </p>
     * 
     * <pre>
     * <code>
     *   System.out.println(cli.getValueByFlag("-f"));
     * </code>
     * </pre>
     * 
     * @param name
     *        the flag identifier with or without leading dashes
     * @return the value being requested or a blank string if none
     */
    public String getValueByFlag(String name)
    {
        return getValueByFlag(name, 0);
    }

    /**
     * Retrieves the value of the specified flag by its index in the value set.
     * 
     * @param name
     *        the flag identifier with or without leading dashes
     * @param index
     *        the position of where the value is located in a collection
     * @return the value of the specified flag or a blank string if there is no value
     * 
     * @throws IndexOutOfBoundsException
     *         if the index is out of bounds
     */
    public String getValueByFlag(String name, int index) throws IndexOutOfBoundsException
    {
        FlagRule rule = registry.getRule(stripLeadingDashes(name));

        return (rule == null ? "" : rule.getValue(index));
    }

    /**
     * Returns the size of the value set which the specified flag is associated with.
     * 
     * @param name
     *        the flag identifier with or without leading dashes
     * @return the size of the values held by the processed flag, otherwise zero if none
     */
    public int getValueLength(String name)
    {
        FlagRule rule = registry.getRule(stripLeadingDashes(name));

        return (rule == null ? 0 : rule.getSize());
    }

    /**
     * Returns the count of free-standing arguments not associated with any registered flags.
     * 
     * @return the number of free arguments (operands)
     */
    public int getFreeArgumentCount()
    {
        return operands.size();
    }

    /**
     * Retrieves the first stand-alone argument from the value set.
     * 
     * @return the first stand-alone argument, or an empty string if none exists
     */
    public String getFirstFreeArgument()
    {
        return getFreeArgumentByIndex(0);
    }

    /**
     * Retrieves the free-standing argument from a specific position in the value set.
     * 
     * @param k
     *        the zero-based index within the argument list
     * @return the stand-alone argument at the specified position, or an empty string if the index
     *         is out of bounds
     */
    public String getFreeArgumentByIndex(int k)
    {
        if (k < 0 || k >= operands.size())
        {
            return "";
        }

        return operands.get(k);
    }

    /**
     * Executes the parsing logic against the raw arguments provided at construction.
     * 
     * <p>
     * This method performs a single-pass parsing operation. It identifies flags, routes to required
     * handlers, collects operands, and finally validates the following constraints.
     * </p>
     * 
     * <ul>
     * <li>The free-standing argument limit (see {@link #setFreeArgumentLimit}).</li>
     * <li>The presence of all mandatory flags.</li>
     * </ul>
     * 
     * @throws ParseException
     *         if an unrecognised flag is encountered, any required argument is missing, or
     *         validation cannot be completed
     */
    public void parse() throws ParseException
    {
        LexicalPreProcessor tokenizer = new LexicalPreProcessor(rawArgs);
        PeekingIterator<String> it = new PeekingIterator<>(tokenizer.getTokens());

        while (it.hasNext())
        {
            String token = it.peek();

            if (isLongOption(token))
            {
                longHandler.handle(it, registry, observer);
            }

            else if (isShortOption(token))
            {
                shortHandler.handle(it, registry, observer);
            }

            else
            {
                /*
                 * It's an operand, representing a list of free-standing
                 * arguments and unrecognised tokens.
                 */
                operands.add(it.next());
            }
        }

        if (debug)
        {
            System.out.println("--- Registry Definitions ---");
            System.out.println(registry);
            System.out.println("--- Tokenization ---");
            System.out.println(tokenizer);
            System.out.printf("Flattened: %s\n\n", tokenizer.flattenArguments());
            System.out.printf("Execution completed. Operands captured: %d%n" + operands.size());
        }

        validateOperands();
        validateRuleConstraints();

        if (!errors.isEmpty())
        {
            throw new ParseException(String.join("\n", errors), 0);
        }
    }

    /**
     * Collapses a comma-separated token into manageable individual values.
     * 
     * <p>
     * Each element is trimmed of leading and trailing whitespace. Empty elements (e.g.,
     * "val1,,val2") are ignored. Validated values are processed via {@link #processSingleValue}.
     * </p>
     * 
     * @param name
     *        the flag identifier
     * @param value
     *        the raw string containing potential multiple values
     * 
     * @throws ParseException
     *         if the flag associated with the name is unknown or does not accept arguments
     */
    private void processRangeValues(String name, String value) throws ParseException
    {
        if (value == null)
        {
            processSingleValue(name, null);
        }

        else
        {
            // Split and trim multi-values (i.e., "win10, rhel")
            String[] parts = value.split(",");

            for (String part : parts)
            {
                String trimmed = part.trim();

                if (!trimmed.isEmpty())
                {
                    processSingleValue(name, trimmed);
                }
            }
        }
    }

    /**
     * Processes the specified value associated with the flag being processed, if available.
     *
     * @param name
     *        the flag identifier
     * @param value
     *        the single data associated with the flag. A value of null is accepted if the value is
     *        not available
     * 
     * @throws ParseException
     *         if the flag associated with the name is unknown or does not accept arguments
     */
    private void processSingleValue(String name, String value) throws ParseException
    {
        registry.assignValue(name, value);
    }

    /**
     * Validates the integrity and quantity of captured operands.
     * 
     * <p>
     * This method ensures that no unrecognised flags, prefixed with a dash ('-'), were mistakenly
     * captured as operands and verifies that the total number of free-standing arguments does not
     * exceed the defined limit.
     * </p>
     */
    private void validateOperands()
    {
        for (String token : operands)
        {
            if (isOption(token))
            {
                errors.add("Unrecognised flag [" + token + "] detected");
            }
        }

        // Check quantity of legitimate free-standing arguments
        if (operands.size() > maxOperands)
        {
            errors.add(String.format("Free-standing arguments [%d] is too many (limit is %d). Found [%s]", operands.size(), maxOperands, String.join(", ", operands)));
        }
    }

    /**
     * Validates the current state of all rules against their defined {@link FlagType} constraints.
     * 
     * <p>
     * This method performs a comprehensive integrity check:
     * </p>
     * 
     * <ul>
     * <li>All mandatory flags have been identified in the input stream.</li>
     * <li>Separator enforcement rules (required vs. forbidden) are strictly followed.</li>
     * <li>Flags requiring an argument have at least one value assigned.</li>
     * </ul>
     *
     * @throws ParseException
     *         if any flag violates its structural or presence constraints. The exception message
     *         aggregates all identified errors
     */
    private void validateRuleConstraints() throws ParseException
    {
        for (FlagRule rule : registry)
        {
            String prefix = rule.isLongFlag() ? "--" : "-";

            if (rule.isRequired() && !rule.isFlagHandled())
            {
                errors.add("Missing required option [" + prefix + rule.getFlagName() + "]");
            }

            if (rule.isFlagHandled() && rule.expectsArgument() && !rule.hasValueAssigned())
            {
                errors.add("Flag [" + prefix + rule.getFlagName() + "] requires an argument value");
            }
        }
    }
}