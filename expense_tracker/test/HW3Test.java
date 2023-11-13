import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import java.text.ParseException;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import controller.ExpenseTrackerController;
import model.ExpenseTrackerModel;
import model.Transaction;
import model.Filter.AmountFilter;
import model.Filter.CategoryFilter;
import model.Filter.TransactionFilter;
import view.ExpenseTrackerView;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class HW3Test {

    private ExpenseTrackerModel model;
    private ExpenseTrackerView view;
    private ExpenseTrackerController controller;

    @Before
    public void setUp() {
        model = new ExpenseTrackerModel();
        view = new ExpenseTrackerView();
        controller = new ExpenseTrackerController(model, view);

        // Initialize view
        view.setVisible(true);



        // Handle add transaction button clicks
        view.getAddTransactionBtn().addActionListener(e -> {
            // Get transaction data from view
            double amount = view.getAmountField();
            String category = view.getCategoryField();
            
            // Call controller to add transaction
            boolean added = controller.addTransaction(amount, category);
            
            if (!added) {
                JOptionPane.showMessageDialog(view, "Invalid amount or category entered");
                view.toFront();
            }
        });

        // Add action listener to the "Apply Category Filter" button
        view.addApplyCategoryFilterListener(e -> {
        try{
        String categoryFilterInput = view.getCategoryFilterInput();
        CategoryFilter categoryFilter = new CategoryFilter(categoryFilterInput);
        if (categoryFilterInput != null) {
            // controller.applyCategoryFilter(categoryFilterInput);
            controller.setFilter(categoryFilter);
            controller.applyFilter();
        }
        }catch(IllegalArgumentException exception) {
        JOptionPane.showMessageDialog(view, exception.getMessage());
        view.toFront();
    }});


        // Add action listener to the "Apply Amount Filter" button
        view.addApplyAmountFilterListener(e -> {
        try{
        double amountFilterInput = view.getAmountFilterInput();
        AmountFilter amountFilter = new AmountFilter(amountFilterInput);
        if (amountFilterInput != 0.0) {
            controller.setFilter(amountFilter);
            controller.applyFilter();
        }
        }catch(IllegalArgumentException exception) {
        JOptionPane.showMessageDialog(view,exception.getMessage());
        view.toFront();
    }});
    }

    public double getTotalCost() {
        double totalCost = 0.0;
        List<Transaction> allTransactions = model.getTransactions(); // Using the model's getTransactions method
        for (Transaction transaction : allTransactions) {
            totalCost += transaction.getAmount();
        }
        return totalCost;
    }
	
    public void checkTransaction(double amount, String category, Transaction transaction) {
        assertEquals(amount, transaction.getAmount(), 0.01);
        assertEquals(category, transaction.getCategory());
        String transactionDateString = transaction.getTimestamp();
        Date transactionDate = null;
        try {
            transactionDate = Transaction.dateFormatter.parse(transactionDateString);
        }
        catch (ParseException pe) {
            pe.printStackTrace();
            transactionDate = null;
        }
        Date nowDate = new Date();
        assertNotNull(transactionDate);
        assertNotNull(nowDate);
        // They may differ by 60 ms
        assertTrue(nowDate.getTime() - transactionDate.getTime() < 60000);
    }
    
    public void testHighlightRows(DefaultTableModel tableModel, JTable transactionTable, List<Integer> rowIndexes) {
        Color expectedColor = new Color(173, 255, 168);
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                TableCellRenderer renderer = transactionTable.getCellRenderer(i, j);
                Component component = transactionTable.prepareRenderer(renderer, i, j);
                Color cellColor = component.getBackground();
                // It's the highlighted row <=> the color of a random cell of that row must be equal to expected light green
                assertTrue(rowIndexes.contains(i) == cellColor.equals(expectedColor));
            }
        }
    }

    @Test
    public void testAddTransaction() {
        // Create JFormattedTextField objects for amount and category
        DefaultTableModel tableModel = view.getTableModel();
        JFormattedTextField amountField = new JFormattedTextField();
        amountField.setValue(Double.valueOf(50.00));
        JTextField categoryField = new JTextField("food");
        // Pre-condition: List of transactions is empty and total cost is 0
        assertEquals(0, tableModel.getRowCount());
        assertEquals(0.0, getTotalCost(), 0.01);
        // Perform the action: Add a transaction
        view.setAmountField(amountField);
        view.setCategoryField(categoryField);
        view.getAddTransactionBtn().doClick();
        int lastRowIndex = tableModel.getRowCount() - 2;
        // Post-condition: New transaction is added and the Total Cost is updated
        assertEquals("50.0", tableModel.getValueAt(lastRowIndex, 1).toString());
        assertEquals("food", tableModel.getValueAt(lastRowIndex, 2).toString());
        assertEquals("50.0", tableModel.getValueAt(lastRowIndex+1, 3).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionConstructorWithInvalidAmount1() {
        model.addTransaction(new Transaction(-50.00, "food"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionConstructorWithInvalidAmount2() {
        model.addTransaction(new Transaction(1001.00, "food"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionConstructorWithInvalidCategory() {
        model.addTransaction(new Transaction(50.00, "something"));
    }

    @Test
    public void testTransactionWithInvalidInput() {
        // Pre-condition: The total cost is initially 0
        assertEquals(0.0, getTotalCost(), 0.01);
        // Perform the action: Add invalid transactions
        boolean result = controller.addTransaction(-50.00, "food");
        assertEquals(false, result);
        result = controller.addTransaction(50.00, "something");
        assertEquals(false, result);
        // Post-condition: The total cost must remain unchanged
        assertEquals(0.0, getTotalCost(), 0.01);
    }

    @Test
    public void testFilterByAmount() {
        DefaultTableModel tableModel = view.getTableModel();
        JTable transactionTable = view.getTransactionsTable();
        // Pre-condition: List of transactions is empty and total cost is 0
        assertEquals(0, model.getTransactions().size());
        assertEquals(0, tableModel.getRowCount());
        // Perform the action: Add 4 transactions and apply filtering
        controller.addTransaction(50.00, "food");
        controller.addTransaction(50.00, "travel");
        controller.addTransaction(20.00, "transport");
        controller.addTransaction(50.00, "bills");
        List<Transaction> allTransactions = model.getTransactions();
        double amountFiltered = 50.00;
        TransactionFilter filter = new AmountFilter(amountFiltered);
        List<Transaction> filteredTransactions = filter.filter(allTransactions);

        // Post-condition: The size of filtered list is 3
        assertEquals(3, filteredTransactions.size());
        for (Transaction transaction : filteredTransactions) {
            assertEquals(amountFiltered, transaction.getAmount(), 0.01);
        }

        // Additional test via View - Check Highlight rows:
        controller.setFilter(filter);
        controller.applyFilter();
        List<Integer> rowIndexes = new ArrayList<>();
        for (Transaction t : filteredTransactions) {
            int rowIndex = allTransactions.indexOf(t);
            if (rowIndex != -1) {
              rowIndexes.add(rowIndex);
            }
        }
        testHighlightRows(tableModel, transactionTable, rowIndexes);
    }

    
    @Test
    public void testFilterByCategory() {
        DefaultTableModel tableModel = view.getTableModel();
        JTable transactionTable = view.getTransactionsTable();
        // Pre-condition: List of transactions is empty and total cost is 0
        assertEquals(0, model.getTransactions().size());
        assertEquals(0, tableModel.getRowCount());
        // Perform the action: Add 4 transactions and apply filtering
        controller.addTransaction(20.00, "food");
        controller.addTransaction(30.00, "travel");
        controller.addTransaction(40.00, "transport");
        controller.addTransaction(50.00, "travel");
        List<Transaction> allTransactions = model.getTransactions();
        String categoryFiltered = "travel";
        TransactionFilter filter = new CategoryFilter(categoryFiltered);
        List<Transaction> filteredTransactions = filter.filter(allTransactions);

        // Post-condition: The size of filtered list is 2
        assertEquals(2, filteredTransactions.size());
        for (Transaction transaction : filteredTransactions) {
            assertEquals(categoryFiltered, transaction.getCategory());
        }

        // Additional test via View - Check Highlight rows:
        controller.setFilter(filter);
        controller.applyFilter();
        List<Integer> rowIndexes = new ArrayList<>();
        for (Transaction t : filteredTransactions) {
            int rowIndex = allTransactions.indexOf(t);
            if (rowIndex != -1) {
              rowIndexes.add(rowIndex);
            }
        }
        testHighlightRows(tableModel, transactionTable, rowIndexes);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testUndoDisallowed() {
        // Pre-condition: the list of transaction if empty
        assertEquals(0, model.getTransactions().size());
        // Perform the action: Undo the last transaction
        controller.undo(0);
        // Post-condition: Exception is thrown
    }

    @Test
    public void testUndoAllowed() {
        // Pre-condition: List of transactions is empty and total cost is 0
        assertEquals(0.0, getTotalCost(), 0.01);
        // Perform the action: Add several transactions and undo the second and third one
        controller.addTransaction(20.00, "food");
        controller.addTransaction(30.00, "travel");
        controller.addTransaction(40.00, "other");
        controller.addTransaction(50.00, "travel");
        assertEquals(140.0, getTotalCost(), 0.01);
        List<Transaction> originalTransactions = model.getTransactions();
        controller.undo(1);
        controller.undo(1);
        List<Transaction> newTransactions = model.getTransactions();
        assertEquals(newTransactions.size(), 2);
        // Post-condition: After remove the first and second transaction, the transaction at index 1 currently was the transaction at index 3 in the original list. The total cost will be updated.
        assertTrue(originalTransactions.get(3) == newTransactions.get(1));
        assertTrue(newTransactions.get(0).getAmount() == 20.00 && newTransactions.get(0).getCategory().equals("food"));
        assertTrue(newTransactions.get(1).getAmount() == 50.00 && newTransactions.get(1).getCategory().equals("travel"));
        assertEquals(70.00, getTotalCost(), 0.01);
    }

    @After
    public void tearDown() {
        model = null;
        view = null;
        controller = null;
    }
}
