package controller;

import view.ExpenseTrackerView;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import model.ExpenseTrackerModel;
import model.Transaction;
import model.Filter.TransactionFilter;

public class ExpenseTrackerController {

  private ExpenseTrackerModel model;
  private ExpenseTrackerView view;
  /**
   * The Controller is applying the Strategy design pattern.
   * This is the has-a relationship with the Strategy class
   * being used in the applyFilter method.
   */
  private TransactionFilter filter;

  public ExpenseTrackerController(ExpenseTrackerModel model, ExpenseTrackerView view) {
    this.model = model;
    this.view = view;
  }

  public void setFilter(TransactionFilter filter) {
    // Sets the Strategy class being used in the applyFilter method.
    this.filter = filter;
  }

  public void refresh() {
    List<Transaction> transactions = model.getTransactions();
    view.refreshTable(transactions);
  }

  public boolean addTransaction(double amount, String category) {
    if (!InputValidation.isValidAmount(amount)) {
      return false;
    }
    if (!InputValidation.isValidCategory(category)) {
      return false;
    }

    Transaction t = new Transaction(amount, category);
    model.addTransaction(t);
    view.getTableModel().addRow(new Object[] { t.getAmount(), t.getCategory(), t.getTimestamp() });
    refresh();
    return true;
  }

  // Undo transaction at `transactionIdx` in the list.
  public void undo(int transactionIdx) throws IndexOutOfBoundsException {
    model.removeTransaction(transactionIdx);
    // Replay `refresh()` logic without removing old rows so:
    // - Less overhead (of inserting all rows from start)
    // - Allow events of the removed row, e.g. MouseEvent, to be fired gracefully.
    List<Transaction> transactions = model.getTransactions();
    double totalCost = 0;
    // Remove and insert new Total row.
    for (Transaction t : transactions) {
      totalCost += t.getAmount();
    }
    DefaultTableModel table = view.getTableModel();
    // Remove row on UI model.
    table.removeRow(transactionIdx);
    // Remove old total row.
    table.removeRow(table.getRowCount() - 1);
    Object[] totalRow = { "Total", null, null, totalCost };
    table.addRow(totalRow);
  }

  public void applyFilter() {
    // null check for filter
    if (filter != null) {
      // Use the Strategy class to perform the desired filtering
      List<Transaction> transactions = model.getTransactions();
      List<Transaction> filteredTransactions = filter.filter(transactions);
      List<Integer> rowIndexes = new ArrayList<>();
      for (Transaction t : filteredTransactions) {
        int rowIndex = transactions.indexOf(t);
        if (rowIndex != -1) {
          rowIndexes.add(rowIndex);
        }
      }
      view.highlightRows(rowIndexes);
    } else {
      JOptionPane.showMessageDialog(view, "No filter applied");
      view.toFront();
    }

  }
}
