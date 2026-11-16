package banyaninbreeze.demo.bookstore.repository.local;

import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Author;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Book;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookAuthor;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookDetails;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookSubject;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Subject;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.SubjectAuthor;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.SubjectBookCount;
import banyaninbreeze.demo.bookstore.repository.remote.BookUserRatingClient;
import banyaninbreeze.demo.bookstore.repository.remote.BookUserRatingClient.BookUserRating;
import banyaninbreeze.populator.Populator.ChildModifier;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * PopulateParams creators. It can be split up into multiple classes each of which focuses on
 * creating PopulateParams for a smaller number of model classes.
 * I put them all in one class just to be convenient.
 * 
 * Note: you can call PopulateParams's ofOne() and ofMany() either like:
 *     PopulateParams.<Parent, Child, ID>ofMany(...)
 *  or just like:
 *     PopulateParams.ofMany(...).
 *  However, placing <Parent, Child, ID> in front of ofMany() and ofOne() allows IDE
 *  to provide hints and detect specific errors for the parameters.
 */
public class BookstorePopulateParamsCreator {
    private final BookstoreRepository bookstoreRepo = new BookstoreRepository();
    private final BookUserRatingClient userRatingClient = new BookUserRatingClient();

    public PopulateParams<Book, BookAuthor, String> bookWithBookAuthors(ChildModifier<BookAuthor> childModifier) {
        return PopulateParams.<Book, BookAuthor, String>ofMany(
                Book::getId,
                BookAuthor::getBookId,
                Book::setBookAuthors,
                bookIds -> childModifier.modify(bookstoreRepo.findBookAuthorsByBookIds(bookIds)));
    }

    public PopulateParams<Book, BookSubject, String> bookWithBookSubjects(ChildModifier<BookSubject> childModifier) {
        return PopulateParams.<Book, BookSubject, String>ofMany(
                Book::getId,
                BookSubject::getBookId,
                Book::setBookSubjects,
                bookIds -> childModifier.modify(bookstoreRepo.findBookSubjectsByBookIds(bookIds)));
    }

    public PopulateParams<Book, BookUserRating, String> bookWithUserRatings() {
        return PopulateParams.<Book, BookUserRating, String>ofMany(
                Book::getIsbn, BookUserRating::isbn, Book::setUserRatings,
                userRatingClient::findUserRatingsByIsbns);
    }

    public PopulateParams<Book, BookDetails, String> bookWithBookDetails() {
        return PopulateParams.<Book, BookDetails, String>ofOne(
                Book::getId,
                BookDetails::getBookId,
                Book::setDetails,
                bookstoreRepo::findBookDetailsByBookIds);
    }

    public PopulateParams<Subject, BookSubject, String> subjectWithBookSubjects(ChildModifier<BookSubject> childModifier) {
        return PopulateParams.<Subject, BookSubject, String>ofMany(
                Subject::getId,
                BookSubject::getSubjectId,
                Subject::setBookSubjects,
                subjectIds -> childModifier.modify(bookstoreRepo.findBookSubjectsBySubjectIds(subjectIds)));
    }

    public PopulateParams<Subject, SubjectAuthor, String> subjectWithSubjectAuthor(ChildModifier<SubjectAuthor> childModifier) {
        return PopulateParams.<Subject, SubjectAuthor, String>ofMany(
            Subject::getId,
            SubjectAuthor::getSubjectId,
            Subject::setSubjectAuthors,
            subjectIds -> childModifier.modify(bookstoreRepo.findSubjectAuthorsBySubjectIds(subjectIds)));
    }

    public PopulateParams<Author, BookAuthor, String> authorWithBookAuthor(ChildModifier<BookAuthor> childModifier) {
        return PopulateParams.<Author, BookAuthor, String>ofMany(
                Author::getId,
                BookAuthor::getAuthorId,
                Author::setBookAuthors,
                authorIds -> childModifier.modify(bookstoreRepo.findBookAuthorsByAuthorIds(authorIds)));
    }

    public PopulateParams<BookAuthor, Author, String> bookAuthorWithAuthor(ChildModifier<Author> childModifier) {
        return PopulateParams.<BookAuthor, Author, String>ofOne(
                BookAuthor::getAuthorId,
                Author::getId,
                BookAuthor::setAuthor,
                authorIds -> childModifier.modify(bookstoreRepo.findAuthorsByIds(authorIds)));
    }

    public PopulateParams<BookAuthor, Book, String> bookAuthorWithBook(ChildModifier<Book> childModifier) {
        return PopulateParams.<BookAuthor, Book, String>ofOne(
                BookAuthor::getBookId,
                Book::getId,
                BookAuthor::setBook,
                bookIds -> childModifier.modify(bookstoreRepo.findBooksByIds(bookIds)));
    }

    public PopulateParams<BookSubject, Subject, String> bookSubjectWithSubject() {
        return PopulateParams.<BookSubject, Subject, String>ofOne(
                BookSubject::getSubjectId,
                Subject::getId,
                BookSubject::setSubject,
                bookstoreRepo::findSubjectsByIds);
    }

    public PopulateParams<BookSubject, Book, String> bookSubjectWithBook(ChildModifier<Book> childModifier) {
        return PopulateParams.<BookSubject, Book, String>ofOne(
                BookSubject::getBookId,
                Book::getId,
                BookSubject::setBook,
                bookIds -> childModifier.modify(bookstoreRepo.findBooksByIds(bookIds)));
    }

    public PopulateParams<SubjectAuthor, Author, String> subjectAuthorWithAuthor() {
        return PopulateParams.<SubjectAuthor, Author, String>ofOne(
            SubjectAuthor::getAuthorId,
            Author::getId,
            SubjectAuthor::setAuthor,
            bookstoreRepo::findAuthorsByIds
        );
    }

    public PopulateParams<Subject, SubjectBookCount, String> subjectWithSubjectBookCounts() {
        return PopulateParams.<Subject, SubjectBookCount, String>ofOne(
            Subject::getId,
            SubjectBookCount::getSubjectId,
            Subject::setSubjectBookCount,
            bookstoreRepo::findSubjectBookCountsBySubjectIds);
    }
}
