package com.lsx.community.controller;
import com.google.code.kaptcha.Producer;
import com.lsx.community.entity.User;
import com.lsx.community.service.UserService;
import com.lsx.community.util.CommunityConstant;
import com.lsx.community.util.CommunityUtil;
import com.lsx.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService ;

    @Autowired
    private Producer kaptchaProducer ;

    @Value("${server.servlet.context-path}")
    private String contextPath ;

    @Autowired
    private RedisTemplate redisTemplate ;

    @RequestMapping(value = "/register" ,method = RequestMethod.GET)
    public String getRegisterPage(){
        return "site/register" ;
    }

    @RequestMapping(value = "/login" ,method = RequestMethod.GET)
    public String getLoginPage(){
        return "site/login" ;
    }

    @RequestMapping(value = "/register",method = RequestMethod.POST)
    public String register (Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map==null||map.isEmpty()){
            model.addAttribute("msg","注册成功,我们已经向您的邮箱发送一封激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "site/operate-result" ;
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "site/register" ;
        }

    }

    @RequestMapping(value = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId ,@PathVariable("code") String code){
         int result = userService.activation(userId,code);
         if(result== CommunityConstant.ACTIVATION_SUCCESS){
             model.addAttribute("msg","激活成功，您的账号已经可以正常使用了");
             model.addAttribute("target","/login");
         }else if(result==CommunityConstant.ACTIVATION_REPEAT){
             model.addAttribute("msg","无效的操作，该账号已经激活成功");
             model.addAttribute("target","/index");
         }else{
             model.addAttribute("msg","激活失败，您提供的激活码不正确");
             model.addAttribute("target","/index");
         }
         return "site/operate-result" ;
    }

    @RequestMapping(value = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha (HttpServletResponse response, HttpSession httpSession){
          //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        /*//将验证码存入Session
        httpSession.setAttribute("kaptcha",text);*/

        //验证码的归属
        String kapthaOwner = CommunityUtil.generateUUID() ;
        Cookie cookie = new Cookie("kaptchaOwner",kapthaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKeptchaKey(kapthaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);
        //将图片输出到浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败："+e.getMessage());
        }


    }

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public String login (String username,String password,String code,boolean rememberMe,
                         Model model/*,HttpSession session*/,
                         HttpServletResponse response,@CookieValue("kaptchaOwner") String kaptchaOwner) {
              /* String kaptcha = (String) session.getAttribute("kaptcha");*/

                String keptchaKey = RedisKeyUtil.getKeptchaKey(kaptchaOwner);
                String kaptcha = null ;
                if(!StringUtils.isBlank(keptchaKey)){
                     kaptcha = (String) redisTemplate.opsForValue().get(keptchaKey);
                }

               //检查验证码
               if(StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code)){
                   model.addAttribute("codeMsg","验证码不正确");
                   return "site/login" ;
               }

               //检查账号,密码
               int expiredSeconds = rememberMe ? CommunityConstant.REMEMBER_EXPIRED_SECONDS:CommunityConstant.DEFAULT_EXPIRED_SECONDS ;
               Map<String, Object> map = userService.login(username, password, expiredSeconds);
               if(map.containsKey("ticket")){
                   Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
                   cookie.setPath(contextPath);
                   cookie.setMaxAge(expiredSeconds);
                   response.addCookie(cookie);
                   return "redirect:/index" ;
               }else{
                   model.addAttribute("usernameMsg",map.get("usernameMsg"));
                   model.addAttribute("passwordMsg",map.get("passwordMsg"));
                   return "site/login" ;
               }



    }

    @RequestMapping(value = "logout",method = RequestMethod.GET)
    public String logout (@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login" ;
    }


}
