/**
 * Provides a robust, strategy-based engine for parsing and validating command-line arguments in
 * Java 8+ environments.
 * 
 * <h2>Key Architectural Pillars</h2>
 * 
 * <ul>
 * <li><b>Lexical Amalgamation:</b> A pre-processing layer that heals fragmented shell inputs and
 * handles complex comma-delimited data.</li>
 * <li><b>The Negative Number Shield:</b> A regex-driven protection layer ensuring numeric data and
 * ranges are not misidentified as command flags.</li>
 * <li><b>Observer-Driven Discovery:</b> Utilises the {@link cli.FlagListener} interface to decouple
 * token extraction from state management.</li>
 * </ul>
 * 
 * <h2>Compliance</h2>
 * 
 * The package follows POSIX conventions for short flags (e.g., {@code -vh}) and GNU conventions for
 * long flags (e.g., {@code --verbose}), supporting both attached values and explicit separators.
 * 
 * @author Trevor Maggs
 * @version 2.0
 * @since 28 February 2026
 */
package cli;