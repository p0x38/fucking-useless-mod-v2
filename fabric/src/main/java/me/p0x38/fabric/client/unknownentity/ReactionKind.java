package me.p0x38.fabric.client.unknownentity;

/**
 * Describes the type and tone of a response produced by the Unknown Entity.
 *
 * <p>The reaction kind is used by the response system and can also affect
 * how long the entity waits before delivering a response.</p>
 */
public enum ReactionKind {
    /** A normal response without a special tone. */
    NORMAL,

    /** A greeting or acknowledgement of the player. */
    GREETING,

    /** A response concerning the entity's identity or nature. */
    IDENTITY,

    /** A response that answers or reacts to a question. */
    QUESTION,

    /** An intentionally eerie, unsettling, or uncomfortable response. */
    UNSETTLING,

    /** An unusual or contextually out-of-place response. */
    OUT_OF_PLACE,

    /** A playful, joking, or teasing response. */
    PLAYFUL,

    /**
     * A response related to Null.
     *
     * <p>This does not represent the Java {@code null} value.</p>
     */
    NULL,

    /** An irritated or annoyed response. */
    ANNOYED,

    /** A response related to sleeping or prolonged player inactivity. */
    SLEEP,

    /** A self-aware, meta, or otherwise unusual response. */
    META
}