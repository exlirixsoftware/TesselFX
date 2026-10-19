package net.tessel.fx.gallery.pages;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.tessel.fx.gallery.Page;

public class CollectionsPage extends Page {

    private record Person(String name, String role, String city) {
    }

    public CollectionsPage() {
        super("Collections", "Lists, tables and trees.");

        ListView<String> list = new ListView<>(FXCollections.observableArrayList(
                "Inbox", "Drafts", "Sent items", "Archive", "Junk", "Deleted items", "Notes"));
        list.getSelectionModel().select(2);
        list.setPrefHeight(230);

        TreeItem<String> root = new TreeItem<>("Project");
        root.setExpanded(true);
        TreeItem<String> src = new TreeItem<>("src");
        src.setExpanded(true);
        src.getChildren().add(new TreeItem<>("Main.java"));
        src.getChildren().add(new TreeItem<>("Util.java"));
        TreeItem<String> res = new TreeItem<>("resources");
        res.getChildren().add(new TreeItem<>("icon.png"));
        res.getChildren().add(new TreeItem<>("app.css"));
        root.getChildren().add(src);
        root.getChildren().add(res);
        root.getChildren().add(new TreeItem<>("README.md"));
        TreeView<String> tree = new TreeView<>(root);
        tree.setPrefHeight(230);
        tree.getSelectionModel().select(src);

        HBox both = new HBox(16, list, tree);
        HBox.setHgrow(list, Priority.ALWAYS);
        HBox.setHgrow(tree, Priority.ALWAYS);
        add(section("List view and tree view", both));

        TableView<Person> table = new TableView<>(FXCollections.observableArrayList(
                new Person("Ada Lovelace", "Mathematician", "London"),
                new Person("Grace Hopper", "Rear admiral", "New York"),
                new Person("Linus Torvalds", "Maintainer", "Portland"),
                new Person("Margaret Hamilton", "Software engineer", "Boston"),
                new Person("Dennis Ritchie", "Computer scientist", "Murray Hill")));
        table.getColumns().add(column("Name", 200, Person::name));
        table.getColumns().add(column("Role", 200, Person::role));
        table.getColumns().add(column("City", 160, Person::city));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().select(1);
        table.setPrefHeight(260);
        add(section("Table view", table));
    }

    private static TableColumn<Person, String> column(String title, double width, java.util.function.Function<Person, String> get) {
        TableColumn<Person, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(d -> new SimpleStringProperty(get.apply(d.getValue())));
        return c;
    }
}
