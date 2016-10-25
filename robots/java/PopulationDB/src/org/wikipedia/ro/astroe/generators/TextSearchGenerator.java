package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.wikipedia.Wiki;

public class TextSearchGenerator implements Generator {
    private String text;
    private Wiki wiki;
    private List<String> pagesList = null;

    public TextSearchGenerator(Wiki wiki, String text) {
        this.wiki = wiki;
        this.text = text;
    }
    public TextSearchGenerator(Wiki wiki) {
        this.wiki = wiki;
    }

    @Override
    public List<String> getGeneratedTitles() throws IOException {
        if (null == pagesList) {
            String[][] searchResultsArray = wiki.search(text);
            pagesList = new ArrayList<String>();
            for (String[] eachArray : searchResultsArray) {
                pagesList.add(eachArray[0]);
            }
        }
        return pagesList;
    }

    @Override
    public String getDescriptionKey() {
        return "generator.search.description";
    }

    @Override
    public int getNumberOfTextFields() {
        return 1;
    }

    @Override
    public String[] getTextFieldsLabelKeys() {
        return new String[] { "generator.search.searchText" };
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

}
