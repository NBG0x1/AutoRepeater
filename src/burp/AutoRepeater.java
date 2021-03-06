package burp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

public class AutoRepeater implements IMessageEditorController {

  private IBurpExtenderCallbacks callbacks;
  private IExtensionHelpers helpers;
  private Gson gson;

  JTabbedPane tabs;

  // Splitpane that holds top and bottom halves of the ui
  private JSplitPane mainSplitPane;

  // These hold the http request viewers at the bottom
  private JSplitPane originalRequestResponseSplitPane;
  private JSplitPane modifiedRequestResponseSplitPane;

  // this split pane holds the request list and configuration panes
  private JSplitPane userInterfaceSplitPane;

  private LogTable logTable;

  private DiffViewerPane requestComparer;
  private DiffViewerPane responseComparer;

  private DiffViewerPane requestLineComparer;
  private DiffViewerPane responseLineComparer;

  // request/response viewers
  private IMessageEditor originalRequestViewer;
  private IMessageEditor originalResponseViewer;
  private IMessageEditor modifiedRequestViewer;
  private IMessageEditor modifiedResponseViewer;

  // Panels for including request/response viewers + labels
  private JPanel originalRequestPanel;
  private JPanel modifiedRequestPanel;
  private JPanel originalResponsePanel;
  private JPanel modifiedResponsePanel;

  private JLabel originalRequestLabel;
  private JLabel modifiedRequestLabel;
  private JLabel originalResponseLabel;
  private JLabel modifiedResponseLabel;

  byte[] originalRequest;
  byte[] originalResponse;
  byte[] modifiedRequest;
  byte[] modifiedResponse;

  String requestDiff;
  String responseDiff;
  String requestLineDiff;
  String responseLineDiff;

  JScrollPane requestComparerScrollPane;
  JScrollPane responseComparerScollPane;

  JScrollPane requestLineComparerScrollPane;
  JScrollPane responseLineComparerScollPane;

  // List of log entries for LogTable
  private LogTableModel logTableModel;

  private LogManager logManager;

  // List of log entries that have yet to receive a response
  //private ArrayList<LogEntry> logEntriesWithoutResponses;

  // The current item selected in the log table
  private IHttpRequestResponsePersisted currentOriginalRequestResponse;
  private IHttpRequestResponsePersisted currentModifiedRequestResponse;

  //private IHttpRequestResponsePersisted currentOriginalRequestResponse;

  // The tabbed pane that holds the configuration options
  private JTabbedPane configurationPane;

  // Panel that holds configuration options
  private JPanel optionsPanel;

  // Panel that holds export options
  private JPanel exportPanel;

  // The button that indicates weather Auto Repater is on or not.
  private JToggleButton activatedButton;

  // Elements for configuration panel
  // Global Replacements UI
  private JScrollPane globalReplacementScrollPane;
  private JTable globalReplacementTable;
  private JPanel globalReplacementsButtonPanel;
  private JButton addGlobalReplacementButton;
  private JButton editGlobalReplacementButton;
  private JButton deleteGlobalReplacementButton;

  // Replacements UI
  private JScrollPane replacementScrollPane;
  private JTable replacementTable;
  private JButton addReplacementButton;
  private JPanel replacementsButtonPanel;
  private JButton editReplacementButton;
  private JButton deleteReplacementButton;

  // Conditions UI
  private JScrollPane conditionScrollPane;
  private JTable conditionTable;
  private JButton addConditionButton;
  private JPanel conditionsButtonPanel;
  private JButton editConditionButton;
  private JButton deleteConditionButton;

  // Add Global Replacement Dialog UI
  private JPanel replacementPanel;

  private JComboBox<String> replacementTypeComboBox;
  private JTextField replacementMatchTextField;
  private JTextField replacementReplaceTextField;
  private JTextField replacementCommentTextField;
  private JCheckBox replacementIsRegexCheckBox;
  private JComboBox<String> replacementCountComboBox;

  private JLabel replacementMatchLabel;
  private JLabel replacementReplaceLabel;
  private JLabel replacementCommentLabel;
  private JLabel replacementTypeLabel;
  private JLabel replacementIsRegexLabel;
  private JLabel replacementCountLabel;

  private ReplacementTableModel globalReplacementTableModel;

  private ReplacementTableModel replacementTableModel;

  private ConditionTableModel conditionTableModel;

  private JPanel conditionPanel;

  private JComboBox<String> booleanOperatorComboBox;
  private JComboBox<String> matchTypeComboBox;
  private JComboBox<String> matchRelationshipComboBox;
  private JTextField matchConditionTextField;

  private JLabel booleanOperatorLabel;
  private JLabel matchTypeLabel;
  private JLabel matchRelationshipLabel;
  private JLabel matchConditionLabel;

  private JPanel helpPanel;

  public AutoRepeater() {
    this.callbacks = BurpExtender.getCallbacks();
    helpers = callbacks.getHelpers();
    gson = BurpExtender.getGson();
    createUI();
    setDefaultState();
    activatedButton.setSelected(true);
  }

  public AutoRepeater(JsonObject configurationJson) {
    this.callbacks = BurpExtender.getCallbacks();
    helpers = callbacks.getHelpers();
    gson = BurpExtender.getGson();
    createUI();
    if (configurationJson.get("isActivated").getAsBoolean()) {
      activatedButton.setSelected(true);
    }

    if (configurationJson.get("baseReplacements") != null) {
      for (JsonElement element : configurationJson.getAsJsonArray("baseReplacements")) {
        globalReplacementTableModel.addReplacement(gson.fromJson(element, Replacement.class));
      }
    }

    if (configurationJson.get("replacements") != null) {
      for (JsonElement element : configurationJson.getAsJsonArray("replacements")) {
        replacementTableModel.addReplacement(gson.fromJson(element, Replacement.class));
      }
    }

    if (configurationJson.get("conditions") != null) {
      for (JsonElement element : configurationJson.getAsJsonArray("conditions")) {
        conditionTableModel.addCondition(gson.fromJson(element, Condition.class));
      }
    }
  }

  public JsonObject toJson() {
    JsonObject autoRepeaterJson = new JsonObject();
    autoRepeaterJson.addProperty("isActivated", activatedButton.isSelected());

    JsonArray baseReplacementsArray = new JsonArray();
    JsonArray replacementsArray = new JsonArray();
    JsonArray conditionsArray = new JsonArray();

    for (Condition c : conditionTableModel.getConditions()) {
      conditionsArray.add(gson.toJsonTree(c));
    }

    for (Replacement r : globalReplacementTableModel.getReplacements()) {
      baseReplacementsArray.add(gson.toJsonTree(r));
    }

    for (Replacement r : replacementTableModel.getReplacements()) {
      replacementsArray.add(gson.toJsonTree(r));
    }

    autoRepeaterJson.add("baseReplacements", baseReplacementsArray);
    autoRepeaterJson.add("replacements", replacementsArray);
    autoRepeaterJson.add("conditions", conditionsArray);
    return autoRepeaterJson;
  }

  private void resetReplacementDialog() {
    replacementTypeComboBox.setSelectedIndex(0);
    replacementCountComboBox.setSelectedIndex(0);
    replacementMatchTextField.setText("");
    replacementReplaceTextField.setText("");
    replacementCommentTextField.setText("");
    replacementIsRegexCheckBox.setSelected(false);
  }

  private void resetConditionDialog() {
    booleanOperatorComboBox.setSelectedIndex(0);
    matchTypeComboBox.setSelectedIndex(0);
    matchRelationshipComboBox.setSelectedIndex(0);
    matchConditionTextField.setText("");
  }

  public JSplitPane getUI() {
    return mainSplitPane;
  }

  private void createUI() {
    GridBagConstraints c;
    Border grayline = BorderFactory.createLineBorder(Color.GRAY);
    // main splitpane
    mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    //mainSplitPane.setResizeWeight(0.1);

    // splitpane that holds request and response viewers
    originalRequestResponseSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    modifiedRequestResponseSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    // This tabbedpane includes the configuration panels
    configurationPane = new JTabbedPane();

    // Initialize Activated Button
    activatedButton = new JToggleButton("Activate AutoRepeater");
    activatedButton.addChangeListener(e -> {
      if (activatedButton.isSelected()) {
        activatedButton.setText("Deactivate AutoRepeater");
      } else {
        activatedButton.setText("Activate AutoRepeater");
      }
    });

    activatedButton.setPreferredSize(new Dimension(160, 20));
    activatedButton.setMaximumSize(new Dimension(160, 20));
    activatedButton.setMinimumSize(new Dimension(160, 20));

    Dimension dialogDimension = new Dimension(300, 140);
    Dimension comboBoxDimension = new Dimension(200, 20);
    Dimension textFieldDimension = new Dimension(200, 25);
    //Condition Dialog
    c = new GridBagConstraints();
    conditionPanel = new JPanel();
    conditionPanel.setLayout(new GridBagLayout());
    conditionPanel.setPreferredSize(dialogDimension);

    booleanOperatorComboBox = new JComboBox<>(Condition.BOOLEAN_OPERATOR_OPTIONS);
    matchTypeComboBox = new JComboBox<>(Condition.MATCH_TYPE_OPTIONS);
    matchRelationshipComboBox = new JComboBox<>(Condition.getUIMatchRelationshipOptions(
        Condition.BOOLEAN_OPERATOR_OPTIONS[0]));
    matchConditionTextField = new JTextField();

    booleanOperatorComboBox.setPreferredSize(comboBoxDimension);
    matchTypeComboBox.setPreferredSize(comboBoxDimension);
    matchRelationshipComboBox.setPreferredSize(comboBoxDimension);
    matchConditionTextField.setPreferredSize(textFieldDimension);

    matchTypeComboBox.addActionListener(e -> {
      matchRelationshipComboBox
          .setModel(new DefaultComboBoxModel<>(Condition.getUIMatchRelationshipOptions(
              (String) matchTypeComboBox.getSelectedItem())));
      matchConditionTextField.setEnabled(Condition.matchConditionIsEditable(
          (String) matchTypeComboBox.getSelectedItem()));
    });

    booleanOperatorLabel = new JLabel("Boolean Operator: ");
    matchTypeLabel = new JLabel("Match Type:");
    matchRelationshipLabel = new JLabel("Match Relationship: ");
    matchConditionLabel = new JLabel("Match Condition");

    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.WEST;
    conditionPanel.add(booleanOperatorLabel, c);
    c.gridy = 1;
    conditionPanel.add(matchTypeLabel, c);
    c.gridy = 2;
    conditionPanel.add(matchRelationshipLabel, c);
    c.gridy = 3;
    conditionPanel.add(matchConditionLabel, c);

    c.anchor = GridBagConstraints.EAST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 0;
    conditionPanel.add(booleanOperatorComboBox, c);
    c.gridy = 1;
    conditionPanel.add(matchTypeComboBox, c);
    c.gridy = 2;
    conditionPanel.add(matchRelationshipComboBox, c);
    c.gridy = 3;
    conditionPanel.add(matchConditionTextField, c);

    // GlobalReplacement Buttons
    // Populate Add Global Replacements Dialog
    replacementPanel = new JPanel();
    replacementPanel.setLayout(new GridBagLayout());
    replacementPanel.setPreferredSize(dialogDimension);

    c = new GridBagConstraints();

    replacementTypeComboBox = new JComboBox<>(Replacement.REPLACEMENT_TYPE_OPTIONS);
    replacementCountComboBox = new JComboBox<>(Replacement.REPLACEMENT_COUNT_OPTINONS);
    replacementMatchTextField = new JTextField();
    replacementReplaceTextField = new JTextField();
    replacementCommentTextField = new JTextField();
    replacementIsRegexCheckBox = new JCheckBox();

    replacementTypeComboBox.setPreferredSize(comboBoxDimension);
    replacementCountComboBox.setPreferredSize(comboBoxDimension);
    replacementMatchTextField.setPreferredSize(textFieldDimension);
    replacementReplaceTextField.setPreferredSize(textFieldDimension);
    replacementCommentTextField.setPreferredSize(textFieldDimension);

    replacementTypeLabel = new JLabel("Type: ");
    replacementMatchLabel = new JLabel("Match: ");
    replacementCountLabel = new JLabel("Which: ");
    replacementReplaceLabel = new JLabel("Replace: ");
    replacementCommentLabel = new JLabel("Comment: ");
    replacementIsRegexLabel = new JLabel("Regex Match: ");

    c.anchor = GridBagConstraints.WEST;
    c.gridx = 0;
    c.gridy = 0;
    replacementPanel.add(replacementTypeLabel, c);
    c.gridy = 1;
    replacementPanel.add(replacementMatchLabel, c);
    c.gridy = 2;
    replacementPanel.add(replacementReplaceLabel, c);
    c.gridy = 3;
    replacementPanel.add(replacementCountLabel, c);
    c.gridy = 4;
    replacementPanel.add(replacementCommentLabel, c);
    c.gridy = 5;
    replacementPanel.add(replacementIsRegexLabel, c);

    c.anchor = GridBagConstraints.EAST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 0;
    replacementPanel.add(replacementTypeComboBox, c);
    c.gridy = 1;
    replacementPanel.add(replacementMatchTextField, c);
    c.gridy = 2;
    replacementPanel.add(replacementReplaceTextField, c);
    c.gridy = 3;
    replacementPanel.add(replacementCountComboBox, c);
    c.gridy = 4;
    replacementPanel.add(replacementCommentTextField, c);
    c.gridy = 5;
    replacementPanel.add(replacementIsRegexCheckBox, c);

    // Initialize addGlobalReplacementButton
    Dimension buttonDimension = new Dimension(75, 20);
    addGlobalReplacementButton = new JButton("Add");
    addGlobalReplacementButton.setPreferredSize(buttonDimension);
    addGlobalReplacementButton.setMinimumSize(buttonDimension);
    addGlobalReplacementButton.setMaximumSize(buttonDimension);

    // Add new Global Replacement
    addGlobalReplacementButton.addActionListener(e -> {
      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          replacementPanel,
          "Add Global Replacement",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Replacement newReplacement = new Replacement(
            (String) replacementTypeComboBox.getSelectedItem(),
            replacementMatchTextField.getText(),
            replacementReplaceTextField.getText(),
            (String) replacementCountComboBox.getSelectedItem(),
            replacementCommentTextField.getText(),
            replacementIsRegexCheckBox.isSelected()
        );

        globalReplacementTableModel.addReplacement(newReplacement);
        globalReplacementTableModel.fireTableDataChanged();
      }
      resetReplacementDialog();
    });

    //Initialize editGlobalReplacementButton
    editGlobalReplacementButton = new JButton("Edit");
    editGlobalReplacementButton.setPreferredSize(buttonDimension);
    editGlobalReplacementButton.setMinimumSize(buttonDimension);
    editGlobalReplacementButton.setMaximumSize(buttonDimension);

    // Edit selected Global Replacement
    editGlobalReplacementButton.addActionListener(e -> {
      int selectedRow = globalReplacementTable.getSelectedRow();
      Replacement tempReplacement = globalReplacementTableModel.getReplacement(selectedRow);

      replacementTypeComboBox.setSelectedItem(tempReplacement.getType());
      replacementMatchTextField.setText(tempReplacement.getMatch());
      replacementReplaceTextField.setText(tempReplacement.getReplace());
      replacementCommentTextField.setText(tempReplacement.getComment());
      replacementIsRegexCheckBox.setSelected(tempReplacement.isRegexMatch());

      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          replacementPanel,
          "Edit Global Replacement",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Replacement newReplacement = new Replacement(
            (String) replacementTypeComboBox.getSelectedItem(),
            replacementMatchTextField.getText(),
            replacementReplaceTextField.getText(),
            (String) replacementCountComboBox.getSelectedItem(),
            replacementCommentTextField.getText(),
            replacementIsRegexCheckBox.isSelected()
        );

        globalReplacementTableModel.updateReplacement(selectedRow, newReplacement);
        globalReplacementTableModel.fireTableDataChanged();
      }
      resetReplacementDialog();
    });

    deleteGlobalReplacementButton = new JButton("Remove");
    deleteGlobalReplacementButton.setPreferredSize(buttonDimension);
    deleteGlobalReplacementButton.setMinimumSize(buttonDimension);
    deleteGlobalReplacementButton.setMaximumSize(buttonDimension);

    //Delete Global Replacement
    deleteGlobalReplacementButton.addActionListener(e -> {
      int selectedRow = globalReplacementTable.getSelectedRow();
      globalReplacementTableModel.deleteReplacement(selectedRow);
      globalReplacementTableModel.fireTableDataChanged();
    });

    globalReplacementsButtonPanel = new JPanel();
    globalReplacementsButtonPanel.setLayout(new GridBagLayout());
    globalReplacementsButtonPanel.setPreferredSize(new Dimension(75, 60));
    globalReplacementsButtonPanel.setMaximumSize(new Dimension(75, 60));
    globalReplacementsButtonPanel.setPreferredSize(new Dimension(75, 60));

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_END;
    c.gridx = 0;
    c.weightx = 1;

    globalReplacementsButtonPanel.add(addGlobalReplacementButton, c);
    globalReplacementsButtonPanel.add(editGlobalReplacementButton, c);
    globalReplacementsButtonPanel.add(deleteGlobalReplacementButton, c);

    // Replacement Buttons
    addReplacementButton = new JButton("Add");
    addReplacementButton.setPreferredSize(buttonDimension);
    addReplacementButton.setMinimumSize(buttonDimension);
    addReplacementButton.setMaximumSize(buttonDimension);

    // Add New Replacement
    addReplacementButton.addActionListener(e -> {
      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          replacementPanel,
          "Add Replacement",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Replacement newReplacement = new Replacement(
            (String) replacementTypeComboBox.getSelectedItem(),
            replacementMatchTextField.getText(),
            replacementReplaceTextField.getText(),
            (String) replacementCountComboBox.getSelectedItem(),
            replacementCommentTextField.getText(),
            replacementIsRegexCheckBox.isSelected()
        );
        replacementTableModel.addReplacement(newReplacement);
        replacementTableModel.fireTableDataChanged();
      }
      resetReplacementDialog();
    });

    editReplacementButton = new JButton("Edit");
    editReplacementButton.setPreferredSize(buttonDimension);
    editReplacementButton.setMinimumSize(buttonDimension);
    editReplacementButton.setMaximumSize(buttonDimension);

    // Edit selected Replacement
    editReplacementButton.addActionListener(e -> {
      int selectedRow = replacementTable.getSelectedRow();
      Replacement tempReplacement = replacementTableModel.getReplacement(selectedRow);

      replacementTypeComboBox.setSelectedItem(tempReplacement.getType());
      replacementMatchTextField.setText(tempReplacement.getMatch());
      replacementReplaceTextField.setText(tempReplacement.getReplace());
      replacementCommentTextField.setText(tempReplacement.getComment());
      replacementIsRegexCheckBox.setSelected(tempReplacement.isRegexMatch());

      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          replacementPanel,
          "Edit Replacement",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Replacement newReplacement = new Replacement(
            (String) replacementTypeComboBox.getSelectedItem(),
            replacementMatchTextField.getText(),
            replacementReplaceTextField.getText(),
            (String) replacementCountComboBox.getSelectedItem(),
            replacementCommentTextField.getText(),
            replacementIsRegexCheckBox.isSelected()
        );

        replacementTableModel.updateReplacement(selectedRow, newReplacement);
        replacementTableModel.fireTableDataChanged();
      }
      resetReplacementDialog();
    });

    deleteReplacementButton = new JButton("Remove");
    deleteReplacementButton.setPreferredSize(buttonDimension);
    deleteReplacementButton.setMinimumSize(buttonDimension);
    deleteReplacementButton.setMaximumSize(buttonDimension);

    //Delete Replacement
    deleteReplacementButton.addActionListener(e -> {
      int selectedRow = replacementTable.getSelectedRow();
      replacementTableModel.deleteReplacement(selectedRow);
      replacementTableModel.fireTableDataChanged();
    });

    replacementsButtonPanel = new JPanel();
    replacementsButtonPanel.setLayout(new GridBagLayout());
    replacementsButtonPanel.setPreferredSize(new Dimension(75, 60));

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_END;
    c.gridx = 0;
    c.weightx = 1;

    replacementsButtonPanel.add(addReplacementButton, c);
    replacementsButtonPanel.add(editReplacementButton, c);
    replacementsButtonPanel.add(deleteReplacementButton, c);

    // Condition Buttons
    addConditionButton = new JButton("Add");
    addConditionButton.setPreferredSize(buttonDimension);
    addConditionButton.setMinimumSize(buttonDimension);
    addConditionButton.setMaximumSize(buttonDimension);

    addConditionButton.addActionListener(e -> {
      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          conditionPanel,
          "Add Condition",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Condition newCondition = new Condition(
            (String) booleanOperatorComboBox.getSelectedItem(),
            (String) matchTypeComboBox.getSelectedItem(),
            (String) matchRelationshipComboBox.getSelectedItem(),
            matchConditionTextField.getText()
        );
        conditionTableModel.addCondition(newCondition);
        conditionTableModel.fireTableDataChanged();
      }
      resetConditionDialog();
    });

    editConditionButton = new JButton("Edit");
    editConditionButton.setPreferredSize(buttonDimension);
    editConditionButton.setMinimumSize(buttonDimension);
    editConditionButton.setMaximumSize(buttonDimension);

    editConditionButton.addActionListener(e -> {
      int selectedRow = conditionTable.getSelectedRow();
      Condition tempCondition = conditionTableModel.getCondition(selectedRow);

      booleanOperatorComboBox.setSelectedItem(tempCondition.getBooleanOperator());
      matchTypeComboBox.setSelectedItem(tempCondition.getMatchType());
      matchRelationshipComboBox.setSelectedItem(tempCondition.getMatchRelationship());
      matchConditionTextField.setText(tempCondition.getMatchCondition());

      int result = JOptionPane.showConfirmDialog(mainSplitPane,
          conditionPanel,
          "Edit Condition",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        Condition newCondition = new Condition(
            (String) booleanOperatorComboBox.getSelectedItem(),
            (String) matchTypeComboBox.getSelectedItem(),
            (String) matchRelationshipComboBox.getSelectedItem(),
            matchConditionTextField.getText()
        );
        newCondition.setEnabled(tempCondition.isEnabled());

        conditionTableModel.updateCondition(selectedRow, newCondition);
        conditionTableModel.fireTableDataChanged();
      }
      resetConditionDialog();
    });

    deleteConditionButton = new JButton("Remove");
    deleteConditionButton.setPreferredSize(buttonDimension);
    deleteConditionButton.setMinimumSize(buttonDimension);
    deleteConditionButton.setMaximumSize(buttonDimension);

    deleteConditionButton.addActionListener(e -> {
      int selectedRow = conditionTable.getSelectedRow();
      conditionTableModel.deleteCondition(selectedRow);
      conditionTableModel.fireTableDataChanged();
    });

    conditionsButtonPanel = new JPanel();
    conditionsButtonPanel.setLayout(new GridBagLayout());

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_END;
    c.gridx = 0;
    c.weightx = 1;

    conditionsButtonPanel.add(addConditionButton, c);
    conditionsButtonPanel.add(editConditionButton, c);
    conditionsButtonPanel.add(deleteConditionButton, c);

    // Global Replacement Table
    Dimension globalReplacementTableDimension = new Dimension(300, 40);
    globalReplacementTableModel = new ReplacementTableModel();
    globalReplacementTable = new JTable(globalReplacementTableModel);
    globalReplacementScrollPane = new JScrollPane(globalReplacementTable);

    // Panel containing globalReplacement options
    JPanel globalReplacementsPanel;
    globalReplacementsPanel = new JPanel();
    globalReplacementsPanel
        .setBorder(BorderFactory.createTitledBorder(grayline, "Base Replacements"));
    globalReplacementsPanel.setLayout(new GridBagLayout());

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.PAGE_START;
    c.gridx = 0;
    globalReplacementsPanel.add(globalReplacementsButtonPanel, c);

    c.fill = GridBagConstraints.BOTH;
    c.gridx = 1;
    c.weightx = 1;
    c.weighty = 1;
    globalReplacementsPanel.add(globalReplacementScrollPane, c);

    //Replacement Table
    Dimension replacementTableDimension = new Dimension(250, 40);
    replacementTableModel = new ReplacementTableModel();
    replacementTable = new JTable(replacementTableModel);
    replacementScrollPane = new JScrollPane(replacementTable);
    replacementScrollPane.setMinimumSize(replacementTableDimension);

    // Panel containing replacement options
    JPanel replacementsPanel;
    replacementsPanel = new JPanel();
    replacementsPanel.setBorder(BorderFactory.createTitledBorder(grayline, "Replacements"));
    replacementsPanel.setLayout(new GridBagLayout());

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.PAGE_START;
    c.gridx = 0;
    replacementsPanel.add(replacementsButtonPanel, c);

    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridx = 1;
    replacementsPanel.add(replacementScrollPane, c);

    //Condition Table
    Dimension conditionTableDimension = new Dimension(200, 40);
    conditionTableModel = new ConditionTableModel();
    conditionTable = new JTable(conditionTableModel);
    conditionScrollPane = new JScrollPane(conditionTable);

    // Panel containing condition options
    JPanel conditionsPanel;
    conditionsPanel = new JPanel();
    conditionsPanel.setBorder(BorderFactory.createTitledBorder(grayline, "Conditions"));
    conditionsPanel.setLayout(new GridBagLayout());

    c = new GridBagConstraints();
    c.ipady = 5;
    c.anchor = GridBagConstraints.PAGE_START;
    c.gridx = 0;

    conditionsPanel.add(conditionsButtonPanel, c);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridx = 1;
    conditionsPanel.add(conditionScrollPane, c);

    // Configuration Panels
    optionsPanel = new JPanel();
    //optionsPanel.setPreferredSize(new Dimension(300, 10));
    optionsPanel.setLayout(new GridBagLayout());

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.NORTHWEST;
    optionsPanel.add(activatedButton, c);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridy = 1;
    optionsPanel.add(globalReplacementsPanel, c);
    c.gridy = 2;
    optionsPanel.add(replacementsPanel, c);
    c.gridy = 3;
    optionsPanel.add(conditionsPanel, c);

    exportPanel = createExportPanel();

    configurationPane.addTab("Options", optionsPanel);
    configurationPane.addTab("Export", exportPanel);

    // table of log entries
    //logEntriesWithoutResponses = new ArrayList<>();
    logTableModel = new LogTableModel();
    logManager = new LogManager(logTableModel);
    logTable = new LogTable(logManager.getLogTableModel());
    logTable.setAutoCreateRowSorter(true);

    logTable.getColumnModel().getColumn(0).setPreferredWidth(5);
    logTable.getColumnModel().getColumn(1).setPreferredWidth(30);
    logTable.getColumnModel().getColumn(2).setPreferredWidth(250);
    logTable.getColumnModel().getColumn(3).setPreferredWidth(20);
    logTable.getColumnModel().getColumn(4).setPreferredWidth(20);
    logTable.getColumnModel().getColumn(5).setPreferredWidth(40);
    logTable.getColumnModel().getColumn(6).setPreferredWidth(40);
    logTable.getColumnModel().getColumn(7).setPreferredWidth(30);

    // Make every cell left aligned
    DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
    leftRenderer.setHorizontalAlignment(JLabel.LEFT);
    for (int i = 0; i < 8; i++) {
      logTable.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
    }

    JScrollPane scrollPane = new JScrollPane(logTable);
    scrollPane.setPreferredSize(new Dimension(10000, 10));

    // tabs with request/response viewers
    tabs = new JTabbedPane();

    tabs.addChangeListener(e -> {
      switch (tabs.getSelectedIndex()) {
        case 0:
          updateOriginalRequestResponseViewer();
          break;
        case 1:
          updateModifiedRequestResponseViewer();
          break;
        case 2:
          updateDiffViewer();
          break;
        default:
          updateLineDiffViewer();
          break;
      }
    });

    // Request / Response Viewers
    originalRequestViewer = callbacks.createMessageEditor(this, false);
    originalResponseViewer = callbacks.createMessageEditor(this, false);
    modifiedRequestViewer = callbacks.createMessageEditor(this, false);
    modifiedResponseViewer = callbacks.createMessageEditor(this, false);

    // Request / Response Labels
    originalRequestLabel = new JLabel("Request");
    originalResponseLabel = new JLabel("Response");
    modifiedRequestLabel = new JLabel("Request");
    modifiedResponseLabel = new JLabel("Response");

    JLabel diffRequestLabel = new JLabel("Request");
    JLabel diffResponseLabel = new JLabel("Response");

    JLabel lineDiffRequestLabel = new JLabel("Request");
    JLabel lineDiffResponseLabel = new JLabel("Response");

    originalRequestLabel.setForeground(Utils.getBurpOrange());
    originalResponseLabel.setForeground(Utils.getBurpOrange());
    modifiedRequestLabel.setForeground(Utils.getBurpOrange());
    modifiedResponseLabel.setForeground(Utils.getBurpOrange());
    diffRequestLabel.setForeground(Utils.getBurpOrange());
    diffResponseLabel.setForeground(Utils.getBurpOrange());
    lineDiffRequestLabel.setForeground(Utils.getBurpOrange());
    lineDiffResponseLabel.setForeground(Utils.getBurpOrange());

    originalRequestLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    originalResponseLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    modifiedRequestLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    modifiedResponseLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    diffRequestLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    diffResponseLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    lineDiffRequestLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    lineDiffResponseLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

    // Initialize JPanels that hold request/response viewers and labels
    originalRequestPanel = new JPanel();
    modifiedRequestPanel = new JPanel();

    originalResponsePanel = new JPanel();
    modifiedResponsePanel = new JPanel();

    originalRequestPanel.setLayout(new BoxLayout(originalRequestPanel, BoxLayout.PAGE_AXIS));
    modifiedRequestPanel.setLayout(new BoxLayout(modifiedRequestPanel, BoxLayout.PAGE_AXIS));
    originalResponsePanel.setLayout(new BoxLayout(originalResponsePanel, BoxLayout.PAGE_AXIS));
    modifiedResponsePanel.setLayout(new BoxLayout(modifiedResponsePanel, BoxLayout.PAGE_AXIS));

    // Diff viewer stuff
    JSplitPane diffSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    JPanel requestDiffPanel = new JPanel();
    JPanel responseDiffPanel = new JPanel();

    requestDiffPanel.setPreferredSize(new Dimension(100000, 100000));
    responseDiffPanel.setPreferredSize(new Dimension(100000, 100000));

    requestDiffPanel.setLayout(new GridBagLayout());
    responseDiffPanel.setLayout(new GridBagLayout());

    requestComparer = new DiffViewerPane();
    responseComparer = new DiffViewerPane();

    requestComparerScrollPane = new JScrollPane(requestComparer);
    responseComparerScollPane = new JScrollPane(responseComparer);

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    requestDiffPanel.add(diffRequestLabel, c);
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    requestDiffPanel.add(requestComparerScrollPane, c);

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    responseDiffPanel.add(diffResponseLabel, c);
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    responseDiffPanel.add(responseComparerScollPane, c);

    // Line Diff Viewer Stuff
    JSplitPane lineDiffSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    JPanel requestLineDiffPanel = new JPanel();
    JPanel responseLineDiffPanel = new JPanel();

    requestLineDiffPanel.setPreferredSize(new Dimension(100000, 100000));
    responseLineDiffPanel.setPreferredSize(new Dimension(100000, 100000));

    requestLineDiffPanel.setLayout(new GridBagLayout());
    responseLineDiffPanel.setLayout(new GridBagLayout());

    requestLineComparer = new DiffViewerPane();
    responseLineComparer = new DiffViewerPane();

    requestLineComparerScrollPane = new JScrollPane(requestLineComparer);
    responseLineComparerScollPane = new JScrollPane(responseLineComparer);

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    requestLineDiffPanel.add(lineDiffRequestLabel, c);
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    requestLineDiffPanel.add(requestLineComparerScrollPane, c);

    c = new GridBagConstraints();
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    responseLineDiffPanel.add(lineDiffResponseLabel, c);
    c.gridy = 1;
    c.weightx = 1;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    responseLineDiffPanel.add(responseLineComparerScollPane, c);

    // Add Viewers
    originalRequestPanel.add(originalRequestLabel);
    originalRequestPanel.add(originalRequestViewer.getComponent());
    originalRequestPanel.setPreferredSize(new Dimension(100000, 100000));

    originalResponsePanel.add(originalResponseLabel);
    originalResponsePanel.add(originalResponseViewer.getComponent());
    originalResponsePanel.setPreferredSize(new Dimension(100000, 100000));

    modifiedRequestPanel.add(modifiedRequestLabel);
    modifiedRequestPanel.add(modifiedRequestViewer.getComponent());
    modifiedRequestPanel.setPreferredSize(new Dimension(100000, 100000));

    modifiedResponsePanel.add(modifiedResponseLabel);
    modifiedResponsePanel.add(modifiedResponseViewer.getComponent());
    modifiedResponsePanel.setPreferredSize(new Dimension(100000, 100000));

    // Add viewers to the original splitpane
    originalRequestResponseSplitPane.setLeftComponent(originalRequestPanel);
    originalRequestResponseSplitPane.setRightComponent(originalResponsePanel);

    originalRequestResponseSplitPane.setResizeWeight(0.50);
    tabs.addTab("Original", originalRequestResponseSplitPane);

    // Add viewers to the modified splitpane
    modifiedRequestResponseSplitPane.setLeftComponent(modifiedRequestPanel);
    modifiedRequestResponseSplitPane.setRightComponent(modifiedResponsePanel);
    modifiedRequestResponseSplitPane.setResizeWeight(0.5);
    tabs.addTab("Modified", modifiedRequestResponseSplitPane);

    // Add diff tab
    diffSplitPane.setLeftComponent(requestDiffPanel);
    diffSplitPane.setRightComponent(responseDiffPanel);
    diffSplitPane.setResizeWeight(0.50);
    tabs.addTab("Diff", diffSplitPane);

    //Add line diff tab
    lineDiffSplitPane.setLeftComponent(requestLineDiffPanel);
    lineDiffSplitPane.setRightComponent(responseLineDiffPanel);
    lineDiffSplitPane.setResizeWeight(0.50);
    tabs.addTab("Line Diff", lineDiffSplitPane);

    mainSplitPane.setResizeWeight(.00000000000001);
    mainSplitPane.setBottomComponent(tabs);

    // Split pane containing user interface components
    userInterfaceSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    userInterfaceSplitPane.setRightComponent(configurationPane);
    userInterfaceSplitPane.setLeftComponent(scrollPane);
    userInterfaceSplitPane.setResizeWeight(1.0);
    mainSplitPane.setTopComponent(userInterfaceSplitPane);

    // Keep the split panes at the bottom the same size.
    originalRequestResponseSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
        pce -> {
          modifiedRequestResponseSplitPane.setDividerLocation(
              originalRequestResponseSplitPane.getDividerLocation());
          diffSplitPane.setDividerLocation(
              originalRequestResponseSplitPane.getDividerLocation());
          lineDiffSplitPane.setDividerLocation(
              originalRequestResponseSplitPane.getDividerLocation());
        }
    );
    modifiedRequestResponseSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
        pce -> {
          originalRequestResponseSplitPane.setDividerLocation(
              modifiedRequestResponseSplitPane.getDividerLocation());
          diffSplitPane.setDividerLocation(
              modifiedRequestResponseSplitPane.getDividerLocation());
          lineDiffSplitPane.setDividerLocation(
              modifiedRequestResponseSplitPane.getDividerLocation());
        }
    );
    diffSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
        pce -> {
          originalRequestResponseSplitPane.setDividerLocation(
              diffSplitPane.getDividerLocation());
          modifiedRequestResponseSplitPane.setDividerLocation(
              diffSplitPane.getDividerLocation());
          lineDiffSplitPane.setDividerLocation(
              diffSplitPane.getDividerLocation());
        }
    );
    lineDiffSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
        pce -> {
          originalRequestResponseSplitPane.setDividerLocation(
              lineDiffSplitPane.getDividerLocation());
          modifiedRequestResponseSplitPane.setDividerLocation(
              lineDiffSplitPane.getDividerLocation());
          diffSplitPane.setDividerLocation(
              lineDiffSplitPane.getDividerLocation());
        }
    );

    // I don't know what this actually does but I think it's correct
    callbacks.customizeUiComponent(mainSplitPane);
    callbacks.customizeUiComponent(logTable);
    callbacks.customizeUiComponent(scrollPane);
    callbacks.customizeUiComponent(tabs);
    callbacks.customizeUiComponent(globalReplacementScrollPane);

    helpPanel = new JPanel();
    helpPanel.setLayout(new BorderLayout());
    helpPanel.add(createHelpViewer());
    //configurationPane.add("Help", helpPanel);

  }

  private JEditorPane createHelpViewer() {
    JEditorPane helpViewer = new JEditorPane();
    helpViewer.setEditable(false);
    helpViewer.setContentType("text/html");
    String helpText = "<h1>AutoRepeater</h1>" +
        "<h2>Summary<h2>" +
        "<h2>Features<h2>" +
        "<h2>User Interface<h2>" +
        "<h2>Replacements<h2>" +
        "<h2>Conditions<h2>";

    helpViewer.setText(helpText);
    return helpViewer;
  }

  private JPanel createExportPanel() {
    final String[] EXPORT_OPTIONS = {"CSV", "JSON"};
    final String[] EXPORT_WHICH_OPTIONS = {"All Tab Logs", "Selected Tab Logs"};
    final String[] EXPORT_VALUE_OPTIONS = {"Log Entry", "Log Entry + Full HTTP Request"};
    final JComboBox<String> exportTypeComboBox = new JComboBox<>(EXPORT_OPTIONS);
    final JComboBox<String> exportWhichComboBox = new JComboBox<>(EXPORT_WHICH_OPTIONS);
    final JComboBox<String> exportValueComboBox = new JComboBox<>(EXPORT_VALUE_OPTIONS);
    final JButton exportButton = new JButton("Export");
    final JButton importButton = new JButton("Import");
    final JFileChooser exportPathChooser = new JFileChooser();
    final JFileChooser importPathChooser = new JFileChooser();
    final Dimension buttonDimension = new Dimension(75, 20);
    final Dimension comboBoxDimension = new Dimension(200, 20);

    exportButton.setPreferredSize(buttonDimension);
    exportButton.setMaximumSize(buttonDimension);
    exportButton.setMaximumSize(buttonDimension);
    importButton.setPreferredSize(buttonDimension);
    importButton.setMaximumSize(buttonDimension);
    importButton.setMaximumSize(buttonDimension);
    exportTypeComboBox.setPreferredSize(comboBoxDimension);
    exportTypeComboBox.setMinimumSize(comboBoxDimension);
    exportTypeComboBox.setMaximumSize(comboBoxDimension);
    exportWhichComboBox.setPreferredSize(comboBoxDimension);
    exportWhichComboBox.setMinimumSize(comboBoxDimension);
    exportWhichComboBox.setMaximumSize(comboBoxDimension);
    exportValueComboBox.setPreferredSize(comboBoxDimension);
    exportValueComboBox.setMinimumSize(comboBoxDimension);
    exportValueComboBox.setMaximumSize(comboBoxDimension);

    JPanel exportPanel = new JPanel();
    exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.PAGE_AXIS));
    exportWhichComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    exportPanel.add(exportWhichComboBox);
    exportValueComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    exportPanel.add(exportValueComboBox);
    exportTypeComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    exportPanel.add(exportTypeComboBox);
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
    exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonPanel.add(exportButton);
    importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    //buttonPanel.add(importButton);
    buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    exportPanel.add(buttonPanel);

    exportButton.addActionListener((ActionEvent l) -> {
      int returnVal = exportPathChooser.showOpenDialog(mainSplitPane);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = exportPathChooser.getSelectedFile();
        ArrayList<LogEntry> logEntries = new ArrayList<>();
        // Collect relevant entries
        if((exportWhichComboBox.getSelectedItem()).equals("All Tab Logs")) {
          logEntries = logManager.getLogTableModel().getLog();
        } else if((exportWhichComboBox.getSelectedItem()).equals("Selected Tab Logs")) {
          int[] selectedRows = logTable.getSelectedRows();
          for(int row : selectedRows) {
            logEntries.add(logManager.getLogEntry(logTable.convertRowIndexToModel(row)));
          }
        }
        //Determine if whole request should be exported or just the log contents
        boolean exportFullHttp = !((exportValueComboBox.getSelectedItem()).equals("Log Entry"));

        if ((exportTypeComboBox.getSelectedItem()).equals("CSV")) {
          try(PrintWriter out = new PrintWriter(file.getAbsolutePath())){
            out.println(Utils.exportLogEntriesToCsv(logEntries, exportFullHttp));
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
        } else if ((exportTypeComboBox.getSelectedItem()).equals("JSON")) {
          try(PrintWriter out = new PrintWriter(file.getAbsolutePath())){
            out.println(Utils.exportLogEntriesToJson(logEntries, exportFullHttp));
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
        }
      }
    });
    return exportPanel;
  }

  private void setDefaultState() {
    conditionTableModel.addCondition(new Condition(
        "",
        "Sent From Tool",
        "Burp",
        ""
    ));

    conditionTableModel.addCondition(new Condition(
        "Or",
        "Request",
        "Contains Parameters",
        "",
        false
    ));

    conditionTableModel.addCondition(new Condition(
        "Or",
        "HTTP Method",
        "Does Not Match",
        "(GET|POST)",
        false
    ));

    conditionTableModel.addCondition(new Condition(
        "And",
        "URL",
        "Is In Scope",
        "",
        false
    ));
  }


  public void modifyAndSendRequestAndLog(
          int toolFlag,
          boolean messageIsRequest,
          IHttpRequestResponse messageInfo,
          boolean isSentToAutoRepeater) {
    // If the message is a request, check all the conditions, if the conditions end up being trust,
    // perform every replacement and resend the request
    // if the message is a response, search the list of messages that don't have a response and update the log entry

    // At some point i should refactor this whole thing to use a lookup for outgoing requests
    // and incoming responses but that caused a race condition last time and might be more work
    // then it's worth

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> {
      // Handle "Send To AutoRepeater"
     if (isSentToAutoRepeater)  {
       IHttpRequestResponse newMessageInfo;
         if (messageIsRequest) {
           newMessageInfo = BurpExtender.getCallbacks().makeHttpRequest(
               messageInfo.getHttpService(), messageInfo.getRequest());
         } else {
           newMessageInfo = messageInfo;
         }
         // This is the same thing as below. If a response is selected we're good
         HashSet<IHttpRequestResponse> requestSet = new HashSet<>();
         IHttpRequestResponse baseReplacedRequestResponse = Utils
                 .cloneIHttpRequestResponse(newMessageInfo);
         // Perform all the base replacements on the captured request
         for (Replacement globalReplacement : globalReplacementTableModel.getReplacements()) {
           baseReplacedRequestResponse.setRequest(
                   globalReplacement.performReplacement(baseReplacedRequestResponse));
         }
         //Add the base replaced request to the request set
         requestSet.add(baseReplacedRequestResponse);
         // Perform all the seperate replacements on the request+base replacements and add them to the set
         for (Replacement replacement : replacementTableModel.getReplacements()) {
           IHttpRequestResponse newHttpRequest = Utils
                   .cloneIHttpRequestResponse(baseReplacedRequestResponse);
           newHttpRequest.setRequest(replacement.performReplacement(newHttpRequest));
           requestSet.add(newHttpRequest);
         }
         // Perform every unique request and log
         for (IHttpRequestResponse request : requestSet) {
           if (!Arrays.equals(request.getRequest(), newMessageInfo.getRequest())) {
             IHttpRequestResponse modifiedRequestResponse =
                     callbacks.makeHttpRequest(newMessageInfo.getHttpService(), request.getRequest());
             int row = logManager.getRowCount();
             LogEntry newLogEntry = new LogEntry(
                     row + 1,
                     callbacks.saveBuffersToTempFiles(newMessageInfo),
                     callbacks.saveBuffersToTempFiles(modifiedRequestResponse));
             logManager.addEntry(newLogEntry);
             logManager.fireTableRowsUpdated(row, row);
             BurpExtender.highlightTab();
           }
         }
     } else {
       //Although this isn't optimal, i'm generating the modified requests when a response is received.
       //Burp doesn't have a nice way to tie arbitrary sent requests with a response received later.
       //Doing it on request requires a ton of additional book keeping that i don't think warrants the benefits
       if (!messageIsRequest
           && activatedButton.isSelected()
           && toolFlag != BurpExtender.getCallbacks().TOOL_EXTENDER) {
         boolean meetsConditions = false;
         if (conditionTableModel.getConditions().size() == 0) {
           meetsConditions = true;
         } else {
           if (conditionTableModel.getConditions()
               .stream()
               .filter(Condition::isEnabled)
               .filter(c -> c.getBooleanOperator().equals("Or"))
               .anyMatch(c -> c.checkCondition(toolFlag, messageInfo))) {
             meetsConditions = true;
           }
           if (conditionTableModel.getConditions()
               .stream()
               .filter(Condition::isEnabled)
               .filter(
                   c -> c.getBooleanOperator().equals("And") || c.getBooleanOperator().equals(""))
               .allMatch(c -> c.checkCondition(toolFlag, messageInfo))) {
             meetsConditions = true;
           }
         }
         if (meetsConditions) {
           // Create a set to store each new unique request in
           HashSet<IHttpRequestResponse> requestSet = new HashSet<>();
           IHttpRequestResponse baseReplacedRequestResponse = Utils
               .cloneIHttpRequestResponse(messageInfo);
           // Perform all the base replacements on the captured request
           for (Replacement globalReplacement : globalReplacementTableModel.getReplacements()) {
             baseReplacedRequestResponse.setRequest(
                 globalReplacement.performReplacement(baseReplacedRequestResponse));
           }
           //Add the base replaced request to the request set
           requestSet.add(baseReplacedRequestResponse);
           // Perform all the seperate replacements on the request+base replacements and add them to the set
           for (Replacement replacement : replacementTableModel.getReplacements()) {
             IHttpRequestResponse newHttpRequest = Utils
                 .cloneIHttpRequestResponse(baseReplacedRequestResponse);
             newHttpRequest.setRequest(replacement.performReplacement(newHttpRequest));
             requestSet.add(newHttpRequest);
           }
           // Perform every unique request and log
           for (IHttpRequestResponse request : requestSet) {
             if (!Arrays.equals(request.getRequest(), messageInfo.getRequest())) {
               IHttpRequestResponse modifiedRequestResponse =
                   callbacks.makeHttpRequest(messageInfo.getHttpService(), request.getRequest());
               int row = logManager.getRowCount();
               LogEntry newLogEntry = new LogEntry(
                   row + 1,
                   callbacks.saveBuffersToTempFiles(messageInfo),
                   callbacks.saveBuffersToTempFiles(modifiedRequestResponse));
               logManager.addEntry(newLogEntry);
               logManager.fireTableRowsUpdated(row, row);
               BurpExtender.highlightTab();
             }
           }
         }
       }
     }
    });
  }

  // Implement IMessageEditorController
  @Override
  public byte[] getRequest() {
    switch (tabs.getSelectedIndex()) {
      case 0:
        return currentOriginalRequestResponse.getRequest();
      case 1:
        return currentModifiedRequestResponse.getRequest();
      default:
        return new byte[0];
    }
  }

  @Override
  public byte[] getResponse() {
    switch (tabs.getSelectedIndex()) {
      case 0:
        return currentOriginalRequestResponse.getResponse();
      case 1:
        return currentModifiedRequestResponse.getResponse();
      default:
        return new byte[0];
    }
  }

  @Override
  public IHttpService getHttpService() {
    switch (tabs.getSelectedIndex()) {
      case 0:
        return currentOriginalRequestResponse.getHttpService();
      case 1:
        return currentModifiedRequestResponse.getHttpService();
      default:
        return null;
    }
  }

  private void updateOriginalRequestResponseViewer() {
    SwingUtilities.invokeLater(() -> {
      // Set Original Request Viewer
      if (originalRequest != null) {
        originalRequestViewer.setMessage(originalRequest, true);
      } else {
        originalRequestViewer.setMessage(new byte[0], true);
      }

      // Set Original Response Viewer
      if (originalResponse != null) {
        originalResponseViewer.setMessage(originalResponse, false);
      } else {
        originalResponseViewer.setMessage(new byte[0], false);
      }
    });
  }

  private void updateModifiedRequestResponseViewer() {
    SwingUtilities.invokeLater(() -> {
      // Set Modified Request Viewer
      if (modifiedRequest != null) {
        modifiedRequestViewer.setMessage(modifiedRequest, true);
      } else {
        modifiedRequestViewer.setMessage(new byte[0], true);
      }

      // Set Modified Response Viewer
      if (modifiedResponse != null) {
        modifiedResponseViewer.setMessage(modifiedResponse, false);
      } else {
        modifiedResponseViewer.setMessage(new byte[0], false);
      }
    });
  }

  private void updateDiffViewer() {
    SwingUtilities.invokeLater(() -> {
      if (originalRequest != null && modifiedRequest != null) {
        requestComparer.setText(requestDiff);
        requestComparer.setCaretPosition(0);
      } else {
        requestComparer.setText("");
      }

      // Set Response Diff Viewer
      if (originalResponse != null && modifiedResponse != null) {
        responseComparer.setText(responseDiff);
        responseComparer.setCaretPosition(0);
      } else {
        responseComparer.setText("");
      }
    });
  }

  private void updateLineDiffViewer() {
    SwingUtilities.invokeLater(() -> {
      if (originalRequest != null && modifiedRequest != null) {
        requestLineComparer.setText(requestLineDiff);
        requestLineComparer.setCaretPosition(0);
      } else {
        requestLineComparer.setText("");
      }

      // Set Response Diff Viewer
      if (originalResponse != null && modifiedResponse != null) {
        responseLineComparer.setText(responseLineDiff);
        responseLineComparer.setCaretPosition(0);
      } else {
        responseLineComparer.setText("");
      }
    });
  }

  private void updateRequestViewers() {
    switch (tabs.getSelectedIndex()) {
      case 0:
        updateOriginalRequestResponseViewer();
        break;
      case 1:
        updateModifiedRequestResponseViewer();
        break;
      case 2:
        updateDiffViewer();
        break;
      default:
        updateLineDiffViewer();
        break;
    }
  }
  // JTable for Viewing Logs
  private class LogTable extends JTable {

    public LogTable(TableModel tableModel) {
      super(tableModel);
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
      super.changeSelection(row, col, toggle, extend);
      // show the log entry for the selected row
      LogEntry logEntry = logManager.getLogEntry(convertRowIndexToModel(row));

      // There's a delay while changing selections because setting the diff viewer is slow.
      new Thread(() -> {
        originalRequest = logEntry.getOriginalRequestResponse().getRequest();
        originalResponse = logEntry.getOriginalRequestResponse().getResponse();
        modifiedRequest = logEntry.getModifiedRequestResponse().getRequest();
        modifiedResponse = logEntry.getModifiedRequestResponse().getResponse();
        currentOriginalRequestResponse = logEntry.getOriginalRequestResponse();
        currentModifiedRequestResponse = logEntry.getModifiedRequestResponse();

        new Thread(() -> {
          requestDiff = HttpComparer
              .diffText(new String(originalRequest), new String(modifiedRequest));
          updateRequestViewers();
        }).start();
        new Thread(() -> {
          responseDiff = HttpComparer
              .diffText(new String(originalResponse), new String(modifiedResponse));
          updateRequestViewers();
        }).start();
        new Thread(() -> {
          requestLineDiff = HttpComparer
              .diffLines(new String(originalRequest), new String(modifiedRequest));
          updateRequestViewers();
        }).start();
        new Thread(() -> {
          responseLineDiff = HttpComparer
              .diffLines(new String(originalResponse), new String(modifiedResponse));
          updateRequestViewers();
        }).start();

        updateRequestViewers();

        // Hack to speed up the ui
      }).start();
    }
  }
}
