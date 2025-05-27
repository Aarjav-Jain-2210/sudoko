import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class GUI extends JFrame {
    private JTextField[][] cells = new JTextField[9][9];
    private boolean[][] isClue = new boolean[9][9];

    public GUI() {
        setTitle("Sudoku Solver");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(9, 9));
        Font font = new Font("SansSerif", Font.BOLD, 20);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j] = new JTextField();
                cells[i][j].setHorizontalAlignment(JTextField.CENTER);
                cells[i][j].setFont(font);
                final int row = i, col = j;

                cells[i][j].addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        if (!Character.isDigit(e.getKeyChar()) || e.getKeyChar() == '0')
                            e.consume();
                    }
                });

                gridPanel.add(cells[i][j]);
            }
        }

        JPanel buttonPanel = new JPanel();
        JButton loadButton = new JButton("Load");
        JButton saveButton = new JButton("Save");
        JButton validateButton = new JButton("Validate");
        JButton solveButton = new JButton("Solve");
        JButton clearButton = new JButton("Clear");
        JButton hintButton = new JButton("Hint");

        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(validateButton);
        buttonPanel.add(solveButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(hintButton);

        hintButton.addActionListener(e -> giveHint());
        loadButton.addActionListener(e -> loadPuzzle());
        saveButton.addActionListener(e -> savePuzzle());
        validateButton.addActionListener(e -> validateGrid());
        solveButton.addActionListener(e -> solvePuzzle());
        clearButton.addActionListener(e -> clearGrid());

        add(gridPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadPuzzle() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                for (int i = 0; i < 9; i++) {
                    String[] line = br.readLine().split(" ");
                    for (int j = 0; j < 9; j++) {
                        String val = line[j];
                        cells[i][j].setText(val.equals("0") ? "" : val);
                        isClue[i][j] = !val.equals("0");
                        updateCellAppearance(i, j);
                    }
                }
            } catch (IOException ex) {
                showError("Failed to load puzzle.");
            }
        }
    }

    private void savePuzzle() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file)) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        String text = cells[i][j].getText().trim();
                        writer.print(text.isEmpty() ? "0" : text);
                        if (j < 8) writer.print(" ");
                    }
                    writer.println();
                }
            } catch (IOException ex) {
                showError("Failed to save puzzle.");
            }
        }
    }

    private void solvePuzzle() {
        try (PrintWriter writer = new PrintWriter("input.txt")) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    String val = cells[i][j].getText().trim();
                    writer.print(val.isEmpty() ? "0" : val);
                    if (j < 8) writer.print(" ");
                }
                writer.println();
            }
        } catch (IOException e) {
            showError("Error writing to input.txt.");
            return;
        }

        try {
            ProcessBuilder compile = new ProcessBuilder("g++", "main.cpp", "-o", "solver");
            compile.inheritIO().start().waitFor();

            ProcessBuilder run = new ProcessBuilder("./solver");
            run.inheritIO().start().waitFor();
        } catch (Exception e) {
            showError("Error executing solver.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader("output.txt"))) {
            for (int i = 0; i < 9; i++) {
                String[] line = br.readLine().split(" ");
                for (int j = 0; j < 9; j++) {
                    cells[i][j].setText(line[j]);
                    updateCellAppearance(i, j);
                }
            }
        } catch (IOException e) {
            showError("Solution not found or output.txt missing.");
        }
    }

    private void clearGrid() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cells[i][j].setText("");
                cells[i][j].setEditable(true);
                isClue[i][j] = false;
                updateCellAppearance(i, j);
            }
        }
    }

    private void giveHint() {
        try (PrintWriter writer = new PrintWriter("input.txt")) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    String val = cells[i][j].getText().trim();
                    writer.print(val.isEmpty() ? "0" : val);
                    if (j < 8) writer.print(" ");
                }
                writer.println();
            }
        } catch (IOException e) {
            showError("Failed to write to input.txt.");
            return;
        }

        try {
            new ProcessBuilder("./solver").inheritIO().start().waitFor();
        } catch (Exception e) {
            showError("Solver execution failed.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader("output.txt"))) {
            for (int i = 0; i < 9; i++) {
                String[] solvedRow = reader.readLine().split(" ");
                for (int j = 0; j < 9; j++) {
                    String current = cells[i][j].getText().trim();
                    if (current.isEmpty()) {
                        cells[i][j].setText(solvedRow[j]);
                        cells[i][j].setBackground(Color.YELLOW);
                        return;
                    }
                }
            }
            showMessage("No empty cells left to hint.");
        } catch (IOException e) {
            showError("Failed to read from output.txt.");
        }
    }

    private void validateGrid() {
        boolean valid = true;
        resetCellColors();

        for (int i = 0; i < 9; i++) {
            Set<String> row = new HashSet<>();
            Set<String> col = new HashSet<>();
            for (int j = 0; j < 9; j++) {
                String rVal = cells[i][j].getText().trim();
                String cVal = cells[j][i].getText().trim();
                if (!rVal.isEmpty() && !row.add(rVal)) {
                    valid = false;
                    cells[i][j].setBackground(Color.RED);
                }
                if (!cVal.isEmpty() && !col.add(cVal)) {
                    valid = false;
                    cells[j][i].setBackground(Color.RED);
                }
            }
        }

        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<String> box = new HashSet<>();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int r = boxRow * 3 + i;
                        int c = boxCol * 3 + j;
                        String val = cells[r][c].getText().trim();
                        if (!val.isEmpty() && !box.add(val)) {
                            valid = false;
                            cells[r][c].setBackground(Color.RED);
                        }
                    }
                }
            }
        }

        if (valid) showMessage("Grid is valid.");
        else showError("Grid is invalid! Duplicates found.");
    }

    private void updateCellAppearance(int i, int j) {
        if (isClue[i][j]) {
            cells[i][j].setEditable(false);
            cells[i][j].setBackground(new Color(230, 230, 250));
            cells[i][j].setFont(cells[i][j].getFont().deriveFont(Font.BOLD));
        } else {
            cells[i][j].setEditable(true);
            cells[i][j].setBackground(Color.WHITE);
            cells[i][j].setFont(cells[i][j].getFont().deriveFont(Font.PLAIN));
        }
    }

    private void resetCellColors() {
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++)
                updateCellAppearance(i, j);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
