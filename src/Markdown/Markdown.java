package Markdown;
import org.docx4j.Docx4J;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RFonts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.mozilla.universalchardet.UniversalDetector;
import org.pegdown.*;
import org.pegdown.ast.HeaderNode;
import org.pegdown.ast.HtmlBlockNode;
import org.pegdown.ast.Node;
import org.pegdown.ast.RootNode;
import org.markdown4j.Markdown4jProcessor;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JToolBar;
import javax.swing.undo.UndoManager;


/**
 * Created by BBge on 2016/12/4.
 */
class MutableTreeNode extends DefaultMutableTreeNode {
    int startIndex, endIndex;

    public MutableTreeNode(int startIndex) {
        super();
        startIndex = 0;
        endIndex = 0;
    }

    public MutableTreeNode(Object userObject) {
        super(userObject);
        startIndex = 0;
        endIndex = 0;
    }

    public MutableTreeNode(Object userObject, boolean allowChildren) {
        super(userObject, allowChildren);
        startIndex = 0;
        endIndex = 0;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
}


public class Markdown extends JFrame implements ActionListener,DocumentListener,TreeSelectionListener{


   //private Markdown4jProcessor markdownProcessor;

    JTextArea markdownTextArea;
    JEditorPane previewView;

    JScrollPane textScroll;
    JScrollPane previewScroll;

    JScrollBar textScrollBar;
    JScrollBar previewScrollBar;

    private JButton   openBtn, newBtn, saveBtn;
    private JButton   toHtmlBtn,toDocxBtn,setcssBtn;

    private LayoutManager layout;

    private JTree tree;

    private DefaultMutableTreeNode root;
    private DefaultTreeModel model;

    private File cssFile;
    private File markdownFile;

    private int[] lineNumber;

    private String fileName;

    private boolean isNewFile = false;
    private boolean isModified = false;
    private boolean isNewCss = false;

    private AdjustmentListener listener;

    public Markdown()
    {
        initUI();
        newFile();
    }
    private void initUI(){
        super.setTitle("Markdown Editor by BBge");
        super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        super.setSize(1000,1000);
        super.setVisible(true);

        initMenuBar();
        initToolBar();

        layout = new GridLayout(1,2);
        JPanel pane = new JPanel(layout);

        root = new MutableTreeNode("Document");
        model =  new DefaultTreeModel(root);
        tree = new JTree(model);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.addTreeSelectionListener(this);

        /* 创建文本区域 */
        markdownTextArea = new JTextArea();
        markdownTextArea.setBackground(new Color(45, 45, 45));
        markdownTextArea.setForeground(new Color(255, 255, 255));
        markdownTextArea.setCaretColor(new Color(255, 255, 255));   /* 设置插入字符颜色 */
        markdownTextArea.setLineWrap(true);                         /* 自动换行 */
        markdownTextArea.setDragEnabled(true);
        markdownTextArea.setBorder(new EmptyBorder(new Insets(30, 20, 40, 20)));
        markdownTextArea.setFont(new Font("", Font.PLAIN, 13));
        markdownTextArea.getDocument().addDocumentListener(this);

        textScroll = new JScrollPane(markdownTextArea);             /* 设置滚动条 */
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);/* 垂直滚动条 */
        textScroll.setBorder(null);
        textScrollBar = textScroll.getVerticalScrollBar();
        pane.add(textScroll);

        /* 创建预览区域 */
        previewView = new JEditorPane();
        previewView.setContentType("text/html");
        previewView.setBorder(new EmptyBorder(new Insets(1, 10, 10, 10)));
        previewView.setEditable(false);
        previewView.setEditorKit(new HTMLEditorKit());

        previewScroll = new JScrollPane(previewView);
        previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScroll.setBorder(null);
        previewScrollBar = previewScroll.getVerticalScrollBar();
        pane.add(previewScroll);

        /* 设置滚动条同步滚动 */
        textScrollBar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                double textVSBMin = textScrollBar.getMinimum(),
                        textVSBMax = textScrollBar.getMaximum(),
                        textVSBVisibleAmount = textScrollBar.getVisibleAmount();
                double previewVSBMin = previewScrollBar.getMinimum(),
                        previewVSBMax = previewScrollBar.getMaximum(),
                        previewVSBVisibleAmount = previewScrollBar.getVisibleAmount();
                double percent = textScrollBar.getValue() / (textVSBMax - textVSBMin - textVSBVisibleAmount);
                // remove the AdjustmentListener of previewScrollPane temporarily
                AdjustmentListener listener = previewScrollBar.getAdjustmentListeners()[0];
                previewScrollBar.removeAdjustmentListener(listener);
                // set the value of scrollbar in previewScroll
                previewScrollBar.setValue((int)(previewVSBMin + percent * (previewVSBMax - previewVSBMin - previewVSBVisibleAmount)));
                // add the AdjustmentListener of previewScroll
                previewScrollBar.addAdjustmentListener(listener);
            }
        });
        previewScrollBar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {

                double textVSBMin = textScrollBar.getMinimum(),
                        textVSBMax = textScrollBar.getMaximum(),
                        textVSBVisibleAmount = textScrollBar.getVisibleAmount();
                double previewVSBMin = previewScrollBar.getMinimum(),
                        previewVSBMax = previewScrollBar.getMaximum(),
                        previewVSBVisibleAmount = previewScrollBar.getVisibleAmount();
                double percent = previewScrollBar.getValue() / (previewVSBMax - previewVSBMin - previewVSBVisibleAmount);
                // remove the AdjustmentListener of textScroll
                AdjustmentListener listener = textScrollBar.getAdjustmentListeners()[0];
                textScrollBar.removeAdjustmentListener(listener);
                // set the value of scrollbar in textScroll
                textScrollBar.setValue((int) (textVSBMin + percent * (textVSBMax - textVSBMin - textVSBVisibleAmount)));
                // add the AdjustmentListener of textScroll
                textScrollBar.addAdjustmentListener(listener);

            }
        });

        /* 设置初始CSS样式 */
        cssFile = new File("css/Default.css");
        setcss();

        /* 将界面分成两个区域 */
        JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jsp.setLeftComponent(new JScrollPane(tree));
        jsp.setRightComponent(pane);
        jsp.setDividerSize(5);
        add(jsp, BorderLayout.CENTER);

        setSize(1120, 630);
        setVisible(true);
        jsp.setDividerLocation(0.17);

    }
    /* 初始化菜单栏 */
    private void initMenuBar() {
        JMenuBar menuBar=new JMenuBar();
        JMenu menu=new JMenu("File");
        menuBar.add(menu);

        JMenuItem menuItem=new JMenuItem("New");
        menuItem.setActionCommand("new");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem=new JMenuItem("Open");
        menuItem.setActionCommand("open");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem=new JMenuItem("Save");
        menuItem.setActionCommand("save");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem=new JMenuItem("To docx");
        menuItem.setActionCommand("to docx");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem=new JMenuItem("To html");
        menuItem.setActionCommand("to html");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        menu =new JMenu("CSS");
        menuBar.add(menu);

        menuItem=new JMenuItem("Set");
        menuItem.setActionCommand("set css");
        menuItem.addActionListener(this);
        menu.add(menuItem);


        setJMenuBar(menuBar);
    }

    /* 初始化工具栏 */
    private void initToolBar(){
        JToolBar toolBar = new JToolBar("Tool Bar");
        add(toolBar, BorderLayout.PAGE_START);

        newBtn = addButtonToToolBar(toolBar, "new");
        openBtn = addButtonToToolBar(toolBar, "open");
        saveBtn = addButtonToToolBar(toolBar, "save");
        toHtmlBtn = addButtonToToolBar(toolBar,"to html");
        toDocxBtn = addButtonToToolBar(toolBar,"to docx");
        setcssBtn = addButtonToToolBar(toolBar,"set css");


    }
    /* 设置工具栏中按钮的监听事件以及设置图标(图标像素为16px) */
    private JButton addButtonToToolBar(JToolBar toolBar, String actionCommand) {
        Icon icon = new ImageIcon(this.getClass().getResource("/images/" + actionCommand + ".png"));
        JButton btn = new JButton(icon);

        btn.setActionCommand(actionCommand);
        btn.setBorderPainted(false);
        btn.addActionListener(this);
        toolBar.add(btn);

        return btn;
    }

    /* 设置Title,当文件被改动后,会改变Title */
    private void updateTitle(){
        if(markdownFile == null){
            setTitle("Markdown Editor by BBge");
        }
        else if(isModified){
            setTitle(markdownFile.getName()+"* - "+"Markdown Editor by BBge");
        }
        else{
            setTitle(markdownFile.getName()+" - "+"Markdown Editor by BBge");
        }
    }

    /* 对预览界面进行操作 */
    private void preview(){
        try {
            String html1 = new Markdown4jProcessor().process(markdownTextArea.getText());               /* 用Markdown4j解析文内容 */
            PegDownProcessor processor = new PegDownProcessor();                                        /* 用PegDown解析文本内容 */
            RootNode rootNode = processor.parseMarkdown(markdownTextArea.getText().toCharArray());
            ToHtmlSerializer serializer = new ToHtmlSerializer(new LinkRenderer());
            String html = serializer.toHtml(rootNode);
            previewView.setText(html1);
            // parse the html
            Document doc = Jsoup.parseBodyFragment(html);
            // get all <h?> tags
            Elements hTags = doc.select("h1, h2, h3, h4, h5, h6");
            List<Node> nodes = rootNode.getChildren().stream()                                          /* 用正则匹配分过滤标签中的内容 */
                    .filter(node -> node instanceof HeaderNode || ((node instanceof HtmlBlockNode) &&
                            ((HtmlBlockNode)node).getText().matches("<h([1-6])[^>]*>.*</h\\1>")))
                    .collect(Collectors.toList());

            while (root.getChildCount() > 0) {
                model.removeNodeFromParent((DefaultMutableTreeNode) root.getChildAt(0));
            }
            // add all <h?> tags to tree
            ArrayList<Map.Entry<MutableTreeNode, String>> nodeList = new ArrayList<>();
            nodeList.add(new AbstractMap.SimpleEntry<MutableTreeNode, String>((MutableTreeNode)root, "h0"));
            for (int i = 0; i < hTags.size() && i < nodes.size(); i ++) {                               /* 用堆栈的方式来确立节点的父子关系 */
                Element element = hTags.get(i);

                Node node = nodes.get(i);
                while (nodeList.get(nodeList.size() - 1).getValue().compareTo(element.tagName()) >= 0) {
                    nodeList.remove(nodeList.size() - 1);
                }
                MutableTreeNode parent = nodeList.get(nodeList.size() - 1).getKey();
                MutableTreeNode thisChild = new MutableTreeNode(element.text());
                thisChild.setStartIndex(node.getStartIndex());
                thisChild.setEndIndex(node.getEndIndex());
                nodeList.add(new AbstractMap.SimpleEntry<>(thisChild, element.tagName()));
                model.insertNodeInto(thisChild, parent, parent.getChildCount());
            }
            tree.expandPath(new TreePath(root));

        } catch (Exception exp){
            exp.printStackTrace();
        }
    }

    /* 打开文件 */
    private void openFile(){
        if(isModified){
            int i = JOptionPane.showConfirmDialog(this,"This file in not saved.Do you want to save it?",
                    "MarkdownEditor", JOptionPane.YES_NO_CANCEL_OPTION);
            if(i == JOptionPane.YES_OPTION){
                saveFile();
            }
            else if(i == JOptionPane.CANCEL_OPTION)
                return;
        }
        JFileChooser openFile = new JFileChooser();
        openFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFile.removeChoosableFileFilter(openFile.getFileFilter());
        openFile.setFileFilter(new FileNameExtensionFilter("Markdown Files (*.md, *.markdown, *.mdown)",
                "md", "markdown", "mdown"));
        int i = openFile.showOpenDialog(this);
        if( i == JFileChooser.APPROVE_OPTION ) {
            markdownFile = openFile.getSelectedFile();
            fileName = openFile.getSelectedFile().getName();

            try {
                DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(markdownFile)));
                byte[] markdownFileContent = new byte[(int) markdownFile.length()];
                input.read(markdownFileContent);
                markdownTextArea.setText(new String(markdownFileContent));
                input.close();
            } catch (IOException exp) {
                JOptionPane.showMessageDialog(this, "ERROR:Open Markdown File Failed!");
            }
            isNewFile = false;
            isModified = false;
            updateTitle();
        }
    }

    /* 保存文件 */
    private void saveFile(){

        if(isNewFile == true){
            JFileChooser saveFileChooser = new JFileChooser();
            saveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            saveFileChooser.setSelectedFile(new File(markdownFile.getName().substring(0, markdownFile.getName().lastIndexOf('.')) + ".md"));
            saveFileChooser.removeChoosableFileFilter(saveFileChooser.getFileFilter());
            saveFileChooser.setFileFilter(new FileNameExtensionFilter("Markdown Files (*.md, *.markdown, *.mdown)",
                    "md", "markdown", "mdown"));
            if(saveFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
                return;
            markdownFile = saveFileChooser.getSelectedFile();
        }
        try{
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(markdownFile)));
            output.write(markdownTextArea.getText().getBytes());
            output.close();
            System.out.println(markdownTextArea.getText());
        }catch (IOException exp){
            JOptionPane.showMessageDialog(this,"ERROR:Save Markdown File Failed!");
        }
        isNewFile = false;
        isModified = false;
        updateTitle();
    }

    /* 新建文件 */
    private void newFile(){
        if(isModified){
            int i = JOptionPane.showConfirmDialog(this,"This file is not saved.Do you want to save it?",
                    "MarkdownEditor", JOptionPane.YES_NO_CANCEL_OPTION);
            if(i == JOptionPane.YES_OPTION){
                    saveFile();
            }
        }
        markdownFile = new File("Untitled.md");
        markdownTextArea.setText("This is a new markdown file.");
        isNewFile = true;
        isModified = false;
        updateTitle();
    }

    /* 转换成docx文件 */
    private void toDocx(){
        JFileChooser toDocxChooser = new JFileChooser();
        toDocxChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        toDocxChooser.setSelectedFile(new File(markdownFile.getPath().substring(0, markdownFile.getPath().lastIndexOf('.')) + ".docx"));
        toDocxChooser.removeChoosableFileFilter(toDocxChooser.getFileFilter());
        toDocxChooser.setFileFilter(new FileNameExtensionFilter("Word Documents (*.docx)", "docx"));
        int i = toDocxChooser.showSaveDialog(this);
        if( i == JFileChooser.APPROVE_OPTION)
        {
            File docxFile = toDocxChooser.getSelectedFile();
            try{
                  convertDocx(convertHtml()).save(docxFile);
                  JOptionPane.showMessageDialog(this,"Convert Docx File Successfully");
            } catch(Exception exp){
                JOptionPane.showMessageDialog(this,"Convert Docx File Failed!");
            }
        }
    }
    /* 转换成docx文件 */
    private WordprocessingMLPackage convertDocx(String html) throws Exception {
        Document doc = Jsoup.parse(previewView.getText().toString());

        // remove all scripts
        for(org.jsoup.nodes.Element script: doc.getElementsByTag("script")) {
            script.remove();
        }

        // remove onClick and href in <a>
        for(org.jsoup.nodes.Element a: doc.getElementsByTag("a")) {
            a.removeAttr("onClick");
            a.removeAttr("href");
        }

        // replace the addresses in <link> with absolute addresses
        Elements links = doc.getElementsByTag("link");
        for(org.jsoup.nodes.Element element: links) {
            String href = element.absUrl("href");
            element.attr("href", href);
        }

        // change to xhtml
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml);

        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage(PageSizePaper.A4, true);

        RFonts rFonts = Context.getWmlObjectFactory().createRFonts();
        rFonts.setAsciiTheme(null);
        rFonts.setAscii("SimSun");
        XHTMLImporterImpl.addFontMapping("SimSun", rFonts);

        XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
        wordMLPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(doc.html(), doc.baseUri()));

        return wordMLPackage;
    }

    /* 转换成html文件 */
    private void toHtml() {
        JFileChooser toHtmlChooser = new JFileChooser();
        toHtmlChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        toHtmlChooser.setSelectedFile(new File(markdownFile.getPath().substring(0, markdownFile.getPath().lastIndexOf('.')) + ".html"));
        toHtmlChooser.removeChoosableFileFilter(toHtmlChooser.getFileFilter());
        toHtmlChooser.setFileFilter(new FileNameExtensionFilter("HTML Files (*.html)", "html"));
        int i = toHtmlChooser.showSaveDialog(this);
        if (i == JFileChooser.APPROVE_OPTION) {
            File newHtmlFile = toHtmlChooser.getSelectedFile();
            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newHtmlFile)));
                out.write(convertHtml().getBytes());
                out.close();
                JOptionPane.showMessageDialog(this,"Convert Html File Successfully!");
            } catch (IOException exp) {
                JOptionPane.showMessageDialog(this, "ERROR: Convert Html File Failed!");
            }
        }
    }
    /* 转换成html文件 */
    private String convertHtml(){
        String html = "", head = "", title = "", meta = "", style = "", body = "";

        title = "<title>" + markdownFile.getName().substring(0, markdownFile.getName().lastIndexOf('.')) + "</title>";
        meta = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />";
        try {
            if(cssFile != null && cssFile.exists()) {
                DataInputStream input = new DataInputStream(
                        new BufferedInputStream(
                                new FileInputStream(cssFile)
                        )
                );
                byte[] fileContent = new byte[(int)cssFile.length()];
                input.read(fileContent);
                UniversalDetector detector = new UniversalDetector(null);
                detector.handleData(fileContent, 0, fileContent.length);
                detector.dataEnd();
                String encoding = detector.getDetectedCharset();
                if(encoding != null)
                    style = "<style type=\"text/css\">\n" + new String(fileContent, encoding) + "\n</style>";
                else
                    style = "<style type=\"text/css\">\n" + new String(fileContent) + "\n</style>";
            }
        } catch (IOException exp) {
            exp.printStackTrace();
        }

        head = "<head>\n" + title + "\n" + meta + "\n" + style + "\n</head>";

        RootNode rootNode = new PegDownProcessor().parseMarkdown(markdownTextArea.getText().toCharArray());
        body = new ToHtmlSerializer(new LinkRenderer()).toHtml(rootNode);
        body = "<body>\n" + body + "\n</body>";

        html = "<!DOCTYPE HTML>\n" + "<html>\n" + head + "\n" + body + "\n</html>";

        return html;
    }

    /*设置css样式*/
    private void setcss(){
        if(isNewCss) {
            JFileChooser setCssChooser = new JFileChooser();
            setCssChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            setCssChooser.setSelectedFile(cssFile);
            setCssChooser.removeChoosableFileFilter(setCssChooser.getFileFilter());
            setCssChooser.setFileFilter(new FileNameExtensionFilter("CSS Files (*.css)", "css"));
            int i = setCssChooser.showOpenDialog(this);
            if (i == JFileChooser.APPROVE_OPTION)
                cssFile = setCssChooser.getSelectedFile();
        }
        try {
            StyleSheet style = ((HTMLEditorKit)previewView.getEditorKit()).getStyleSheet();
            URL css = cssFile.toURI().toURL();
            Enumeration<?> enu = style.getStyleNames();
            ArrayList<String> styleNames = new ArrayList<>();
            while(enu.hasMoreElements()) {
                styleNames.add(enu.nextElement().toString());
            }
            for(String styleName: styleNames) {
                style.removeStyle(styleName);
            }
            style.importStyleSheet(css);
            preview();
            previewView.setCaretPosition(0);
        } catch (IOException exp) {
            JOptionPane.showMessageDialog(this, "Open css file failed!");
        }
        isNewCss = true;
    }


    /* 对监听到的时间进行处理 */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();

        if("open".equals(command)){
            openFile();
        }
        else if("new".equals(command)){
            newFile();
        }
        else if("save".equals(command)){
            saveFile();
        }
        else if("new".equals(command)) {
            newFile();
        }
        else if("set css".equals(command)){
            setcss();
        }
        else if("to docx".equals(command)){
            toDocx();
        }
        else if("to html".equals(command)){
            toHtml();
        }

    }
    @Override
    public void insertUpdate(DocumentEvent e){
        preview();
        isModified = true;
        updateTitle();
    }
    @Override
    public void removeUpdate(DocumentEvent e){
        preview();
        isModified = true;
        updateTitle();
    }
    @Override
    public void changedUpdate(DocumentEvent e) {

    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        MutableTreeNode node = (MutableTreeNode)tree.getLastSelectedPathComponent();

        if(node == null)
            return;

        int startIndex = node.getStartIndex(),
                endIndex = node.getEndIndex();
        double textScrollBarMax = textScrollBar.getMaximum();
        double textScrollBarMin = textScrollBar.getMinimum();
        try {
            double percent = (double)markdownTextArea.getLineOfOffset(startIndex) / markdownTextArea.getLineCount();
            textScrollBar.setValue((int) (textScrollBarMin + percent * (textScrollBarMax - textScrollBarMin)));
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
}
