package josef.jilek.open_anime_diffusion;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

//-- we don't know how to generate root <with-no-name> (class Root) :(
//create table admin
//(
//    key1 TEXT not null,
//    key2 TEXT not null
//);
//
//create table models
//(
//    id                integer                             not null
//        constraint id
//            primary key autoincrement,
//    rating            TEXT                                not null,
//    type              TEXT                                not null,
//    tags              TEXT                                not null,
//    thumbnail         TEXT                                not null,
//    descriptionFileID INTEGER                             not null,
//    date              TIMESTAMP default CURRENT_TIMESTAMP not null,
//    liked             INTEGER   default 0                 not null,
//    disliked          INTEGER   default 0                 not null,
//    email             TEXT                                not null,
//    name              TEXT                                not null
//);
//
//create unique index models_id_uindex
//    on models (id desc);
//
//create table unconfirmedUsers
//(
//    email       TEXT                                not null,
//    hash        TEXT                                not null,
//    verificator TEXT                                not null
//        constraint unconfirmedUsers_pk
//            primary key,
//    date        TIMESTAMP default CURRENT_TIMESTAMP not null
//);
//
//create table users
//(
//    email               TEXT                                not null
//        constraint email
//            primary key,
//    hash                TEXT                                not null,
//    liked               TEXT,
//    disliked            TEXT,
//    date                TIMESTAMP default CURRENT_TIMESTAMP not null,
//    token               TEXT,
//    tokenTimestamp      integer,
//    resetToken          TEXT,
//    resetTokenTimestamp TEXT,
//    models              TEXT,
//    favourited          TEXT
//);

public class DatabaseAction {
    //private static final ReentrantLock lock = new ReentrantLock(true);
    static final Connection conn;

    static {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:./main.sqlite");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Gson gson = new Gson();
    static boolean modelsChange = true;
    static String allModelsCached;

    //static Statement statement;

    //static {
    //    try {
    //        statement = conn.createStatement();
    //    } catch (SQLException e) {
    //        throw new RuntimeException(e);
    //    }
    //}

    public static void register(String email, String hash){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("INSERT INTO users(email,hash) VALUES(?,?)");
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, hash);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL register failed: email=" + email + " hash=" + hash);
            e.printStackTrace(System.err);
        }

    }

    public static boolean userExists(String email) {
        PreparedStatement preparedStatement = null;
        boolean out;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            preparedStatement.setString(1, email);
            synchronized (conn){
                ResultSet standardRS = preparedStatement.executeQuery();
                out = standardRS.next();
            }
        } catch (SQLException e) {
            System.err.println("SQL userExist failed: email=" + email);
            e.printStackTrace(System.err);
            out = false;
        }

        return out;
    }

    public static boolean confirmRegistration(String verificator){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM unconfirmedUsers WHERE verificator=?");
            preparedStatement.setString(1, verificator);
            String email;
            String hash;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return false;
                }
                email = resultSet.getString("email");
                hash = resultSet.getString("hash");
            }
            register(email, hash);
            preparedStatement = conn.prepareStatement("DELETE FROM unconfirmedUsers WHERE verificator=?");
            preparedStatement.setString(1, verificator);
            preparedStatement.execute();
        } catch (SQLException e) {
            System.err.println("SQL confirmRegistration failed: verificator=" + verificator);
            e.printStackTrace(System.err);
            return false;
        }

        return true;
    }

    public static void unconfirmedRegistration(String email, String hash, String vereficator){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("INSERT INTO unconfirmedUsers(email,hash,verificator) VALUES(?,?,?)");
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, DigestUtils.sha256Hex(hash));
            preparedStatement.setString(3, vereficator);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL unconfirmedRegistration failed: email=" + email + " hash=" + hash + " verificator=" + vereficator);
            e.printStackTrace(System.err);
        }

    }

    public static Boolean login(String email, String hash, String token){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT hash FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String rsHash;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return null;
                }
                rsHash = resultSet.getString("hash");
            }

            if(rsHash.equals(DigestUtils.sha256Hex(hash))){
                PreparedStatement statement = conn.prepareStatement("UPDATE users SET token=?, tokenTimestamp=? WHERE email=?");
                statement.setString(1, token);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, email);
                statement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL login failed: email=" + email + " hash=" + hash + " token=" + token);
            e.printStackTrace(System.err);
        }

        return false;
    }

    public static boolean isLogged(String email, String token){
        PreparedStatement preparedStatement = null;
        boolean out;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return false;
                }
                out = resultSet.getString("token").equals(token) && resultSet.getLong("tokenTimestamp") > System.currentTimeMillis() - 7 * 86400000;
            }

        } catch (SQLException e) {
            System.err.println("SQL isLogged failed: email=" + email + " token=" + token);
            e.printStackTrace(System.err);
            out = false;
        }

        return out;
    }

    public static void logOut(String email, String token) {
        if(isLogged(email, token)){
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = conn.prepareStatement("UPDATE users SET tokenTimestamp=? WHERE email=?");
                preparedStatement.setLong(1, 0);
                preparedStatement.setString(2, email);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                System.err.println("SQL logOut failed: email=" + email + " token=" + token);
                e.printStackTrace(System.err);
            }

        }
    }

    public static void addResetToken(String email, String vereficator){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("UPDATE users SET resetToken=?, resetTokenTimestamp=? WHERE email=?");
            preparedStatement.setString(1, vereficator);
            preparedStatement.setLong(2, System.currentTimeMillis());
            preparedStatement.setString(3, email);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL addResetToken failed: email=" + email + " vereficator=" + vereficator);
            e.printStackTrace(System.err);
        }

    }

    public static boolean resetPassword(String email, String token, String hash){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            boolean passwordResetAllowed;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return false;
                }
                passwordResetAllowed = resultSet.getString("resetToken").equals(token) && resultSet.getLong("tokenTimestamp") > System.currentTimeMillis() - 86400000;
            }

            if(passwordResetAllowed){
                PreparedStatement statement = conn.prepareStatement("UPDATE users SET hash=? WHERE email=?");
                statement.setString(1, hash);
                statement.setString(2, email);
                statement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL resetPassword failed: email=" + email + " hash=" + hash + " token=" + token);
            e.printStackTrace(System.err);
        }

        return false;
    }

    public static Boolean deleteAccount(String email, String hash) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT hash FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String rsHash;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return null;
                }
                rsHash = resultSet.getString("hash");
            }

            if(rsHash.equals(DigestUtils.sha256Hex(hash))){
                adminDeleteAccount(email);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL deleteAccount failed: email=" + email + " hash=" + hash);
            e.printStackTrace(System.err);
        }

        return false;
    }

    public static void addModel(String email, HTTPController.AddModel addModel){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("INSERT INTO models(rating,type,tags,thumbnail,descriptionFileID,email,name) VALUES(?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, addModel.modelRating);
            preparedStatement.setString(2, addModel.modelType);
            preparedStatement.setString(3, String.join(" ", addModel.purifiedModelTags));
            preparedStatement.setString(4, addModel.thumbnailLink);

            long descriptionFileID = System.currentTimeMillis();
            File descriptionFile = new File("./modelDescriptions/" + descriptionFileID);
            descriptionFile.createNewFile();
            Files.writeString(descriptionFile.toPath(), addModel.description);
            preparedStatement.setLong(5, descriptionFileID);
            preparedStatement.setString(6, email);
            preparedStatement.setString(7, addModel.modelName);
            preparedStatement.executeUpdate();

            int modelID;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.getGeneratedKeys();
                if(!resultSet.next()){
                    return;
                }
                modelID = resultSet.getInt(1);
            }

            preparedStatement = conn.prepareStatement("SELECT models FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String models;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                models = resultSet.getString("models");
            }

            preparedStatement = conn.prepareStatement("UPDATE users SET models=? WHERE email=?");
            if(models == null || models.equals("")){
                preparedStatement.setString(1, String.valueOf(modelID));
            }else{
                preparedStatement.setString(1, models + " " + modelID);
            }
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();
            modelsChange = true;
        } catch (SQLException | IOException e) {
            System.err.println("SQL addModel failed: email=" + email + " model=" + addModel);
            e.printStackTrace(System.err);
        }
    }

    public static String getMyModels(String email){
        PreparedStatement preparedStatement = null;
        LinkedList<Models> modelsLinkedList = new LinkedList<>();
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE email=?");
            preparedStatement.setString(1, email);

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()){
                    modelsLinkedList.add(new Models(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getString("rating"),
                            resultSet.getString("type"),
                            resultSet.getString("tags"),
                            resultSet.getString("thumbnail"),
                            resultSet.getLong("date"),
                            resultSet.getInt("liked"),
                            resultSet.getInt("disliked")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL getMyModels failed: email=" + email);
            e.printStackTrace(System.err);
        }

        return gson.toJson(modelsLinkedList);
    }

    public static String getLikedModels(String email) {
        PreparedStatement preparedStatement = null;
        LinkedList<Models> modelsLinkedList = new LinkedList<>();
        try {
            preparedStatement = conn.prepareStatement("SELECT favourited FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String liked = "";
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return "email does not exist";
                }
                liked = resultSet.getString("favourited");

            }

            if(liked != null && !liked.equals("")){
                preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id in (" + liked.replace(" ", ",") + ")");

                synchronized (conn){
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()){
                        modelsLinkedList.add(new Models(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("rating"),
                                resultSet.getString("type"),
                                resultSet.getString("tags"),
                                resultSet.getString("thumbnail"),
                                resultSet.getLong("date"),
                                resultSet.getInt("liked"),
                                resultSet.getInt("disliked")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL getLikedModels failed: email=" + email);
            e.printStackTrace(System.err);
        }

        return gson.toJson(modelsLinkedList);
    }

    public static void upvoteModel(String modelID, String email){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String liked;
            String disliked;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                liked = resultSet.getString("liked");
                disliked = resultSet.getString("disliked");
            }

            String[] likedArr = new String[0];
            if(liked != null && !liked.equals("")){
                likedArr = liked.split(" ");
            }
            boolean unlike = false;
            StringBuilder stringBuilder = new StringBuilder();
            //if is already liked than remove
            if(likedArr.length != 0){
                if(!likedArr[0].equals(modelID)){
                    stringBuilder.append(likedArr[0]);
                }else if(likedArr.length > 1){
                    stringBuilder.append(likedArr[1]);
                    unlike = true;
                }
            }
            for (int i = 1; i < likedArr.length; i++) {
                if(!likedArr[i].equals(modelID)){
                    stringBuilder.append(" ");
                    stringBuilder.append(likedArr[i]);
                }else{
                    unlike = true;
                }
            }
            liked = stringBuilder.toString();

            //if was not unliked than append to liked
            int likedCount;
            if(!unlike){
                if(liked.equals("")){
                    liked = modelID;
                }else{
                    liked += " " + modelID;
                }
                likedCount = 1;
            }else{
                likedCount = -1;
            }

            //if liked than remove from disliked
            StringBuilder stringBuilder2 = new StringBuilder();
            int dislikedCount = 0;
            if(!unlike){
                String[] dislikedArr = new String[0];
                if(disliked != null && !disliked.equals("")){
                    dislikedArr = disliked.split(" ");
                }
                if(dislikedArr.length != 0){
                    if(!dislikedArr[0].equals(modelID)){
                        stringBuilder2.append(dislikedArr[0]);
                    }else if(dislikedArr.length > 1){
                        stringBuilder2.append(dislikedArr[1]);
                        dislikedCount++;
                    }else{
                        dislikedCount++;
                    }
                }
                for (int i = 1; i < dislikedArr.length; i++) {
                    if(!dislikedArr[i].equals(modelID)){
                        stringBuilder2.append(" ");
                        stringBuilder2.append(dislikedArr[i]);
                    }else{
                        dislikedCount++;
                    }
                }
            }
            disliked = stringBuilder2.toString();

            preparedStatement = conn.prepareStatement("UPDATE users SET liked=? WHERE email=?");
            preparedStatement.setString(1, liked);
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();
            if(!unlike){
                preparedStatement = conn.prepareStatement("UPDATE users SET disliked=? WHERE email=?");
                preparedStatement.setString(1, disliked);
                preparedStatement.setString(2, email);
                preparedStatement.executeUpdate();
            }

            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                likedCount = resultSet.getInt("liked") + likedCount;
                dislikedCount = resultSet.getInt("disliked") - dislikedCount;
            }

            preparedStatement = conn.prepareStatement("UPDATE models SET liked=? WHERE id=?");
            preparedStatement.setInt(1, likedCount);
            preparedStatement.setInt(2, Integer.parseInt(modelID));
            preparedStatement.executeUpdate();

            if(!unlike){
                preparedStatement = conn.prepareStatement("UPDATE models SET disliked=? WHERE id=?");
                preparedStatement.setInt(1, dislikedCount);
                preparedStatement.setInt(2, Integer.parseInt(modelID));
                preparedStatement.executeUpdate();
            }

            modelsChange = true;
        } catch (SQLException e) {
            System.err.println("SQL upvoteModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static void downvoteModel(String modelID, String email) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String disliked;
            String liked;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                disliked = resultSet.getString("disliked");
                liked = resultSet.getString("liked");
            }

            String[] dislikedArr = new String[0];
            if(disliked != null && !disliked.equals("")){
                dislikedArr = disliked.split(" ");
            }

            boolean undislike = false;
            StringBuilder stringBuilder = new StringBuilder();
            if(dislikedArr.length != 0){
                if(!dislikedArr[0].equals(modelID)){
                    stringBuilder.append(dislikedArr[0]);
                }else if(dislikedArr.length > 1){
                    stringBuilder.append(dislikedArr[1]);
                    undislike = true;
                }
            }
            for (int i = 1; i < dislikedArr.length; i++) {
                if(!dislikedArr[i].equals(modelID)){
                    stringBuilder.append(" ");
                    stringBuilder.append(dislikedArr[i]);
                }else{
                    undislike = true;
                }
            }
            disliked = stringBuilder.toString();
            int dislikedCount;
            if(!undislike){
                if(disliked.equals("")){
                    disliked = modelID;
                }else{
                    disliked += " " + modelID;
                }
                dislikedCount = 1;
            }else{
                dislikedCount = -1;
            }

            StringBuilder stringBuilder2 = new StringBuilder();
            int likedCount = 0;
            if(!undislike){
                String[] likedArr = new String[0];
                if(liked != null  && !liked.equals("")){
                    likedArr = liked.split(" ");
                }
                if(likedArr.length != 0){
                    if(!likedArr[0].equals(modelID)){
                        stringBuilder2.append(likedArr[0]);
                    }else if(likedArr.length > 1){
                        stringBuilder2.append(likedArr[1]);
                        likedCount++;
                    }else{
                        likedCount++;
                    }
                }
                for (int i = 1; i < likedArr.length; i++) {
                    if(!likedArr[i].equals(modelID)){
                        stringBuilder2.append(" ");
                        stringBuilder2.append(likedArr[i]);
                    }else{
                        likedCount++;
                    }
                }
            }

            liked = stringBuilder2.toString();

            preparedStatement = conn.prepareStatement("UPDATE users SET disliked=? WHERE email=?");
            preparedStatement.setString(1, disliked);
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();
            if(!undislike){
                preparedStatement = conn.prepareStatement("UPDATE users SET liked=? WHERE email=?");
                preparedStatement.setString(1, liked);
                preparedStatement.setString(2, email);
                preparedStatement.executeUpdate();
            }

            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                dislikedCount = resultSet.getInt("disliked") + dislikedCount;
                likedCount = resultSet.getInt("liked") - likedCount;
            }

            preparedStatement = conn.prepareStatement("UPDATE models SET disliked=? WHERE id=?");
            preparedStatement.setInt(1, dislikedCount);
            preparedStatement.setInt(2, Integer.parseInt(modelID));
            preparedStatement.executeUpdate();
            if(!undislike){
                preparedStatement = conn.prepareStatement("UPDATE models SET liked=? WHERE id=?");
                preparedStatement.setInt(1, likedCount);
                preparedStatement.setInt(2, Integer.parseInt(modelID));
                preparedStatement.executeUpdate();
            }

            modelsChange = true;
        } catch (SQLException e) {
            System.err.println("SQL downvoteModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static void addToLikedModel(String modelID, String email) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT favourited FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String favourited;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                favourited = resultSet.getString("favourited");
            }

            boolean unfavourite = false;
            StringBuilder stringBuilder = new StringBuilder();

            String[] favouritedArr = new String[0];
            if(favourited != null  && !favourited.equals("")){
                favouritedArr = favourited.split(" ");
            }

            if(favouritedArr.length != 0){
                if(!favouritedArr[0].equals(modelID)){
                    stringBuilder.append(favouritedArr[0]);
                }else if(favouritedArr.length > 1){
                    stringBuilder.append(favouritedArr[1]);
                    unfavourite = true;
                }
            }
            for (int i = 1; i < favouritedArr.length; i++) {
                if(!favouritedArr[i].equals(modelID)){
                    stringBuilder.append(" ");
                    stringBuilder.append(favouritedArr[i]);
                }else{
                    unfavourite = true;
                }
            }
            favourited = stringBuilder.toString();
            if(!unfavourite){
                if(favourited.equals("")){
                    favourited = modelID;
                }else{
                    favourited += " " + modelID;
                }
            }

            preparedStatement = conn.prepareStatement("UPDATE users SET favourited=? WHERE email=?");
            preparedStatement.setString(1, favourited);
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("SQL addToLikedModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static void deleteModel(String modelID, String email) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));

            String rsEmail;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                rsEmail = resultSet.getString("email");
            }

            //die if it's not yours
            if(!rsEmail.equals(email)){
                return;
            }

            adminDeleteModel(modelID);
        } catch (SQLException e) {
            System.err.println("SQL deleteModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static void editModel(String email, String modelID, HTTPController.AddModel addModel){
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("UPDATE models SET rating=?,type=?,tags=?,thumbnail=?,email=?,name=? WHERE id=?");
            preparedStatement.setString(1, addModel.modelRating);
            preparedStatement.setString(2, addModel.modelType);
            preparedStatement.setString(3, String.join(" ", addModel.purifiedModelTags));
            preparedStatement.setString(4, addModel.thumbnailLink);
            preparedStatement.setString(5, email);
            preparedStatement.setString(6, addModel.modelName);
            preparedStatement.setInt(7, Integer.parseInt(modelID));
            preparedStatement.executeUpdate();

            preparedStatement = conn.prepareStatement("SELECT descriptionFileID FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));

            long descriptionFileID;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                descriptionFileID = resultSet.getLong("descriptionFileID");
            }

            File descriptionFile = new File("./modelDescriptions/" + descriptionFileID);
            Files.writeString(descriptionFile.toPath(), addModel.description);

            modelsChange = true;
        } catch (SQLException | IOException e) {
            System.err.println("SQL editModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static boolean adminIsLogged(String key1, String key2) {
        PreparedStatement preparedStatement = null;
        boolean out;
        try {
            preparedStatement = conn.prepareStatement("SELECT key2 FROM admin WHERE key1=?");
            preparedStatement.setString(1, key1);

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return false;
                }
                out = resultSet.getString("key2").equals(key2);
            }

        } catch (SQLException e) {
            System.err.println("SQL adminIsLogged failed: key1=" + key1 + " key2=" + key2);
            e.printStackTrace(System.err);
            out = false;
        }

        return out;
    }

    public static void adminDeleteModel(String modelID) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));

            String email;
            long descriptionFileID;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                email = resultSet.getString("email");
                descriptionFileID = resultSet.getLong("descriptionFileID");
            }

            preparedStatement = conn.prepareStatement("DELETE FROM models WHERE id=?");
            preparedStatement.setInt(1, Integer.parseInt(modelID));
            preparedStatement.executeUpdate();

            preparedStatement = conn.prepareStatement("SELECT models FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String models;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                models = resultSet.getString("models");
            }

            StringBuilder stringBuilder = new StringBuilder();

            String[] modelsArr = new String[0];
            if(models != null  && !models.equals("")){
                modelsArr = models.split(" ");
            }

            if(modelsArr.length != 0){
                if(!modelsArr[0].equals(modelID)){
                    stringBuilder.append(modelsArr[0]);
                }else if(modelsArr.length > 1){
                    stringBuilder.append(modelsArr[1]);
                }
            }
            for (int i = 1; i < modelsArr.length; i++) {
                if(!modelsArr[i].equals(modelID)){
                    stringBuilder.append(" ");
                    stringBuilder.append(modelsArr[i]);
                }
            }
            models = stringBuilder.toString();

            preparedStatement = conn.prepareStatement("UPDATE users SET models=? WHERE email=?");
            preparedStatement.setString(1, models);
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();

            Files.deleteIfExists(Path.of("./modelDescriptions/" + descriptionFileID));

            modelsChange = true;
        } catch (SQLException | IOException e) {
            System.err.println("SQL adminDeleteModel failed: modelID=" + modelID);
            e.printStackTrace(System.err);
        }
    }

    public static void adminDeleteAccount(String email) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT models FROM users WHERE email=?");
            preparedStatement.setString(1, email);

            String models;
            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return;
                }
                models = resultSet.getString("models");
            }

            String[] modelArr = new String[0];
            if(models != null  && !models.equals("")){
                modelArr = models.split(" ");
            }

            for (String s : modelArr) {
                adminDeleteModel(s);
            }

            preparedStatement = conn.prepareStatement("DELETE FROM users WHERE email=?");
            preparedStatement.setString(1, email);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            System.err.println("SQL adminDeleteAccount failed: email=" + email);
            e.printStackTrace(System.err);
        }
    }

    public static class Models{
        String name;
        int id;
        String rating;
        String type;
        String tags;
        String thumbnail;
        long date;
        int liked;
        int disliked;

        public Models(int id, String name, String rating, String type, String tags, String thumbnail, long date, int liked, int disliked) {
            this.id = id;
            this.name = name;
            this.rating = rating;
            this.type = type;
            this.tags = tags;
            this.thumbnail = thumbnail;
            this.date = date;
            this.liked = liked;
            this.disliked = disliked;
        }
    }
    public static String getModels() {
        if(modelsChange){
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = conn.prepareStatement("SELECT * FROM models");
                LinkedList<Models> modelsLinkedList = new LinkedList<>();

                synchronized (conn){
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()){
                        modelsLinkedList.add(new Models(
                                resultSet.getInt("id"),
                                resultSet.getString("name"),
                                resultSet.getString("rating"),
                                resultSet.getString("type"),
                                resultSet.getString("tags"),
                                resultSet.getString("thumbnail"),
                                resultSet.getLong("date"),
                                resultSet.getInt("liked"),
                                resultSet.getInt("disliked")
                        ));
                    }
                }

                allModelsCached = gson.toJson(modelsLinkedList);
                modelsChange = false;
            }catch (SQLException e) {
                System.err.println("SQL getModels failed");
                e.printStackTrace(System.err);
            }
        }

        return allModelsCached;
    }

    public static class Model{
        boolean upvoted;
        boolean downvoted;
        boolean favourited;
        boolean owned;
        String descriptionHTML;
        String name;
        String rating;
        String type;
        String tags;
        String thumbnail;
    }
    public static String getModel(int modelID, String email) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement("SELECT * FROM models WHERE id=?");
            preparedStatement.setInt(1, modelID);
            Model out = new Model();

            synchronized (conn){
                ResultSet resultSet = preparedStatement.executeQuery();
                if(!resultSet.next()){
                    return "modelID not found";
                }
                out.descriptionHTML = Files.readString(Path.of("./modelDescriptions/" + resultSet.getLong("descriptionFileID")));
                out.owned = resultSet.getString("email").equals(email);
                out.name = resultSet.getString("name");
                out.rating = resultSet.getString("rating");
                out.type = resultSet.getString("type");
                out.tags = resultSet.getString("tags");
                out.thumbnail = resultSet.getString("thumbnail");
            }

            if(email.equals("null")){
                out.upvoted = false;
                out.downvoted = false;
                out.favourited = false;
            }else{
                preparedStatement = conn.prepareStatement("SELECT * FROM users WHERE email=?");
                preparedStatement.setString(1, email);

                String liked = null;
                String disliked = null;
                String favourited = null;

                synchronized (conn){
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if(resultSet.next()){
                        liked = resultSet.getString("liked");
                        disliked = resultSet.getString("disliked");
                        favourited = resultSet.getString("favourited");
                    }
                }

                if(liked != null){
                    out.upvoted = liked.contains(String.valueOf(modelID));
                }
                if(disliked != null){
                    out.downvoted = disliked.contains(String.valueOf(modelID));
                }
                if(favourited != null){
                    out.favourited = favourited.contains(String.valueOf(modelID));
                }
            }
            return gson.toJson(out);
        } catch (SQLException | IOException e) {
            System.err.println("SQL getModel failed: email=" + email + " modelID=" + modelID);
            e.printStackTrace(System.err);
        }
        return "Server error";
    }

    //TODO autodelete old and duplicit rows from unconfirmed users
    //TODO liked/disliked/favourite fragments?
}
