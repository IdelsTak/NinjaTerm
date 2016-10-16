package ninja.mbedded.ninjaterm.util.streamedText;

import com.sun.javaws.exceptions.InvalidArgumentException;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import ninja.mbedded.ninjaterm.util.debugging.Debugging;
import ninja.mbedded.ninjaterm.util.loggerUtils.LoggerUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which is designed to encapsulate a "unit" of streamed text, which is generated by the ANSI escape
 * code parser. This <code>{@link StreamedText}</code> object is then fed into the filter engine,
 * whose output is another <code>{@link StreamedText}</code> object.
 *
 * @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since 2016-09-28
 * @last-modified 2016-10-14
 */
public class StreamedText {

    //================================================================================================//
    //=========================================== CLASS FIELDS =======================================//
    //================================================================================================//

    private String text = "";
    private List<TextColour> textColours = new ArrayList<>();
    private Color colorToBeInsertedOnNextChar = null;

    /**
     * Holds the locations in <code>text</code> at which new lines are detected. This is populated by
     * a <code>NewLineParser</code> object. New lines are to be inserted AFTER the character pointed
     * to by each newLineMarker.
     *
     * <code>shiftCharsIn()</code> and <code>copyCharsIn()</code> modifies the markers as appropriate.
     */
    private List<Integer> newLineMarkers = new ArrayList<>();

    private Logger logger = LoggerUtils.createLoggerFor(getClass().getName());

    //================================================================================================//
    //========================================== CLASS METHODS =======================================//
    //================================================================================================//

    /**
     * Default constructor.
     */
    public StreamedText() {

    }

    /**
     * Copy constructor. Uses the <code>copyCharsFrom()</code> to do the actual copying.
     * @param streamedText
     */
    public StreamedText(StreamedText streamedText) {
        this.copyCharsFrom(streamedText, streamedText.getText().length());
    }


    public String getText() {
        return text;
    }

    public List<TextColour> getTextColours () {
        return textColours;
    }

    public Color getColorToBeInsertedOnNextChar() {
        return colorToBeInsertedOnNextChar;
    }

    public void setColorToBeInsertedOnNextChar(Color color) {
        logger.debug("setColorToBeInsertedOnNextChar() called with color = " + color);
        this.colorToBeInsertedOnNextChar = color;
    }

    /**
     * The method extracts the specified number of chars from the input and places them in the output.
     * It extract chars from the "to append" String first, and then starts removing chars from the first of the
     * Text nodes contained within the list.
     * <p>
     * It also shifts any chars from still existing input nodes into the "to append" String
     * as appropriate.
     *
     * @param numChars
     * @return
     */
    public void shiftCharsIn(StreamedText inputStreamedText, int numChars) {
        copyOrShiftCharsFrom(inputStreamedText, numChars, CopyOrShift.SHIFT);
    }

    public void clear() {
        // "Reset" this object
        text = "";
        getTextColours().clear();
        colorToBeInsertedOnNextChar = null;

        getNewLineMarkers().clear();
    }

    public void removeChars(int numChars) {
        StreamedText dummyStreamedText = new StreamedText();
        dummyStreamedText.shiftCharsIn(this, numChars);
        checkAllColoursAreInOrder();
    }

    public enum CopyOrShift {
        COPY,
        SHIFT,
    }

    /**
     * The method copies/shifts the specified number of chars from the input into the output.
     * It copies/shifts chars from the "to append" String first, and then starts copying/shifting chars from the first of the
     * Text nodes contained within the list.
     * <p>
     * It also copies/shift any chars from still existing input nodes into the "to append" String
     * as appropriate.
     * <p>
     *     Designed to be exposed publically via copy() or shift() methods.
     * </p>
     *
     * @param numChars  The number of characters to copy or shift (starting from the start of the text).
     */
    private void copyOrShiftCharsFrom(StreamedText inputStreamedText, int numChars, CopyOrShift copyOrShift) {

        if(numChars > inputStreamedText.getText().length())
            throw new IllegalArgumentException("numChars is greater than the number of characters in inputStreamedText.");

        // Copy/shift the new line markers first
        copyOrShiftNewLineMarkers(inputStreamedText, numChars, copyOrShift);

        // Apply the colour to be inserted on next char, if at least one char is
        // going to be placed into this StreamedText object
        if((numChars > 0) && (this.colorToBeInsertedOnNextChar != null)) {

            this.textColours.add(new TextColour(this.text.length(), this.colorToBeInsertedOnNextChar));

            // We have applied the color to a character, remove the placeholder
            this.colorToBeInsertedOnNextChar = null;
        }

        for (ListIterator<TextColour> iter = inputStreamedText.textColours.listIterator(); iter.hasNext(); ) {
            TextColour oldTextColour = iter.next();
            TextColour newTextColor;

            if(copyOrShift == CopyOrShift.COPY) {
                // Copy text color object
                newTextColor = new TextColour(oldTextColour);
            } else if(copyOrShift == CopyOrShift.SHIFT) {
                // We can just modify the existing object, since we are shifting
                newTextColor = oldTextColour;
            } else {
                throw new RuntimeException("copyOrShift not recognised.");
            }

            // Check if we have reached TextColour objects which index characters beyond the range
            // we are shifting, and if so, break out of this loop
            if(oldTextColour.position < numChars) {

                // We need to offset set the position by the length of the existing text
                newTextColor.position = oldTextColour.position + text.length();
                // Now add this TextColour object to this objects list, and remove from the input
                textColours.add(newTextColor);

                if(copyOrShift == CopyOrShift.SHIFT) {
                    iter.remove();
                }

            } else {
                // We are beyond the range that is being shifted, so adjust the position, but
                // don't shift the object to this list (keep in input)
                if(copyOrShift == CopyOrShift.SHIFT) {
                    newTextColor.position -= numChars;
                }
            }
        }

        text = text + inputStreamedText.text.substring(0, numChars);

        if(copyOrShift == CopyOrShift.SHIFT) {
            inputStreamedText.text = inputStreamedText.text.substring(numChars, inputStreamedText.text.length());
        }



        // Transfer the "color to be inserted on next char", if one exists in input
        // This could overwrite an existing "color to be inserted on next char" in the output, if
        // no chars were shifted
        if(inputStreamedText.getColorToBeInsertedOnNextChar() != null) {
            this.setColorToBeInsertedOnNextChar(inputStreamedText.getColorToBeInsertedOnNextChar());

            if(copyOrShift == CopyOrShift.SHIFT) {
                inputStreamedText.setColorToBeInsertedOnNextChar(null);
            }
        }

        checkAllColoursAreInOrder();
    }

    /**
     * This method expects the chars to be removed after this method is finished (otherwise
     * the input and output StreamedText objects will be left in an invalid state).
     * @param input
     * @param numChars
     * @param copyOrShift
     */
    private void copyOrShiftNewLineMarkers(StreamedText input, int numChars, CopyOrShift copyOrShift) {

        // Copy/shift markers within range
        for (ListIterator<Integer> iter = input.getNewLineMarkers().listIterator(); iter.hasNext(); ) {
            Integer element = iter.next();

            if (element <= numChars) {


                // Make a copy of this marker in the output
                addNewLineMarkerAt(getText().length() + element);

                switch(copyOrShift) {
                    case COPY:
                        // Do nothing

                        break;
                    case SHIFT:
                        // Remove the marker from the input
                        iter.remove();
                        break;
                    default:
                        throw new RuntimeException("CopyOrShift enum unrecognised.");
                }

            } else {
                // We have copied/shifted all markers within range,
                // we just need to adjust the marker values for the remaining
                // markers in the input
                if(copyOrShift == CopyOrShift.SHIFT) {
                    iter.set(element - numChars);
                }
            }
        }
    }

    public void copyCharsFrom(StreamedText inputStreamedText, int numChars) {
        copyOrShiftCharsFrom(inputStreamedText, numChars, CopyOrShift.COPY);
    }

    /**
     * Adds the provided text to the stream, using the given <code>addMethod</code>.
     *
     * @param textToAppend
     */
    public void append(String textToAppend) {
        logger.debug("append() called with text = \"" + Debugging.convertNonPrintable(textToAppend) + "\".");

        // Passing in an empty string is not invalid, but we don't have to do anything,
        // so just return.
        if(textToAppend.equals(""))
            return;

        text = text + textToAppend;

        // Apply the "color to be inserted on next char" if there is one to apply.
        // This will never be applied if no chars are inserted because of the return above
        if(colorToBeInsertedOnNextChar != null) {
            addColour(text.length() - textToAppend.length(), colorToBeInsertedOnNextChar);
            colorToBeInsertedOnNextChar = null;
        }

        checkAllColoursAreInOrder();
    }

    public void addColour(int position, Color color) {

        if(position < 0 || position > text.length() - 1)
            throw new IllegalArgumentException("position was either too small or too large.");

        // Make sure all the TextColor objects in the list remain in order
        if(textColours.size() != 0 && textColours.get(textColours.size() - 1).position > position)
            throw new IllegalArgumentException("position was not greater than all existing positions.");

        // Check if we are overwriting the last TextColor object (if they apply to the same text position),
        // or we are needed to create a new TextColor object
        if(textColours.size() != 0 && textColours.get(textColours.size() - 1).position == position) {
            textColours.get(textColours.size() - 1).color = color;
        } else {
            textColours.add(new TextColour(position, color));
        }

        checkAllColoursAreInOrder();
    }

    @Override
    public String toString() {
        String output = " { ";

        output += "text: \"" + text + "\", ";
        int i = 0;
        for(TextColour textColour : textColours) {
            output += " textColor[" + i + "]: ," + textColour.toString();
            i++;
        }

        output += "colorToBeInsertedOnNextChar: " + colorToBeInsertedOnNextChar;
        output += " }";
        return output;
    }

    /**
     * Shifts all the text in this streamed text object into the provided list of text nodes, leaving
     * this streamed text object empty.
     * @param existingTextNodes
     * @param nodeIndexToStartShift      The index in the observable list at which you want the streaming text to
     *                              start being shifted to.
     */
    public void shiftToTextNodes(ObservableList<Node> existingTextNodes, int nodeIndexToStartShift) {

        //==============================================//
        //============= INPUT ARG CEHCKS ===============//
        //==============================================//

        if(existingTextNodes.size() == 0) {
            throw new IllegalArgumentException("existingTextNodes must have at least one text node already present.");
        }

        if(nodeIndexToStartShift < 0 || nodeIndexToStartShift > existingTextNodes.size()) {
            throw new IllegalArgumentException("nodeIndexToStartShift must be greater than 0 and less than the size() of existingTextNodes.");
        }

        Text lastTextNode = (Text)existingTextNodes.get(nodeIndexToStartShift - 1);

        // Copy all text before first TextColour entry into the first text node

        int indexOfLastCharPlusOne;
        if(getTextColours().size() == 0) {
            indexOfLastCharPlusOne = getText().length();
        } else {
            indexOfLastCharPlusOne = getTextColours().get(0).position;
        }

        lastTextNode.setText(lastTextNode.getText() + getText().substring(0, indexOfLastCharPlusOne));

        // Create new text nodes and copy all text
        // This loop won't run if there is no elements in the TextColors array
        int currIndexToInsertNodeAt = nodeIndexToStartShift;
        for(int x = 0; x < getTextColours().size(); x++) {
            Text newText = new Text();

            int indexOfFirstCharInNode = getTextColours().get(x).position;

            int indexOfLastCharInNodePlusOne;
            if(x >= getTextColours().size() - 1) {
                indexOfLastCharInNodePlusOne = getText().length();
            } else {
                indexOfLastCharInNodePlusOne = getTextColours().get(x + 1).position;
            }

            newText.setText(getText().substring(indexOfFirstCharInNode, indexOfLastCharInNodePlusOne));
            newText.setFill(getTextColours().get(x).color);

            existingTextNodes.add(currIndexToInsertNodeAt, newText);

            currIndexToInsertNodeAt++;
        }

        if(colorToBeInsertedOnNextChar != null) {
            // Add new node with no text
            Text text = new Text();
            text.setFill(colorToBeInsertedOnNextChar);
            existingTextNodes.add(currIndexToInsertNodeAt, text);
            colorToBeInsertedOnNextChar = null;
        }

        // Clear all text and the TextColor list
        text = "";
        textColours.clear();

        checkAllColoursAreInOrder();
    }

    private void checkAllColoursAreInOrder() {

        int charIndex = -1;
        for(TextColour textColour : textColours) {
            if(textColour.position <= charIndex)
                throw new RuntimeException("Colours were not in order!");

            charIndex = textColour.position;
        }
    }

    public boolean checkAllNewLinesHaveColors() {

        // Check all characters but the last one (since there can't
        // be any char after this new line to have a color attached to it)
        for(int x = 0; x < text.length() - 1; x++) {

            if (text.charAt(x) != '\n') {
                continue;
            }

            // Look for entry in color array
            if (!isColorAt(x + 1)) {
                logger.debug("The was no color on the line starting at position " + Integer.toString(x + 1) + ".");
                return false;
            }
        }

        // If we make it here, all new lines must of had colors
        return true;
    }

    /**
     * Checks if there is a colour change at the specified character index.
     * @param charIndex
     * @return
     */
    public boolean isColorAt(int charIndex) {
        for(TextColour textColour : textColours) {
            if(textColour.position == charIndex)
                return true;
        }

        // If we make it here, no color at the specified index was found!
        return false;
    }

    public void addNewLineMarkerAt(int charIndex) {

        // We can't check this, because some of the other methods in this class add the markers
        // before adding the text
//        if(charIndex > getText().length()) {
//            throw new RuntimeException("charIndex must be between 0 and the num. of chars (inclusive at both ends).");
//        }

        newLineMarkers.add(charIndex);
    }

    public List<Integer> getNewLineMarkers() {
        return newLineMarkers;
    }

    public void shiftCharsInUntilPartialMatch(StreamedText input, Pattern pattern) {

        int firstCharAfterLastFullMatch = 0;
        int currPositionInString = 0;

        // Look for index of partial match
        int startIndexOfPartialMatch = -1;
        while((startIndexOfPartialMatch == -1) && (currPositionInString <= (input.getText().length() - 1))) {

            Matcher matcher = pattern.matcher(input.getText().substring(currPositionInString));
            matcher.matches();
            if(matcher.hitEnd()) {
                startIndexOfPartialMatch = currPositionInString;
            }

            // Remove first character from input and try again
            currPositionInString++;
        }

        // There might be remaining input after the last ANSI escpe code has been processed.
        // This can all be put in the last text node, which should be by now set up correctly.
        if (startIndexOfPartialMatch == -1) {

            String charsToAppend = input.getText().substring(firstCharAfterLastFullMatch);
//            System.out.println("No partial match found. charsToAppend = " + Debugging.convertNonPrintable(charsToAppend));
            //addTextToLastNode(outputStreamedText, charsToAppend);
            shiftCharsIn(input, input.getText().length());
            //numCharsAdded += charsToAppend.length();
        } else {

            //String charsToAppend = withheldCharsAndInputString.substring(firstCharAfterLastFullMatch, startIndexOfPartialMatch);
//            System.out.println("Partial match found. charsToAppend = " + Debugging.convertNonPrintable(charsToAppend));
            //addTextToLastNode(outputStreamedText, charsToAppend);
            shiftCharsIn(input, startIndexOfPartialMatch);
            //numCharsAdded += charsToAppend.length();
        }
    }

    public String[] splitTextAtNewLines() {

        // Work out how many strings there will be
        int numOfLines = getNewLineMarkers().size() + 1;

        String[] lines = new String[numOfLines];

        int startIndex = 0;
        for(int i = 0; i < numOfLines; i++) {

            if(i == numOfLines - 1) {
                lines[i] = getText().substring(startIndex, getText().length());
            } else {
                lines[i] = getText().substring(startIndex, getNewLineMarkers().get(i));
                startIndex = getNewLineMarkers().get(i);
            }

        }

        return lines;
    }

    public void removeChar(int charIndex) {

        if(charIndex >= getText().length()) {
            throw new IllegalArgumentException("charIndex pointed outside of length of text.");
        }

        // Remove the character from the text
        String oldText = text;

        text = oldText.substring(0, charIndex) + oldText.substring(charIndex + 1, oldText.length());

        // Shift all new line markers from the deleted char onwards
        for (ListIterator<Integer> iter = newLineMarkers.listIterator(); iter.hasNext(); ) {
            Integer element = iter.next();

            if(element != 0 && element >= charIndex) {
                iter.set(element - 1);
            }
        }

    }
}
