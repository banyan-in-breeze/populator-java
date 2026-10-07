package banyaninbreeze.demo.bookstore;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import banyaninbreeze.demo.bookstore.service.BookstoreDtos.AuthorBooksDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.AuthorDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.BookDetailsDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.BookDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.SubjectAuthorsDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.SubjectBooksDto;
import banyaninbreeze.demo.bookstore.service.BookstoreDtos.SubjectDto;
import banyaninbreeze.demo.bookstore.service.BookstoreService;
import banyaninbreeze.populator.Populator;

public class Bookstore {
    private static BookstoreService bookService = new BookstoreService();
    static {
        // Note: if you are running Java 21+, uncomment this line to use virtual threads
        // Populator.setGlobalExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        while (choice != 0) {
            displayMenu();
            System.out.print("Enter your choice: ");
            
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume the leftover newline character
                printLine("");
                handleMenuChoice(choice, scanner);
            } else {
                printLine("\n[Error] Invalid input. Please enter a number.\n");
                scanner.nextLine();
            }
        }
        
        scanner.close();
        Populator.getGlobalExecutor().shutdown();
    }

    private static void displayMenu() {
        printLine("=================================");
        printLine("1. List All Subjects and Authors");
        printLine("2. Search Books by Subject");
        printLine("3. Search Books by Author");
        printLine("4. Search Books by Keyword");
        printLine("5. View Book Details");
        printLine("0. Quit");
        printLine("=================================");
    }

    private static void handleMenuChoice(int choice, Scanner scanner) {
        switch (choice) {
            case 1: listSubjectsAndAuthors(); break;
            case 2: searchBySubject(scanner); break;
            case 3: searchByAuthor(scanner); break;
            case 4: searchByKeyword(scanner); break;
            case 5: viewBookDetails(scanner); break;
            case 0: printLine("Thank you, bye"); break;
            default: printLine("[Error] Unknown choice. Please pick a number from 0 to 5.\n");
        }
    }

    private static void listSubjectsAndAuthors() {
        printLine("--- Listing All Subjects and Authors ---");
        List<SubjectAuthorsDto> subjectAuthorsList = bookService.findAllSubjectsWithAuthors();
        printLine("");
        printSubjectAuthors(subjectAuthorsList);
    }
    
    private static void searchBySubject(Scanner scanner) {
        printLine("--- Search Books by Subject ---");
        System.out.print("Enter subject IDs (space separated list): ");
        String input = scanner.nextLine();
        List<String> subjectIds = Arrays.asList(input.split("\\s+"));
        List<SubjectBooksDto> subjectBooksList = bookService.findBooksBySubjects(subjectIds);
        printLine("");
        printSubjectBooks(subjectBooksList);
    }

    private static void searchByAuthor(Scanner scanner) {
        printLine("--- Search Books by Author ---");
        System.out.print("Enter author IDs (space separated list): ");
        String input = scanner.nextLine();
        List<String> authorIds = Arrays.asList(input.split("\\s+"));
        List<AuthorBooksDto> authorBooksList = bookService.findBooksByAuthors(authorIds);
        printLine("");
        printAuthorBooks(authorBooksList);
    }

    private static void searchByKeyword(Scanner scanner) {
        printLine("--- Search Books by Keyword ---");
        System.out.print("Enter keyword: ");
        String keyword = scanner.nextLine();
        List<BookDto> books = bookService.findBooksByKeyword(keyword);
        printLine("");
        printBooks(books);
        printLine("");
    }

    private static void viewBookDetails(Scanner scanner) {
        printLine("--- View Book Details ---");
        System.out.print("Enter Book IDs (space separated list): ");
        String input = scanner.nextLine();
        List<String> bookIds = Arrays.asList(input.split("\\s+"));
        List<BookDetailsDto> bookDetailsList = bookService.findBookDetailsByBookIds(bookIds);
        printLine("");
        printBookDetails(bookDetailsList);
    }

    private static void printSubjectAuthors(List<SubjectAuthorsDto> subjectAuthorsList) {
        subjectAuthorsList.forEach(subjectAuthors -> {
            printLine("Subject: " + subjectToString(subjectAuthors.subject()));
            subjectAuthors.authors().forEach(author -> {
                printLine("  " + authorToString(author));
            });
            printLine("");
        });
    }

    private static void printSubjectBooks(List<SubjectBooksDto> subjectBooksList) {
        subjectBooksList.forEach(subjectBooks -> {
            printLine("Subject: " + subjectToString(subjectBooks.subject()));
            subjectBooks.books().forEach(book -> {
                printLine("  " + bookToString(book));
            });
            printLine("");
        });
    }

    private static void printAuthorBooks(List<AuthorBooksDto> authorBooksList) {
        authorBooksList.forEach(authorBooks -> {
            printLine("Author: " + authorToString(authorBooks.author()));
            authorBooks.books().forEach(book -> {
                printLine("  " + bookToString(book));
                if (book.subjects() != null && !book.subjects().isEmpty())
                    printLine("    [Subjects: " + subjectsToString(book.subjects()) + "]");
            });
            printLine("");
        });
    }

    private static void printBooks(List<BookDto> books) {
        books.forEach(book -> {
            printLine("  " + bookToString(book));
            if (book.subjects() != null && !book.subjects().isEmpty())
                printLine("    [Subjects: " + subjectsToString(book.subjects()) + "]");
        });
    }

    private static void printBookDetails(List<BookDetailsDto> bookDetailsList) {
        bookDetailsList.forEach(bookDetails -> {
            printLine(bookDetails.title() + " (ID = " + bookDetails.bookId() + ")");
            printLine("  Authors: ");
            bookDetails.authors().forEach(author -> {
                printLine("    " + author.authorName());
                printLine("      Other books by this author: "
                     + String.join("; ", author.allBooks().stream().filter(title -> !title.equals(bookDetails.title())).toList()));
            });
            printLine("  Summary: " + bookDetails.summary());
            printLine("  ISBN: " + bookDetails.isbn());
            printLine("  User Ratings: " +
                 (bookDetails.stars() == null ? "No rating" : (bookDetails.stars()) + " stars"));
            if (!bookDetails.userRatings().isEmpty()) {
                bookDetails.userRatings().forEach(userRating -> {
                    printLine("    " + userRating.stars() + " stars, " + userRating.numberOfReviews() + " reviews");
                });
            }
            printLine("  Subjects: " + subjectsToString(bookDetails.subjects()));
            printLine("");
        });
    }

    public static String subjectToString(SubjectDto subject) {
        return subject.subject() + " (ID = " + subject.subjectId() + ")";
    }

    public static String authorToString(AuthorDto author) {
        return author.authorName() + " (ID = " + author.authorId() + ")";
    }

    public static String bookToString(BookDto book) {
        return (new StringBuilder())
                .append(book.title()).append(" (ID = ").append(book.bookId()).append(")")
                .append(" | By ")
                .append(String.join(", ", book.authors().stream().map(author -> authorToString(author)).sorted().toList()))
                .append(" | ").append(book.stars() == null ? "No rating" : book.stars() + " stars")
                .append(" | Price: $").append(book.priceInCent() / 100.0)
                .toString();
    }

    private static String subjectsToString(List<SubjectDto> subjects) {
        return String.join(", ", subjects.stream().map(subject -> subjectToString(subject)).toList());
    }

    private static void printLine(String line) {
        System.out.println(line);
    }
}
