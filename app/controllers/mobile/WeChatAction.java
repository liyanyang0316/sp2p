package controllers.mobile;

import business.User;
import constants.Constants;
import controllers.BaseController;
import net.sf.json.JSONObject;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Scope;
import utils.ErrorInfo;
import utils.WebChartUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by libaozhong on 2015/6/4.
 */
public class WeChatAction extends BaseController {

    public static void authentication() throws IOException {
       play.mvc.Http.Response.current().setHeader("contentType", "text/html; charset=utf-8");

       String result = "";
       /** 判断是否是微信接入激活验证，只有首次接入验证时才会收到echostr参数，此时需要把它直接返回 */
       Http.Request reuqets = Http.Request.current();
       String echostr =reuqets.params.get("echostr");
       if (echostr != null && echostr.length() > 1) {
           result = echostr;
       }

       try {
           OutputStream os = Http.Response.current().out;
           os.write(result.getBytes("UTF-8"));
           os.flush();
           os.close();
       } catch (Exception e) {
           e.printStackTrace();
       }
   }



    /**
     * 微信回调
     * @throws IOException
     */
    public static void weChatCB() throws IOException {
        Http.Response.current().setContentTypeIfNotSet("text/html; charset=utf-8");
        Logger.info("用户进入：");

        String code= params.get("code");
        String status= params.get("state");
        String mobile= params.get("mobile");
        Logger.info("code为："+code+"status:"+status);
        String openId = WebChartUtil.getOpenIdAuth(code);
        Logger.info("处理微信openid为："+openId+"code:"+code+"status:"+status+"mobile:"+mobile);

        if (openId == null) {//请求过期失效
            renderTemplate("mobile/WeChatAction/weChatFailTip.html");
        }

        JSONObject paramsJson = new JSONObject();
        paramsJson.put("openId", openId);
        paramsJson.put("status", status);
        Logger.info(">>  weChatCB  openid:" + openId + "status:" + status);

        ErrorInfo error = new ErrorInfo();
        User user = new User();
        String name = user.findBySocialToFp(WebChartUtil.WECHAT, openId, error);
        Logger.info("查询结果：name"+name);

        if(status.equals(Constants.WEIXINSTATUS.LOGIN)){
            Logger.info("登录openid:"+openId+"status:"+status);
            weChatLogin(user, name, openId, error);
         }else if(status.equals(Constants.WEIXINSTATUS.REGISTER)){
            Logger.info("openid:"+openId+"status:"+status);
            weChatRegister(user, name, openId, error);
        }else if(status.equals(Constants.WEIXINSTATUS.QUICKREGISTERSUCCESS)){
            Logger.info("快速注册openid:"+openId+"status:"+status+"name:"+name);
         webChartQuickRegister(user,name, openId,mobile);
        }else if(status.equals(Constants.WEIXINSTATUS.MOBILEHADREGISTER)){
            Logger.info("openid:"+openId+"status:"+status);
            QuickRegister.registerSuccess(openId,status);
        }else{
            //TODO
            weChatLogin(user, name, openId, error);

        }
    }

    private static void weChatRegister(User user, String name, String openId, ErrorInfo error) {
        JSONObject jsonOne = new JSONObject();
        jsonOne.put("openId",openId);
        jsonOne.put("name",name);
        renderTemplate("mobile/LoginAction/register.html", jsonOne);
    }

    private static void  webChartQuickRegister(User user, String name, String openId,String mobile){
        String fpHots= Constants.FP_HOST;
        JSONObject jsonOne = new JSONObject();
        jsonOne.put("mobile",mobile);
        jsonOne.put("openId",openId);
        jsonOne.put("name",name);
        renderTemplate("mobile/QuickRegister/quickRegister.html", jsonOne,fpHots);
    }

    private static void weChatLogin(User user, String name,String openId, ErrorInfo error){
               Logger.info("weChatLogin:openid"+openId);
        if (name == null) {
            Logger.info("weChatLogin:name为空");
            renderTemplate("mobile/LoginAction/login.html", openId);
        }
        user.name = name;
        if (user.id < 0) {
            error.code = -1;
            error.msg = "该用户名不存在";
            renderTemplate("mobile/LoginAction/register.html", openId);
        }

        user.loginCommon(error);
        if (error.code < 0) {
            renderTemplate("mobile/LoginAction/login.html", openId);
        }

        MainContent.property();
    }
    public static void landding(){
        render();
    }

}
