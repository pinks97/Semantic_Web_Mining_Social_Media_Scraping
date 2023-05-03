package com.automation.selenium.reddit;

import com.automation.selenium.utils.Utils;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.openqa.selenium.*;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Vinodh, Sandhya, Ganesh
 * <p>
 * Selenium script to login to reddit and collect posts along with comments,votes, posttitle and posturl for a given list of keywords.
 */

public class RedditApplication {

    static String userName = "";
    static String passWord = "";
    static String redditBaseURL = "https://reddit.com/";
    static String login = "login";
    static String home = "home";
    static int totalScrolls = 10;

    private static String query = "java";
    public static void main(String[] args) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        WebDriver driver = Utils.getDriverWithCustomOptions();

        driver.get(redditBaseURL + "r/"+query+"/");
        getPostsFromSubreddit("r/"+query+"/", driver, 10);

        getPerformance(driver);
        driver.quit();
    }

    private static void getPerformance(WebDriver driver) {
        List<LogEntry> logs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
        for(LogEntry log:logs) {
            for(String key : log.toJson().keySet())
                System.out.println(log.toJson().get(key));
        }

        JavascriptExecutor executor = (JavascriptExecutor) driver;
        long value = (long) executor.executeScript("return window.performance.memory.usedJSHeapSize");
        long valueInMB = value / (1024 * 1024);
        System.out.println("Heap Size: " + valueInMB);
    }

    public static void getPostsFromSubreddit(String subreddit, WebDriver driver, int totalScrolls) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        PrintStream stream
                = new PrintStream(System.out);
        Thread.sleep(5000);
        List<com.automation.selenium.reddit.RedditPost> redditPosts = new ArrayList<>();
        String lastID = "";
        while (totalScrolls > 0) {
            String url = redditBaseURL + subreddit;
            if (!lastID.equals("")) {
                url += "?count=10&after=" + lastID;
            } else {
                url += "?count=10";
            }

            driver.get(url);
            Thread.sleep(5000);
            List<WebElement> postDivs = driver.findElements(By.className("scrollerItem"));
            int idx = 0;
            for (WebElement postDiv : postDivs) {
                if (idx >= 10) {
                    break;
                }
                String id = postDiv.getAttribute("id");
                String postTitle = postDiv.findElement(By.xpath(".//div/div/div/a/div/h3")).getText();
                System.out.println("postTitle " + postTitle);
                postTitle = postTitle.replace(",", "");
                String postUrl = postDiv.findElements(By.xpath(".//div/div/div/a")).get(1).getAttribute("href");
                String votes = postDiv.findElement(By.xpath(".//div/div/div")).getText();

                votes = getVotes(votes);

                String postedBy = getAuthor(postDiv);
                String comments = getComments(postDiv);

                String postedAt = postDiv.findElements(By.xpath(".//div/div/div/div/span")).get(1).getText();

                RedditPost redditPost = new RedditPost(postedBy, postedAt, comments, postTitle, postUrl, votes);
                stream.println(redditPost);

                if (!redditPost.getPostTitle().contentEquals("")) {
                    redditPosts.add(redditPost);
                    lastID = id;
                    idx++;
                }

            }
            totalScrolls--;
        }

        RedditMapper<RedditPost> mappingStrategy = new RedditMapper<>();
        mappingStrategy.setType(RedditPost.class);

        writeToFile(subreddit, redditPosts, mappingStrategy);
    }

    private static String getComments(WebElement postDiv) {
        String comments = postDiv.findElement(By.xpath(".//div/div/div/a/span")).getText();
        comments = comments.replace("\n", "");
        System.out.println(comments);
        if (comments.length() % 2 == 1) {
            StringBuffer sb = new StringBuffer(comments);
            sb = sb.reverse();
            comments = sb.toString();
        }
        if (!comments.toLowerCase().contentEquals("comments")) {
            if (comments.length() >= 2 && comments.length() % 2 == 0) {
                comments = comments.substring((comments.length() / 2), comments.length());
            }
        }
        return comments;
    }

    private static String getAuthor(WebElement postDiv) {
        String postedBy = null;

        try{
            postedBy = postDiv.findElement(By.xpath(".//div/div/div/div/div/a")).getText();
        } catch(NoSuchElementException ex){
            System.err.println("Author not found");
        }
        return postedBy;
    }

    private static String getVotes(String votes) {
        votes = votes.replace("\n", "");
        if (!votes.toLowerCase().contentEquals("vote")) {
            if (votes.length() >= 2) {
                votes = votes.substring((votes.length() / 2), votes.length());
            }
        }
        return votes;
    }

    private static void writeToFile(String subreddit, List<RedditPost> redditPosts, RedditMapper<RedditPost> mappingStrategy) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        String fileName = subreddit.replace("r/", "").replace("/", "") + ".csv";
        Writer writer = new FileWriter(fileName);
        StatefulBeanToCsv<RedditPost> csvwriter = new StatefulBeanToCsvBuilder<RedditPost>(writer)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(true)
                .withMappingStrategy(mappingStrategy)
                .build();
        csvwriter.write(redditPosts);
        writer.close();
    }


}
