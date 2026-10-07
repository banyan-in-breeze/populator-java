package banyaninbreeze.demo.bookstore.sampledata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides hardcoded sample data for a bookstore to simulate database tables.
 * <p>Book to Author is many-to-many, Book to Subject is also many to many.</p>
 */
public class SampleDatabase {
    public static record BookRow(String id, String isbn, String title, int priceInCents) {}
    public static record BookDetailsRow(String bookId, String summary) {}
    public static record AuthorRow(String id, String name) {}
    public static record BookAuthorRow(String bookId, String authorId) {}
    public static record SubjectRow(String id, String name) {}
    public static record BookSubjectRow(String bookId, String subjectId) {}

    public static final List<SubjectRow> SUBJECT_TABLE_DATA = List.of(
        new SubjectRow("s01", "Technology & Computing"),
        new SubjectRow("s02", "Philosophy"),
        new SubjectRow("s03", "History"),
        new SubjectRow("s04", "Health"),
        new SubjectRow("s05", "Travel")
    );

    public static final List<AuthorRow> AUTHOR_TABLE_DATA = List.of(
        new AuthorRow("a01", "Dr. Elena Vance"),
        new AuthorRow("a02", "Marcus Thorne"),
        new AuthorRow("a03", "Sarah Jenkins"),
        new AuthorRow("a04", "Liam O'Connor"),
        new AuthorRow("a05", "Professor David Kim"),
        new AuthorRow("a06", "Amira Patel"),
        new AuthorRow("a07", "Chloe Laurent")
    );

    public static final List<BookRow> BOOK_TABLE_DATA = List.of(
        new BookRow("b01", "9780132350884", "The Sentient Stack", 2999),
        new BookRow("b02", "9781491950296", "Rust for Radical Scale", 3495),
        new BookRow("b03", "9780596007126", "Code as Culture", 2499),
        new BookRow("b04", "9781119642817", "The Zero-Trust Mirage", 3999),
        new BookRow("b05", "9780140449136", "An Echo in the Cave", 1495),
        new BookRow("b06", "9780190947293", "Ethics of the Unseen", 1899),
        new BookRow("b07", "9780465065707", "The Quantified Soul", 2250),
        new BookRow("b08", "9781107604612", "Post-Human Logic", 1999),
        new BookRow("b09", "9780307279460", "Shadows of the Silk Road", 1699),
        new BookRow("b10", "9780393064650", "The Ink-Stained Century", 2800),
        new BookRow("b11", "9781107539037", "Empires of the Monsoon", 3250),
        new BookRow("b12", "9780307353306", "Before the Concrete", 1599),
        new BookRow("b13", "9781501171345", "The Circadian Rhythm Code", 1799),
        new BookRow("b14", "9780544256446", "The Wellness Trekker", 2100),
        new BookRow("b15", "9780143122111", "A History of Healing Herbs", 1995),
        new BookRow("b16", "9781449331818", "The Ergonomic Engineer", 2799),
        new BookRow("b17", "9781452182056", "Atlas of Lost Trails", 2500),
        new BookRow("b18", "9780393608311", "The Ancient Wayfarer", 1899),
        new BookRow("b19", "9781607746058", "Flavors of the Mediterranean", 3000),
        new BookRow("b20", "9780811874557", "Postcards from the Coast", 1499)
    );

    public static final List<BookDetailsRow> BOOK_DETAILS_TABLE_DATA = List.of(
        new BookDetailsRow("b01", "A comprehensive guide to building autonomous, self-healing software architectures using modern AI design patterns."),
        new BookDetailsRow("b02", "An advanced look at deploying highly concurrent, memory-safe backend services globally using the Rust programming language."),
        new BookDetailsRow("b03", "An analytical study of how development workflows, open-source communities, and engineering practices shape modern corporate cultures."),
        new BookDetailsRow("b04", "A practical evaluation of contemporary cybersecurity architectures, breaking down the myths and realities of zero-trust networks."),
        new BookDetailsRow("b05", "A thoughtful exploration of ancient philosophical dialogues and their surprising relevance to today's ethical dilemmas."),
        new BookDetailsRow("b06", "An examination of organizational transparency and the hidden human factors that drive decision-making in large tech systems."),
        new BookDetailsRow("b07", "A deep dive into data tracking, bio-metrics, and how modern software tools attempt to measure human productivity and well-being."),
        new BookDetailsRow("b08", "An academic text investigating mathematical reasoning and logic systems designed for next-generation automated problem solvers."),
        new BookDetailsRow("b09", "A historic travelogue retracing the ancient trade networks that connected diverse cultures across central Asia for centuries."),
        new BookDetailsRow("b10", "A detailed historical narrative covering the evolution of printing, independent publishing, and journalism in the twentieth century."),
        new BookDetailsRow("b11", "An economic history exploring how seasonal weather patterns and global trade routes shaped the coastal kingdoms of the Indian Ocean."),
        new BookDetailsRow("b12", "An architectural and environmental look at urban landscapes before the widespread adoption of industrial materials and concrete."),
        new BookDetailsRow("b13", "A science-backed guide to understanding human biological clocks and optimizing daily routines for better sleep and focus."),
        new BookDetailsRow("b14", "A travel memoir following a researcher's global journey to understand different cultural habits around longevity and wellness."),
        new BookDetailsRow("b15", "A botanical history documenting how various ancient civilizations cultivated and classified plants for holistic remedies."),
        new BookDetailsRow("b16", "An industrial design handbook focused on constructing workspaces and digital interfaces that maximize comfort and reduce physical strain."),
        new BookDetailsRow("b17", "A beautiful geographic collection charting abandoned pathways, historical maps, and forgotten trade routes across North America."),
        new BookDetailsRow("b18", "A historical biography highlighting the lives, navigation techniques, and tools used by ancient nomadic explorers."),
        new BookDetailsRow("b19", "A culinary journey exploring regional ingredients, traditional techniques, and healthy recipes from Mediterranean coastal towns."),
        new BookDetailsRow("b20", "A visual appreciation of seaside geography, local architectures, and cultural landmarks captured along the world's coastlines.")
    );
    
    public static final List<BookAuthorRow> BOOK_AUTHOR_TABLE_DATA = List.of(
        // The Sentient Stack (Dr. Elena Vance, Marcus Thorne)
        new BookAuthorRow("b01", "a01"),
        new BookAuthorRow("b01", "a02"),
        
        // Rust for Radical Scale (Marcus Thorne)
        new BookAuthorRow("b02", "a02"),
        
        // Code as Culture (Sarah Jenkins, Dr. Elena Vance)
        new BookAuthorRow("b03", "a03"),
        new BookAuthorRow("b03", "a01"),
        
        // The Zero-Trust Mirage (Liam O'Connor)
        new BookAuthorRow("b04", "a04"),
        
        // An Echo in the Cave (Dr. Elena Vance)
        new BookAuthorRow("b05", "a01"),
        
        // Ethics of the Unseen (Professor David Kim, Sarah Jenkins)
        new BookAuthorRow("b06", "a05"),
        new BookAuthorRow("b06", "a03"),
        
        // The Quantified Soul (Dr. Elena Vance)
        new BookAuthorRow("b07", "a01"),
        
        // Post-Human Logic (Marcus Thorne)
        new BookAuthorRow("b08", "a02"),
        
        // Shadows of the Silk Road (Amira Patel)
        new BookAuthorRow("b09", "a06"),
        
        // The Ink-Stained Century (Sarah Jenkins)
        new BookAuthorRow("b10", "a03"),
        
        // Empires of the Monsoon (Amira Patel, Professor David Kim)
        new BookAuthorRow("b11", "a06"),
        new BookAuthorRow("b11", "a05"),
        
        // Before the Concrete (Liam O'Connor)
        new BookAuthorRow("b12", "a04"),
        
        // The Circadian Rhythm Code (Liam O'Connor)
        new BookAuthorRow("b13", "a04"),
        
        // The Wellness Trekker (Chloe Laurent, Liam O'Connor)
        new BookAuthorRow("b14", "a07"),
        new BookAuthorRow("b14", "a04"),
        
        // A History of Healing Herbs (Chloe Laurent)
        new BookAuthorRow("b15", "a07"),
        
        // The Ergonomic Engineer (Marcus Thorne)
        new BookAuthorRow("b16", "a02"),
        
        // Atlas of Lost Trails (Marcus Thorne)
        new BookAuthorRow("b17", "a02"),
        
        // The Ancient Wayfarer (Dr. Elena Vance, Chloe Laurent)
        new BookAuthorRow("b18", "a01"),
        new BookAuthorRow("b18", "a07"),
        
        // Flavors of the Mediterranean (Sarah Jenkins)
        new BookAuthorRow("b19", "a03"),
        
        // Postcards from the Coast (Amira Patel)
        new BookAuthorRow("b20", "a06")
    );

    public static final List<BookSubjectRow> BOOK_SUBJECT_TABLE_DATA = List.of(
        new BookSubjectRow("b01", "s01"), // The Sentient Stack -> Tech
        new BookSubjectRow("b01", "s02"), // The Sentient Stack -> Philosophy
        new BookSubjectRow("b02", "s01"), // Rust for Radical Scale -> Tech
        new BookSubjectRow("b03", "s01"), // Code as Culture -> Tech
        new BookSubjectRow("b03", "s03"), // Code as Culture -> History
        new BookSubjectRow("b04", "s01"), // The Zero-Trust Mirage -> Tech
        new BookSubjectRow("b05", "s02"), // An Echo in the Cave -> Philosophy
        new BookSubjectRow("b06", "s02"), // Ethics of the Unseen -> Philosophy
        new BookSubjectRow("b07", "s02"), // The Quantified Soul -> Philosophy
        new BookSubjectRow("b07", "s01"), // The Quantified Soul -> Tech
        new BookSubjectRow("b08", "s02"), // Post-Human Logic -> Philosophy
        new BookSubjectRow("b09", "s03"), // Shadows of the Silk Road -> History
        new BookSubjectRow("b10", "s03"), // The Ink-Stained Century -> History
        new BookSubjectRow("b11", "s03"), // Empires of the Monsoon -> History
        new BookSubjectRow("b12", "s03"), // Before the Concrete -> History
        new BookSubjectRow("b13", "s04"), // The Circadian Rhythm Code -> Health
        new BookSubjectRow("b14", "s04"), // The Wellness Trekker -> Health
        new BookSubjectRow("b14", "s05"), // The Wellness Trekker -> Travel
        new BookSubjectRow("b15", "s04"), // A History of Healing Herbs -> Health
        new BookSubjectRow("b15", "s03"), // A History of Healing Herbs -> History
        new BookSubjectRow("b16", "s04"), // The Ergonomic Engineer -> Health
        new BookSubjectRow("b16", "s01"), // The Ergonomic Engineer -> Tech
        new BookSubjectRow("b17", "s05"), // Atlas of Lost Trails -> Travel
        new BookSubjectRow("b18", "s05"), // The Ancient Wayfarer -> Travel
        new BookSubjectRow("b18", "s03"), // The Ancient Wayfarer -> History
        new BookSubjectRow("b19", "s05"), // Flavors of the Mediterranean -> Travel
        new BookSubjectRow("b19", "s04"), // Flavors of the Mediterranean -> Health
        new BookSubjectRow("b20", "s05")  // Postcards from the Coast -> Travel
    );

    public static Map<String, BookRow> getBookRowMap() {
        return BOOK_TABLE_DATA.stream()
            .collect(Collectors.toMap(
                BookRow::id,
                book -> book
            ));
    }

    public static Map<String, AuthorRow> getAuthorRowMap() {
        return AUTHOR_TABLE_DATA.stream()
            .collect(Collectors.toMap(
                AuthorRow::id,
                author -> author
            ));
    }

    public static Map<String, SubjectRow> getSubjectRowMap() {
        return SUBJECT_TABLE_DATA.stream()
            .collect(Collectors.toMap(
                SubjectRow::id,
                subject -> subject
            ));
    }
}
