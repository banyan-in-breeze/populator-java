package banyaninbreeze.demo.bookstore.repository.local;

import java.util.List;

import banyaninbreeze.demo.bookstore.repository.remote.BookUserRatingClient.BookUserRating;

/*
  The model classes manage two categories of fields:
  - Native data fields mapped directly from the data store (i.e., database table)
  - References to related or child objects that are uninitialized at instantiation and populated later

  You may use mutable classes, immutable classes or record type for them, as long as the references to the child objects
  can be populated at a later time after the instantiation of the class.
  
  The classes can be Spring JPA or Spring Data JDBC entities mapped from database queries, and they can be any POJO classes.
  When using Spring JPA or Spring Data JDBC for data mapping, the references to child objects need to be annotated by @Transient
  (there are more than one @Transient annotations, make sure you use the right one).

  If you use record or immutable class, provide withers for the child object references, which
  means a new copy of the object will be created when populating a child object.

  In this demo, I'm using immutable variables for native data fields and mutable variables for child object references.
  Native data fields are populated by constructor at the time of instantiation, and child object references are populated
  later by calling their setters. Using setters reduces memory usage comparing to withers. Also to minimize chances of
  unwanted calls to the setters, I make them protected so that they can be called only within the same package.

  To eliminate dependency on third part libraries, I'm manually writing the constructor, getters and setters. You may
  use lombok in your projects for the boilerplate methods.
 */
public class BookstoreModel {

    public static class Book {
        private final String id;
        private final String isbn;
        private final String title;
        private final int priceInCent;

        // References to child objects, optional
        private List<BookAuthor> bookAuthors;
        private List<BookSubject> bookSubjects;
        private List<BookUserRating> userRatings;
        private BookDetails details;

        public Book(String id, String isbn, String title, int priceInCent) {
            this.id = id;
            this.isbn = isbn;
            this.title = title;
            this.priceInCent = priceInCent;
        }

        public String getId() { return id; }
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public int getPriceInCent() { return priceInCent; }

        public List<BookAuthor> getBookAuthors() { return bookAuthors; }
        public List<BookSubject> getBookSubjects() { return bookSubjects; }
        public List<BookUserRating> getUserRatings() { return userRatings; }
        public BookDetails getDetails() { return details; }

        protected Book setBookAuthors(List<BookAuthor> bookAuthors) {
            this.bookAuthors = bookAuthors;
            return this;
        }

        protected Book setBookSubjects(List<BookSubject> bookSubjects) {
            this.bookSubjects = bookSubjects;
            return this;
        }

        protected Book setUserRatings(List<BookUserRating> userRatings) {
            this.userRatings = userRatings;
            return this;
        }

        protected Book setDetails(BookDetails details) {
            this.details = details;
            return this;
        }
    }

    public static class BookDetails {
        private final String bookId;
        private final String summary;

        public BookDetails(String bookId, String summary) {
            this.bookId = bookId;
            this.summary = summary;
        }

        public String getBookId() { return bookId; }
        public String getSummary() { return summary; }
    }

    public static class Author {
        private final String id;
        private final String name;

        // References to child objects, optional
        private List<BookAuthor> bookAuthors;

        public Author(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<BookAuthor> getBookAuthors() { return bookAuthors; }

        protected Author setBookAuthors(List<BookAuthor> bookAuthors) {
            this.bookAuthors = bookAuthors;
            return this;
        }
    }

    public static class BookAuthor {
        private final String bookId;
        private final String authorId;

        // References to child objects, optional
        private Book book;
        private Author author;

        public BookAuthor(String bookId, String authorId) {
            this.bookId = bookId;
            this.authorId = authorId;
        }

        public String getBookId() { return bookId; }
        public String getAuthorId() { return authorId; }
        public Book getBook() { return book; }
        public Author getAuthor() { return author; }

        protected BookAuthor setBook(Book book) {
            this.book = book;
            return this;
        }

        protected BookAuthor setAuthor(Author author) {
            this.author = author;
            return this;
        }
    }

    public static class Subject {
        private final String id;
        private final String name;

        // References to child objects, optional
        private List<BookSubject> bookSubjects;
        private List<SubjectAuthor> subjectAuthors;

        public Subject(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<BookSubject> getBookSubjects() { return bookSubjects; }
        public List<SubjectAuthor> getSubjectAuthors() { return subjectAuthors; }

        protected Subject setBookSubjects(List<BookSubject> bookSubjects) {
            this.bookSubjects = bookSubjects;
            return this;
        }

        protected Subject setSubjectAuthors(List<SubjectAuthor> subjectAuthors) {
            this.subjectAuthors = subjectAuthors;
            return this;
        }
    }

    public static class BookSubject {
        private final String bookId;
        private final String subjectId;

        // References to child objects, optional
        private Book book;
        private Subject subject;

        public BookSubject(String bookId, String subjectId) {
            this.bookId = bookId;
            this.subjectId = subjectId;
        }

        public String getBookId() { return bookId; }
        public String getSubjectId() { return subjectId; }
        public Book getBook() { return book; }
        public Subject getSubject() { return subject; }

        protected BookSubject setBook(Book book) {
            this.book = book;
            return this;
        }

        protected BookSubject setSubject(Subject subject) {
            this.subject = subject;
            return this;
        }
    }

    /** This class doesn't direct map to a table, but is a derived assocation between subjects and authors */
    public static class SubjectAuthor {
        private final String subjectId;
        private final String authorId;

        // References to child objects, optional
        private Subject subject;
        private Author author;

        public SubjectAuthor(String subjectId, String authorIdId) {
            this.subjectId = subjectId;
            this.authorId = authorIdId;
        }

        public String getSubjectId() { return subjectId; }
        public String getAuthorId() { return authorId; }
        public Subject getSubject() { return subject; }
        public Author getAuthor() { return author; }

        protected SubjectAuthor setAuthor(Author author) {
            this.author = author;
            return this;
        }

        protected SubjectAuthor setSubject(Subject subject) {
            this.subject = subject;
            return this;
        }
    }
}
