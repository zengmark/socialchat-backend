package com.socialchat.email;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @author 清闲
 * @description: 发送邮箱业务
 */
@Component
public class EmailHelper {
 
    @Resource
    private JavaMailSenderImpl mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;
 
    public boolean sendEmail(String email, String verifyCode) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        //设置一个html邮件信息
        helper.setText("<p style='color: blue'>你的验证码为：" + verifyCode + "(有效期为五分钟)</p>", true);
        //设置邮件主题名
        helper.setSubject("socialchat平台验证码----验证码");
        //发给谁-》邮箱地址
        helper.setTo(email);
        //谁发的-》发送人邮箱
        helper.setFrom(emailFrom);
        //将邮箱验证码以邮件地址为key存入redis,1分钟过期
        mailSender.send(mimeMessage);
        return true;
    }
}
