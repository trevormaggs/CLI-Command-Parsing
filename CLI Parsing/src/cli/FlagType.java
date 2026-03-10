package cli;
/**
 * Defines the expected behaviour and syntactical requirements for flag arguments and value
 * separators.
 * 
 * <p>
 * This enumeration dictates how the parsing strategies identify and validate values associated with
 * specific flags.
 * </p>
 */
public enum FlagType
{
    /**
     * Indicates a mandatory value is required, typically following a space.
     * 
     * <p>
     * Example: {@code -f value}
     * </p>
     */
    ARG_REQUIRED,

    /**
     * Indicates an optional value is permitted.
     * 
     * <p>
     * Example: {@code -f [value]}
     * </p>
     */
    ARG_OPTIONAL,

    /**
     * Indicates a mandatory value must be provided using an equals separator.
     * 
     * <p>
     * Example: {@code --file=data.txt}
     * </p>
     */
    SEP_REQUIRED,

    /**
     * Indicates an optional value is permitted via an equals separator.
     * 
     * <p>
     * Example: {@code --file[=data.txt]}
     * </p>
     */
    SEP_OPTIONAL,

    /**
     * Represents a boolean or "switch" flag with no associated value.
     * 
     * <p>
     * Example: {@code --verbose}
     * </p>
     */
    ARG_BLANK
}