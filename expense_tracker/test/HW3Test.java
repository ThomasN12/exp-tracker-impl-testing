import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import controller.ExpenseTrackerController;
import model.ExpenseTrackerModel;
import model.Transaction;
import model.Filter.AmountFilter;
import model.Filter.CategoryFilter;
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
}
