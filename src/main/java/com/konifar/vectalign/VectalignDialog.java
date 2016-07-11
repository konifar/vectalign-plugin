package com.konifar.vectalign;

import com.bonnyfone.vectalign.Main;
import com.bonnyfone.vectalign.SVGParser;
import com.bonnyfone.vectalign.Utils;
import com.bonnyfone.vectalign.VectAlign;
import com.bonnyfone.vectalign.viewer.SVGDrawingPanel;
import com.bonnyfone.vectalign.viewer.SVGDrawingPanelListener;
import com.bonnyfone.vectalign.viewer.VectAlignExportDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.sun.java.swing.plaf.windows.WindowsBorders;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class VectalignDialog extends DialogWrapper implements SVGDrawingPanelListener {

    private JPanel mainPanel;

    private VectAlign.Mode currentAlignMode = VectAlign.Mode.BASE;
    private int hgap = 0;
    private int vgap = 0;
    private int btnIconSize = 23;
    private Color svgPanelBackgroundColor = Color.WHITE;
    private String currentSvgStrokeColor = "#000000";
    private String currentSvgFillColor = "#0099CC"; //SVGDrawingPanel.TRANSPARENT_COLOR;

    private float strokeSize = 2.0f;

    private SVGDrawingPanel[] svgs;

    //Morphing
    private JPanel panelMorphing;
    private JSlider sliderMorphing;
    private JButton btnMorphAnimation;
    private ImageIcon icnPlay;
    private ImageIcon icnPause;
    private ImageIcon icnCopy;
    private ImageIcon icnExport;
    private SVGDrawingPanel svgMorphing;

    //Input SVGs
    private JPanel panelInput;
    private JPanel panelConfig;
    private JButton btnEditFrom;
    private JButton btnEditTo;
    private JButton btnSvgFrom;
    private JButton btnSvgTo;
    private File fileFrom;
    private File fileTo;

    //Controls
    private JPanel panelControls;
    private JRadioButton radioStrategyBase;
    private JRadioButton radioStrategyLinear;
    private JRadioButton radioStrategySubBase;
    private JRadioButton radioStrategySubLinear;
    private JCheckBox checkStrokeColor;
    private JCheckBox checkFillColor;
    private JPanel panelStrokeColor;
    private JPanel panelFillColor;
    private JSpinner spinnerStroke;


    //Output
    private JPanel panelOutput;
    private JTextArea svgFromOutput;
    private JTextArea svgToOutput;
    private JButton btnCopyFrom;
    private JButton btnCopyTo;
    private JButton btnExport;
    private File outDir;
    private String lastUsedPrefix;

    private SVGDrawingPanel svgFrom;
    private SVGDrawingPanel svgTo;
    private BorderLayout mainLayout;

    private String[] result;

    VectalignDialog(@Nullable Project project) {
        super(project, true);
        mainPanel = new JPanel();
        setModal(true);

        initIcons();
        initComponents();
        addListeners();
        pack();
        center();

        //FIXME show demo
        demo();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    private void initIcons() {
        icnPlay = new ImageIcon((new ImageIcon(this.getClass().getResource("/icn_play.png")).getImage().getScaledInstance(btnIconSize, btnIconSize, java.awt.Image.SCALE_SMOOTH)));
        icnPause = new ImageIcon((new ImageIcon(this.getClass().getResource("/icn_pause.png")).getImage().getScaledInstance(btnIconSize, btnIconSize, java.awt.Image.SCALE_SMOOTH)));
        icnCopy = new ImageIcon((new ImageIcon(this.getClass().getResource("/icn_copy.png")).getImage().getScaledInstance(btnIconSize, btnIconSize, java.awt.Image.SCALE_SMOOTH)));
        icnExport = new ImageIcon((new ImageIcon(this.getClass().getResource("/export-icon.png")).getImage().getScaledInstance(btnIconSize, btnIconSize, java.awt.Image.SCALE_SMOOTH)));
    }

    private void center(){
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
    }

    private void demo() {
        String sampleA = "M 366.64407,65.08474 L 295.25723,225.79703 L 469.14417,252.02396 L 294.26984,270.55728 L 358.5001,434.26126 L 255.0126,292.0823 L 145.35594,429.55933 L 216.74277,268.84705 L 42.855843,242.62012 L 217.73016,224.08678 L 153.49991,60.38282 L 256.9874,202.56177 L 366.64407,65.08474";
        String sampleB = "M 91.095527,384.35546 L 254.23312,110.62095 L 405.71803,386.1926 z";

        svgFrom.setPath(sampleA);
        svgTo.setPath(sampleB);

        reloadSvgWithProperties();
        reloadMorphing(true);
        for (SVGDrawingPanel svgp : svgs) {
            svgp.renderStep(0.0f);
        }
    }

    private File showOpenFile(String title, File lastFile){
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        if(lastFile != null)
            fc.setSelectedFile(lastFile);

        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;

                String extension = Utils.getExtension(f);
                return extension != null && extension.equals("svg");
            }

            @Override
            public String getDescription() {
                return "SVG images";
            }

        });

        int returnVal = fc.showOpenDialog(null);
        File file = null;
        if(returnVal == JFileChooser.APPROVE_OPTION)
            file = fc.getSelectedFile();

        return file;
    }

    private Color showColorChooser(String title, Color current){
        return JColorChooser.showDialog(mainPanel, title, current); //TODO simplify chooser
    }

    private String showInputDialog(String title, String defaultText){
        JTextArea msg = new JTextArea(defaultText);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        JScrollPane scrollPane = new JBScrollPane(msg);
        scrollPane.setPreferredSize(new Dimension(600, 250));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        int ris = JOptionPane.showConfirmDialog(null, scrollPane, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(ris == JOptionPane.OK_OPTION)
            return msg.getText();
        else
            return defaultText;
    }

    private void reloadMorphing(boolean recalculateMorphing){
        try{
            svgMorphing.stopAnimation();

            String strokeColorToApply = checkStrokeColor.isSelected() ? currentSvgStrokeColor : SVGDrawingPanel.TRANSPARENT_COLOR;
            String fillColorToApply =  checkFillColor.isSelected() ? currentSvgFillColor : SVGDrawingPanel.TRANSPARENT_COLOR;
            svgMorphing.setStrokeColor(strokeColorToApply);
            svgMorphing.setFillColor(fillColorToApply);
            svgMorphing.setStrokeSize(strokeSize);

            if(recalculateMorphing){
                result = VectAlign.align(svgFrom.getPath(), svgTo.getPath(), currentAlignMode);
                svgFromOutput.setText(result[0]);
                svgToOutput.setText(result[1]);
                svgMorphing.setPaths(result[0], result[1]);
                svgMorphing.reset();
                updateMorphingControls();
                recalculateMorphing = false;
            }else
                svgMorphing.redraw();

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void reloadSvgWithProperties(){
        String strokeColorToApply =  checkStrokeColor.isSelected() ? currentSvgStrokeColor : SVGDrawingPanel.TRANSPARENT_COLOR;
        String fillColorToApply =  checkFillColor.isSelected() ? currentSvgFillColor : SVGDrawingPanel.TRANSPARENT_COLOR;
        svgFrom.setStrokeColor(strokeColorToApply);
        svgTo.setStrokeColor(strokeColorToApply);
        svgFrom.setFillColor(fillColorToApply);
        svgTo.setFillColor(fillColorToApply);
        svgFrom.setStrokeSize(strokeSize);
        svgTo.setStrokeSize(strokeSize);
        svgFrom.redraw();
        svgTo.redraw();
    }

    private void initComponents() {
        ToolTipManager.sharedInstance().setInitialDelay(400);

        //Title
        setTitle(Main.NAME + " " + Main.VERSION);

        //Input SVG
        panelInput = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel panelFrom = new JPanel(new BorderLayout());
        JPanel panelTo = new JPanel(new BorderLayout());
        svgFrom = new SVGDrawingPanel();
        svgFrom.setPreferredSize(new Dimension(190, 200));
        svgTo = new SVGDrawingPanel();
        svgTo.setPreferredSize(new Dimension(190, 200));

        btnEditFrom = new JButton("Edit Path");
        btnEditTo = new JButton("Edit Path");
        btnSvgFrom = new JButton("Load SVG");
        btnSvgTo = new JButton("Load SVG");

        JPanel panelBtnFrom = new JPanel();
        JPanel panelBtnTo = new JPanel();
        panelBtnFrom.setLayout(new GridLayout(1, 2));
        panelBtnTo.setLayout(new GridLayout(1, 2));
        panelBtnFrom.add(btnEditFrom);
        panelBtnFrom.add(btnSvgFrom);
        panelBtnTo.add(btnEditTo);
        panelBtnTo.add(btnSvgTo);

        panelFrom.add(svgFrom, BorderLayout.CENTER);
        panelFrom.add(panelBtnFrom, BorderLayout.SOUTH);
        panelFrom.setBorder(getCommonBorder("Starting SVG/VD", true));

        panelTo.add(svgTo, BorderLayout.CENTER);
        panelTo.add(panelBtnTo, BorderLayout.SOUTH);
        panelTo.setBorder(getCommonBorder("Ending SVG/VD", true));

        //Controls
        panelControls = new JPanel(new BorderLayout(hgap, vgap));
        panelControls.setPreferredSize(new Dimension(400, 200));
        panelControls.setBorder(getCommonBorder("Configure morphing", true));
        panelControls.setLayout(new GridLayout(1, 2));
        JPanel panelTec = new JPanel();
        panelTec.setBorder(getCommonBorder("Aligment strategy", false));
        panelTec.setLayout(new GridLayout(5, 1));
        ButtonGroup btngrop = new ButtonGroup();
        radioStrategyBase = new JRadioButton("Base");
        radioStrategyBase.setSelected(true);
        radioStrategyLinear = new JRadioButton("Linear");
        radioStrategySubBase = new JRadioButton("Subalign Base");
        radioStrategySubLinear = new JRadioButton("Subalign Linear");
        btngrop.add(radioStrategyBase);
        btngrop.add(radioStrategyLinear);
        btngrop.add(radioStrategySubBase);
        btngrop.add(radioStrategySubLinear);
        panelTec.add(radioStrategyBase);
        panelTec.add(radioStrategyLinear);
        panelTec.add(radioStrategySubBase);
        panelTec.add(radioStrategySubLinear);

        JPanel panelPreviewOpt = new JPanel();
        panelPreviewOpt.setBorder(getCommonBorder("Preview options", false));
        panelPreviewOpt.setLayout(new GridLayout(5, 1));
        checkStrokeColor = new JCheckBox("Stroke (color)", true);
        checkFillColor = new JCheckBox("Fill (color)", false);
        panelStrokeColor = new JPanel();
        panelStrokeColor.setBackground(getCurrentStrokeColor());
        panelStrokeColor.setPreferredSize(new Dimension(30, 20));
        panelStrokeColor.setBorder(new WindowsBorders.DashedBorder(Color.darkGray));
        JPanel innerPanel1 = new JPanel(new FlowLayout());
        innerPanel1.add(panelStrokeColor);
        JPanel panelStroke = new JPanel(new BorderLayout());
        panelStroke.add(checkStrokeColor, BorderLayout.CENTER);
        panelStroke.add(innerPanel1, BorderLayout.EAST);
        panelFillColor = new JPanel();
        panelFillColor.setBackground(getCurrentFillColor());
        panelFillColor.setPreferredSize(new Dimension(30, 20));
        panelFillColor.setBorder(new WindowsBorders.DashedBorder(Color.darkGray));
        JPanel innerPanel2 = new JPanel(new FlowLayout());
        innerPanel2.add(panelFillColor);
        JPanel panelFill = new JPanel(new BorderLayout());
        panelFill.add(checkFillColor, BorderLayout.CENTER);
        panelFill.add(innerPanel2, BorderLayout.EAST);

        JPanel panelStrokeSize = new JPanel(new BorderLayout());
        panelStrokeSize.add(new JLabel("Stroke size: "), BorderLayout.WEST);
        SpinnerModel spinnerModel =
                new SpinnerNumberModel(2, //initial value
                        0, //min
                        100, //max
                        0.1);//step
        spinnerStroke = new JSpinner(spinnerModel);
        spinnerStroke.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                try {
                    double newValue = (double) ((JSpinner) e.getSource()).getValue();
                    strokeSize = (float) newValue;
                    reloadSvgWithProperties();
                    reloadMorphing(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        panelStrokeSize.add(spinnerStroke, BorderLayout.CENTER);

        panelPreviewOpt.add(panelFill);
        panelPreviewOpt.add(panelStroke);
        panelPreviewOpt.add(panelStrokeSize);


        panelControls.add(panelTec);
        panelControls.add(panelPreviewOpt);

        panelInput.add(panelFrom);
        panelInput.add(panelTo);

        panelConfig = new JPanel(new GridLayout(2, 1, 0, 10));
        panelConfig.add(panelInput);
        panelConfig.add(panelControls);

        //Morphing
        svgMorphing = new SVGDrawingPanel();
        svgMorphing.setListener(this);
        svgMorphing.setPreferredSize(new Dimension(400, 400));
        sliderMorphing = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
        sliderMorphing.setPreferredSize(new Dimension(350, 25));
        btnMorphAnimation = new JButton(icnPlay);
        btnMorphAnimation.setPreferredSize(new Dimension(35, 35));
        btnMorphAnimation.setBorderPainted(false);
        btnMorphAnimation.setBorder(null);
        btnMorphAnimation.setMargin(new Insets(0, 0, 0, 0));
        btnMorphAnimation.setContentAreaFilled(false);

        JPanel bottomMorphing = new JPanel(new FlowLayout());
        bottomMorphing.add(btnMorphAnimation);
        bottomMorphing.add(sliderMorphing);
        panelMorphing = new JPanel(new BorderLayout(hgap, vgap));
        panelMorphing.add(svgMorphing, BorderLayout.CENTER);
        panelMorphing.add(bottomMorphing, BorderLayout.SOUTH);
        panelMorphing.setBorder(getCommonBorder("AnimatedVectorDrawable (preview)", true));

        //Output
        panelOutput = new JPanel();
        panelOutput.setBorder(getCommonBorder("Results (aligned path sequences)", true));
        panelOutput.setLayout(new GridBagLayout());
        GridBagConstraints gc1 = new GridBagConstraints();
        gc1.fill = GridBagConstraints.BOTH;
        gc1.gridx = 0;
        gc1.gridy = 0;
        gc1.weightx = 0.975f;
        gc1.weighty = 0.5f;
        GridBagConstraints gcBtn1 = new GridBagConstraints();
        gcBtn1.fill = GridBagConstraints.BOTH;
        gcBtn1.gridx = 1;
        gcBtn1.gridy = 0;
        gcBtn1.weightx = 0.025f;
        gcBtn1.weighty = 0.5f;
        GridBagConstraints gc2 = new GridBagConstraints();
        gc2.fill = GridBagConstraints.BOTH;
        gc2.gridx = 0;
        gc2.gridy = 1;
        gc2.weightx = 0.975f;
        gc2.weighty = 0.5f;
        GridBagConstraints gcBtn2 = new GridBagConstraints();
        gcBtn2.fill = GridBagConstraints.BOTH;
        gcBtn2.gridx = 1;
        gcBtn2.gridy = 1;
        gcBtn2.weightx = 0.025f;
        gcBtn2.weighty = 0.5f;
        GridBagConstraints export = new GridBagConstraints();
        export.fill = GridBagConstraints.BOTH;
        export.gridheight = 2;
        //export.weightx = 0.025f;
        //export.weighty = 1.0f;

        svgFromOutput = new JTextArea();
        svgFromOutput.setEditable(false);
        svgToOutput = new JTextArea();
        svgToOutput.setEditable(false);
        JScrollPane scroll1 = new JScrollPane(svgFromOutput);
        scroll1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        JScrollPane scroll2 = new JScrollPane(svgToOutput);
        scroll2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        btnCopyFrom = new JButton(icnCopy);
        btnCopyFrom.setToolTipText("Copy aligned STARTING path to clipboard");
        btnCopyTo = new JButton(icnCopy);
        btnCopyTo.setToolTipText("Copy aligned ENDING path to clipboard");
        btnExport = new JButton("Export", icnExport);
        btnExport.setToolTipText("Export Android XML files");
        panelOutput.add(scroll1, gc1);
        panelOutput.add(btnCopyFrom, gcBtn1);
        panelOutput.add(scroll2, gc2);
        panelOutput.add(btnCopyTo, gcBtn2);
        panelOutput.add(btnExport, export);

        mainLayout = new BorderLayout(hgap, vgap);
        mainPanel.setLayout(mainLayout);
        getContentPane().add(panelConfig, BorderLayout.WEST);
        getContentPane().add(panelMorphing, BorderLayout.EAST);
        getContentPane().add(panelOutput, BorderLayout.SOUTH);
        mainPanel.setPreferredSize(new Dimension(900, 650));
        mainPanel.setBackground(Color.white);

        svgs = new SVGDrawingPanel[]{svgFrom, svgTo, svgMorphing};

        for (SVGDrawingPanel svgp : svgs) {
            svgp.setBackground(svgPanelBackgroundColor);
        }
    }

    private Color getCurrentStrokeColor(){
        return (SVGDrawingPanel.TRANSPARENT_COLOR.equals(currentSvgStrokeColor) ? Color.getColor("#00FFFFFF") : Color.decode(currentSvgStrokeColor));
    }

    private Color getCurrentFillColor(){
        return (SVGDrawingPanel.TRANSPARENT_COLOR.equals(currentSvgFillColor) ? Color.getColor("#00FFFFFF") : Color.decode(currentSvgFillColor));
    }


    private String getHexColor(Color color){
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Border getCommonBorder(String title, boolean borderLine){
        return new TitledBorder((borderLine ? new TitledBorder("").getBorder() : new EmptyBorder(0,0,0,0)), title, TitledBorder.LEFT, TitledBorder.ABOVE_TOP, mainPanel.getFont(), Color.DARK_GRAY);
    }

    private void handleSVGLoad(File f, SVGDrawingPanel svg){
        if(SVGParser.isSVGImage(f)) {
            svg.setPath(SVGParser.getPathDataFromSVGFile(f));
            svg.redraw();
            reloadMorphing(true);
        } else
            System.out.println("Error: not a valid SVG");
    }

    private void addListeners() {

        ActionListener colorAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadMorphing(false);
                reloadSvgWithProperties();
            }
        };

        checkStrokeColor.addActionListener(colorAction);
        checkFillColor.addActionListener(colorAction);

        panelFillColor.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                panelFillColor.setBackground(showColorChooser("Select fill color", getCurrentFillColor()));
                currentSvgFillColor = getHexColor(panelFillColor.getBackground());
                reloadSvgWithProperties();
                reloadMorphing(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        panelStrokeColor.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                panelStrokeColor.setBackground(showColorChooser("Select stroke color", getCurrentStrokeColor()));
                currentSvgStrokeColor = getHexColor(panelStrokeColor.getBackground());
                reloadSvgWithProperties();
                reloadMorphing(false);
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });

        radioStrategyBase.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentAlignMode = VectAlign.Mode.BASE;
                reloadMorphing(true);
            }
        });

        radioStrategyLinear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentAlignMode = VectAlign.Mode.LINEAR;
                reloadMorphing(true);
            }
        });

        radioStrategySubBase.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentAlignMode = VectAlign.Mode.SUB_BASE;
                reloadMorphing(true);
            }
        });

        radioStrategySubLinear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentAlignMode = VectAlign.Mode.SUB_LINEAR;
                reloadMorphing(true);
            }
        });

        btnCopyFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.copyToClipboard(svgFromOutput.getText());
                svgFromOutput.select(0, svgFromOutput.getText().length());
            }
        });

        btnCopyTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.copyToClipboard(svgToOutput.getText());
                svgToOutput.select(0, svgToOutput.getText().length());
            }
        });

        btnExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VectAlignExportDialog exportPanel = new VectAlignExportDialog(result, currentSvgStrokeColor, currentSvgFillColor,
                        (int)Math.max(1.0f, Math.round(strokeSize)), checkStrokeColor.isSelected(), checkFillColor.isSelected(),
                        svgMorphing.getSVGViewBoxWidth(), svgMorphing.getSVGViewBoxHeight());
                exportPanel.setModal(true);
                exportPanel.setVisible(true);
            }
        });

        sliderMorphing.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(sliderMorphing.isEnabled()){
                    svgMorphing.renderStep(((float)sliderMorphing.getValue())/1000.f);
                }
            }
        });

        btnMorphAnimation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleToggleAnimation();
            }
        });

        btnSvgFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileFrom = showOpenFile("Load SVG", fileFrom);
                handleSVGLoad(fileFrom, svgFrom);
            }
        });

        btnSvgTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileTo = showOpenFile("Load SVG", fileTo);
                handleSVGLoad(fileTo, svgTo);
            }
        });

        btnEditFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                svgFrom.setPath(showInputDialog("Edit STARTING path", svgFrom.getPath()));
                svgFrom.redraw();
                reloadMorphing(true);
            }
        });

        btnEditTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                svgTo.setPath(showInputDialog("Edit ENDING path", svgTo.getPath()));
                svgTo.redraw();
                reloadMorphing(true);
            }
        });

        svgMorphing.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleToggleAnimation();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    private void handleToggleAnimation(){
        svgMorphing.toggleAnimation();
        updateMorphingControls();
    }

    private void updateMorphingControls(){
        if(svgMorphing.isAnimating()){
            btnMorphAnimation.setIcon(icnPause);
            sliderMorphing.setValue((int) (svgMorphing.getCurrentStep() * sliderMorphing.getMaximum()));
            sliderMorphing.setEnabled(false);
        }
        else{
            btnMorphAnimation.setIcon(icnPlay);
            sliderMorphing.setValue((int) (svgMorphing.getCurrentStep() * sliderMorphing.getMaximum()));
            sliderMorphing.setEnabled(true);
        }
    }

    @Override
    public void onMorphingChanges(float step) {
        updateMorphingControls();
    }
}
