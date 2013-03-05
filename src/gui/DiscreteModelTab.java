package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NexusImporter;
import jebl.evolution.trees.RootedTree;
import templates.DiscreteTreeToKML;
import templates.DiscreteTreeToProcessing;
import templates.MapBackground;
import utils.Utils;
import app.SpreadApp;
import checks.DiscreteSanityCheck;
import colorpicker.swing.ColorPicker;

@SuppressWarnings("serial")
public class DiscreteModelTab extends JPanel {

	//Shared Frame
	private SpreadApp frame;
	
	// Sizing constants
	private final int leftPanelWidth = 260;
	private final int leftPanelHeight = 1000;
	private final int spinningPanelHeight = 20;
	private final int mapImageWidth = MapBackground.MAP_IMAGE_WIDTH;
	private final int mapImageHeight = MapBackground.MAP_IMAGE_HEIGHT;
	private final Dimension minimumDimension = new Dimension(0, 0);

	// Colors
	private Color backgroundColor;
	private Color polygonsMaxColor;
	private Color branchesMaxColor;
	private Color polygonsMinColor;
	private Color branchesMinColor;

	// Locations & coordinates table
	private InteractiveTableModel table = null;

	// Strings for paths
	private String treeFilename = null;
	private File workingDirectory = null;

	// Text fields
	private JTextField numberOfIntervalsParser;
	private JTextField maxAltMappingParser;
	private JTextField kmlPathParser;
	private JTextField timescalerParser;

	// Spinners
	private DateSpinner dateSpinner;

	// Buttons
	private JButton generateKml;
	private JButton openTree;
	private JButton openLocationCoordinatesEditor;
	private JButton generateProcessing;
	private JButton saveProcessingPlot;
	private JButton polygonsMaxColorChooser;
	private JButton branchesMaxColorChooser;
	private JButton polygonsMinColorChooser;
	private JButton branchesMinColorChooser;

	// Sliders
	private JSlider branchesWidthParser;
	private JSlider polygonsRadiusMultiplierParser;

	// Combo boxes
	private JComboBox eraParser;
	private JComboBox stateAttributeNameParser;

	// left tools pane
	private JPanel leftPanel;
	private JPanel tmpPanel;
	private SpinningPanel sp;
	private JPanel tmpPanelsHolder;

	// Processing pane
	private DiscreteTreeToProcessing discreteTreeToProcessing;

	// Progress bar
	private JProgressBar progressBar;

	public DiscreteModelTab(SpreadApp spreadApp) {

		this.frame = spreadApp;
		
		// Setup miscallenous
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		backgroundColor = new Color(231, 237, 246);
		polygonsMinColor = new Color(0, 0, 0, 100);
		polygonsMaxColor = new Color(50, 255, 255, 255);
		branchesMinColor = new Color(0, 0, 0, 255);
		branchesMaxColor = new Color(255, 5, 50, 255);
		GridBagConstraints c = new GridBagConstraints();

		// Setup text fields
		numberOfIntervalsParser = new JTextField("100", 10);
		maxAltMappingParser = new JTextField("5000000", 10);
		kmlPathParser = new JTextField("output.kml", 10);
		timescalerParser = new JTextField("1", 10);

		// Setup buttons for tab
		generateKml = new JButton("Generate", SpreadApp.nuclearIcon);
		openTree = new JButton("Open", SpreadApp.treeIcon);
		openLocationCoordinatesEditor = new JButton("Setup", SpreadApp.locationsIcon);
		generateProcessing = new JButton("Plot", SpreadApp.processingIcon);
		saveProcessingPlot = new JButton("Save", SpreadApp.saveIcon);
		polygonsMaxColorChooser = new JButton("Setup max");
		branchesMaxColorChooser = new JButton("Setup max");
		polygonsMinColorChooser = new JButton("Setup min");
		branchesMinColorChooser = new JButton("Setup min");

		// Setup sliders
		branchesWidthParser = new JSlider(JSlider.HORIZONTAL, 2, 10, 4);
		branchesWidthParser.setMajorTickSpacing(2);
		branchesWidthParser.setMinorTickSpacing(1);
		branchesWidthParser.setPaintTicks(true);
		branchesWidthParser.setPaintLabels(true);
		polygonsRadiusMultiplierParser = new JSlider(JSlider.HORIZONTAL, 1, 11,
				1);
		polygonsRadiusMultiplierParser.setMajorTickSpacing(2);
		polygonsRadiusMultiplierParser.setMinorTickSpacing(1);
		polygonsRadiusMultiplierParser.setPaintTicks(true);
		polygonsRadiusMultiplierParser.setPaintLabels(true);

		// Setup Combo boxes
		stateAttributeNameParser = new JComboBox(new String[] {"state"});
		stateAttributeNameParser.setName("state");
		
		// Setup progress bar
		progressBar = new JProgressBar();

		// Left tools pane
		leftPanel = new JPanel();
		leftPanel.setBackground(backgroundColor);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		openTree.addActionListener(new ListenOpenTree());
		generateKml.addActionListener(new ListenGenerateKml());
		openLocationCoordinatesEditor
				.addActionListener(new ListenOpenLocationCoordinatesEditor());
		generateProcessing.addActionListener(new ListenGenerateProcessing());
		saveProcessingPlot.addActionListener(new ListenSaveProcessingPlot());
		polygonsMaxColorChooser
				.addActionListener(new ListenPolygonsMaxColorChooser());
		branchesMaxColorChooser
				.addActionListener(new ListenBranchesMaxColorChooser());
		polygonsMinColorChooser
				.addActionListener(new ListenPolygonsMinColorChooser());
		branchesMinColorChooser
				.addActionListener(new ListenBranchesMinColorChooser());

		// /////////////
		// ---INPUT---//
		// /////////////

		tmpPanelsHolder = new JPanel();
		tmpPanelsHolder.setLayout(new BoxLayout(tmpPanelsHolder,
				BoxLayout.Y_AXIS));

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Load tree file:"));
		tmpPanel.add(openTree);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Setup location coordinates:"));
		tmpPanel.add(openLocationCoordinatesEditor);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setLayout(new GridBagLayout());
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Most recent sampling date:"));
		dateSpinner = new DateSpinner();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		tmpPanel.add(dateSpinner, c);
		String era[] = { "AD", "BC" };
		eraParser = new JComboBox(era);
		c.gridx = 2;
		c.gridy = 0;
		tmpPanel.add(eraParser, c);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("State attribute name:"));
		tmpPanel.add(stateAttributeNameParser);
		tmpPanelsHolder.add(tmpPanel);

		sp = new SpinningPanel(tmpPanelsHolder, "   Input", new Dimension(
				leftPanelWidth, spinningPanelHeight));
		sp.showBottom(true);
		leftPanel.add(sp);

		// ////////////////////////
		// ---BRANCHES MAPPING---//
		// ////////////////////////

		tmpPanelsHolder = new JPanel();
		tmpPanelsHolder.setLayout(new BoxLayout(tmpPanelsHolder,
				BoxLayout.Y_AXIS));

		// Branches color mapping:
		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setLayout(new GridBagLayout());
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Branches color mapping:"));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		tmpPanel.add(branchesMinColorChooser, c);
		c.gridx = 2;
		c.gridy = 0;
		tmpPanel.add(branchesMaxColorChooser, c);
		tmpPanelsHolder.add(tmpPanel);

		// Branches width:
		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Branches width:"));
		tmpPanel.add(branchesWidthParser);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Maximal altitude:"));
		tmpPanel.add(maxAltMappingParser);
		tmpPanelsHolder.add(tmpPanel);

		sp = new SpinningPanel(tmpPanelsHolder, "   Branches mapping",
				new Dimension(leftPanelWidth, spinningPanelHeight));
		sp.showBottom(false);
		leftPanel.add(sp);

		// ////////////////////////
		// ---POLYGONS MAPPING---//
		// ////////////////////////

		tmpPanelsHolder = new JPanel();
		tmpPanelsHolder.setLayout(new BoxLayout(tmpPanelsHolder,
				BoxLayout.Y_AXIS));

		// Circles color mapping:
		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setLayout(new GridBagLayout());
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Circles color mapping:"));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		tmpPanel.add(polygonsMinColorChooser, c);
		c.gridx = 2;
		c.gridy = 0;
		tmpPanel.add(polygonsMaxColorChooser, c);
		tmpPanelsHolder.add(tmpPanel);

		// Circles radius multiplier:
		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Circles radius multiplier:"));
		tmpPanel.add(polygonsRadiusMultiplierParser);
		tmpPanelsHolder.add(tmpPanel);

		sp = new SpinningPanel(tmpPanelsHolder, "   Circles mapping",
				new Dimension(leftPanelWidth, spinningPanelHeight));
		sp.showBottom(false);
		leftPanel.add(sp);

		// ////////////////////
		// ---COMPUTATIONS---//
		// ////////////////////

		tmpPanelsHolder = new JPanel();
		tmpPanelsHolder.setLayout(new BoxLayout(tmpPanelsHolder,
				BoxLayout.Y_AXIS));

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Number of intervals:"));
		tmpPanel.add(numberOfIntervalsParser);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Time scale multiplier:"));
		tmpPanel.add(timescalerParser);
		tmpPanelsHolder.add(tmpPanel);

		sp = new SpinningPanel(tmpPanelsHolder, "   Computations",
				new Dimension(leftPanelWidth, spinningPanelHeight));
		sp.showBottom(false);
		leftPanel.add(sp);

		// //////////////
		// ---OUTPUT---//
		// //////////////

		tmpPanelsHolder = new JPanel();
		tmpPanelsHolder.setLayout(new BoxLayout(tmpPanelsHolder,
				BoxLayout.Y_AXIS));

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("KML name:"));
		tmpPanel.add(kmlPathParser);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setLayout(new GridBagLayout());
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Generate KML / Plot map:"));
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		tmpPanel.add(generateKml, c);
		c.gridx = 2;
		c.gridy = 0;
		tmpPanel.add(generateProcessing, c);
		c.ipady = 7;
		c.gridwidth = 3;
		c.gridx = 0;
		c.gridy = 1;
		tmpPanel.add(progressBar, c);
		tmpPanelsHolder.add(tmpPanel);

		tmpPanel = new JPanel();
		tmpPanel.setMaximumSize(new Dimension(leftPanelWidth, 100));
		tmpPanel.setBackground(backgroundColor);
		tmpPanel.setBorder(new TitledBorder("Save plot:"));
		tmpPanel.add(saveProcessingPlot);
		tmpPanelsHolder.add(tmpPanel);

		sp = new SpinningPanel(tmpPanelsHolder, "   Output", new Dimension(
				leftPanelWidth, spinningPanelHeight));
		sp.showBottom(false);
		leftPanel.add(sp);

		// ////////////////////////
		// ---LEFT SCROLL PANE---//
		// ////////////////////////

		JScrollPane leftScrollPane = new JScrollPane(leftPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftScrollPane.setMinimumSize(minimumDimension);
		leftScrollPane.setMaximumSize(new Dimension(leftPanelWidth, leftPanelHeight));

		/**
		 * Processing pane
		 * */
		discreteTreeToProcessing = new DiscreteTreeToProcessing();
		discreteTreeToProcessing.setPreferredSize(new Dimension(mapImageWidth,
				mapImageHeight));

//		if (System.getProperty("java.runtime.name").toLowerCase().startsWith(
//				"openjdk")) {
//
//			JScrollPane rightScrollPane = new JScrollPane(
//					discreteTreeToProcessing,
//					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
//					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//			rightScrollPane.setMinimumSize(minimumDimension);
//
//			SplitPane splitPane = new SplitPane(JSplitPane.HORIZONTAL_SPLIT,
//					leftScrollPane, rightScrollPane);
//			splitPane.setDividerLocation(leftPanelWidth);
//
//			this.add(splitPane);
//
//		} else {

			ScrollPane rightScrollPane = new ScrollPane(
					ScrollPane.SCROLLBARS_ALWAYS);
			rightScrollPane.add(discreteTreeToProcessing);
			rightScrollPane.setMinimumSize(minimumDimension);

			SplitPane splitPane = new SplitPane(JSplitPane.HORIZONTAL_SPLIT,
					leftScrollPane, rightScrollPane);
			splitPane.setDividerLocation(leftPanelWidth);

			this.add(splitPane);

//		}

	} // END: discreteModelTab

	private void populateAttributeComboboxes() {
		
		try {
			
			NexusImporter importer = new NexusImporter(new FileReader(
					treeFilename));
			RootedTree tree = (RootedTree) importer.importNextTree();

			LinkedHashSet<String> uniqueAttributes = new LinkedHashSet<String>();

			for (Node node : tree.getNodes()) {
				if (!tree.isRoot(node)) {

					uniqueAttributes.addAll(node.getAttributeNames());

				}// END: root check
			}// END: nodeloop

			// re-initialise comboboxes
			ComboBoxModel stateNameParserModel = new DefaultComboBoxModel(uniqueAttributes.toArray(new String[0]));
			stateAttributeNameParser.setModel(stateNameParserModel);
			
		} catch (FileNotFoundException e) {
			Utils.handleException(e, e.getMessage());
		} catch (IOException e) {
			Utils.handleException(e, e.getMessage());
		} catch (ImportException e) {
			Utils.handleException(e, e.getMessage());
		}// END: try-catch block

	}// END: populateAttributeComboboxes
	
	private class ListenOpenTree implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			try {

				String[] treeFiles = new String[] { "tre", "tree", "trees" };

				final JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Loading tree file...");
				chooser.setMultiSelectionEnabled(false);
				chooser.addChoosableFileFilter(new SimpleFileFilter(treeFiles,
						"Tree files (*.tree(s), *.tre)"));
				chooser.setCurrentDirectory(workingDirectory);

				int returnVal = chooser.showOpenDialog(Utils.getActiveFrame());
				if (returnVal == JFileChooser.APPROVE_OPTION) {

					File file = chooser.getSelectedFile();
					treeFilename = file.getAbsolutePath();
					
					frame.setStatus("Opened " + treeFilename + "\n");

					File tmpDir = chooser.getCurrentDirectory();

					if (tmpDir != null) {
						workingDirectory = tmpDir;
					}

					populateAttributeComboboxes();

				} else {
					frame.setStatus("Could not Open! \n");
				}

			} catch (Exception e) {
				Utils.handleException(e, e.getMessage());
			}// END: try-catch block

		}// END: actionPerformed
	}// END: ListenOpenTree

	private class ListenOpenLocationCoordinatesEditor implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			try {

				LocationCoordinatesEditor locationCoordinatesEditor = new LocationCoordinatesEditor(frame);
				locationCoordinatesEditor.launch(treeFilename,
						stateAttributeNameParser.getSelectedItem().toString(), workingDirectory);

				table = locationCoordinatesEditor.getTable();

			} catch (NullPointerException e) {

				Utils.handleException(e,
						"Have you imported the proper tree file?");

			} catch (RuntimeException e) {

				Utils.handleException(
						e,
						"Have you specified proper state attribute name?"
								+ "\nHave you set the posterior probability limit in treeAnnotator to zero?");

			}// END: try-catch block

		}// END: actionPerformed
	}// END: ListenOpenLocations

	private class ListenPolygonsMinColorChooser implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			Color c = ColorPicker.showDialog(Utils.getActiveFrame(),
					"Choose minimum polygons color...", polygonsMinColor, true);

			if (c != null)
				polygonsMinColor = c;

		}
	}

	private class ListenPolygonsMaxColorChooser implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			Color c = ColorPicker.showDialog(Utils.getActiveFrame(),
					"Choose maximum polygons color...", polygonsMaxColor, true);

			if (c != null)
				polygonsMaxColor = c;

		}
	}

	private class ListenBranchesMinColorChooser implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			Color c = ColorPicker.showDialog(Utils.getActiveFrame(),
					"Choose minimum branches color...", branchesMinColor, true);

			if (c != null)
				branchesMinColor = c;

		}
	}

	private class ListenBranchesMaxColorChooser implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			Color c = ColorPicker.showDialog(Utils.getActiveFrame(),
					"Choose maximum branches color...", branchesMaxColor, true);

			if (c != null)
				branchesMaxColor = c;

		}
	}

	private class ListenGenerateKml implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			if (treeFilename == null) {

				new ListenOpenTree().actionPerformed(ev);

			} else if (table == null) {

				new ListenOpenLocationCoordinatesEditor().actionPerformed(ev);

			} else {

				generateKml.setEnabled(false);
				progressBar.setIndeterminate(true);

				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

					// Executed in background thread
					public Void doInBackground() {

						try {

							if (new DiscreteSanityCheck().check(treeFilename,
									stateAttributeNameParser.getSelectedItem().toString(), table)) {

								DiscreteTreeToKML discreteTreeToKML = new DiscreteTreeToKML();

								discreteTreeToKML.setTable(table);

								discreteTreeToKML
										.setStateAttName(stateAttributeNameParser.getSelectedItem().toString());

								discreteTreeToKML
										.setMaxAltitudeMapping(Double
												.valueOf(maxAltMappingParser
														.getText()));

								discreteTreeToKML
										.setMrsdString(dateSpinner.getValue()
												+ " "
												+ (eraParser.getSelectedIndex() == 0 ? "AD"
														: "BC"));

								discreteTreeToKML.setTimescaler(Integer
										.valueOf(timescalerParser.getText()));

								discreteTreeToKML.setNumberOfIntervals(Integer
										.valueOf(numberOfIntervalsParser
												.getText()));

								discreteTreeToKML
										.setKmlWriterPath(workingDirectory
												.toString()
												.concat("/")
												.concat(kmlPathParser.getText()));

								discreteTreeToKML.setTreePath(treeFilename);

								discreteTreeToKML
										.setMinPolygonRedMapping(polygonsMinColor
												.getRed());

								discreteTreeToKML
										.setMinPolygonGreenMapping(polygonsMinColor
												.getGreen());

								discreteTreeToKML
										.setMinPolygonBlueMapping(polygonsMinColor
												.getBlue());

								discreteTreeToKML
										.setMinPolygonOpacityMapping(polygonsMinColor
												.getAlpha());

								discreteTreeToKML
										.setMaxPolygonRedMapping(polygonsMaxColor
												.getRed());

								discreteTreeToKML
										.setMaxPolygonGreenMapping(polygonsMaxColor
												.getGreen());

								discreteTreeToKML
										.setMaxPolygonBlueMapping(polygonsMaxColor
												.getBlue());

								discreteTreeToKML
										.setMaxPolygonOpacityMapping(polygonsMaxColor
												.getAlpha());

								discreteTreeToKML
										.setPolygonsRadiusMultiplier(polygonsRadiusMultiplierParser
												.getValue());

								discreteTreeToKML
										.setMinBranchRedMapping(branchesMinColor
												.getRed());

								discreteTreeToKML
										.setMinBranchGreenMapping(branchesMinColor
												.getGreen());

								discreteTreeToKML
										.setMinBranchBlueMapping(branchesMinColor
												.getBlue());

								discreteTreeToKML
										.setMinBranchOpacityMapping(branchesMinColor
												.getAlpha());

								discreteTreeToKML
										.setMaxBranchRedMapping(branchesMaxColor
												.getRed());

								discreteTreeToKML
										.setMaxBranchGreenMapping(branchesMaxColor
												.getGreen());

								discreteTreeToKML
										.setMaxBranchBlueMapping(branchesMaxColor
												.getBlue());

								discreteTreeToKML
										.setMaxBranchOpacityMapping(branchesMaxColor
												.getAlpha());

								discreteTreeToKML
										.setBranchWidth(branchesWidthParser
												.getValue());

								discreteTreeToKML.GenerateKML();

								System.out.println("Finished in: "
										+ discreteTreeToKML.time + " msec \n");

							}// END: check

						} catch (final Exception e) {
							Utils.handleException(e, null);
						}

						return null;
					}// END: doInBackground()

					// Executed in event dispatch thread
					public void done() {
						generateKml.setEnabled(true);
						progressBar.setIndeterminate(false);

						frame.setStatus("Generated "
								+ workingDirectory.toString().concat("/")
										.concat(kmlPathParser.getText()));
					}
				};

				worker.execute();

			}// END: if not loaded

		}// END: actionPerformed
	}// END: ListenGenerateKml

	private class ListenGenerateProcessing implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			if (treeFilename == null) {

				new ListenOpenTree().actionPerformed(ev);

			} else if (table == null) {

				new ListenOpenLocationCoordinatesEditor().actionPerformed(ev);

			} else {

				generateProcessing.setEnabled(false);
				progressBar.setIndeterminate(true);

				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

					// Executed in background thread
					public Void doInBackground() {

						// TODO This work should be activated after each option
						// changes (automatic responsive plotting); will make
						// for a much slicker program

						try {

							if (new DiscreteSanityCheck().check(treeFilename,
									stateAttributeNameParser.getSelectedItem().toString(), table)) {

								// TODO Should only be done with state data
								// changes, not with each draw
								discreteTreeToProcessing
										.setStateAttName(stateAttributeNameParser.getSelectedItem().toString());

								// TODO Should only be done when changed,
								// not with each draw
								discreteTreeToProcessing.setTable(table);

								// TODO Should only be done when changed,
								// not with each draw
								discreteTreeToProcessing
										.setTreePath(treeFilename);

								discreteTreeToProcessing
										.setNumberOfIntervals(Integer
												.valueOf(numberOfIntervalsParser
														.getText()));

								discreteTreeToProcessing
										.setMinPolygonRedMapping(polygonsMinColor
												.getRed());

								discreteTreeToProcessing
										.setMinPolygonGreenMapping(polygonsMinColor
												.getGreen());

								discreteTreeToProcessing
										.setMinPolygonBlueMapping(polygonsMinColor
												.getBlue());

								discreteTreeToProcessing
										.setMinPolygonOpacityMapping(polygonsMinColor
												.getAlpha());

								discreteTreeToProcessing
										.setMaxPolygonRedMapping(polygonsMaxColor
												.getRed());

								discreteTreeToProcessing
										.setMaxPolygonGreenMapping(polygonsMaxColor
												.getGreen());

								discreteTreeToProcessing
										.setMaxPolygonBlueMapping(polygonsMaxColor
												.getBlue());

								discreteTreeToProcessing
										.setMaxPolygonOpacityMapping(polygonsMaxColor
												.getAlpha());

								discreteTreeToProcessing
										.setPolygonsRadiusMultiplier(polygonsRadiusMultiplierParser
												.getValue());

								discreteTreeToProcessing
										.setMinBranchRedMapping(branchesMinColor
												.getRed());

								discreteTreeToProcessing
										.setMinBranchGreenMapping(branchesMinColor
												.getGreen());

								discreteTreeToProcessing
										.setMinBranchBlueMapping(branchesMinColor
												.getBlue());

								discreteTreeToProcessing
										.setMinBranchOpacityMapping(branchesMinColor
												.getAlpha());

								discreteTreeToProcessing
										.setMaxBranchRedMapping(branchesMaxColor
												.getRed());

								discreteTreeToProcessing
										.setMaxBranchGreenMapping(branchesMaxColor
												.getGreen());

								discreteTreeToProcessing
										.setMaxBranchBlueMapping(branchesMaxColor
												.getBlue());

								discreteTreeToProcessing
										.setMaxBranchOpacityMapping(branchesMaxColor
												.getAlpha());

								discreteTreeToProcessing
										.setBranchWidth(branchesWidthParser
												.getValue() / 2);

								discreteTreeToProcessing.init();

							}// END: check

						} catch (final Exception e) {

							Utils.handleException(e, null);

						}

						return null;
					}// END: doInBackground

					// Executed in event dispatch thread
					public void done() {

						generateProcessing.setEnabled(true);
						progressBar.setIndeterminate(false);
						frame.setStatus("Finished. \n");
						
					}
				};

				worker.execute();

			}// END: if not loaded

		}// END: actionPerformed
	}// END: ListenGenerateProcessing

	private class ListenSaveProcessingPlot implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			try {

				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Saving as png file...");

				int returnVal = chooser.showSaveDialog(Utils.getActiveFrame());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					
					File file = chooser.getSelectedFile();
					String filename = file.getAbsolutePath();

					discreteTreeToProcessing.save(filename);
					
					frame.setStatus("Saved " + filename + "\n");

				} else {
					frame.setStatus("Could not Save! \n");
				}

			} catch (Exception e) {
				Utils.handleException(e, e.getMessage());
			}// END: try-catch block

		}// END: actionPerformed
	}// END: ListenSaveProcessingPlot

}// END: class
