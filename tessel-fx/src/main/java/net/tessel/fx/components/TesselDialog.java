package net.tessel.fx.components;

import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * A dialog window with the Tessel chrome (title bar, rounded frame, shadow) and no minimize or maximize button.
 * Modal by default.
 *
 * <pre>{@code
 * TesselDialog dialog = new TesselDialog(window, "Rename file");
 * dialog.setContent(form);
 * dialog.showDialog();       // sizes to the content, centers on the owner, waits until it is closed
 * }</pre>
 *
 * Esc and the title bar's close button fire a close request first, so a {@code setOnCloseRequest} handler can veto
 * (by consuming the event); {@link #close()} closes without asking.
 *
 * <p>For confirmations that should be drawn over the window itself, use {@link ContentDialog}.
 */
public class TesselDialog extends TesselWindow {

    public TesselDialog(Window owner, String title) {
        this(owner, title, Modality.WINDOW_MODAL);
    }

    public TesselDialog(Window owner, String title, Modality modality) {
        super(title, true);
        if (owner != null) {
            initOwner(owner);
        }
        initModality(owner == null && modality == Modality.WINDOW_MODAL ? Modality.APPLICATION_MODAL : modality);
        getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && !e.isConsumed() && !escapeIsForSomethingElse()) {
                e.consume();
                fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        });
    }

    /** Esc belongs to an open ContentDialog / TeachingTip, or to a focused node that marks itself as handling it. */
    private boolean escapeIsForSomethingElse() {
        for (javafx.scene.Node n : getOverlayLayer().getChildren()) {
            if (n.getStyleClass().contains("tsl-dialog-scrim") || n.getStyleClass().contains("tsl-teaching-tip")) {
                return true;
            }
        }
        javafx.scene.Node focus = getScene().getFocusOwner();
        return focus != null && Boolean.TRUE.equals(focus.getProperties().get("tessel.handlesEscape"));
    }

    /** Packs the dialog, centers it on its owner (or the screen), shows it and waits until it is closed. */
    public void showDialog() {
        prepare();
        showAndWait();
    }

    /** Like {@link #showDialog()} but does not wait. */
    public void showCentered() {
        prepare();
        show();
    }

    private void prepare() {
        getScene().getRoot().applyCss();
        sizeToScene();
        setOnShown(e -> center());
    }

    private void center() {
        Window owner = getOwner();
        if (owner != null && owner.isShowing()) {
            setX(owner.getX() + (owner.getWidth() - getWidth()) / 2);
            setY(owner.getY() + (owner.getHeight() - getHeight()) / 2);
        } else {
            Rectangle2D area = Screen.getPrimary().getVisualBounds();
            setX(area.getMinX() + (area.getWidth() - getWidth()) / 2);
            setY(area.getMinY() + (area.getHeight() - getHeight()) / 2);
        }
    }
}
