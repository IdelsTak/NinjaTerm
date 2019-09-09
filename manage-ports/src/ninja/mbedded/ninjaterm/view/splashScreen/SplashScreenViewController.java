package ninja.mbedded.ninjaterm.view.splashScreen;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;
import ninja.mbedded.ninjaterm.util.appInfo.AppInfo;
import ninja.mbedded.ninjaterm.util.loggerUtils.LoggerUtils;
import org.slf4j.Logger;

/**
 Controller for the splash screen.

 @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 @since 2016-09-20
 @last-modified 2017-10-09
 */
public class SplashScreenViewController {

    //================================================================================================//
    //========================================== FXML BINDINGS =======================================//
    //================================================================================================//
    @FXML
    private VBox splashScreenVBox;

    @FXML
    public TextFlow loadingMsgsTextFlow;

    //================================================================================================//
    //======================================== CLASS VARIABLES =======================================//
    //================================================================================================//
    private int loadingMsgIndex = 0;

    Text loadingMsgText = new Text();

    private Timeline timeline;

    /**
     Used to indicate when the splash screen has finished.
     */
    public SimpleBooleanProperty isFinished = new SimpleBooleanProperty(false);

    /**
     Show name of application and version
     */
    Timeline nameAndVersionTimeline = new Timeline();

    private int charIndex = 0;

    private String nameAndVersionString;

    private Logger logger = LoggerUtils.createLoggerFor(getClass().getName());

    /**
     The bogus "loading" messages displayed on the splash screen after the app name,
     version and basic
     info is displayed.
     */
    private final String[] bogusMessages = {
        "Using all processing power to render splash screen.",
        "Looking for operating system.",
        "Releasing the Kraken.",
        "Scanning for ports, found many ships.",
        "Turning volume up to 11.",
        "Bricking fake FTDI chips.",
        "Downloading more RAM.",
        "Unpacking christmas presents.",
        "Forgot which one was CTS and which was RTS.",
        "The answer is 42.",
        "Incorrect voltage levels are serial killers.",
        "All your base are belong to us.",
        "Uploading user data to NSA.",
        "Setting IE9 as default browser.",
        "Booting SkyNet.",
        "Crypto-locking personal files.",
        "Remember, DB-9 is actually DE-9.",
        "Installing bitcoin miner.",
        "Correcting speling.",
        "Setting heisenbugs to unknown states.",
        "Uninstalling RealTerm, Terminal by Br@y, PuTTy and Termite.",
        "Wondering if you've remembered the baud rate.",
        "Finding nearby hot singles.",
        "Putting down Windows XP seach dog.",
        "Persecuting minorities.",
        "Patching Java security flaws.",
        "Voting for Donald Trump.",
        "Loading clippy animation.",
        "Discovering this one weird trick, mind will be blown.",
        "Finished wasting user's time."
    };

    private double intervalBetweenEachBogusMsgMs = 100;

    /**
     This array is used to give the typing of characters onto the splash screen a
     "human-like"
     feel. Each entry corresponds to the time (in milliseconds) before the mapped
     character
     in the nameAndVerisonString is displayed.
     <p>
     Make sure this array has the same number of entries as the number of characters in
     the
     string.
     */
    private double[] charIntervalsMs = new double[]{
        25, //
        1500, // N
        75, // i
        50, // n
        150, // j
        100, // a
        75, // T
        100, // e
        50, // r
        75, // m
        25, //
        300, // v (NOTE: VERSION NUMBER DELAYS GET ADDED HERE AT RUNTIME, dependent on the number of chars in the version number)
        50, // \r
        25, // \r
        300, // A
        100, //
        100, // f
        50, // r
        20, // e
        25, // e
        10, //
        100, // t
        75, // o
        20, // o
        60, // l
        50, //
        100, // b
        20, // y
        50, //
        250, // w
        50, // w
        20, // w
        50, // .
        150, // m
        100, // b
        90, // e
        100, // d
        25, // d
        70, // e
        90, // d
        50, // .
        140, // n
        75, // i
        60, // n
        50, // j
        70, // a
        75, // \r
        75, // \r
    // <---- START OF BOGUS MESSAGES HERE
    };

    //================================================================================================//
    //========================================== CLASS METHODS =======================================//
    //================================================================================================//
    public SplashScreenViewController() {
        logger.debug("SplashScreenViewController() called.");
    }

    public void init() {

        // This makes the bogus text look more like it's in a proper terminal window
        loadingMsgsTextFlow.setTextAlignment(TextAlignment.JUSTIFY);

        //==============================================//
        //============= CREATE "^" TEXT ================//
        //==============================================//
        Text terminalStartText = new Text(">");
        terminalStartText.setFont(Font.font("monospace", FontWeight.BOLD, 20));
        terminalStartText.setFill(Color.LIME);

        loadingMsgsTextFlow.getChildren().add(terminalStartText);

        //==============================================//
        //========= CREATE FLASHING CARET ==============//
        //==============================================//
        Text caretText = new Text("█");
        caretText.setFont(Font.font("monospace", FontWeight.BOLD, 20));
        caretText.setFill(Color.LIME);

        // Add an animation so the caret blinks
        FadeTransition caretFt = new FadeTransition(Duration.millis(200), caretText);
        caretFt.setFromValue(1.0);
        caretFt.setToValue(0.1);
        caretFt.setCycleCount(Timeline.INDEFINITE);
        caretFt.setAutoReverse(true);
        caretFt.play();

        // Add caret to textflow object. It should always remain as the last child, to give the
        // proper appearance
        loadingMsgsTextFlow.getChildren().add(caretText);

        splashScreenVBox.setFocusTraversable(true);
        splashScreenVBox.requestFocus();
        splashScreenVBox.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            event.getCharacter();
        });

    }

    /**
     Initialises and starts the animation of the app name, version and info on
     the splash screen.
     */
    public void startNameVersionInfoMsg() {
        // Create Text object to hold application and version text
        loadingMsgText = new Text();
        loadingMsgText.setFill(Color.LIME);
        loadingMsgText.setFont(Font.font("monospace", FontWeight.NORMAL, 20));

        // Add text object as second-to-lat child of TextFlow (flashing caret is last child)
        loadingMsgsTextFlow.getChildren().add(loadingMsgsTextFlow.getChildren().size() - 1, loadingMsgText);

        // Get version (does not have "v" at the start)
        String versionNumber = AppInfo.getVersionNumber();

        // The version can be null, but this should only occur in a development
        // environment
        if (versionNumber == null) {
            versionNumber = "?.?.?";
        }

        nameAndVersionString = " NinjaTerm v" + versionNumber + "\r\rA free tool by www.mbedded.ninja\r\r";

        //==============================================//
        //======== ADD DELAYS FOR VERSION NUMBER =======//
        //==============================================//
        List<Double> newCharIntervalsMs = new ArrayList<Double>();
        // Copy first 12 elements (up to the "v" for the version number)
        for (int i = 0; i < 12; i++) {
            newCharIntervalsMs.add(charIntervalsMs[i]);
        }

        // Insert enough delays for the version
        for (int i = 0; i < versionNumber.length(); i++) {
            newCharIntervalsMs.add(50.0);
        }

        // Copy rest of array
        for (int i = 12; i < charIntervalsMs.length; i++) {
            newCharIntervalsMs.add(charIntervalsMs[i]);
        }

        charIntervalsMs = new double[newCharIntervalsMs.size()];
        for (int i = 0; i < newCharIntervalsMs.size(); i++) {
            charIntervalsMs[i] = newCharIntervalsMs.get(i);
        }

        // Check that there are enough char interval elements for the string we are going to display.
        if (charIntervalsMs.length != nameAndVersionString.length()) {
            throw new RuntimeException("The charIntervalsMs array does not have enough elements for the length of nameAndVersionString.");
        }

        createNextKeyFrame();
    }

    private void createNextKeyFrame() {

        if (charIndex == nameAndVersionString.length() - 1) {
            timeline = new Timeline(new KeyFrame(Duration.millis(charIntervalsMs[charIndex]), event -> {
                loadingMsgText.setText(loadingMsgText.getText() + nameAndVersionString.charAt(charIndex));

                // Start the next sequence after a fixed delay, where we display all of the bogus loading messages
                timeline = new Timeline(new KeyFrame(
                        Duration.millis(500),
                        ae -> updateBogusLoadingMsgs()));

                timeline.play();

            }));
            timeline.play();
        } else {

            // Create first keyframe
            timeline = new Timeline(new KeyFrame(Duration.millis(charIntervalsMs[charIndex]), event -> {

                // Update screen
                loadingMsgText.setText(loadingMsgText.getText() + nameAndVersionString.charAt(charIndex));
                charIndex++;
                // RECURSIVE!!!
                createNextKeyFrame();

            }));
            timeline.play();
        }
    }

    /**
     Called by lambda expression defined in startBogusLoadingMsgs(), once every keyframe,
     and
     adds a new bogus message to the splash screen.
     */
    public void updateBogusLoadingMsgs() {

        timeline = new Timeline(new KeyFrame(
                Duration.millis(intervalBetweenEachBogusMsgMs),
                event -> updateBogusLoadingMsgs()));
        timeline.play();

        loadingMsgText.setText(loadingMsgText.getText() + bogusMessages[loadingMsgIndex++] + " ");

        if (loadingMsgIndex >= bogusMessages.length) {
            // We have reached the end of the loading messages!
            timeline.stop();

            // We are now finished. Setting this property to true allows the main function
            // to listen for this change and load up the main window now.
            isFinished.set(true);
        }
    }

    /**
     Call this to drastically speedup the splash screen. The idea is to do this if the
     user presses
     a designated key, such as the space bar.
     */
    public void speedUpSplashScreen() {

        for (int i = 0; i < charIntervalsMs.length; i++) {
            charIntervalsMs[i] = 5.0;
        }

        intervalBetweenEachBogusMsgMs = 5.0;
    }
}
