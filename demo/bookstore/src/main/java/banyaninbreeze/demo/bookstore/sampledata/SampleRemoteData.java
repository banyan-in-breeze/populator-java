package banyaninbreeze.demo.bookstore.sampledata;

import java.util.List;

public class SampleRemoteData {
    
    /**
     * Represents a calculated aggregate summary of remote website user ratings for a specific book.
     * 
     * <p>A single book edition (tracked by its {@code isbn}) may have up to five corresponding 
     * summary records—one for each distinct star tier (1 through 5). If a book has received 
     * no ratings for a particular tier, that tier's record will be omitted entirely. 
     * If a book has zero ratings overall, it will have no records in the table.
     * 
     * @param isbn            The universal 13-digit International Standard Book Number acting as the cross-platform foreign key.
     * @param stars           The rating tier being summarized, strictly ranging from 1 (lowest) to 5 (highest).
     * @param numberOfReviews The total count of user submissions that awarded this specific number of stars.
     */
    public static final record BookUserRatingRecord(String isbn, int stars, int numberOfReviews) {}

    public static final List<BookUserRatingRecord> BOOK_USER_RATING_DATA = List.of(
        // b01: The Sentient Stack (Mixed/Highly Reviewed)
        new BookUserRatingRecord("9780132350884", 5, 142),
        new BookUserRatingRecord("9780132350884", 4, 85),
        new BookUserRatingRecord("9780132350884", 3, 12),
        new BookUserRatingRecord("9780132350884", 2, 4),
        new BookUserRatingRecord("9780132350884", 1, 2),

        // b02: Rust for Radical Scale (Universally Acclaimed)
        new BookUserRatingRecord("9781491950296", 5, 210),
        new BookUserRatingRecord("9781491950296", 4, 45),

        // b03: Code as Culture (Moderate reception)
        new BookUserRatingRecord("9780596007126", 5, 18),
        new BookUserRatingRecord("9780596007126", 4, 32),
        new BookUserRatingRecord("9780596007126", 3, 15),
        new BookUserRatingRecord("9780596007126", 2, 6),

        // b04: The Zero-Trust Mirage (Polarizing response)
        new BookUserRatingRecord("9781119642817", 5, 64),
        new BookUserRatingRecord("9781119642817", 3, 40),
        new BookUserRatingRecord("9781119642817", 1, 55),

        // b05: An Echo in the Cave (Few reviews, but positive)
        new BookUserRatingRecord("9780140449136", 5, 9),
        new BookUserRatingRecord("9780140449136", 4, 14),

        // b06: Ethics of the Unseen (Niche, single review tier)
        new BookUserRatingRecord("9780190947293", 4, 8),

        // [b07: The Quantified Soul is omitted intentionally to simulate 0 total reviews]

        // b08: Post-Human Logic
        new BookUserRatingRecord("9781107604612", 5, 23),
        new BookUserRatingRecord("9781107604612", 4, 19),
        new BookUserRatingRecord("9781107604612", 3, 2),

        // b09: Shadows of the Silk Road
        new BookUserRatingRecord("9780307279460", 5, 88),
        new BookUserRatingRecord("9780307279460", 4, 41),
        new BookUserRatingRecord("9780307279460", 2, 3),

        // b10: The Ink-Stained Century
        new BookUserRatingRecord("9780393064650", 5, 112),
        new BookUserRatingRecord("9780393064650", 4, 76),
        new BookUserRatingRecord("9780393064650", 3, 22),

        // b11: Empires of the Monsoon
        new BookUserRatingRecord("9781107539037", 5, 45),
        new BookUserRatingRecord("9781107539037", 4, 30),

        // b12: Before the Concrete
        new BookUserRatingRecord("9780307353306", 4, 15),
        new BookUserRatingRecord("9780307353306", 3, 19),
        new BookUserRatingRecord("9780307353306", 2, 1),

        // b13: The Circadian Rhythm Code
        new BookUserRatingRecord("9781501171345", 5, 304),
        new BookUserRatingRecord("9781501171345", 4, 122),
        new BookUserRatingRecord("9781501171345", 3, 45),
        new BookUserRatingRecord("9781501171345", 2, 12),
        new BookUserRatingRecord("9781501171345", 1, 8),

        // b14: The Wellness Trekker
        new BookUserRatingRecord("9780544256446", 5, 52),
        new BookUserRatingRecord("9780544256446", 4, 48),

        // b15: A History of Healing Herbs
        new BookUserRatingRecord("9780143122111", 5, 19),
        new BookUserRatingRecord("9780143122111", 4, 25),
        new BookUserRatingRecord("9780143122111", 3, 7),

        // b16: The Ergonomic Engineer
        new BookUserRatingRecord("9781449331818", 5, 37),
        new BookUserRatingRecord("9781449331818", 4, 14),

        // b17: Atlas of Lost Trails
        new BookUserRatingRecord("9781452182056", 5, 160),
        new BookUserRatingRecord("9781452182056", 4, 92),
        new BookUserRatingRecord("9781452182056", 3, 14),

        // b18: The Ancient Wayfarer
        new BookUserRatingRecord("9780393608311", 5, 41),
        new BookUserRatingRecord("9780393608311", 4, 22),

        // b19: Flavors of the Mediterranean
        new BookUserRatingRecord("9781607746058", 5, 512),
        new BookUserRatingRecord("9781607746058", 4, 189),
        new BookUserRatingRecord("9781607746058", 3, 34),
        new BookUserRatingRecord("9781607746058", 2, 5),
        new BookUserRatingRecord("9781607746058", 1, 3),

        // b20: Postcards from the Coast
        new BookUserRatingRecord("9780811874557", 5, 28),
        new BookUserRatingRecord("9780811874557", 4, 15)
    );
}
