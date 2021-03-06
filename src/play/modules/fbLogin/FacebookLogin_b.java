package fbLogin;

//import models.*;
import java.util.*;
import play.mvc.*;
import play.libs.*;
import play.libs.WS;
import play.Play;
import play.Logger;
import play.libs.Codec;
//import play.libs.Crypto;
import org.apache.commons.lang3.StringUtils;
import com.google.gson.*;
//import org.apache.commons.io.IOUtils;




public class FacebookLogin {

  public static void authorize(String state){
    //IMPORTANT: "state" is double UTF-8 encoded 
    //WARN: have to use &state as parameter name (check oauth api for more ...)
    //Note: "state" is the url that the user is currently on, and the page that will finaly redirect to
    String url = StringUtils.EMPTY;
    if(state == null || !StringUtils.isNotBlank(state) || state.equals("null")){
      url = getFacebookCode(); 
    }else{
      url = getFacebookCode() + "&state=" + state;
    }
    redirect(url);
  }

  private static JsonParser parser = new JsonParser(); 

  // --v--        Facebook        --v-- //
  //[Facebook athentication step.1] get facebook code
  public static String getFacebookCode(){
    String url = "https://www.facebook.com/dialog/oauth?";
    url += "client_id=" + Play.configuration.getProperty("facebookLogin.appId");
    url += "&redirect_uri=" + Router.getFullUrl(Play.configuration.getProperty("facebookLogin.redirect_uri"));
    url += "&scope=" + Play.configuration.getProperty("facebookLogin.scope");//"email,user_birthday,user_about_me,user_location";
    return url;  
  }
  //[Facebook athentication step.2] get AccessToken from 3rd party
  //for facebook
  public static String getFacebookAccessToken(String code){
    String result = StringUtils.EMPTY;
    try{
      String url = "https://graph.facebook.com/oauth/access_token";
      Map params = new HashMap();
      params.put("client_id", Play.configuration.getProperty("facebookLogin.appId"));
      params.put("redirect_uri", Play.configuration.getProperty("facebookLogin.redirect_uri"));
      params.put("client_secret", Play.configuration.getProperty("facebookLogin.secret"));
      params.put("code", code);
      String callbackData = WS.url(url).params(params).post().getString();
      Logger.info("aat:" + callbackData);
      if(StringUtils.contains(callbackData,"access_token")){
        result = callbackData;
        Logger.info("access_token=" + result);
      }
    }catch(Exception e){
      Logger.error("ThirdPartyLogin.getFacebookAccessToken error! can't get access_token form facebook");
    }
    return result;
  }
  //[Facebook athentication step.3] get user info as json
  public static JsonObject getFacebookUserInfoAsJson(String token){
    JsonObject json = new JsonObject();
    try{
      Logger.info("token:" + token);
      String url = "https://graph.facebook.com/me?" + token;
      String callbackData = WS.url(url).get().getString();
      json = parser.parse(callbackData).getAsJsonObject(); 
      Logger.info("json: " + json);
    }catch(Exception e){
      json.addProperty("error", "error_getFacebookUserInfoAsJson");
      Logger.error("ThirdPartyLogin.getFacebookUserInfoAsJson error!");
    }
    return json;
  }
  //[Facebook athentication step.4] if property dosn't exist, fill in data
  public static JsonObject initFacebookUserInfo(JsonObject info){
    if(!info.has("error")){
      String[] data = {"name", "location", "about", "gender", "birthday", "locationId", "bio" };
      for(String s : data){
        if(!info.has(s)){
          Logger.info("new property filled in:" + s);
          info.addProperty(s, StringUtils.EMPTY);
        }
      }
    }
    return info;
  }

  // --v--        Utility       --v-- //
  //check after getting user info (json)
  //TODO : check Weibo json
  public static boolean success(JsonObject info){
    boolean result = false;
    try{
      if(!info.has("error") &&info.has("id") && info.has("name") && StringUtils.isNotBlank(info.get("name").getAsString()) ){
        result = true;
      }
    }catch(Exception e){
      Logger.error(e, "ThirdPartyLogin.success() = fasle (Ahtentication failed!)");
    }
    return result;
  }
  //check if jason has error message
  public static void overrideExistedUserInfo(JsonObject json, String key, String message){
    try{
      if(json.has(key)){
        json.addProperty(key, message);
      }
    }catch(Exception e){
      Logger.info(e, "Error! setErrorMessage");
    }
  }
}
