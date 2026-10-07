package banyaninbreeze.demo.bookstore.service;

import java.util.List;

import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Author;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Book;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Subject;
import banyaninbreeze.demo.bookstore.repository.local.BookstorePopulateParamsCreator;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreRepository;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.AuthorBooksDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.BookDetailsDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.BookDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.SubjectAuthorsDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.SubjectBooksDto;
import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.ChildModifier;

public class BookstoreService {
    private final BookstoreRepository bookstoreRepo = new BookstoreRepository();
    private final BookstorePopulateParamsCreator popParamsCreator = new BookstorePopulateParamsCreator();

    public List<BookDto> findBooksByKeyword(String keyword) {
        List<Book> books = bookstoreRepo.findBooksByKeyword(keyword);
        books = Populator.of(books)
                .populate(popParamsCreator.bookWithBookAuthors(
                    bookAuthors -> Populator.of(bookAuthors)
                        .populate(popParamsCreator.bookAuthorWithAuthor(ChildModifier.none()))
                        .run()
                ))
                .populate(popParamsCreator.bookWithBookSubjects(
                    bookSubjects -> Populator.of(bookSubjects)
                        .populate(popParamsCreator.bookSubjectWithSubject())
                        .run()
                ))
                .populate(popParamsCreator.bookWithUserRatings())
                .runParallel(); // or .run() to populate children sequentially

        var bookstoreHelper = BookstoreHelper.of();
        return books.stream().map(book -> BookDto.from(book, bookstoreHelper)).toList();
    }

    public List<SubjectBooksDto> findBooksBySubjects(List<String> subjectIds) {
        List<Subject> subjects = bookstoreRepo.findSubjectsByIds(subjectIds);
        subjects = Populator.of(subjects)
                .populate(popParamsCreator.subjectWithBookSubjects(
                    bookSubjects -> Populator.of(bookSubjects)
                        .populate(popParamsCreator.bookSubjectWithBook(
                            books -> Populator.of(books)
                                .populate(popParamsCreator.bookWithBookAuthors(
                                    bookAuthors -> Populator.of(bookAuthors)
                                        .populate(popParamsCreator.bookAuthorWithAuthor(ChildModifier.none()))
                                        .run()
                                ))
                                .populate(popParamsCreator.bookWithUserRatings())
                                .run() // or .runParallel() to populate children sequentially
                        ))
                        .run()
                ))
                .run();

        var bookstoreHelper = BookstoreHelper.of();
        return subjects.stream().map(subject -> SubjectBooksDto.from(subject, bookstoreHelper)).toList();
    }

    public List<AuthorBooksDto> findBooksByAuthors(List<String> authorIds) {
        List<Author> authors = bookstoreRepo.findAuthorsByIds(authorIds);
        authors = Populator.of(authors)
                .populate(popParamsCreator.authorWithBookAuthor(
                    bookAuthors -> Populator.of(bookAuthors)
                        .populate(popParamsCreator.bookAuthorWithBook(
                            books -> Populator.of(books)
                                .populate(popParamsCreator.bookWithBookAuthors(
                                    subBookAuthors -> Populator.of(subBookAuthors)
                                        .populate(popParamsCreator.bookAuthorWithAuthor(ChildModifier.none()))
                                        .run()
                                ))
                                .populate(popParamsCreator.bookWithBookSubjects(
                                    bookSubjects -> Populator.of(bookSubjects)
                                        .populate(popParamsCreator.bookSubjectWithSubject())
                                        .run()
                                ))
                                .populate(popParamsCreator.bookWithUserRatings())
                                .runParallel() // or .run() to populate children sequentially
                        ))
                        .run()
                ))
                .run();

        var bookstoreHelper = BookstoreHelper.of();
        return authors.stream().map(author -> AuthorBooksDto.from(author, bookstoreHelper)).toList();
    }

    public List<BookDetailsDto> findBookDetailsByBookIds(List<String> bookIds) {
        List<Book> books = bookstoreRepo.findBooksByBookIds(bookIds);
        books = Populator.of(books)
                .populate(popParamsCreator.bookWithBookAuthors(
                    bookAuthors -> Populator.of(bookAuthors)
                        .populate(popParamsCreator.bookAuthorWithAuthor(
                            // Populate the authors with all their books that buyers may want to see
                            authors -> Populator.of(authors)
                                .populate(popParamsCreator.authorWithBookAuthor(
                                    subBookAuthors -> Populator.of(subBookAuthors)
                                        .populate(popParamsCreator.bookAuthorWithBook(ChildModifier.none()))
                                        .run()
                                ))
                                .run()
                        ))
                        .run()
                ))
                .populate(popParamsCreator.bookWithBookSubjects(
                    bookSubjects -> Populator.of(bookSubjects)
                        .populate(popParamsCreator.bookSubjectWithSubject())
                        .run()
                ))
                .populate(popParamsCreator.bookWithBookDetails())
                .populate(popParamsCreator.bookWithUserRatings())
                .runParallel(); // or .run() to populate children sequentially

        var bookstoreHelper = BookstoreHelper.of();
        return books.stream().map(book -> BookDetailsDto.from(book, bookstoreHelper)).toList();
    }

    public List<SubjectAuthorsDto> findAllSubjectsWithAuthors() {
        List<Subject> subjects = bookstoreRepo.findAllSubjects();
        subjects = Populator.of(subjects)
                .populate(popParamsCreator.subjectWithSubjectAuthor(
                    subjectAuthors -> Populator.of(subjectAuthors)
                        .populate(popParamsCreator.subjectAuthorWithAuthor())
                        .run()
                ))
                .run();
        return subjects.stream().map(SubjectAuthorsDto::from).toList();
    }
}
