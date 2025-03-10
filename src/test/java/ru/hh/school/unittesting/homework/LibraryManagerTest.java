package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

  @Mock
  NotificationService notificationService;

  @Mock
  UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @Test
  void successfulAddingBookToInventory() {
    libraryManager.addBook("1", 5);
    int availableCopies = libraryManager.getAvailableCopies("1");
    assertEquals(5, availableCopies);
    libraryManager.addBook("1", 3);
    availableCopies = libraryManager.getAvailableCopies("1");
    assertEquals(8, availableCopies);
  }

  @Test
  void unSuccessfulAddingBookToInventoryWithNotActiveUser() {
    String userId = "1";
    when(userService.isUserActive(userId)).thenReturn(false);
    boolean isBorrow = libraryManager.borrowBook("1", userId);
    verify(notificationService, times(1)).notifyUser(userId, "Your account is not active.");
    assertFalse(isBorrow);
  }

  @Test
  void notSuccessfulBorrowBookWithoutAddingBookToInventory() {
    String userId = "1";
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBookBorrowed = libraryManager.borrowBook("1", userId);
    assertFalse(isBookBorrowed);
    verify(notificationService, times(0)).notifyUser(anyString(), anyString());
  }

  @Test
  void notSuccessfulBorrowBookWithAddingBookToInventoryQuantityZero() {
    String userId = "1";
    String bookId = "1";
    libraryManager.addBook(bookId, 0);
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBookBorrowed = libraryManager.borrowBook(bookId, userId);
    assertFalse(isBookBorrowed);
  }

  @Test
  void successfulBorrowBook() {
    String userId = "1";
    String bookId = "1";
    libraryManager.addBook(bookId, 1);
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBookBorrowed = libraryManager.borrowBook(bookId, userId);
    verify(notificationService, times(1)).notifyUser(userId, "You have borrowed the book: " + userId);
    int availableCopies = libraryManager.getAvailableCopies(bookId);
    assertEquals(0, availableCopies);
    assertTrue(isBookBorrowed);
  }

  @ParameterizedTest
  @CsvSource({
      "1, 2",
      "2, 1",
  })
  void noSuccessfullReturnBookIfNotSuchBorrowedBooks(String bookId, String userId) {
    String borrowedBookUserId = "1";
    String borrowedBookId = "1";
    libraryManager.addBook(borrowedBookId, 1);
    when(userService.isUserActive(borrowedBookUserId)).thenReturn(true);
    boolean isBookBorrowed = libraryManager.borrowBook(borrowedBookId, borrowedBookUserId);
    assertTrue(isBookBorrowed);
    boolean isBookReturned = libraryManager.returnBook(bookId, userId);
    assertFalse(isBookReturned);
  }

  @Test
  void successfullReturnBook() {
    String userId = "1";
    String bookId = "1";
    libraryManager.addBook(bookId, 1);
    when(userService.isUserActive(userId)).thenReturn(true);
    boolean isBookBorrowed = libraryManager.borrowBook(bookId, userId);
    assertTrue(isBookBorrowed);
    boolean isBookReturned = libraryManager.returnBook(bookId, userId);
    assertTrue(isBookReturned);
    int availableCopies = libraryManager.getAvailableCopies(bookId);
    assertEquals(1, availableCopies);
    verify(notificationService, times(1)).notifyUser(userId, "You have returned the book: " + bookId);
  }


  @Test
  void calculateDynamicLateFeeShouldThrowExceptionIfOverdueDaysIsNegative() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, true, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "0, true, true, 0",
      "2, true, true, 1.2",
      "1, true, false, 0.75",
      "1, false, true, 0.4",
  })
  void calculateDynamicLateFeeWithDifferentParams(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedSum
  ) {

    double sum = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedSum, sum);
  }
}
