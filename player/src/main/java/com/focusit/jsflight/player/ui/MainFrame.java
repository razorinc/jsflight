package com.focusit.jsflight.player.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.jsflight.player.input.Events;
import com.focusit.jsflight.player.input.FileInput;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JToolBar;
import java.awt.FlowLayout;
import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MainFrame
{

    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);
    private List<JSONObject> events = new ArrayList<>();
    private int position = 0;
    private WebDriver driver;
    private AbstractTableModel model;
    private List<Boolean> checks;
    private Events rawevents;

    private JFrame frmJsflightrecorderPlayer;
    private JTextField filenameField;
    private JTable table;
    private JTextArea contentPane;
    private String lastUrl = "";
    private JTextArea eventContent;
    private JLabel statisticsLabel;
    private JTextField proxyHost;
    private JTextField proxyPort;
    private JTextField ffPath;
    private JTextField textField_1;
    private JTextField maxStepDelayField;

    // I will use it, probably, another day
    // private Pattern urlPattern = Pattern.compile(
    // "^((http[s]?|ftp):\\/)?\\/?([^:\\/\\s]+)((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)(.*)?(#[\\w\\-]+)?$");

    /**
     * Create the application.
     */
    public MainFrame()
    {
        initialize();
    }

    public JFrame getFrame()
    {
        return frmJsflightrecorderPlayer;
    }

    protected void playTheScenario()
    {
        long begin = System.currentTimeMillis();

        log.info("playing the scenario");

        while (position < events.size())
        {
            if (position > 0)
            {
                JSONObject event = events.get(position - 1);
                long prev = event.getBigDecimal("timestamp").longValue();
                event = events.get(position);
                long now = event.getBigDecimal("timestamp").longValue();

                if ((now - prev) > 0)
                {
                    try
                    {
                        log.info("Emulate wait " + (now - prev) + "ms + 2000ms");
                        Thread.sleep(now - prev/* + 2000 */);
                    }
                    catch (InterruptedException e)
                    {
                        log.error("interrupted", e);
                    }
                }
                log.info("Step " + position);
            }
            else
            {
                log.info("Step 0");
            }
            applyStep(position);
            checks.set(position, true);
            position++;
            if (position == events.size())
            {
                for (int i = 0; i < position; i++)
                {
                    checks.set(i, false);
                }
            }
        }
        log.info(String.format("Done(%d):playing", System.currentTimeMillis() - begin));
        position--;
        model.fireTableDataChanged();
    }

    private void applyStep(int position)
    {
        JSONObject event = events.get(position);
        if (event.getString("type").equalsIgnoreCase("xhr"))
        {
            return;
        }
        String event_url = event.getString("url");

        if (!lastUrl.equalsIgnoreCase(event_url))
        {
            lastUrl = event_url;

            if (!driver.getCurrentUrl().equalsIgnoreCase(lastUrl))
            {
                driver.get(lastUrl);
            }
        }

        if (!event.has("target") || event.get("target") == null || event.get("target") == JSONObject.NULL)
        {
            return;
        }
        String eventType = event.getString("type");

        if (!eventType.equalsIgnoreCase("mousedown") && !eventType.equalsIgnoreCase("keypress")
                && !eventType.equalsIgnoreCase("keyup"))
        {
            return;
        }

        WebElement element = driver.findElement(By.xpath(event.getString("target")));
        ((JavascriptExecutor)driver).executeScript("window.focus();");
        if (eventType.equalsIgnoreCase("mousedown"))
        {
            if (event.getInt("button") == 2)
            {
                new Actions(driver).contextClick(element).perform();
            }
            else
            {
                element.click();
            }
        }

        if (eventType.equalsIgnoreCase("keypress"))
        {
            if (event.has("charCode"))
            {
                char ch = (char)event.getBigInteger(("charCode")).intValue();
                char keys[] = new char[1];
                keys[0] = ch;
                element.sendKeys(new String(keys));
            }
        }

        if (eventType.equalsIgnoreCase("keyup"))
        {
            if (event.has("charCode"))
            {
                int code = event.getBigInteger(("charCode")).intValue();

                if (event.getBoolean("ctrlKey") == true)
                {
                    element.sendKeys(Keys.chord(Keys.CONTROL, new String(new byte[] { (byte)code })));
                }
                else
                {
                    switch (code)
                    {
                    case 8:
                        element.sendKeys(Keys.BACK_SPACE);
                        break;
                    case 27:
                        element.sendKeys(Keys.ESCAPE);
                        break;
                    case 127:
                        element.sendKeys(Keys.DELETE);
                        break;
                    case 13:
                        element.sendKeys(Keys.ENTER);
                        break;
                    case 37:
                        element.sendKeys(Keys.ARROW_LEFT);
                        break;
                    case 38:
                        element.sendKeys(Keys.ARROW_UP);
                        break;
                    case 39:
                        element.sendKeys(Keys.ARROW_RIGHT);
                        break;
                    case 40:
                        element.sendKeys(Keys.ARROW_DOWN);
                        break;
                    }
                }
            }
        }
    }

    private void checkElement(int position)
    {
        JSONObject event = events.get(position);
        if (!event.has("target") || event.getString("type").equalsIgnoreCase("xhr"))
        {
            return;
        }
        driver.findElement(By.xpath(event.getString("target")));
        checks.set(position, true);
        model.fireTableDataChanged();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        frmJsflightrecorderPlayer = new JFrame();
        frmJsflightrecorderPlayer.setTitle("JSFlightRecorder Player");
        frmJsflightrecorderPlayer.setBounds(100, 100, 938, 635);
        frmJsflightrecorderPlayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        frmJsflightrecorderPlayer.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        tabbedPane.addTab("Input", null, inputPanel, null);
        GridBagLayout gbl_inputPanel = new GridBagLayout();
        gbl_inputPanel.columnWidths = new int[] { 0, 0, 0 };
        gbl_inputPanel.rowHeights = new int[] { 0, 0, 0, 0 };
        gbl_inputPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_inputPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
        inputPanel.setLayout(gbl_inputPanel);

        JLabel lblFilename = new JLabel("Filename");
        GridBagConstraints gbc_lblFilename = new GridBagConstraints();
        gbc_lblFilename.insets = new Insets(0, 0, 5, 5);
        gbc_lblFilename.anchor = GridBagConstraints.NORTHEAST;
        gbc_lblFilename.gridx = 0;
        gbc_lblFilename.gridy = 0;
        inputPanel.add(lblFilename, gbc_lblFilename);

        filenameField = new JTextField();
        GridBagConstraints gbc_filenameField = new GridBagConstraints();
        gbc_filenameField.insets = new Insets(0, 0, 5, 0);
        gbc_filenameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_filenameField.gridx = 1;
        gbc_filenameField.gridy = 0;
        inputPanel.add(filenameField, gbc_filenameField);
        filenameField.setColumns(10);
        filenameField.setText("/tmp/1.json");
        filenameField.setText("/tmp/2.json");
        filenameField.setText("/tmp/3.json");

        JButton btnLoad = new JButton("Load");
        btnLoad.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                String filename = filenameField.getText().trim();
                if (filename.length() == 0)
                {
                    return;
                }
                try
                {
                    String data = FileInput.getContentInString(filename);
                    rawevents = new Events();
                    rawevents.parse(FileInput.getContent(filename));
                    contentPane.setText(data);
                }
                catch (IOException e1)
                {
                    log.error(e1.toString(), e1);
                }
            }
        });
        GridBagConstraints gbc_btnLoad = new GridBagConstraints();
        gbc_btnLoad.insets = new Insets(0, 0, 5, 5);
        gbc_btnLoad.gridx = 0;
        gbc_btnLoad.gridy = 1;
        inputPanel.add(btnLoad, gbc_btnLoad);

        JButton btnBrowse = new JButton("Browse");
        btnBrowse.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    filenameField.setText(selectedFile);
                }
            }
        });
        GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
        gbc_btnBrowse.insets = new Insets(0, 0, 5, 0);
        gbc_btnBrowse.gridx = 1;
        gbc_btnBrowse.gridy = 1;
        inputPanel.add(btnBrowse, gbc_btnBrowse);

        JLabel lblContent = new JLabel("Content");
        GridBagConstraints gbc_lblContent = new GridBagConstraints();
        gbc_lblContent.anchor = GridBagConstraints.NORTH;
        gbc_lblContent.insets = new Insets(0, 0, 0, 5);
        gbc_lblContent.gridx = 0;
        gbc_lblContent.gridy = 2;
        inputPanel.add(lblContent, gbc_lblContent);

        JScrollPane scrollPane_1 = new JScrollPane();
        GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
        gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_1.gridx = 1;
        gbc_scrollPane_1.gridy = 2;
        inputPanel.add(scrollPane_1, gbc_scrollPane_1);

        contentPane = new JTextArea();
        scrollPane_1.setViewportView(contentPane);
        contentPane.setWrapStyleWord(true);
        contentPane.setLineWrap(true);
        contentPane.setEditable(false);

        JPanel scenarioPanel = new JPanel();
        tabbedPane.addTab("Scenario", null, scenarioPanel, null);
        GridBagLayout gbl_scenarioPanel = new GridBagLayout();
        gbl_scenarioPanel.columnWidths = new int[] { 279, 0, 0, 0, 0, 0, 378, 0 };
        gbl_scenarioPanel.rowHeights = new int[] { 0, 449, 70, 0, 0 };
        gbl_scenarioPanel.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
        gbl_scenarioPanel.rowWeights = new double[] { 0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE };
        scenarioPanel.setLayout(gbl_scenarioPanel);
        
        JPanel panel_2 = new JPanel();
        GridBagConstraints gbc_panel_2 = new GridBagConstraints();
        gbc_panel_2.fill = GridBagConstraints.BOTH;
        gbc_panel_2.insets = new Insets(0, 0, 5, 5);
        gbc_panel_2.gridx = 0;
        gbc_panel_2.gridy = 0;
        scenarioPanel.add(panel_2, gbc_panel_2);
                panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
        
                JButton btnParse = new JButton("Parse");
                panel_2.add(btnParse);
                
                JButton btnSave_1 = new JButton("Save");
                panel_2.add(btnSave_1);
                btnParse.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        events = rawevents.getEvents();
                        checks = new ArrayList<>(events.size());
                        for (int i = 0; i < events.size(); i++)
                        {
                            checks.add(new Boolean(false));
                        }

                        Collections.sort(events, new Comparator<JSONObject>()
                        {
                            @Override
                            public int compare(JSONObject o1, JSONObject o2)
                            {
                                return ((Long)o1.getLong("timestamp")).compareTo(o2.getLong("timestamp"));
                            }
                        });

                        long secs = events.get(events.size() - 1).getBigDecimal("timestamp").longValue()
                                - events.get(0).getBigDecimal("timestamp").longValue();
                        statisticsLabel.setText(String.format("Events %d, duration %f sec", events.size(), secs / 1000.0));
                        model = new AbstractTableModel()
                        {

                            private static final long serialVersionUID = 1L;

                            private String[] columns = { "#", "*", "tab", "type", "url", "char", "button", "target",
                                    "timestamp", "status" };

                            @Override
                            public int getColumnCount()
                            {
                                return 10;
                            }

                            @Override
                            public String getColumnName(int column)
                            {
                                return columns[column];
                            }

                            @Override
                            public int getRowCount()
                            {
                                return events.size();
                            }

                            @Override
                            public Object getValueAt(int rowIndex, int columnIndex)
                            {
                                if (rowIndex == position && columnIndex == 1)
                                {
                                    return "*";
                                }
                                if (columnIndex == 9)
                                {
                                    return checks.get(rowIndex);
                                }

                                JSONObject event = events.get(rowIndex);

                                switch (columnIndex)
                                {
                                case 0:
                                    return rowIndex;
                                case 2:
                                    return event.get("tabuuid");
                                case 3:
                                    return event.get("type");
                                case 4:
                                    return event.get("url");
                                case 5:
                                {
                                    if (!event.has("charCode"))
                                    {
                                        return null;
                                    }
                                    int code = event.getInt("charCode");
                                    char[] key = new char[1];
                                    key[0] = (char)code;
                                    return String.format("%d ( %s )", code, new String(key));
                                }
                                case 6:
                                    return event.has("button") ? event.get("button") : null;
                                case 7:
                                    return event.get("target");
                                case 8:
                                    return new Date(event.getBigDecimal("timestamp").longValue());
                                }
                                return null;
                            }
                        };

                        table.setModel(model);
                    }
                });

        JButton btnOpenBrowser = new JButton("Open browser");
        btnOpenBrowser.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                FirefoxProfile profile = new FirefoxProfile();
                DesiredCapabilities cap = new DesiredCapabilities();
                if (proxyHost.getText().trim().length() > 0)
                {
                    String host = proxyHost.getText();
                    if (proxyPort.getText().trim().length() > 0)
                    {
                        host += ":" + proxyPort.getText();
                    }
                    org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
                    proxy.setHttpProxy(host).setFtpProxy(host).setSslProxy(host);
                    cap.setCapability(CapabilityType.PROXY, proxy);
                }
                if (ffPath.getText() != null && ffPath.getText().trim().length() > 0)
                {
                    FirefoxBinary binary = new FirefoxBinary(new File(ffPath.getText()));
                    driver = new FirefoxDriver(binary, profile, cap);
                }
                else
                {
                    driver = new FirefoxDriver(new FirefoxBinary(), profile, cap);
                }
            }
        });
        GridBagConstraints gbc_btnOpenBrowser = new GridBagConstraints();
        gbc_btnOpenBrowser.insets = new Insets(0, 0, 5, 5);
        gbc_btnOpenBrowser.gridx = 1;
        gbc_btnOpenBrowser.gridy = 0;
        scenarioPanel.add(btnOpenBrowser, gbc_btnOpenBrowser);

        JButton btnCloseBrowser = new JButton("Close browser");
        btnCloseBrowser.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (driver != null)
                {
                    driver.close();
                }
            }
        });
        GridBagConstraints gbc_btnCloseBrowser = new GridBagConstraints();
        gbc_btnCloseBrowser.insets = new Insets(0, 0, 5, 5);
        gbc_btnCloseBrowser.gridx = 2;
        gbc_btnCloseBrowser.gridy = 0;
        scenarioPanel.add(btnCloseBrowser, gbc_btnCloseBrowser);

        JButton btnRewind = new JButton("Rewind");
        btnRewind.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                for (int i = 0; i < events.size(); i++)
                {
                    checks.set(i, false);
                }
                position = 0;
                model.fireTableDataChanged();
            }
        });
        GridBagConstraints gbc_btnRewind = new GridBagConstraints();
        gbc_btnRewind.insets = new Insets(0, 0, 5, 5);
        gbc_btnRewind.gridx = 3;
        gbc_btnRewind.gridy = 0;
        scenarioPanel.add(btnRewind, gbc_btnRewind);

        JButton btnNext = new JButton("Next");
        btnNext.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                applyStep(position);
                checks.set(position, true);
                position++;
                if (position == events.size())
                {
                    for (int i = 0; i < position; i++)
                    {
                        checks.set(i, false);
                    }
                    position = 0;
                }
                model.fireTableDataChanged();
            }
        });
        GridBagConstraints gbc_btnNext = new GridBagConstraints();
        gbc_btnNext.insets = new Insets(0, 0, 5, 5);
        gbc_btnNext.gridx = 4;
        gbc_btnNext.gridy = 0;
        scenarioPanel.add(btnNext, gbc_btnNext);

        JButton btnSkip = new JButton("Skip");
        btnSkip.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                position++;
                model.fireTableDataChanged();
            }
        });
        GridBagConstraints gbc_btnSkip = new GridBagConstraints();
        gbc_btnSkip.insets = new Insets(0, 0, 5, 5);
        gbc_btnSkip.gridx = 5;
        gbc_btnSkip.gridy = 0;
        scenarioPanel.add(btnSkip, gbc_btnSkip);

        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 6;
        gbc_panel.gridy = 0;
        scenarioPanel.add(panel, gbc_panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JButton btnCheck = new JButton("Check");
        panel.add(btnCheck);
        btnCheck.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                checkElement(table.getSelectedRow());
            }
        });

        JButton btnPlay = new JButton("Play");
        btnPlay.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                playTheScenario();
            }
        });
        panel.add(btnPlay);

        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridwidth = 7;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        scenarioPanel.add(scrollPane, gbc_scrollPane);

        table = new JTable();
        scrollPane.setViewportView(table);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                int index = table.getSelectedRow();
                if (index >= 0)
                {
                    eventContent.setText(events.get(index).toString());
                }
            }
        });

        JScrollPane scrollPane_2 = new JScrollPane();
        GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
        gbc_scrollPane_2.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_2.gridwidth = 7;
        gbc_scrollPane_2.gridx = 0;
        gbc_scrollPane_2.gridy = 2;
        scenarioPanel.add(scrollPane_2, gbc_scrollPane_2);

        eventContent = new JTextArea();
        scrollPane_2.setViewportView(eventContent);
        eventContent.setLineWrap(true);
        eventContent.setEditable(false);
        eventContent.setWrapStyleWord(true);

        JPanel panel_1 = new JPanel();
        GridBagConstraints gbc_panel_1 = new GridBagConstraints();
        gbc_panel_1.anchor = GridBagConstraints.SOUTH;
        gbc_panel_1.fill = GridBagConstraints.HORIZONTAL;
        gbc_panel_1.gridx = 6;
        gbc_panel_1.gridy = 3;
        scenarioPanel.add(panel_1, gbc_panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        JLabel lblEvents = new JLabel("Statistics");
        panel_1.add(lblEvents);

        statisticsLabel = new JLabel("DATA");
        panel_1.add(statisticsLabel);
        
        JPanel postProcessPanel = new JPanel();
        tabbedPane.addTab("Post process", null, postProcessPanel, null);
        postProcessPanel.setLayout(new BorderLayout(0, 0));
        
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        postProcessPanel.add(toolBar, BorderLayout.NORTH);
        
        JLabel lblScript = new JLabel("Script");
        toolBar.add(lblScript);
        
        textField_1 = new JTextField();
        toolBar.add(textField_1);
        textField_1.setColumns(15);
        
        JButton btnBrowse_1 = new JButton("Browse");
        toolBar.add(btnBrowse_1);
        
        JButton btnLoad_1 = new JButton("Load");
        btnLoad_1.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        	}
        });
        toolBar.add(btnLoad_1);
        
        JButton btnSave = new JButton("Save");
        toolBar.add(btnSave);
        
        JButton btnReset = new JButton("Reset");
        toolBar.add(btnReset);
        
        JButton btnRun = new JButton("Run");
        toolBar.add(btnRun);
        
        RTextScrollPane scrollPane_3 = new RTextScrollPane();
        postProcessPanel.add(scrollPane_3, BorderLayout.CENTER);
        
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        textArea.setCodeFoldingEnabled(true);
        scrollPane_3.setViewportView(textArea);
        
                JPanel optionsPanel = new JPanel();
                tabbedPane.addTab("Options", null, optionsPanel, null);
                optionsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                		FormSpecs.RELATED_GAP_COLSPEC,
                		FormSpecs.DEFAULT_COLSPEC,
                		FormSpecs.RELATED_GAP_COLSPEC,
                		ColumnSpec.decode("default:grow"),},
                	new RowSpec[] {
                		FormSpecs.RELATED_GAP_ROWSPEC,
                		FormSpecs.DEFAULT_ROWSPEC,
                		FormSpecs.RELATED_GAP_ROWSPEC,
                		FormSpecs.DEFAULT_ROWSPEC,
                		FormSpecs.RELATED_GAP_ROWSPEC,
                		FormSpecs.DEFAULT_ROWSPEC,
                		FormSpecs.RELATED_GAP_ROWSPEC,
                		FormSpecs.DEFAULT_ROWSPEC,
                		FormSpecs.RELATED_GAP_ROWSPEC,
                		FormSpecs.DEFAULT_ROWSPEC,}));
                
                        JLabel lblFirefoxProxyHost = new JLabel("Firefox proxy host");
                        optionsPanel.add(lblFirefoxProxyHost, "2, 2, right, default");
                        
                                proxyHost = new JTextField();
                                optionsPanel.add(proxyHost, "4, 2, fill, default");
                                proxyHost.setColumns(10);
                                
                                        JLabel lblFirefoxProxyPort = new JLabel("Firefox proxy port");
                                        optionsPanel.add(lblFirefoxProxyPort, "2, 4, right, default");
                                        
                                                proxyPort = new JTextField();
                                                optionsPanel.add(proxyPort, "4, 4, fill, default");
                                                proxyPort.setColumns(10);
                                                
                                                        JLabel lblFirefoxPath = new JLabel("Firefox path");
                                                        optionsPanel.add(lblFirefoxPath, "2, 6, right, default");
                                                        
                                                                ffPath = new JTextField();
                                                                optionsPanel.add(ffPath, "4, 6, fill, default");
                                                                ffPath.setColumns(10);
                                                                
                                                                JLabel lblMaxDelayBetween = new JLabel("Max delay between steps");
                                                                optionsPanel.add(lblMaxDelayBetween, "2, 8, right, default");
                                                                
                                                                maxStepDelayField = new JTextField();
                                                                optionsPanel.add(maxStepDelayField, "4, 8, fill, default");
                                                                maxStepDelayField.setColumns(10);
    }
}
