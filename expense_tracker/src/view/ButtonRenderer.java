package view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.util.function.IntConsumer;

// Custom cell renderer to render a button in a cell on the UI.
class ButtonRenderer extends JButton implements TableCellRenderer {
  private DefaultTableModel model;

  public ButtonRenderer(DefaultTableModel model) {
    this.model = model;
    setText("Undo");
    setOpaque(true);
  }

  // When the cell is rendered, a JButton is returned and shown.
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    if (row == model.getRowCount() - 1) {
      return null;
    }
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    return this;
  }
}

// Custom cell editor to run logic when a cell is clicked on the UI.
class ButtonEditor extends DefaultCellEditor {
  protected JButton button;

  public ButtonEditor(JTable table, IntConsumer undoCallback) {
    super(new JCheckBox());
    button = new JButton();
    button.setText("Undo");

    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Perform the row removal action
        int row = table.getEditingRow();
        fireEditingStopped();
        stopCellEditing();
        undoCallback.accept(row);
      }
    });
  }

  // When a cell is clicked, this function returns a button with Undo logic
  // prepared.
  @Override
  public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected, int row, int column) {
    if (row == table.getRowCount() - 1) {
      return null;
    }
    if (isSelected) {
      button.setForeground(table.getSelectionForeground());
      button.setBackground(table.getSelectionBackground());
    } else {
      button.setForeground(table.getForeground());
      button.setBackground(table.getBackground());
    }
    return button;
  }
}
