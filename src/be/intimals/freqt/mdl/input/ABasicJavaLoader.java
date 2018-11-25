package be.intimals.freqt.mdl.input;

import be.intimals.freqt.util.PeekableIterator;
import be.intimals.freqt.util.Util;
import be.intimals.freqt.util.XMLUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Basic abstract class to be able to load Java ASTS into a Database as-is.
 */
public abstract class ABasicJavaLoader<T> implements IDatabaseLoader<T> {
    private static final Logger LOGGER = Logger.getLogger(ABasicJavaLoader.class.getName());

    private Database<T> db = Database.create();
    private int currentTID = 0;

    @Override
    public Database<T> loadDirectory(String path) throws IOException, XPathExpressionException, SAXException,
        ParserConfigurationException {
        init();
        List<File> files = XMLUtil.loadXMLDirectory(path, (String file) -> true);
        LOGGER.info("loadDirectory : " + path);
        for (File f : files) {
            loadFile(f);
        }
        return db;
    }

    @Override
    public Database<T> loadFile(String path) throws IOException, XPathExpressionException, SAXException,
        ParserConfigurationException {
        init();
        File f = XMLUtil.loadXMLFile(path, (String name) -> true);
        loadFile(f);
        return db;
    }

    private void loadFile(File f) throws IOException, XPathExpressionException, SAXException,
            ParserConfigurationException {
        if (f != null) {
            LOGGER.info("loadFile : " + f.getName());
            Node root = XMLUtil.getXMLRoot(f);

            IDatabaseNode<T> newTreeRoot = traverse(root);
            db.addTransaction(newTreeRoot);

            ++currentTID;
            newTreeRoot.resetID();
        }
    }

    private IDatabaseNode<T> traverse(Node root) {
        IDatabaseNode<T> treeRoot = null;
        Map<Integer, IDatabaseNode<T>> treeNodeCache = new HashMap<>();

        PeekableIterator<Node> dfsIterator = Util.asPreOrderIterator(Util.asIterator(root), (Node e) ->
                XMLUtil.asIterator(e.getChildNodes()));
        dfsIterator.next();

        while (dfsIterator.hasNext()) {
            Node currentNode = dfsIterator.peek();
            IDatabaseNode<T> newTreeNode = null;

            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                newTreeNode = asDBNode(treeNodeCache, currentNode);
                T nodeKey = getKeyForNode(currentNode);
                newTreeNode.setLabel(nodeKey);
                if (currentNode.hasChildNodes()) {
                    List<IDatabaseNode<T>> treeChildren = new ArrayList<>();
                    NodeList nodeList = currentNode.getChildNodes();
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node child = nodeList.item(i);
                        IDatabaseNode<T> newChildNode = asDBNode(treeNodeCache, child);
                        T childKey = getKeyForNode(child);
                        newChildNode.setLabel(childKey);
                        newChildNode.setParent(newTreeNode);
                        treeChildren.add(newChildNode);
                    }
                    newTreeNode.setChildren(treeChildren);
                }
                treeNodeCache.remove(getNodeID(currentNode));

            } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
                // Do nothing
            } else {
                LOGGER.severe("Unhandled XML nodes");
            }

            if (treeRoot == null) treeRoot = newTreeNode;
            dfsIterator.next();
        }

        return treeRoot;
    }

    private IDatabaseNode<T> asDBNode(Map<Integer, IDatabaseNode<T>> cache, Node current) {
        Integer nodeID = getNodeID(current);
        if (cache.containsKey(nodeID)) {
            return cache.get(nodeID);
        } else {
            IDatabaseNode<T> newTreeNode = DatabaseNode.create();
            newTreeNode.setTID(currentTID);
            if (nodeID != -1) cache.put(nodeID, newTreeNode);
            return newTreeNode;
        }
    }

    private static Integer getNodeID(Node node) {
        return Integer.valueOf(Optional.ofNullable(node)
                .map(Node::getAttributes).map(x -> x.getNamedItem("ID")).map(Node::getNodeValue)
                .orElse("-1"));
    }

    protected abstract T getKeyForNode(Node current);

    protected void init() {
        currentTID = 0;
        db = Database.create();
        IDatabaseNode<T> temp = DatabaseNode.create();
        temp.resetID();
        temp.resetUID();
    }

}
