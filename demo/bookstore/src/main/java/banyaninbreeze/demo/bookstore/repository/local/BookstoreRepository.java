package banyaninbreeze.demo.bookstore.repository.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Author;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Book;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookAuthor;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookDetails;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookSubject;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Subject;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.SubjectAuthor;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.AuthorRow;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.BookAuthorRow;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.BookDetailsRow;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.BookRow;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.BookSubjectRow;
import banyaninbreeze.demo.bookstore.sampledata.SampleDatabase.SubjectRow;
import banyaninbreeze.populator.BatchFinder;

/**
 * Simulate repository for Book Store database access.
 * The find-by-ids methods are all done in batch to avoid going beyond Database's IN clause limit,
 * as well as reduce SQL statument cache.
 * Well, of course, this class is just a simulation and it doesn't make any
 * real database queries, but this demonstrates the idea of batch finding in the real world.
 * 
 * If you are using Spring Data Repository interfaces to implement this, you may define the batch finding
 * find-by-ids methods as interface "default" methods that call the local-find-by-ids methods in batches.
 */
public class BookstoreRepository {

    public List<Subject> findAllSubjects() {
        return SampleDatabase.SUBJECT_TABLE_DATA.stream()
                .map(this::subjectFromRow)
                .toList();
    }

    /**
     * Similate query:
     * SELECT ... FROM book WHERE id IN (bookIds)
     */
    public List<Book> findBooksByBookIds(Collection<String> bookIds) {
        return SampleDatabase.BOOK_TABLE_DATA.stream()
                .filter(bookRow -> bookIds.contains(bookRow.id()))
                .map(this::bookFromRow)
                .toList();
    }
    
    /**
     * Simulate query:
     * SELECT book. ...
     * FROM book INNER JOIN book_subject ON book.id = book_subject.book_id
     * WHERE book_subject.subject_id IN (:subjectIds)
     */
    public List<Book> findBooksBySubjectIds(Collection<String> subjectIds) {
        Set<String> bookIds = SampleDatabase.BOOK_SUBJECT_TABLE_DATA.stream()
                        .filter(bookSubjectRow -> subjectIds.contains(bookSubjectRow.subjectId()))
                        .map(BookSubjectRow::bookId)
                        .collect(Collectors.toSet());
        return SampleDatabase.BOOK_TABLE_DATA.stream()
                .filter(bookRow -> bookIds.contains(bookRow.id()))
                .map(this::bookFromRow)
                .toList();
    }

    /**
     * Simulate query:
     * SELECT book. ...
     * FROM book INNER JOIN book_author ON book.id = book_author.book_id
     * WHERE book_author.author_id IN (:authorIds)
     */
    public List<Book> findBooksByAuthorIds(Collection<String> authorIds) {
        Set<String> bookIds = SampleDatabase.BOOK_AUTHOR_TABLE_DATA.stream()
                        .filter(bookAuthorRow -> authorIds.contains(bookAuthorRow.authorId()))
                        .map(BookAuthorRow::bookId)
                        .collect(Collectors.toSet());
        return SampleDatabase.BOOK_TABLE_DATA.stream()
                .filter(bookRow -> bookIds.contains(bookRow.id()))
                .map(this::bookFromRow)
                .toList();
    }

    /**
     * Simulate query to match keyword in book title and author name:
     * SELECT DISTINCT book. ...
     * FROM book INNER JOIN book_author ON book.id = book_author.book_id
     *      INNER JOIN author on book_author.author_id = author.id
     * WHERE book.title LIKE :keyword OR author.title LIKE :keyword
     */
    public List<Book> findBooksByKeyword(String keyword) {
        Map<String, BookRow> bookMap = SampleDatabase.getBookRowMap();
        Map<String, AuthorRow> authorMap = SampleDatabase.getAuthorRowMap();
        var lowercaseKeyword = keyword.toLowerCase();
        return SampleDatabase.BOOK_AUTHOR_TABLE_DATA.stream()
                .filter(bookAuthorRow -> bookMap.get(bookAuthorRow.bookId()).title().toLowerCase().contains(lowercaseKeyword)
                                    || authorMap.get(bookAuthorRow.authorId()).name().toLowerCase().contains(lowercaseKeyword))
                .map(BookAuthorRow::bookId)
                .distinct()
                .map(bookMap::get)
                .map(this::bookFromRow)
                .toList();
    }

    /**
     * Similate query:
     * SELECT ... FROM book_details WHERE book_id IN (bookIds)
     */
    public List<BookDetails> findBookDetailsByBookIds(Collection<String> bookIds) {
        return SampleDatabase.BOOK_DETAILS_TABLE_DATA.stream()
                .filter(bookDetailsRow -> bookIds.contains(bookDetailsRow.bookId()))
                .map(this::bookDetailsFromRow)
                .toList();
    }

    /**
     * Simulate query:
     * SELECT DISTINCT book_subject.subject_id, book_author.author_id
     * FROM book_subject INNER JOIN book_author ON book_subject.book_id = book_author.book_id
     * WHERE book_subject.subject_id IN (:subjectIds)
     */
    public List<SubjectAuthor> findSubjectAuthorsBySubjectIds(Collection<String> subjectIds) {
        List<BookSubjectRow> bookSubjectRows = SampleDatabase.BOOK_SUBJECT_TABLE_DATA.stream()
                                .filter(bookSubjectRow -> subjectIds.contains(bookSubjectRow.subjectId()))
                                .toList();
        return composeSubjectAuthors(bookSubjectRows, SampleDatabase.BOOK_AUTHOR_TABLE_DATA);
    }

    private List<SubjectAuthor> composeSubjectAuthors(List<BookSubjectRow> bookSubjectRows, List<BookAuthorRow> bookAuthorRows) {
        Map<String, List<BookSubjectRow>> bookSubjectMap = bookSubjectRows.stream()
                        .collect(Collectors.groupingBy(BookSubjectRow::subjectId));
        Map<String, List<BookAuthorRow>> bookAuthorMap = bookAuthorRows.stream()
                        .collect(Collectors.groupingBy(BookAuthorRow::bookId));

        List<SubjectAuthor> subjectAuthors = new ArrayList<>();
        for (String subjectId : bookSubjectMap.keySet()) {
            Set<String> bookIds = bookSubjectMap.get(subjectId).stream()
                    .map(BookSubjectRow::bookId)
                    .collect(Collectors.toSet());
            Set<String> authorIds = new HashSet<String>();
            for (String bookId : bookIds) {
                authorIds.addAll(bookAuthorMap.get(bookId).stream().map(BookAuthorRow::authorId).collect(Collectors.toSet()));
            }
            subjectAuthors.addAll(authorIds.stream().map(authorId -> new SubjectAuthor(subjectId, authorId)).toList());
        }
        return subjectAuthors;
    }

    public List<Book> findBooksByIds(Collection<String> bookIds) {
        return BatchFinder.find(bookIds, this::localFindBooksByIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM book WHERE id IN (:bookIds)
     */
    private List<Book> localFindBooksByIds(Collection<String> bookIds) {
        return SampleDatabase.BOOK_TABLE_DATA.stream()
                .filter(bookRow -> bookIds.contains(bookRow.id()))
                .map(this::bookFromRow)
                .toList();
    }

    public List<Author> findAuthorsByIds(Collection<String> authorIds) {
        return BatchFinder.find(authorIds, this::localFindAuthorsByIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM author WHERE id IN (:authorIds)
     */
    private List<Author> localFindAuthorsByIds(Collection<String> authorIds) {
        return SampleDatabase.AUTHOR_TABLE_DATA.stream()
                .filter(authorRow -> authorIds.contains(authorRow.id()))
                .map(this::authorFromRow)
                .toList();
    }

    public List<Subject> findSubjectsByIds(Collection<String> subjectIds) {
        return BatchFinder.find(subjectIds, this::localFindSubjectsByIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM subject WHERE id IN (:subjectIds)
     */
    private List<Subject> localFindSubjectsByIds(Collection<String> subjectIds) {
        return SampleDatabase.SUBJECT_TABLE_DATA.stream()
                .filter(subjectRow -> subjectIds.contains(subjectRow.id()))
                .map(this::subjectFromRow)
                .toList();
    }

    public List<BookAuthor> findBookAuthorsByBookIds(Collection<String> bookIds) {
        return BatchFinder.find(bookIds, this::localFindBookAuthorsByBookIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM book_author WHERE book_id IN (:bookIds)
     */
    private List<BookAuthor> localFindBookAuthorsByBookIds(Collection<String> bookIds) {
        return SampleDatabase.BOOK_AUTHOR_TABLE_DATA.stream()
                .filter(bookAuthorRow -> bookIds.contains(bookAuthorRow.bookId()))
                .map(this::bookAuthorFromRow)
                .toList();
    }

    public List<BookAuthor> findBookAuthorsByAuthorIds(Collection<String> authorIds) {
        return BatchFinder.find(authorIds, this::localFindBookAuthorsByAuthorIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM book_author WHERE author_id IN (:authorIds)
     */
    private List<BookAuthor> localFindBookAuthorsByAuthorIds(Collection<String> authorIds) {
        return SampleDatabase.BOOK_AUTHOR_TABLE_DATA.stream()
                .filter(bookAuthorRow -> authorIds.contains(bookAuthorRow.authorId()))
                .map(this::bookAuthorFromRow)
                .toList();
    }

    public List<BookSubject> findBookSubjectsByBookIds(Collection<String> bookIds) {
        return BatchFinder.find(bookIds, this::localFindBookSubjectsByBookIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM book_subject WHERE book_id IN (:bookIds)
     */
    private List<BookSubject> localFindBookSubjectsByBookIds(Collection<String> bookIds) {
        return SampleDatabase.BOOK_SUBJECT_TABLE_DATA.stream()
                .filter(bookSubjectRow -> bookIds.contains(bookSubjectRow.bookId()))
                .map(this::bookSubjectFromRow)
                .toList();
    }

    public List<BookSubject> findBookSubjectsBySubjectIds(Collection<String> subjectIds) {
        return BatchFinder.find(subjectIds, this::localFindBookSubjectsBySubjectIds);
    }

    /**
     * Simulate query:
     * SELECT ... FROM book_subject WHERE subject_id IN (:subjectIds)
     */
    private List<BookSubject> localFindBookSubjectsBySubjectIds(Collection<String> subjectIds) {
        return SampleDatabase.BOOK_SUBJECT_TABLE_DATA.stream()
                .filter(bookSubjectRow -> subjectIds.contains(bookSubjectRow.subjectId()))
                .map(this::bookSubjectFromRow)
                .toList();
    }

    private Book bookFromRow(BookRow row) {
        return new Book(row.id(), row.isbn(), row.title(), row.priceInCents());
    }

    private BookDetails bookDetailsFromRow(BookDetailsRow row) {
        return new BookDetails(row.bookId(), row.summary());
    }

    private Author authorFromRow(AuthorRow row) {
        return new Author(row.id(), row.name());
    }

    private Subject subjectFromRow(SubjectRow row) {
        return new Subject(row.id(), row.name());
    }

    private BookAuthor bookAuthorFromRow(BookAuthorRow row) {
        return new BookAuthor(row.bookId(), row.authorId());
    }

    private BookSubject bookSubjectFromRow(BookSubjectRow row) {
        return new BookSubject(row.bookId(), row.subjectId());
    }

}
