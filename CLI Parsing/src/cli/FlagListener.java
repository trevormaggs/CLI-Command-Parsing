package cli;

/**
 * Defines a contract for observing and reacting to flag discovery events during
 * the parsing process.
 * 
 * <p>
 * This interface facilitates the <b>Observer Pattern</b>, allowing {@link FlagHandler}
 * implementations to report findings back to the core engine without being coupled
 * to the engine's internal state processing or validation logic.
 * </p>
 *
 * @author Trevor Maggs
 * @version 2.0
 * @since 28 February 2026
 */
public interface FlagListener
{
    /**
     * Invoked when a registered flag is successfully identified within the token stream.
     * 
     * @param name
     *        the identifier of the flag discovered with or without dashes, for example: "-v" or
     *        "--platform"
     * @param value
     *        the associated argument value, or {@code null} if the flag is a stand-alone
     *        boolean/blank flag
     * @param hasSeparator
     *        indicates whether the flag is accompanied by an equals separator
     */
    void onDiscovery(String name, String value, boolean hasSeparator);

    /**
     * Invoked when a token matching the pattern of a flag, for example: starting with
     * "-" or "--", is captured, but does not match any registered definitions.
     * 
     * <p>
     * Implementation of this method allows the core parsing logic to decide whether to treat
     * unknown flags as errors, log them, or capture them as potential operands.
     * </p>
     * 
     * @param token
     *        the raw unrecognised command-line token
     */
    void onUnrecognisedFlag(String token);
}