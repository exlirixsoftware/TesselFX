package net.tessel.fx.gallery.pages;

import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import net.tessel.fx.ButtonStyle;
import net.tessel.fx.ProgressState;
import net.tessel.fx.Severity;
import net.tessel.fx.Tessel;
import net.tessel.fx.components.Avatar;
import net.tessel.fx.components.Badge;
import net.tessel.fx.components.InfoBar;
import net.tessel.fx.components.ProgressRing;
import net.tessel.fx.components.Snackbar;
import net.tessel.fx.components.TeachingTip;
import net.tessel.fx.gallery.Page;
import net.tessel.fx.icons.Symbol;

public class FeedbackPage extends Page {

    public FeedbackPage() {
        super("Feedback", "Info bars, snackbars, teaching tips, progress, badges and avatars.");

        InfoBar info = new InfoBar(Severity.INFORMATIONAL, "Did you know?", "Tessel follows the system theme and its accent color.");
        InfoBar success = new InfoBar(Severity.SUCCESS, "Saved", "Your changes were saved.");
        InfoBar warning = new InfoBar(Severity.WARNING, "Heads up", "Your subscription expires in 3 days.");
        warning.setClosable(true);
        Button renew = Tessel.style(new Button("Renew"), ButtonStyle.SUBTLE);
        warning.setAction(renew);
        InfoBar error = new InfoBar(Severity.ERROR, "Connection lost", "Could not reach the server. Check your network and try again.");
        error.setClosable(true);
        add(section("Info bar", column(10, info, success, warning, error)));

        // snackbars and the teaching tip
        Button saveButton = Tessel.style(new Button("Save"), ButtonStyle.ACCENT);
        TeachingTip tip = new TeachingTip("Save your work", "Press Ctrl+S at any time to save.");
        tip.setSymbol(Symbol.LIGHTBULB);
        tip.setActionButton("Got it", () -> Snackbar.show(saveButton, "Tip dismissed", "We will not show it again.", Severity.SUCCESS));
        Button showTip = new Button("Show teaching tip");
        showTip.setOnAction(e -> tip.show(saveButton));

        Button s1 = new Button("Informational");
        s1.setOnAction(e -> Snackbar.show(s1, "New version", "Tessel 1.0 is available.", Severity.INFORMATIONAL));
        Button s2 = new Button("Success");
        s2.setOnAction(e -> Snackbar.show(s2, "Saved", "Your changes were saved.", Severity.SUCCESS));
        Button s3 = new Button("Warning");
        s3.setOnAction(e -> Snackbar.show(s3, "Low disk space", "Only 2 GB are left on this drive.", Severity.WARNING));
        Button s4 = new Button("Error with action");
        s4.setOnAction(e -> Snackbar.show(s4, "Upload failed", "report.pdf could not be uploaded.", Severity.ERROR,
                javafx.util.Duration.seconds(8), "Retry", () -> { }));
        add(section("Snackbar and teaching tip",
                row(s1, s2, s3, s4),
                row(saveButton, showTip),
                caption("The snackbar stays while the mouse is over it. The tip follows its target when the window is resized.")));

        ToggleGroup states = new ToggleGroup();
        HBox stateButtons = new HBox(6);
        ProgressBar determinate = new ProgressBar(0.62);
        determinate.setPrefWidth(320);
        ProgressBar busy = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        busy.setPrefWidth(320);
        ProgressRing ring = new ProgressRing(36);
        ProgressRing ringDone = new ProgressRing(36);
        ringDone.setProgress(0.7);
        for (ProgressState s : ProgressState.values()) {
            ToggleButton b = new ToggleButton(s.name().charAt(0) + s.name().substring(1).toLowerCase());
            b.setToggleGroup(states);
            b.setUserData(s);
            stateButtons.getChildren().add(b);
            if (s == ProgressState.NORMAL) {
                b.setSelected(true);
            }
        }
        states.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                states.selectToggle(a);
                return;
            }
            ProgressState s = (ProgressState) b.getUserData();
            Tessel.progressState(determinate, s);
            Tessel.progressState(busy, s);
            ring.setState(s);
            ringDone.setState(s);
        });
        ProgressIndicator classic = new ProgressIndicator();
        classic.setPrefSize(36, 36);
        add(section("Progress",
                column(12, determinate, busy, row(ring, ringDone, classic), stateButtons),
                caption("Progress states: normal, paused, error and success. Paused or failed indeterminate indicators stop animating.")));

        add(section("Badges and avatars",
                row(new Badge("Neutral"), new Badge("New", Badge.Kind.ACCENT), new Badge("Online", Badge.Kind.SUCCESS),
                        new Badge("Pending", Badge.Kind.WARNING), new Badge("Offline", Badge.Kind.DANGER), new Badge("Beta", Badge.Kind.INFO),
                        solid(new Badge("12", Badge.Kind.ACCENT)), solid(new Badge("Error", Badge.Kind.DANGER))),
                row(new Avatar("Ada Lovelace", 48), new Avatar("Grace Hopper", 40), new Avatar("Linus", 32),
                        new Avatar("Margaret Hamilton", 40), new Avatar("Dennis Ritchie", 40), new Avatar("Tim", 28))));
    }

    private static Badge solid(Badge badge) {
        badge.setSolid(true);
        return badge;
    }
}
