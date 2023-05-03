package com.automation.selenium.reddit;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class RedditMapper<T> extends ColumnPositionMappingStrategy<T> {
    private final String[] HEADER = new String[]{"postTitle", "postUrl", "votes", "postedBy", "postedAt", "comments"};

    @Override
    public String[] generateHeader(T data) throws CsvRequiredFieldEmptyException {
        super.generateHeader(data);
        return HEADER;
    }
}