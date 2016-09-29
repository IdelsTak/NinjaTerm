package ninja.mbedded.ninjaterm.util.textInListUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;

/**
 * Class which is designed to encapsulate a "unit" of streamed text, which is generated by the ANSI escape
 * code parser. This <code>{@link StreamedText}</code> object is then fed into the filter engine,
 * whose output is another <code>{@link StreamedText}</code> object.
 *
 * @author          Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since           2016-09-28
 * @last-modified   2016-09-29
 */
public class StreamedText {

    public String appendText = "";
    public ObservableList<Node> textNodes = FXCollections.observableArrayList();

    /**
     * The states that the method <code>shiftChars</code> can be in as it does it's internal
     * processing. This state is not persistent from call to call.
     */
    private enum ShiftCharsState {
        EXTRACTING_FROM_APPEND_TEXT,
        EXTRACTING_FROM_NODES,
        FINISHED,
    }

    public void shiftCharsTo(StreamedText inputStreamedText, int numChars) {
        StreamedText.shiftChars(inputStreamedText, this, numChars);
    }

    public void removeChars(int numChars) {

        // We can perform a removal by "shifting" the chars to a dummy StreamedText
        // object that is then thrown away

        // Firstly, remove chars from appendText
        if(numChars <= appendText.length()) {
            // appendText has enough chars to completely satisfy this remove operation
            appendText = appendText.substring(numChars, appendText.length());
            return;
        } else {
            // appendText does not have enough chars to completely satisfy this remove operation
            numChars -= appendText.length();
            appendText = "";
        }

        // Loop will be broken when return; is called
        while(true) {

            Text currTextNode = (Text)textNodes.get(0);

            if(numChars <= currTextNode.getText().length()) {
                // Text node has enough chars to completely satisfy this remove operation
                currTextNode.setText(currTextNode.getText().substring(numChars, currTextNode.getText().length()));
                return;
            } else {
                // Text node does not have enough chars to completely satisfy this remove operation
                numChars -= currTextNode.getText().length();
                // Remove node completely
                textNodes.remove(0);
            }
        }
    }

    /**
     * The method extracts the specified number of chars from the input and places them in the output.
     * It extract chars from the "to append" String first, and then starts removing chars from the first of the
     * Text nodes contained within the list.
     *
     * It also shifts any chars from still existing input nodes into the "to append" String
     * as appropriate.
     *
     * @param inputStreamedText
     * @param numChars
     * @return
     */
    public static void shiftChars(StreamedText inputStreamedText, StreamedText outputStreamedText, int numChars) {

        ShiftCharsState shiftCharsState = ShiftCharsState.EXTRACTING_FROM_APPEND_TEXT;

        while(true) {

            switch(shiftCharsState) {
                case EXTRACTING_FROM_APPEND_TEXT:

                    // There might not be any text to append, but rather the text
                    // starts in a fresh node
                    if(inputStreamedText.appendText.length() == 0) {
                        shiftCharsState = ShiftCharsState.EXTRACTING_FROM_NODES;
                        break;
                    }

                    // There are chars to extract
                    if(inputStreamedText.appendText.length() >= numChars) {


                        //outputStreamedText.appendText = inputStreamedText.appendText.substring(0, numChars);
                        outputStreamedText.addTextToStream(inputStreamedText.appendText.substring(0, numChars), AddMethod.APPEND);

                        inputStreamedText.appendText =
                                inputStreamedText.appendText.substring(
                                        numChars, inputStreamedText.appendText.length());

                        shiftCharsState = ShiftCharsState.FINISHED;
                        break;

                    } else {
                        // Need to extract more chars than what the append String has, so extract all, and continue in loop
                        // to extract more from nodes

                        //outputStreamedText.appendText = inputStreamedText.appendText;
                        outputStreamedText.addTextToStream(inputStreamedText.appendText, AddMethod.APPEND);
                        numChars -= inputStreamedText.appendText.length();
                        inputStreamedText.appendText = "";

                        shiftCharsState = ShiftCharsState.EXTRACTING_FROM_NODES;
                        break;
                    }

                case EXTRACTING_FROM_NODES:

                    // We can always work on node 0 since next time around the loop with old node 0
                    // would have been deleted
                    if(inputStreamedText.textNodes.size() == 0) {
                        int x = 2;
                    }
                    Text textNode = (Text)inputStreamedText.textNodes.get(0);

                    if(textNode.getText().length() >= numChars) {
                        // There is enough chars in this node to complete the shift

                        outputStreamedText.addTextToStream(textNode.getText().substring(0, numChars), AddMethod.NEW_NODE);
                        //outputStreamedText.textNodes.add(new Text(textNode.getText().substring(0, numChars)));
                        if(textNode.getText().equals("")) {
                            inputStreamedText.textNodes.remove(textNode);
                        } else {
                            // Although we have finished shifting chars into the output, the lingering chars in the
                            // input need to be moves in the "to append" string, and the node deleted
                            inputStreamedText.appendText = textNode.getText().substring(numChars, textNode.getText().length());
                            inputStreamedText.textNodes.remove(textNode);
                        }

                        shiftCharsState = ShiftCharsState.FINISHED;
                        break;
                    } else {
                        // Node isn't big enough, extract all characters, delete node and move onto the next
                        //outputStreamedText.textNodes.add(new Text(textNode.getText()));
                        outputStreamedText.addTextToStream(textNode.getText(), AddMethod.NEW_NODE);

                        inputStreamedText.textNodes.remove(textNode);
                        numChars -= textNode.getText().length();
                        break;
                    }

                case FINISHED:
                    return;
            }
        }
    }

    /**
     * Used as an input to the <code>addTextToStream()</code> method.
     */
    private enum AddMethod {
        APPEND,
        NEW_NODE
    }

    /**
     * Adds the provided text to the stream, using the given <code>addMethod</code>.
     *
     * @param text
     * @param addMethod     <code>APPEND</code> will add the text to either the <code>appendText</code> or
     *                      last node, <code>NEW_NODE</code> will create a new Text node and insert the text
     *                      into that.
     */
    private void addTextToStream(String text, AddMethod addMethod) {

        switch (addMethod) {
            case APPEND:

                if(textNodes.size() == 0) {
                    appendText += text;
                    return;
                } else {
                    Text textNode = (Text)textNodes.get(textNodes.size() - 1);
                    textNode.setText(textNode.getText() + text);
                    return;
                }

            case NEW_NODE:
                textNodes.add(new Text(text));
                return;

        }
    }

    public String serialize() {

        String output = "";

        output = output + appendText;
        for(Node node: textNodes) {
            output = output + ((Text)node).getText();
        }

        return output;
    }

    /**
     * Calculates the total number of characters stored in this streamed text.
     * @return
     */
    public int numChars() {
        int numChars = 0;
        numChars += appendText.length();
        for(Node node: textNodes) {
            numChars += ((Text)node).getText().length();
        }
        return numChars;
    }

}
