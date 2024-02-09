package josef.jilek.open_anime_diffusion;

import com.google.gson.Gson;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class HTTPController {
    Gson gson = new Gson();
    String domain = "194.126.177.32";
    String protocol = "http";
    String port = "37423";
    final String mainEmail = "";
    final String emailAppKey = "";
    //JavaMailSender javaMailSender = getJavaMailSender();
    Pattern findTags = Pattern.compile("([a-z0-9]+)");
    Pattern findLink = Pattern.compile("https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)");

    @PostMapping("/register")
    public boolean register(@RequestBody String newUser) {
        System.out.println("register");
        String[] temp = newUser.split("\n");
        if(DatabaseAction.userExists(temp[0])){
            return false;
        }
        String vereficator = randomAlphanumericString(64);
        String message =
                "Please follow this link to confirm your email: " + protocol + "://" + domain + ":" + port + "/confirmemail/" + vereficator;
        sendGmailMessage(temp[0], "AiDose email confirmation", message);

        DatabaseAction.unconfirmedRegistration(temp[0], temp[1], vereficator);
        return true;
    }
    @GetMapping("/confirmemail/{verificator}")
    public Resource confirmEmail(@PathVariable String verificator) {
        System.out.println("/confirmemail/{verificator}");
        if(DatabaseAction.confirmRegistration(verificator)){
            return new ClassPathResource("static/login.html");
        }else{
            return null;
        }
    }

    @PostMapping("/login")
    public String login(@RequestBody String user) throws SQLException {
        System.out.println("login");
        String token = randomAlphanumericString(128);
        String[] temp = user.split("\n");
        Boolean loginApproved = DatabaseAction.login(temp[0], temp[1], token);
        if(loginApproved == null){
            return "email";
        } else if (loginApproved) {
            return token;
        }
        return "pass";
    }
    @PostMapping("/islogged")
    public boolean isLogged(@RequestBody String credit) throws SQLException {
        System.out.println("islogged");
        String[] temp = credit.split("\n");
        return DatabaseAction.isLogged(temp[0], temp[1]);
    }
    @PostMapping("/logout")
    public void logout(@RequestBody String credit) throws SQLException {
        System.out.println("logout");
        String[] temp = credit.split("\n");
        DatabaseAction.logOut(temp[0], temp[1]);
    }

    @PostMapping("/sendresetemail")
    public boolean sendresetemail(@RequestBody String email) throws SQLException {
        System.out.println("sendresetemail");
        if(!DatabaseAction.userExists(email)){
            return false;
        }
        String vereficator = randomAlphanumericString(64);
        String message =
                "Please follow this link to reset your password: " + protocol + "://" + domain + ":" + port + "/resetpassword.html?verificator=" + vereficator;
        sendGmailMessage(email, "AiDose email password reset", message);
        DatabaseAction.addResetToken(email, vereficator);
        return true;
    }
    @PostMapping("/resetpassword")
    public String resetPassword(@RequestBody String credits) throws SQLException {
        System.out.println("resetpassword");
        String[] temp = credits.split("\n");
        if(!DatabaseAction.userExists(temp[0])){
            return "email";
        }
        if(DatabaseAction.resetPassword(temp[0], temp[1], temp[2])){
            return "success";
        }
        return "outdated";
    }

    //return static content for requests with variable
    @GetMapping("/resetpassword.html")
    public Resource getResetPasswordPage(@RequestParam(required = false) String verificator){
        System.out.println("resetpassword.html");
        return new ClassPathResource("static/signup.html");
    }

    @PostMapping("/deleteaccount")
    public String deleteAccount(@RequestBody String user) {
        System.out.println("deleteaccount");
        String[] temp = user.split("\n");
        Boolean accountDeleted = DatabaseAction.deleteAccount(temp[0], temp[1]);
        if(accountDeleted == null){
            return "email";
        } else if (accountDeleted) {
            return "deleted";
        }
        return "pass";
    }

    public static class AddModel{
        String modelName;
        String modelTags;
        transient LinkedList<String> purifiedModelTags;
        String thumbnailLink;
        String modelType;
        String modelRating;
        String description;

        @Override
        public String toString() {
            return "AddModel{" +
                    "modelName='" + modelName + '\'' +
                    ", modelTags='" + modelTags + '\'' +
                    ", purifiedModelTags=" + purifiedModelTags +
                    ", thumbnailLink='" + thumbnailLink + '\'' +
                    ", modelType='" + modelType + '\'' +
                    ", modelRating='" + modelRating + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }

    @PostMapping("/addmodel")
    public String addModel(@RequestBody String data) throws SQLException, IOException {
        System.out.println("addmodel");
        String[] temp = data.split("\n",3);
        if(!DatabaseAction.isLogged(temp[0], temp[1])){
            return "Not logged in";
        }
        AddModel addModel = gson.fromJson(temp[2], josef.jilek.open_anime_diffusion.HTTPController.AddModel.class);

        if(addModel.modelName.equals("")){
            return "Model name can't be empty.";
        }
        if(addModel.modelTags.equals("")){
            return "Model tags can't be empty.";
        }
        if(!addModel.thumbnailLink.matches(String.valueOf(findLink))){
            return "Thumbnail link is invalid.";
        }
        if(!(addModel.modelType.equals("Checkpoint") ||
                addModel.modelType.equals("LoRa") ||
                addModel.modelType.equals("Textual Inversion") ||
                addModel.modelType.equals("VAE") ||
                addModel.modelType.equals("ControlNet") ||
                addModel.modelType.equals("Other")
        )){
            return "Something bad happened :(";
        }
        if(!(addModel.modelRating.equals("Safe") ||
                addModel.modelRating.equals("Questionable")||
                addModel.modelRating.equals("Explicit")
        )){
            return "Something bad happened :(";
        }

        Matcher matcher = findTags.matcher(addModel.modelTags);
        LinkedList<String> linkedList = new LinkedList<>();
        while (matcher.find()) {
            linkedList.add(matcher.group());
        }
        addModel.purifiedModelTags = linkedList;

        matcher = findLink.matcher(addModel.description);
        if(!matcher.find()){
            return "Description does not contain any download link.";
        }

        DatabaseAction.addModel(temp[0], addModel);

        return "success";
    }

    @GetMapping("/models.html")
    public Resource getModelsPage(@RequestParam(required = false) String mode){
        System.out.println("models.html");
        return new ClassPathResource("static/models.html");
    }

    @PostMapping("/getmodels")
    public ResponseEntity<String> getModels(@RequestBody String body) throws SQLException {
        System.out.println("getmodels");
        CacheControl cacheControl = CacheControl.maxAge(60, TimeUnit.SECONDS)
                .noTransform()
                .mustRevalidate();
        String[] temp = body.split("\n");
        if(temp[0].equals("global")){
            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .body(DatabaseAction.getModels());
        } else if (temp[0].equals("my")) {
            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .body(DatabaseAction.getMyModels(temp[1]));
        } else {
            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .body(DatabaseAction.getLikedModels(temp[1]));
        }
    }

    @GetMapping("/modelpage.html")
    public Resource getModelPage(@RequestParam("modelid") String modelid){
        System.out.println("modelpage.html");
        return new ClassPathResource("static/modelpage.html");
    }

    @PostMapping("/getmodel")
    public ResponseEntity<String> getModel(@RequestBody String body){
        System.out.printf("get model");
        CacheControl cacheControl = CacheControl.maxAge(60, TimeUnit.SECONDS)
                .noTransform()
                .mustRevalidate();
        String[] temp = body.split("\\r?\\n");
        String model = DatabaseAction.getModel(Integer.parseInt(temp[0]), temp[1]);
        System.out.println("databese get model");
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(model);
    }

    @GetMapping("/addmodel.html")
    public Resource getAddModelPage(@RequestParam(required = false) String edit){
        System.out.println("addmodel.html");
        return new ClassPathResource("static/addmodel.html");
    }

    @PostMapping("/upvotemodel")
    public String upvoteModel(@RequestBody String data) throws SQLException {
        System.out.println("upvotemodel");
        String[] temp = data.split("\n");
        if(!DatabaseAction.isLogged(temp[1], temp[2])){
            return "login";
        }
        DatabaseAction.upvoteModel(temp[0], temp[1]);
        return "success";
    }

    @PostMapping("/downvotemodel")
    public String downvoteModel(@RequestBody String data) throws SQLException {
        System.out.println("downvoemodel");
        String[] temp = data.split("\n");
        if(!DatabaseAction.isLogged(temp[1], temp[2])){
            return "login";
        }
        DatabaseAction.downvoteModel(temp[0], temp[1]);
        return "success";
    }

    @PostMapping("/addtolikedmodel")
    public String addToLikedModel(@RequestBody String data) throws SQLException {
        System.out.println("addtoliked");
        String[] temp = data.split("\n");
        if(!DatabaseAction.isLogged(temp[1], temp[2])){
            return "login";
        }
        DatabaseAction.addToLikedModel(temp[0], temp[1]);
        return "success";
    }

    @PostMapping("/deletemodel")
    public String deleteModel(@RequestBody String data) throws SQLException, IOException {
        System.out.println("deletamodel");
        String[] temp = data.split("\n");
        if(!DatabaseAction.isLogged(temp[1], temp[2])){
            return "login";
        }
        DatabaseAction.deleteModel(temp[0], temp[1]);
        return "success";
    }

    @PostMapping("/editmodel")
    public String editModel(@RequestBody String data) throws SQLException, IOException {
        System.out.println("editmodel");
        String[] temp = data.split("\n",4);
        if(!DatabaseAction.isLogged(temp[0], temp[1])){
            return "Not logged in";
        }
        AddModel addModel = gson.fromJson(temp[3], josef.jilek.open_anime_diffusion.HTTPController.AddModel.class);

        if(addModel.modelName.equals("")){
            return "Model name can't be empty.";
        }
        if(addModel.modelTags.equals("")){
            return "Model tags can't be empty.";
        }
        if(!addModel.thumbnailLink.matches(String.valueOf(findLink))){
            return "Thumbnail link is invalid.";
        }
        if(!(addModel.modelType.equals("Checkpoint") ||
                addModel.modelType.equals("LoRa") ||
                addModel.modelType.equals("Textual Inversion") ||
                addModel.modelType.equals("VAE") ||
                addModel.modelType.equals("ControlNet") ||
                addModel.modelType.equals("Other")
        )){
            return "Something bad happened :(";
        }
        if(!(addModel.modelRating.equals("Safe") ||
                addModel.modelRating.equals("Questionable")||
                addModel.modelRating.equals("Explicit")
        )){
            return "Something bad happened :(";
        }

        Matcher matcher = findTags.matcher(addModel.modelTags);
        LinkedList<String> linkedList = new LinkedList<>();
        while (matcher.find()) {
            linkedList.add(matcher.group());
        }
        addModel.purifiedModelTags = linkedList;

        matcher = findLink.matcher(addModel.description);
        if(!matcher.find()){
            return "Description does not contain any download link.";
        }

        DatabaseAction.editModel(temp[0], temp[2], addModel);

        return "success";
    }

    @PostMapping("/admindeletemodel")
    public String adminDeleteModel(@RequestBody String data) throws SQLException, IOException {
        System.out.println("admindeletemodel");
        String[] temp = data.split("\n");
        if(!DatabaseAction.adminIsLogged(temp[0], temp[1])){
            return "login";
        }
        DatabaseAction.adminDeleteModel(temp[2]);
        return "success";
    }
    @PostMapping("/admindeleteaccount")
    public String adminDeleteAccount(@RequestBody String data) throws SQLException, IOException {
        System.out.println("admindeleteasccount");
        String[] temp = data.split("\n");
        if(!DatabaseAction.adminIsLogged(temp[1], temp[2])){
            return "login";
        }
        DatabaseAction.adminDeleteAccount(temp[2]);
        return "success";
    }

    @GetMapping("/")
    public Resource getDefault(){
        System.out.println("root");
        return new ClassPathResource("static/models.html");
    }

    private String randomString(int length) {
        byte[] array = new byte[length];
        new Random().nextBytes(array);

        return new String(array, StandardCharsets.UTF_8);
    }
    public String randomAlphanumericString(int length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public void sendGmailMessage(String receiverEmail, String subject, String text){
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); //TLS

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mainEmail, emailAppKey);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mainEmail));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(receiverEmail)
            );
            message.setSubject(subject);
            message.setText(text);

            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /*public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername(email);
        mailSender.setPassword("");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }*/
}
