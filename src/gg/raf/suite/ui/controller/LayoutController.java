package gg.raf.suite.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

/**
 * Created by Allen Kinzalow on 9/27/2015.
 * This class is a controller for the layout.fxml.
 * It holds the Components of the main application layout.
 */
public class LayoutController extends Controller  {

    /**
     * FXML components
     */
    @FXML private TreeView<String> cacheTree;
    @FXML private TextArea logger;
    @FXML private TabPane fileTabs;
    @FXML private Label currentAction;
    @FXML private MenuItem replaceMenuButton;
    @FXML private MenuItem exportMenuButton;
    @FXML private MenuItem exportAllMenuButton;
    @FXML private MenuItem searchMenuButton;
    @FXML private MenuItem ddsPngCheckButton;
    @FXML private MenuItem aboutButton;
    @FXML private MenuItem checkUpdateButton;
    @FXML private MenuItem exitButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) { }

    /**
     * Retrieve the TreeView for cache directories.
     * @return
     */
    public TreeView<String> getCacheTree() {
        return cacheTree;
    }

    /**
     * Retrieve the TextArea used as a logging function.
     * @return
     */
    public TextArea getLogger() {
        return logger;
    }

    /**
     * Retrieve the TabPane for opened RiotFile's
     * @return
     */
    public TabPane getFileTabs() {
        return fileTabs;
    }

    /**
     * Retrieve the Label of the current action on
     *  the toolbar.
     * @return
     */
    public Label getCurrentAction() {
        return currentAction;
    }

    public MenuItem getReplaceMenuButton() {
        return replaceMenuButton;
    }

    public MenuItem getExportMenuButton() {
        return exportMenuButton;
    }

    public MenuItem getSearchMenuButton() {
        return searchMenuButton;
    }

    public MenuItem getExportAllMenuButton() {
        return exportAllMenuButton;
    }

    public MenuItem getDdsPngCheckButton() {
        return ddsPngCheckButton;
    }

    public MenuItem getAboutButton() {
        return aboutButton;
    }

    public MenuItem getCheckUpdateButton() {
        return checkUpdateButton;
    }

    public MenuItem getExitButton() {
        return exitButton;
    }
}
