package org.wikipedia.ro.astroe;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.swing.ActionMap;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.astroe.generators.Generator;
import org.wikipedia.ro.astroe.generators.PageGenerator;
import org.wikipedia.ro.astroe.operations.Operation;
import org.wikipedia.ro.astroe.operations.WikiOperation;

public class WikipediaToolboxGUI {

    private static ResourceBundle bundle;
    private static Wiki sourceWiki = null;
    private static Wiki targetWiki = null;
    private static Wikibase dataWiki = new Wikibase("www.wikidata.org");
    private static Map<String, Component> dataComponentsMap = new HashMap<String, Component>();
    private static JFrame frame;

    public static void main(String[] args) {
        bundle = ResourceBundle.getBundle("uitexts.uitexts", new Locale("ro"));
        // TODO Auto-generated method stub
        frame = new JFrame();

        frame.setTitle(bundle.getString("frame.title"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension paneSize = new Dimension(650, 500);

        JPanel panel = new JPanel();
        frame.setContentPane(panel);
        BorderLayout mainLayout = new BorderLayout();
        mainLayout.setVgap(40);
        panel.setLayout(mainLayout);

        BorderLayout wikiConfigLayout = new BorderLayout();
        JPanel wikiConfigPanel = new JPanel(wikiConfigLayout);
        wikiConfigLayout.setHgap(10);
        JPanel sourceTargetConfigPanel = createSourceAndTargetWikiConfigPanel();
        wikiConfigPanel.add(sourceTargetConfigPanel, BorderLayout.WEST);
        JPanel loginConfigPanel = createLoginConfigPanel();
        wikiConfigPanel.add(loginConfigPanel, BorderLayout.EAST);
        panel.add(wikiConfigPanel, BorderLayout.NORTH);

        JPanel generatorConfigPanel = createGeneratorConfigPanel();
        panel.add(generatorConfigPanel, BorderLayout.CENTER);

        JPanel actionConfigPanel = createActionConfigPanel();
        panel.add(actionConfigPanel, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) (screenSize.getWidth() - 400) / 2, (int) (screenSize.getHeight() - 400) / 2);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel createActionConfigPanel() {
        BorderLayout actionConfigLayout = new BorderLayout();
        actionConfigLayout.setVgap(4);
        JPanel actionConfigPanel = new JPanel(actionConfigLayout);

        JPanel actionChoicePanel = new JPanel();
        GroupLayout actionChoiceLayout = new GroupLayout(actionChoicePanel);
        actionChoiceLayout.setAutoCreateGaps(true);
        actionChoicePanel.setLayout(actionChoiceLayout);
        JLabel actionLabel = new JLabel(bundle.getString("operation"));
        JComboBox<ChoiceElement> actionDropDown = new JComboBox<ChoiceElement>();
        dataComponentsMap.put("action", actionDropDown);
        JLabel summaryLabel = new JLabel(bundle.getString("edit.summary"));
        JTextField summaryTF = new JTextField();
        dataComponentsMap.put("summary", summaryTF);

        SequentialGroup vGroup = actionChoiceLayout.createSequentialGroup();
        vGroup.addGroup(actionChoiceLayout.createParallelGroup(Alignment.BASELINE).addComponent(actionLabel)
            .addComponent(actionDropDown));
        vGroup.addGroup(
            actionChoiceLayout.createParallelGroup(Alignment.BASELINE).addComponent(summaryLabel).addComponent(summaryTF));
        actionChoiceLayout.setVerticalGroup(vGroup);
        SequentialGroup hGroup = actionChoiceLayout.createSequentialGroup();
        hGroup.addGroup(actionChoiceLayout.createParallelGroup().addComponent(actionLabel).addComponent(summaryLabel));
        hGroup.addGroup(actionChoiceLayout.createParallelGroup().addComponent(actionDropDown).addComponent(summaryTF));
        actionChoiceLayout.setHorizontalGroup(hGroup);

        Reflections refl = new Reflections("org.wikipedia.ro.astroe.operations");
        Set<Class<?>> operationClasses = refl.getTypesAnnotatedWith(Operation.class);
        for (Class<?> eachOperationClass : operationClasses) {
            Operation annotation = eachOperationClass.getAnnotation(Operation.class);
            ChoiceElement el = new ChoiceElement();
            el.clazz = (Class<Generator>) eachOperationClass;
            el.label = bundle.getString(annotation.labelKey());
            actionDropDown.addItem(el);
        }
        actionConfigPanel.add(actionChoicePanel, BorderLayout.NORTH);

        JPanel configurationPanel = new JPanel();
        JButton goButton = new JButton(bundle.getString("go"));
        goButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                executeSelectedAction();
            }
        });
        configurationPanel.add(goButton);
        actionConfigPanel.add(configurationPanel, BorderLayout.SOUTH);
        return actionConfigPanel;
    }

    private static void executeSelectedAction() {
        if (null == targetWiki) {
            JOptionPane.showMessageDialog(frame, bundle.getString("error.not.logged.in"),
                bundle.getString("error.operation.cannot.run"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        final boolean markBot = ((JCheckBox) dataComponentsMap.get("bot")).isSelected();
        int throttle = 10;
        String throttleString = ((JTextField) dataComponentsMap.get("throttle")).getText();
        if (StringUtils.isNotBlank(throttleString)) {
            try {
                throttle = Integer.parseInt(throttleString);
            } catch (NumberFormatException e2) {
            }
        }
        Class<Generator> generatorClass =
            ((ChoiceElement<Generator>) ((JComboBox<ChoiceElement<Generator>>) dataComponentsMap.get("generator"))
                .getSelectedItem()).clazz;
        Class<WikiOperation> actionClass =
            ((ChoiceElement<WikiOperation>) ((JComboBox<ChoiceElement<WikiOperation>>) dataComponentsMap.get("action"))
                .getSelectedItem()).clazz;

        String srcwikilang = ((JTextField) dataComponentsMap.get("sourcewiki")).getText();
        if (StringUtils.isEmpty(srcwikilang)) {
            JOptionPane.showMessageDialog(frame, bundle.getString("error.srcwiki.not.specified"),
                bundle.getString("error.operation.cannot.run"), JOptionPane.ERROR_MESSAGE);
        }
        sourceWiki = new Wiki(StringUtils.removeEnd(srcwikilang, "wiki") + ".wikipedia.org");
        final String commitMessage = StringUtils.defaultIfBlank(((JTextField) dataComponentsMap.get("summary")).getText(),
            bundle.getString(actionClass.getAnnotation(Operation.class).labelKey()));

        String generatorParam = ((JTextField) dataComponentsMap
            .get(generatorClass.getAnnotation(PageGenerator.class).stringsConfigLabelKeys()[0])).getText();

        final JDialog pBarDialog = new JDialog(frame, bundle.getString("progress.generating"), true);
        JProgressBar generationProgressBar = new JProgressBar();
        generationProgressBar.setPreferredSize(new Dimension(400, 30));
        generationProgressBar.setStringPainted(true);
        generationProgressBar.setString(bundle.getString("progress.generating"));
        pBarDialog.add(generationProgressBar);

        GeneratorWorker genWorker = new GeneratorWorker(generatorClass, generatorParam, pBarDialog);
        genWorker.execute();
        pBarDialog.pack();
        pBarDialog.setLocationRelativeTo(frame);
        pBarDialog.setVisible(true);

        final JDialog actionDialog = new JDialog(frame, bundle.getString("progress.working"), true);
        List<String> articleList;
        try {
            articleList = genWorker.get();
        } catch (InterruptedException | ExecutionException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage(), bundle.getString("error.processing"),
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        // String actionParam =
        // ((JTextField) dataComponentsMap.get(actionClass.getAnnotation(Operation.class).labelKey())).getText();
        final JProgressBar actionProgressBar = new JProgressBar();
        actionProgressBar.setPreferredSize(new Dimension(400, 30));
        actionProgressBar.setStringPainted(true);
        actionProgressBar.setMinimum(0);
        actionProgressBar.setValue(0);
        actionProgressBar.setMaximum(articleList.size());
        actionDialog.add(actionProgressBar);
        actionDialog.pack();
        actionDialog.setLocationRelativeTo(frame);
        final long throttleCopy = throttle;

        ActionWorker actWorker = new ActionWorker(actionClass, articleList.toArray(new String[articleList.size()]),
            actionProgressBar, throttleCopy, commitMessage, markBot);
        actWorker.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    int progress = (Integer) evt.getNewValue();
                    actionProgressBar.setValue(progress);
                }
            }
        });
        actWorker.execute();
        actionDialog.setVisible(true);
    }

    private static JPanel createGeneratorConfigPanel() {
        BorderLayout generatorConfigLayout = new BorderLayout();
        generatorConfigLayout.setVgap(4);
        JPanel generatorConfigPanel = new JPanel(generatorConfigLayout);

        JPanel generatorChoicePanel = new JPanel();
        GroupLayout generatorChoiceLayout = new GroupLayout(generatorChoicePanel);
        generatorChoiceLayout.setAutoCreateGaps(true);
        generatorChoicePanel.setLayout(generatorChoiceLayout);
        JLabel generatorLabel = new JLabel(bundle.getString("generator"));
        JComboBox<ChoiceElement> generatorDropDown = new JComboBox<ChoiceElement>();
        dataComponentsMap.put("generator", generatorDropDown);
        SequentialGroup vGroup = generatorChoiceLayout.createSequentialGroup();
        vGroup.addGroup(generatorChoiceLayout.createParallelGroup(Alignment.BASELINE).addComponent(generatorLabel)
            .addComponent(generatorDropDown));
        generatorChoiceLayout.setVerticalGroup(vGroup);
        SequentialGroup hGroup = generatorChoiceLayout.createSequentialGroup();
        hGroup.addGroup(generatorChoiceLayout.createParallelGroup().addComponent(generatorLabel));
        hGroup.addGroup(generatorChoiceLayout.createParallelGroup().addComponent(generatorDropDown));
        generatorChoiceLayout.setHorizontalGroup(hGroup);

        final JPanel generatorOptionsPanel = new JPanel();
        generatorConfigPanel.add(generatorOptionsPanel, BorderLayout.SOUTH);
        generatorDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ChoiceElement<Generator> item = (ChoiceElement<Generator>) e.getItem();
                    PageGenerator annotation = item.clazz.getAnnotation(PageGenerator.class);
                    int noOfConfigs = annotation.stringsConfigNumber();
                    String[] configKeys = annotation.stringsConfigLabelKeys();
                    generatorOptionsPanel.removeAll();
                    GroupLayout generatorOptionsLayout = new GroupLayout(generatorOptionsPanel);
                    generatorOptionsLayout.setAutoCreateGaps(true);
                    generatorOptionsPanel.setLayout(generatorOptionsLayout);
                    SequentialGroup vGroup = generatorOptionsLayout.createSequentialGroup();
                    SequentialGroup hGroup = generatorOptionsLayout.createSequentialGroup();
                    ParallelGroup[] pvGroups = new ParallelGroup[] { generatorOptionsLayout.createParallelGroup(),
                        generatorOptionsLayout.createParallelGroup() };
                    for (int i = 0; i < noOfConfigs; i++) {
                        JLabel crtLabel = new JLabel(bundle.getString(configKeys[i]));
                        JTextField crtTF = new JTextField();
                        crtTF.setPreferredSize(new Dimension(200, 20));
                        dataComponentsMap.put(configKeys[i], crtTF);
                        pvGroups[0].addComponent(crtLabel);
                        pvGroups[1].addComponent(crtTF);
                        vGroup.addGroup(generatorOptionsLayout.createParallelGroup(Alignment.BASELINE).addComponent(crtLabel)
                            .addComponent(crtTF));
                    }
                    hGroup.addGroup(pvGroups[0]).addGroup(pvGroups[1]);
                    generatorOptionsLayout.setHorizontalGroup(hGroup);
                    generatorOptionsLayout.setVerticalGroup(vGroup);
                }
            }
        });

        Reflections refl = new Reflections("org.wikipedia.ro.astroe.generators");
        Set<Class<?>> generatorClasses = refl.getTypesAnnotatedWith(PageGenerator.class);
        for (Class<?> eachGeneratorClass : generatorClasses) {
            PageGenerator annotation = eachGeneratorClass.getAnnotation(PageGenerator.class);
            ChoiceElement el = new ChoiceElement();
            el.clazz = (Class<Generator>) eachGeneratorClass;
            el.label = bundle.getString(annotation.labelKey());
            generatorDropDown.addItem(el);
        }
        generatorConfigPanel.add(generatorChoicePanel, BorderLayout.NORTH);
        return generatorConfigPanel;
    }

    private static JPanel createSourceAndTargetWikiConfigPanel() {
        JPanel sourceTargetConfigPanel = new JPanel();
        GroupLayout sourceTargetConfigLayout = new GroupLayout(sourceTargetConfigPanel);
        JLabel sourceWikiLabel = new JLabel(bundle.getString("sourcewiki.label"));
        JLabel targetWikiLabel = new JLabel(bundle.getString("targetwiki.label"));
        JLabel throttleLabel = new JLabel(bundle.getString("throttle.label"));
        final JTextField sourceWikiTextField = new JTextField("en");
        sourceWikiTextField.setPreferredSize(new Dimension(50, 20));
        dataComponentsMap.put("sourcewiki", sourceWikiTextField);
        final JTextField targetWikiTextField = new JTextField("ro");
        targetWikiTextField.setPreferredSize(new Dimension(50, 20));
        dataComponentsMap.put("targetwiki", targetWikiTextField);
        final JTextField throttleTextField = new JTextField(String.valueOf(10));
        throttleTextField.setPreferredSize(new Dimension(50, 20));
        dataComponentsMap.put("throttle", throttleTextField);

        sourceTargetConfigPanel.setLayout(sourceTargetConfigLayout);
        sourceTargetConfigLayout.setAutoCreateGaps(true);
        SequentialGroup hGroup = sourceTargetConfigLayout.createSequentialGroup();
        hGroup.addGroup(sourceTargetConfigLayout.createParallelGroup().addComponent(sourceWikiLabel)
            .addComponent(targetWikiLabel).addComponent(throttleLabel));
        hGroup.addGroup(sourceTargetConfigLayout.createParallelGroup().addComponent(sourceWikiTextField)
            .addComponent(targetWikiTextField).addComponent(throttleTextField));
        sourceTargetConfigLayout.setHorizontalGroup(hGroup);
        SequentialGroup vGroup = sourceTargetConfigLayout.createSequentialGroup();
        vGroup.addGroup(sourceTargetConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(sourceWikiLabel)
            .addComponent(sourceWikiTextField));
        vGroup.addGroup(sourceTargetConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(targetWikiLabel)
            .addComponent(targetWikiTextField));
        vGroup.addGroup(sourceTargetConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(throttleLabel)
            .addComponent(throttleTextField));
        sourceTargetConfigLayout.setVerticalGroup(vGroup);
        return sourceTargetConfigPanel;
    }

    private static JPanel createLoginConfigPanel() {
        JPanel loginConfigPanel = new JPanel();
        GroupLayout loginConfigLayout = new GroupLayout(loginConfigPanel);
        JLabel usernameLabel = new JLabel(bundle.getString("username"));
        JLabel passwordLabel = new JLabel(bundle.getString("password"));
        JTextField usernameTextField = new JTextField();
        usernameTextField.setPreferredSize(new Dimension(150, 20));
        dataComponentsMap.put("username", usernameTextField);
        JTextField passwordTextField = new JPasswordField();
        passwordTextField.setPreferredSize(new Dimension(150, 20));
        dataComponentsMap.put("password", passwordTextField);
        JCheckBox botCB = new JCheckBox(bundle.getString("auth.bot"));
        dataComponentsMap.put("bot", botCB);
        JButton loginButton = new JButton(bundle.getString("login.button"));

        loginButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField unameTF = (JTextField) dataComponentsMap.get("username");
                JPasswordField pwdTF = (JPasswordField) dataComponentsMap.get("password");
                JTextField twTF = (JTextField) dataComponentsMap.get("targetwiki");

                String uname = unameTF.getText();
                char[] pwd = pwdTF.getPassword();
                String tw = StringUtils.appendIfMissing(twTF.getText(), "wiki");
                if (StringUtils.isNoneEmpty(uname, tw)) {
                    String twLang = StringUtils.removeEnd(tw, "wiki");
                    targetWiki = new Wiki(twLang + ".wikipedia.org");
                    try {
                        targetWiki.login(uname, pwd);
                        JOptionPane.showMessageDialog(frame, bundle.getString("auth.successful"));
                    } catch (FailedLoginException | IOException e1) {
                        JOptionPane.showMessageDialog(frame, e1.getMessage(), bundle.getString("error.autherror"),
                            JOptionPane.ERROR_MESSAGE);
                        e1.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, bundle.getString("error.specify.targetwiki.credentials"),
                        bundle.getString("error.autherror"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        loginConfigPanel.setLayout(loginConfigLayout);
        loginConfigLayout.setAutoCreateGaps(true);
        SequentialGroup loginHGroup = loginConfigLayout.createSequentialGroup();
        loginHGroup
            .addGroup(loginConfigLayout.createParallelGroup().addComponent(usernameLabel).addComponent(passwordLabel));
        loginHGroup.addGroup(loginConfigLayout.createParallelGroup().addComponent(usernameTextField)
            .addComponent(passwordTextField).addComponent(botCB).addComponent(loginButton));
        loginConfigLayout.setHorizontalGroup(loginHGroup);
        SequentialGroup loginVGroup = loginConfigLayout.createSequentialGroup();
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(usernameLabel)
            .addComponent(usernameTextField));
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(passwordLabel)
            .addComponent(passwordTextField));
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(botCB));
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup().addComponent(loginButton));
        loginConfigLayout.setVerticalGroup(loginVGroup);
        return loginConfigPanel;
    }

    private static class ChoiceElement<T> {
        public String label;
        public Class<T> clazz;

        @Override
        public String toString() {
            return label;
        }

    }

    private static class GeneratorWorker extends SwingWorker<List<String>, String> {

        private Class<Generator> generatorClass;
        private String generatorParam;
        private JDialog dialog;

        public GeneratorWorker(Class<Generator> generatorClass, String generatorParam, JDialog dialog) {
            super();
            this.generatorClass = generatorClass;
            this.generatorParam = generatorParam;
            this.dialog = dialog;
        }

        @Override
        protected List<String> doInBackground() throws Exception {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Constructor<Generator> generatorConstr = generatorClass.getConstructor(Wiki.class, String.class);
            Generator generator = generatorConstr.newInstance(targetWiki, generatorParam);

            return generator.getGeneratedTitles();
        }

        @Override
        protected void done() {
            frame.setCursor(null);
            dialog.setVisible(false);
        }

    }

    private static class ActionWorker extends SwingWorker<String, String> {

        private Class<WikiOperation> actionClass;
        private String[] actionParams;
        private JProgressBar pBar;
        private Thread operationWatcher = null;
        private boolean finished = false;
        private long throttle = 10000l;
        private String commitMessage;
        private boolean bot;
        private WikiOperation action;

        public ActionWorker(Class<WikiOperation> actionClass, String[] params, JProgressBar pBar, long throttle,
            String commitMessage, boolean bot) {
            super();
            this.actionClass = actionClass;
            this.actionParams = params;
            this.pBar = pBar;
            this.throttle = throttle;
            this.commitMessage = commitMessage;
        }

        @Override
        protected String doInBackground() throws Exception {
            long timeToStart = System.currentTimeMillis();
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Constructor<WikiOperation> actionConstr =
                actionClass.getConstructor(Wiki.class, Wiki.class, Wikibase.class, String.class);
            targetWiki.setMarkBot(bot);
            operationWatcher = new Thread() {

                @Override
                public void run() {
                    while (!finished) {
                        if (null != action) {
                            String[] status = action.getStatus();
                            if (null != status && 0 < status.length) {
                                String statusKey = status[0];
                                if (1 < status.length) {
                                    String[] statusParams = new String[status.length - 1];
                                    System.arraycopy(status, 1, statusParams, 0, status.length - 1);
                                    publish(MessageFormat.format(bundle.getString(statusKey), (Object[]) statusParams));
                                } else {
                                    publish(bundle.getString(statusKey));
                                }
                            }
                        }
                        try {
                            Thread.sleep(1000l);
                        } catch (InterruptedException e) {
                        }
                    }
                }

            };
            operationWatcher.start();
            for (int i = 0; i < actionParams.length; i++) {
                try {
                    action = actionConstr.newInstance(targetWiki, sourceWiki, dataWiki, actionParams[i]);
                    setProgress(i);
                    long crtTime = System.currentTimeMillis();
                    while (crtTime < timeToStart) {
                        crtTime = System.currentTimeMillis();
                        publish(MessageFormat.format(bundle.getString("waiting"), ((crtTime - timeToStart) / 100) / 10.0d));
                        Thread.sleep(500l);
                    }

                    String result = action.execute();

                    targetWiki.edit(actionParams[i], result, commitMessage);
                    timeToStart = System.currentTimeMillis() + throttle;
                } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e1) {
                    int userOption = JOptionPane.showOptionDialog(frame, e1.getMessage(),
                        bundle.getString("error.processing"), JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE, null, new String[] { bundle.getString("error.abort"),
                            bundle.getString("error.retry"), bundle.getString("error.ignore") },
                        bundle.getString("error.retry"));
                    if (userOption == 0 || userOption == JOptionPane.CLOSED_OPTION) {
                        break;
                    }
                    if (userOption == 1) {
                        i--;
                        continue;
                    }
                    if (userOption == 2) {
                        continue;
                    }
                    e1.printStackTrace();
                }
            }
            return "Success";
        }

        @Override
        protected void process(List<String> chunks) {
            if (chunks.size() == 0) {
                return;
            }
            String lastMessage = chunks.get(chunks.size() - 1);
            pBar.setString(lastMessage);
        }

        @Override
        protected void done() {
            finished = true;
            if (null != operationWatcher)
                try {
                    operationWatcher.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }

    }

}