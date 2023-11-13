import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.JOptionPane;

import java.text.ParseException;

import org.junit.Before;
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

        // Inject the undo function to each Undo button in transactions.
        view.setupUndo(transactionIdx -> {
            if (!controller.removeTransaction(transactionIdx)) {
                JOptionPane.showMessageDialog(view, "Cannot remove this transaction.");
            }
        });
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
            for (int j = 0; j < tableModel.getColumnCount() - 1; j++) {
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

    // @Test(expected = IllegalStateException.class)
    // public void testUndoDisallowed() {
    //     controller.undoLastTransaction();
    // }
    public void addTransactionByView(double amount, String category) {
        JFormattedTextField amountField = new JFormattedTextField();
        amountField.setValue(Double.valueOf(amount));
        JTextField categoryField = new JTextField(category);
        view.setAmountField(amountField);
        view.setCategoryField(categoryField);
        view.getAddTransactionBtn().doClick();
    }

public void clickUndoButton(JTable table, int row) {
    // Assuming "Undo" is the header of the column with the undo buttons
    TableCellEditor cellEditor = table.getColumn("Undo").getCellEditor();
    // int undoColumn = table.getColumnModel().getColumnIndex("Undo");

    // // Check if the row and column are valid
    // if (row < 0 || row >= table.getRowCount() || undoColumn < 0) {
    //     throw new IllegalArgumentException("Row or column out of bounds");
    // }

    // // Start editing the cell, which should be configured to trigger the undo action
    // table.editCellAt(row, undoColumn);

    // // Optionally, you can retrieve the editor and invoke any specific methods on it
    // TableCellEditor editor = table.getCellEditor(row, undoColumn);
    // if (editor != null) {
    //     // Stopping cell editing usually triggers the action bound to the button
    //     editor.stopCellEditing();
    // }

    table.editCellAt(row, table.getColumnModel().getColumnIndex("Undo"));
}

    // @Test
    // public void testUndoAllowed() {
    //     // Pre-condition: List of transactions is empty and total cost is 0
    //     assertEquals(0.0, getTotalCost(), 0.01);
    //     controller.addTransaction(20.00, "food");
    //     controller.addTransaction(30.00, "travel");
    //     controller.addTransaction(40.00, "other");
    //     controller.addTransaction(50.00, "travel");

    //     // Perform the action: Add a transaction
    //     // addTransactionByView(30.00, "other");
    //     // Thread.sleep(1000);
    //     // addTransactionByView(20.00, "entertainment");
    //     // Thread.sleep(1000);
    //     // addTransactionByView(10.00, "bills");
    //     JTable transactionTable = view.getTransactionsTable();
    //     System.out.println(getTotalCost());
    //     clickUndoButton(transactionTable, 1);
    //     System.out.println(getTotalCost());
    //     // int lastRowIndex = tableModel.getRowCount() - 2;
    //     // assertEquals("50.0", tableModel.getValueAt(lastRowIndex, 1).toString());
    //     // assertEquals("food", tableModel.getValueAt(lastRowIndex, 2).toString());
    //     // assertEquals("50.0", tableModel.getValueAt(lastRowIndex+1, 3).toString());
    // }

    // @After
    // public void tearDown() {
    //     model = null;
    //     view = null;
    //     controller = null;
    // }
}
