package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.Account;
import org.openqa.selenium.By;import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@CommandLine.Command(name = "CheckMarks", version = "CheckMarks 1.0", mixinStandardHelpOptions = true)
public class CheckMarks implements Runnable {

    private static final Logger logger
            = LoggerFactory.getLogger(CheckMarks.class);
    @CommandLine.Option(names = "-f",paramLabel = "JSON FILE")
    File jsonFile = new File("./secret.json");
    public static final String DIR_STRING = "./check_marks";
    public static final Path DIR = Path.of(DIR_STRING);
    public static final File FILE = Path.of(DIR_STRING + "/marks.html").toFile();
    public static final String BROWSER_CONTAINER_NAME = "firefox_service";

    public Map<String, Account> accountMap = new HashMap<>();

    @Override
    public void run() {
        initAccountMap();
        /* Start up remote web driver and give it buffer time */
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        RemoteWebDriver remoteWebDriver = null;
        try {
            remoteWebDriver = new RemoteWebDriver(new URL("http://"+BROWSER_CONTAINER_NAME+":4444"),firefoxOptions);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        login("https://apps.ecs.vuw.ac.nz/cgi-bin/studentmarks?current-year=1",accountMap.get("ecs"),remoteWebDriver);


        /* Create file if it doesn't exist */
        if (!fileExists()) {
            File file = new File(DIR_STRING + "/marks.html");
            boolean newFile = false;
            try {
                newFile = file.createNewFile();} catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!newFile) {
                throw new RuntimeException("couldn't create new file");
            }
            try {
                Files.writeString(FILE.toPath(), remoteWebDriver.getPageSource());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        boolean sameContents = false;
        try {
            sameContents = Files.readString(FILE.toPath()).equals(remoteWebDriver.getPageSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /* Send email as websites returned html is different */
        if (!sameContents) {
            makeNotif(this);
            try {
                Files.writeString(FILE.toPath(), remoteWebDriver.getPageSource());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            remoteWebDriver.quit();
            System.exit(0);
        }
        remoteWebDriver.quit();
    }

    /**
     * Set the accounts made in the secret.json file
     */
    public void initAccountMap() {
        /* Get accounts from file */
        String jsonString = null;
        try {
            jsonString = Files.readString(jsonFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        TypeToken<List<Account>> listType = new TypeToken<List<Account>>(){};
        List<Account> accounts = gson.fromJson(jsonString, listType);
        accounts.forEach(e->{
            accountMap.put(e.type(),e);
        });
    }

    /**
     * Run checkmarks cli
     * @param args
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CheckMarks()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Finds username and password elements and fills them with given accounts values.
     * @param site
     * @param acc
     * @param rwd
     */
    public void login(String site, Account acc, RemoteWebDriver rwd) {
        rwd.get(site);
        WebElement usernameEl = rwd.findElement(By.name("username"));
        usernameEl.sendKeys(acc.username());
        WebElement passwordEl = rwd.findElement(By.name("password"));
        passwordEl.sendKeys(acc.password());
        WebElement login = rwd.findElement(By.name("login"));
        login.click();
    }

    /**
     * Check file Exists on host where running
     * @return boolean
     */
    public static boolean fileExists() {
        if (!DIR.toFile().exists()) {
            boolean mkdir = DIR.toFile().mkdir();
            if (!mkdir) {
                throw new RuntimeException("can't make dir");
            }
        }
        return FILE.exists();
    }
    /**
     * Send email to myself to notify.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    /**
     * Send email
     * @param checkMarks
     */
    public static void makeNotif(CheckMarks checkMarks) {
        String host = "smtp.gmail.com";
        Account sender = checkMarks.accountMap.get("sender");
        final String username = sender.username();
        final String password = sender.password();

        String receiverUsername = checkMarks.accountMap.get("receiver").username();

        //Get the session object
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable",true);


        Session session = Session.getDefaultInstance(props, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        //Compose the message
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiverUsername));
            message.setSubject("marks changed");
            message.setText("Notification that marks page has changed.");

            //send the message
            Transport.send(message);

            System.out.println("message sent successfully...");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}