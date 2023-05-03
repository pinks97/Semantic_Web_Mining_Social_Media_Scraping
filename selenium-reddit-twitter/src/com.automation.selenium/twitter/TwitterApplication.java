package com.automation.selenium.twitter;

import com.automation.selenium.utils.Utils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static com.automation.selenium.utils.Utils.scroll;


/**
 * @author Sandhya, Ganesh, Vinodh
 * <p>
 * Selenium script to login to twitter and collect tweets along with comments,retweet and likes count for a given list of keywords.
 */

public class TwitterApplication {


    static String mobileTwitterBaseURL = "https://mobile.twitter.com/";

    static int totalScrolls = 10;
    static String mobileTwitterSearchStart = "https://mobile.twitter.com/search?q=";
    static String mobileTwitterSearchEnd = "&src=typed_query";

    static String keyWords[] = new String[]{"Java"};
    static String userProfiles[] = new String[]{"JoeBiden"};


    public static void main(String[] args) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        WebDriver driver = Utils.getDriverWithCustomOptions();

        Thread.sleep(5000);

        for (String keyWord : keyWords) {
            getTweetByKeywords(driver, keyWord, totalScrolls);
        }


        for (String user : userProfiles) {
            getAllTweetsOfAUser(driver, user, totalScrolls);
        }


        getPerformanceStats(driver);
        driver.quit();

    }

    private static void getPerformanceStats(WebDriver driver) {
        List<LogEntry> logs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
        for (LogEntry log : logs) {
            for (String key : log.toJson().keySet())
                System.out.println(log.toJson().get(key));
        }
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        long value = (long) executor.executeScript("return window.performance.memory.usedJSHeapSize");
        long valueInMB = value / (1024 * 1024);
        System.out.println("Heap Size: " + valueInMB);
    }

    /**
     * Gets all the tweets for a given hashtag/keyword
     *
     * @param driver
     * @param keyWord
     * @param totalScrolls
     * @throws InterruptedException
     * @throws CsvRequiredFieldEmptyException
     * @throws CsvDataTypeMismatchException
     * @throws IOException
     */

    public static void getTweetByKeywords(WebDriver driver, String keyWord, int totalScrolls) throws InterruptedException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException {
        List<Tweet> tweetList = new ArrayList<Tweet>();
        String url = new StringBuilder(mobileTwitterSearchStart).append(keyWord).append(mobileTwitterSearchEnd).toString();
        System.out.println(url);
        driver.get(url);

        extractTweets(totalScrolls, driver, tweetList, keyWord + ".csv");


    }
    /**
     * Gets all the tweets for a given user
     *
     * @param driver
     * @param twitterUsername
     * @param totalScrolls
     * @throws InterruptedException
     * @throws IOException
     * @throws CsvRequiredFieldEmptyException
     * @throws CsvDataTypeMismatchException
     */

    public static void getAllTweetsOfAUser(WebDriver driver, String twitterUsername, int totalScrolls) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        String profileURL = mobileTwitterBaseURL + twitterUsername;
        driver.get(profileURL);
        System.out.println(profileURL);
        List<Tweet> outputTweetList = new ArrayList<>();

        extractTweets(totalScrolls, driver, outputTweetList, twitterUsername + ".csv");

    }

    private static void extractTweets(int totalScrolls, WebDriver driver, List<Tweet> tweetList, String keyWord) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        while (totalScrolls > 0) {

            Thread.sleep(5000);


            List<WebElement> tweetElements = driver.findElements(By.cssSelector("[data-testid='tweet']"));
            for (WebElement tweetElement : tweetElements) {
                String author;
                try {
                    author = tweetElement.findElement(By.cssSelector("[data-testid='tweet'] a[href^='/'] > div > span")).getText();

                } catch (Exception ex) {
                    author = "No author";
                }

                String retweets;

                try {
                    retweets = tweetElement.findElement(By.cssSelector("[data-testid='tweet'] div[data-testid='retweet']")).getText();

                    if(retweets.length() == 0) retweets = "0";
                } catch (Exception ex) {
                    retweets = "0";
                }

                String content;

                try {
                    content = tweetElement.findElement(By.cssSelector("[data-testid='tweet'] div[dir='auto']")).getText();

                } catch (Exception ex) {
                    content = "";
                }

                String tweetLink;

                try {
                    tweetLink = tweetElement.findElements(By.cssSelector("[data-testid='tweet'] a[href^='/'][role='link']")).get(3).getAttribute("href");

                } catch (Exception ex) {

                    tweetLink = "";
                }

                String tweetId;

                try {
                    String[] arr = tweetLink.split("/");
                    tweetId = arr[arr.length - 1];
                } catch (Exception ex) {
                    tweetId = "";
                }

                String timePosted;

                try {
                    timePosted = tweetElement.findElement(By.cssSelector("[data-testid='tweet'] time")).getAttribute("datetime");

                } catch (Exception ex) {
                    timePosted = "";
                }

                String likes;

                try {
                    likes = tweetElement.findElement(By.cssSelector("[data-testid='tweet'] div[data-testid='like']")).getText();
                    if(likes.length()==0) likes = "0";
                } catch (Exception ex) {
                    likes = "0";
                }

                String comments;

                try {

                    comments = tweetElement.findElement(By.cssSelector("[data-testid='reply'] div > span")).getText();

                    if (comments.length() == 0) comments = "0";

                } catch (Exception ex) {
                    comments = "0";
                }


                System.out.println("ID: " + tweetId);
                System.out.println("Link: " + tweetLink);
                System.out.println("Author: " + author);
                System.out.println("Time Posted: " + timePosted);
                System.out.println("Retweets: " + retweets);
                System.out.println("Likes: " + likes);
                System.out.println("Content: " + content);
                System.out.println("Comment count: " + comments);
                System.out.println("-------------");
                Tweet tweet = new Tweet();
                tweet.setId(tweetId);
                tweet.setBody(content);
                tweet.setTime(timePosted);
                tweet.setLink(tweetLink);
                tweet.setUsername(author);
                tweet.setLikeCount(likes);
                tweet.setRetweetCount(retweets);
                tweet.setCommentCount(comments);
                tweetList.add(tweet);
            }
            scroll(driver);
            totalScrolls--;

        }
        TwitterMapper<Tweet> mappingStrategy = new TwitterMapper<>();
        mappingStrategy.setType(Tweet.class);

        String fileName = keyWord;
        Writer writer = new FileWriter(fileName);
        StatefulBeanToCsv<Tweet> csvwriter = new StatefulBeanToCsvBuilder<Tweet>(writer)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(true)
                .withMappingStrategy(mappingStrategy)
                .build();
        csvwriter.write(tweetList);
        writer.close();
    }





}
