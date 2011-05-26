/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pdftestextractor;

import com.itextpdf.text.pdf.parser.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author acipu
 */
public class MyTextExtractionStrategy implements TextExtractionStrategy {

    /** set to true for debugging */
    static boolean DUMP_STATE = false;
    
    /** a summary of all found text */
    private final List<TextChunk> locationalResult = new ArrayList<TextChunk>();
    
    private final String[] templateParams = {"\n| Cod = ", "\n| Denumire = ", "\n| Localitate = ", "\n| Adresă = ", "\n| Datare = "};
    
    private static ArrayList<Integer> columnOffset = new ArrayList<Integer>();

    /**
     * Creates a new text extraction renderer.
     */
    public MyTextExtractionStrategy() {
    }

    /**
     * @see com.itextpdf.text.pdf.parser.RenderListener#beginTextBlock()
     */
    public void beginTextBlock(){
    }

    /**
     * @see com.itextpdf.text.pdf.parser.RenderListener#endTextBlock()
     */
    public void endTextBlock(){
    }

    /**
     * Returns the result so far.
     * @return  a String with the resulting text.
     */
    public String getResultantText(){
        
        //Collections.sort(locationalResult);
        groupChunks(locationalResult);
        
        if (DUMP_STATE) {
            dumpState();
        }
        
        StringBuffer sb = new StringBuffer();
        TextChunk lastChunk = null;
        for (TextChunk chunk : locationalResult) {
            if (lastChunk != null){
                if (chunk.sameColumn(lastChunk)){
                    sb.append(needSpace(lastChunk, chunk));

                    sb.append(chunk.text);
                } else {
                    if(chunk.column == 0)
                    {
                        sb.append("\n}}\n{{ElementLMI");
                    }
                    sb.append(templateParams[chunk.column]);
                    sb.append(chunk.text);
                }
            } else {
                sb.append("{{ElementLMI");
                sb.append(templateParams[chunk.column]);
                sb.append(chunk.text);
            }
            lastChunk = chunk;
        }
        
        String result = sb.toString();
        result = result.replace("MONITORUL OFICIAL AL ROMÂNIEI, PARTEA I, Nr. 670 bis/1.X.2010", "");
        result = result.replace("MINISTERUL CULTURII ŞI PATRIMONIULUI NAŢIONAL", "");
        result = result.replace("INSTITUTUL NAŢIONAL AL PATRIMONIULUI", "");
        result = result.replace("", "\"");

        return result;

    }

    /** Used for debugging only */
    private void dumpState(){
        for (Iterator<TextChunk> iterator = locationalResult.iterator(); iterator.hasNext(); ) {
            TextChunk location = (TextChunk) iterator.next();
            
            location.printDiagnostics();
            
            System.out.println();
        }
        
    }
    
    private boolean chunksInDifferentColumns(TextChunk thisChunk, TextChunk nextChunk) {
        if(nextChunk.distanceFromEndOf(thisChunk) >= thisChunk.charSpaceWidth &&
           (thisChunk.column >= columnOffset.size() - 1 ||
           columnOffset.contains((int)Math.floor(nextChunk.distParallelStart))))
            return true;
        return false;
    }
    
    /**
     * 
     * @see com.itextpdf.text.pdf.parser.RenderListener#renderText(com.itextpdf.text.pdf.parser.TextRenderInfo)
     */
    public void renderText(TextRenderInfo renderInfo) {
    	LineSegment segment = renderInfo.getBaseline();
        //bold text contains table header, so just throw it out
        if(!renderInfo.getFont().getPostscriptFontName().contains("Bold"))
        {
            TextChunk location = new TextChunk(renderInfo.getText(), segment.getStartPoint(), segment.getEndPoint(), 4/*renderInfo.getSingleSpaceWidth()*/);
            if(location.orientationMagnitude != 0)//non-vertical text, should be horisontal
                locationalResult.add(location);        
        }
    }

    private void groupChunks(List<TextChunk> locationalResult) {
        Collections.sort(locationalResult);
        if(true){
            locationalResult.get(0).column = 0;
            columnOffset.add((int)Math.abs(locationalResult.get(0).distParallelStart));
            for(int i = 0; i < locationalResult.size() - 1; i++)
            {
                TextChunk thisChunk = locationalResult.get(i);
                TextChunk nextChunk = locationalResult.get(i+1);

                if (thisChunk.sameLine(nextChunk) && 
                        !chunksInDifferentColumns(thisChunk, nextChunk)) {
                    nextChunk.column = thisChunk.column;
                }
                else if (thisChunk.sameLine(nextChunk)) {//new column
                    int index = columnOffset.indexOf((int)Math.floor(nextChunk.distParallelStart));
                    if(index > -1)
                        nextChunk.column = index;
                    else {
                        System.out.println("Unknown column for offset " + 
                                nextChunk.distParallelStart);
                        nextChunk.column = thisChunk.column + 1;
                    }
                    if(nextChunk.column < columnOffset.size())
                        columnOffset.set(nextChunk.column, 
                                (int)Math.floor(nextChunk.distParallelStart));
                    else
                        columnOffset.add((int)Math.floor(nextChunk.distParallelStart));
                }
                else { //new line
                    int index = columnOffset.indexOf((int)Math.floor(nextChunk.distParallelStart));
                    if(index > -1)
                        nextChunk.column = index;
                    else {
                        System.out.println("Unknown column for offset" + 
                                nextChunk.distParallelStart);
                        nextChunk.column = 0;
                    }
                }
            }
        }
    }

    private String needSpace(TextChunk lastChunk, TextChunk nextChunk) {
        float dist = nextChunk.distanceFromEndOf(lastChunk);
        
        if(!lastChunk.sameLine(nextChunk))
            return " ";
                    
        if (dist < -nextChunk.charSpaceWidth)
            return " ";

        // we only insert a blank space if the trailing character of the previous string wasn't a space, and the leading character of the current string isn't a space
        else if (dist > nextChunk.charSpaceWidth/2.0f && nextChunk.text.charAt(0) != ' ' && lastChunk.text.charAt(lastChunk.text.length()-1) != ' ')
        {
            return " ";
        }
        
        return "";
    }
    


    /**
     * Represents a chunk of text, it's orientation, and location relative to the orientation vector
     */
    private static class TextChunk implements Comparable<TextChunk>{
        /** the text of the chunk */
        final String text;
        /** the starting location of the chunk */
        final Vector startLocation;
        /** the ending location of the chunk */
        final Vector endLocation;
        /** unit vector in the orientation of the chunk */
        final Vector orientationVector;
        /** the orientation as a scalar for quick sorting */
        final int orientationMagnitude;
        /** perpendicular distance to the orientation unit vector (i.e. the Y position in an unrotated coordinate system)
         * we round to the nearest integer to handle the fuzziness of comparing floats */
        final int distPerpendicular;
        /** distance of the start of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
        final float distParallelStart;
        /** distance of the end of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system) */
        final float distParallelEnd;
        /** the width of a single space character in the font of the chunk */
        final float charSpaceWidth;
        /** the column this chunk belongs to */
        public int column;
        
        public TextChunk(String string, Vector startLocation, Vector endLocation, float charSpaceWidth) {
            this.text = string;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
            this.charSpaceWidth = charSpaceWidth;
            this.column = -1;
            
            orientationVector = endLocation.subtract(startLocation).normalize();
            orientationMagnitude = (int)(Math.atan2(orientationVector.get(Vector.I2), orientationVector.get(Vector.I1))*1000);

            // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
            // the two vectors we are crossing are in the same plane, so the result will be purely
            // in the z-axis (out of plane) direction, so we just take the I3 component of the result
            Vector origin = new Vector(0,0,1);
            distPerpendicular = (int)(startLocation.subtract(origin)).cross(orientationVector).get(Vector.I3);

            distParallelStart = orientationVector.dot(startLocation);
            distParallelEnd = orientationVector.dot(endLocation);
        }

        private void printDiagnostics(){
            System.out.println("Text (@" + startLocation + " -> " + endLocation + "): " + text);
            System.out.println("orientationMagnitude: " + orientationMagnitude);
            System.out.println("distPerpendicular: " + distPerpendicular);
            System.out.println("distParallelStart: " + distParallelStart);
            System.out.println("distParallelEnd: " + distParallelEnd);
            System.out.println("column: " + column);
        }
        
        /**
         * @param as the location to compare to
         * @return true is this location is on the the same line as the other
         */
        public boolean sameLine(TextChunk as){
            if (orientationMagnitude != as.orientationMagnitude) return false;
            if (compareInts(distPerpendicular, as.distPerpendicular) != 0)
                return false;
            return true;
        }

        /**
         * Computes the distance between the end of 'other' and the beginning of this chunk
         * in the direction of this chunk's orientation vector.  Note that it's a bad idea
         * to call this for chunks that aren't on the same line and orientation, but we don't
         * explicitly check for that condition for performance reasons.
         * @param other
         * @return the number of spaces between the end of 'other' and the beginning of this chunk
         */
        public float distanceFromEndOf(TextChunk other){
            float distance = distParallelStart - other.distParallelEnd;
            return distance;
        }
        
        /**
         * Compares based on orientation, perpendicular distance, then parallel distance
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(TextChunk rhs) {
            if (this == rhs) return 0; // not really needed, but just in case
            
            int rslt;
            rslt = compareInts(orientationMagnitude, rhs.orientationMagnitude);
            if (rslt != 0) return rslt;

            //different line, same column
            /*if(Math.abs(distPerpendicular - rhs.distPerpendicular) < 22 &&
                    distParallelStart < rhs.distParallelStart)
                return -1;*/
            
            //TODO: we also need to take into account the parallel distance
            // it should come before the previous chunk if the distance is significant
            
            rslt = compareInts(distPerpendicular, rhs.distPerpendicular);
            if (rslt != 0) return rslt;

            // note: it's never safe to check floating point numbers for equality, and if two chunks
            // are truly right on top of each other, which one comes first or second just doesn't matter
            // so we arbitrarily choose this way.
            rslt = distParallelStart < rhs.distParallelStart ? -1 : 1;

            return rslt;
        }

        /**
         *
         * @param int1
         * @param int2
         * @return comparison of the two integers
         */
        private static int compareInts(int int1, int int2){
            if(Math.abs(int1 - int2) <= 1)
                return 0;
            else 
                return int1 < int2 ? -1 : 1;
        }

        private boolean sameColumn(TextChunk as) {
            if(column != as.column)
                return false;
            return true;
        }

        
    }

    /**
     * no-op method - this renderer isn't interested in image events
     * @see com.itextpdf.text.pdf.parser.RenderListener#renderImage(com.itextpdf.text.pdf.parser.ImageRenderInfo)
     * @since 5.0.1
     */
    public void renderImage(ImageRenderInfo renderInfo) {
        // do nothing
    }

}