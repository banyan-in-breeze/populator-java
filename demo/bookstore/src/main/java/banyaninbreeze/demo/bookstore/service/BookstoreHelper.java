package banyaninbreeze.demo.bookstore.service;

import java.util.Collection;

import banyaninbreeze.demo.bookstore.repository.remote.BookUserRatingClient.BookUserRating;

public class BookstoreHelper {
    public static BookstoreHelper of() {
        return new BookstoreHelper();
    }

    private BookstoreHelper() {}
    
    public Double calculateAverageStars(Collection<BookUserRating> userRatings) {
        if (userRatings == null || userRatings.isEmpty())
            return null;
        else {
            long totalWeightedStars = userRatings.stream()
                .mapToLong(r -> (long) r.stars() * r.numberOfReviews())
                .sum();
            long totalReviews = userRatings.stream()
                .mapToLong(BookUserRating::numberOfReviews)
                .sum();

            double stars = totalReviews > 0 ? (double) totalWeightedStars / totalReviews : 0.0;
            return (Math.round(stars * 10.0) / 10.0);
        }
    }
}
