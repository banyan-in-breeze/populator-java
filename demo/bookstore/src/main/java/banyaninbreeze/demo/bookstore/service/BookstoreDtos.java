package banyaninbreeze.demo.bookstore.service;

import java.util.List;

import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Author;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Book;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookAuthor;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.BookSubject;
import banyaninbreeze.demo.bookstore.repository.local.BookstoreModel.Subject;
import banyaninbreeze.demo.bookstore.repository.remote.BookUserRatingClient.BookUserRating;

public class BookstoreDtos {

    public static record SubjectDto(String subjectId, String subject) {
        public static SubjectDto from(Subject subject) {
            return new SubjectDto(subject.getId(), subject.getName());
        }
    }

    public static record AuthorDto(String authorId, String authorName, List<String> allBooks) {
        public static AuthorDto from(Author author) {
            return new AuthorDto(author.getId(), author.getName(),
                        author.getBookAuthors() == null || author.getBookAuthors().isEmpty() ? List.of() :
                            author.getBookAuthors().stream().map(ba -> ba.getBook().getTitle()).toList()
                    );
        }
    }

    public static record BookDto(String bookId, String title, List<AuthorDto> authors,
                             Integer priceInCent, Double stars, List<SubjectDto> subjects) {
        public static BookDto from(Book book, BookstoreHelper bookstoreHelper) {
            return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getBookAuthors().stream().map(bookAuthor -> AuthorDto.from(bookAuthor.getAuthor())).toList(),
                book.getPriceInCent(),
                bookstoreHelper.calculateAverageStars(book.getUserRatings()),
                book.getBookSubjects() == null ? null :
                    book.getBookSubjects().stream()
                        .map(bookSubject -> SubjectDto.from(bookSubject.getSubject()))
                        .toList()
            );
        }
    }

    public static record SubjectAuthorsDto(SubjectDto subject, List<AuthorDto> authors) {
        public static SubjectAuthorsDto from(Subject subject) {
            return new SubjectAuthorsDto(
                        SubjectDto.from(subject),
                        subject.getSubjectAuthors().stream()
                            .map(subjectAuthor -> AuthorDto.from(subjectAuthor.getAuthor()))
                            .toList()
                    );

        }
    }
    
    public static record SubjectBooksDto(SubjectDto subject, List<BookDto> books) {
        public static SubjectBooksDto from(Subject subject, BookstoreHelper bookstoreHelper) {
            return new SubjectBooksDto(
                SubjectDto.from(subject),
                subject.getBookSubjects().stream()
                    .map(BookSubject::getBook)
                    .map(book -> BookDto.from(book, bookstoreHelper))
                    .toList()
            );
        }
    }

    public static record AuthorBooksDto(AuthorDto author, List<BookDto> books) {
        public static AuthorBooksDto from(Author author, BookstoreHelper bookstoreHelper) {
            return new AuthorBooksDto(
                AuthorDto.from(author),
                author.getBookAuthors().stream()
                    .map(BookAuthor::getBook)
                    .map(book -> BookDto.from(book, bookstoreHelper))
                    .toList()
            );
        }
    }

    public static record UserRatingDto(int stars, int numberOfReviews) {
        public static UserRatingDto from(BookUserRating userRating) {
            return new UserRatingDto(userRating.stars(), userRating.numberOfReviews());
        }
    }

    public static record BookDetailsDto(String bookId, String isbn, String title, List<AuthorDto> authors,  String summary,
                             Integer priceInCent, Double stars, List<UserRatingDto> userRatings, List<SubjectDto> subjects) {
        public static BookDetailsDto from(Book book, BookstoreHelper bookstoreHelper) {
            return new BookDetailsDto(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getBookAuthors().stream().map(bookAuthor -> AuthorDto.from(bookAuthor.getAuthor())).toList(),
                book.getDetails().getSummary(),
                book.getPriceInCent(),
                bookstoreHelper.calculateAverageStars(book.getUserRatings()),
                book.getUserRatings().stream().map(UserRatingDto::from).toList(),
                book.getBookSubjects() == null ? null :
                    book.getBookSubjects().stream()
                        .map(bookSubject -> SubjectDto.from(bookSubject.getSubject()))
                        .toList()
            );
        }
    }
}
