package banyaninbreeze.demo.bookstore.repository.remote;

import java.util.Collection;
import java.util.List;

import banyaninbreeze.demo.bookstore.sampledata.SampleRemoteData;
import banyaninbreeze.demo.bookstore.sampledata.SampleRemoteData.BookUserRatingRecord;

/**
 * Simulate client for remote API calls to retrieve book user ratings
 */
public class BookUserRatingClient {
    public static record BookUserRating(String isbn, int stars, int numberOfReviews) {}

    /**
     * Simulate remote call to POST https://SomeBookReviewSite.com/api/user-ratings/by-isbn
     */
    public List<BookUserRating> findUserRatingsByIsbns(Collection<String> isbns) {
        return SampleRemoteData.BOOK_USER_RATING_DATA.stream()
                .filter(userRating -> isbns.contains(userRating.isbn()))
                .map(BookUserRatingClient::userRatingFromRecord)
                .toList();
    }

    private static BookUserRating userRatingFromRecord(BookUserRatingRecord record) {
        return new BookUserRating(record.isbn(), record.stars(), record.numberOfReviews());
    }
}
