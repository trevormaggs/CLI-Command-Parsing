package cli;
import common.PeekingIterator;

/**
 * Strategy interface for processing different types of command-line flags.
 */
public interface FlagHandler
{
    /**
     * Processes a flag and its potential arguments.
     *
     * @param tokens
     *        the iterator over normalised tokens
     * @param registry
     *        the registry to look up rules
     * @param observer
     *        an event-driven listener used to relay any state updates to the caller
     */
    void handle(PeekingIterator<String> tokens, FlagRegistry registry, FlagListener observer);
}